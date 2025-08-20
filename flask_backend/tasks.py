from celery import current_app as celery_app
from datetime import datetime, timedelta
import logging
from typing import Dict, Any, List

from models import (
    db, SensorData, Device, Room, Farm, 
    AutomationRule, Notification, Recommendation
)
from bedrock_service import bedrock_service
from mqtt_service import mqtt_service

logger = logging.getLogger(__name__)

@celery_app.task(bind=True, max_retries=3)
def process_sensor_data(self, device_id: str, sensor_data: Dict[str, Any]):
    """Process incoming sensor data and trigger automation rules"""
    try:
        # Get device information
        device = Device.query.get(device_id)
        if not device:
            logger.error(f"Device {device_id} not found")
            return {'status': 'error', 'message': 'Device not found'}
        
        # Create sensor data record
        sensor_record = SensorData(
            device_id=device_id,
            room_id=device.room_id,
            farm_id=device.room.farm_id,
            temperature_c=sensor_data.get('temperature_c'),
            humidity_pct=sensor_data.get('humidity_pct'),
            co2_ppm=sensor_data.get('co2_ppm'),
            light_lux=sensor_data.get('light_lux'),
            ph_level=sensor_data.get('ph_level'),
            substrate_moisture=sensor_data.get('substrate_moisture'),
            air_pressure=sensor_data.get('air_pressure'),
            battery_v=sensor_data.get('battery_v'),
            signal_strength=sensor_data.get('signal_strength'),
            recorded_at=datetime.utcnow()
        )
        
        db.session.add(sensor_record)
        db.session.commit()
        
        # Trigger automation rule checking
        check_automation_rules.delay(device.room_id, sensor_data)
        
        # Check if AI recommendations should be generated
        # (e.g., every hour or when significant changes detected)
        last_recommendation = Recommendation.query.filter_by(
            room_id=device.room_id
        ).order_by(Recommendation.created_at.desc()).first()
        
        should_generate_ai = False
        if not last_recommendation:
            should_generate_ai = True
        elif last_recommendation.created_at < datetime.utcnow() - timedelta(hours=1):
            should_generate_ai = True
        
        if should_generate_ai:
            generate_ai_recommendations.delay(device.room_id)
        
        return {
            'status': 'success',
            'sensor_data_id': str(sensor_record.id),
            'device_id': device_id
        }
        
    except Exception as exc:
        logger.error(f"Error processing sensor data: {str(exc)}")
        # Retry with exponential backoff
        raise self.retry(exc=exc, countdown=60 * (2 ** self.request.retries))

@celery_app.task(bind=True, max_retries=2)
def check_automation_rules(self, room_id: str, sensor_data: Dict[str, Any]):
    """Check and execute automation rules based on sensor data"""
    try:
        # Get active automation rules for the room
        rules = AutomationRule.query.filter_by(
            room_id=room_id,
            is_active=True
        ).all()
        
        executed_rules = []
        
        for rule in rules:
            try:
                # Parse rule conditions
                conditions = rule.conditions
                should_execute = True
                
                # Check each condition
                for condition in conditions:
                    sensor_type = condition.get('sensor_type')
                    operator = condition.get('operator')
                    threshold = condition.get('threshold')
                    
                    if sensor_type not in sensor_data:
                        continue
                    
                    current_value = sensor_data[sensor_type]
                    
                    # Evaluate condition
                    if operator == 'gt' and not (current_value > threshold):
                        should_execute = False
                        break
                    elif operator == 'lt' and not (current_value < threshold):
                        should_execute = False
                        break
                    elif operator == 'eq' and not (current_value == threshold):
                        should_execute = False
                        break
                    elif operator == 'gte' and not (current_value >= threshold):
                        should_execute = False
                        break
                    elif operator == 'lte' and not (current_value <= threshold):
                        should_execute = False
                        break
                
                if should_execute:
                    # Execute rule actions
                    actions = rule.actions
                    
                    for action in actions:
                        device_id = action.get('device_id')
                        command = action.get('command')
                        value = action.get('value')
                        
                        # Send command via MQTT
                        device = Device.query.get(device_id)
                        if device:
                            mqtt_service.send_command(
                                device.mqtt_topic,
                                {
                                    'command': command,
                                    'value': value,
                                    'timestamp': datetime.utcnow().isoformat(),
                                    'rule_id': str(rule.rule_id)
                                }
                            )
                    
                    # Update rule last executed time
                    rule.last_executed_at = datetime.utcnow()
                    executed_rules.append(str(rule.rule_id))
                    
                    # Create notification
                    send_notification.delay(
                        room_id=room_id,
                        title=f"Automation Rule Executed: {rule.name}",
                        message=f"Rule '{rule.name}' was triggered by sensor data",
                        notification_type='automation'
                    )
            
            except Exception as rule_exc:
                logger.error(f"Error executing rule {rule.rule_id}: {str(rule_exc)}")
                continue
        
        if executed_rules:
            db.session.commit()
        
        return {
            'status': 'success',
            'executed_rules': executed_rules,
            'room_id': room_id
        }
        
    except Exception as exc:
        logger.error(f"Error checking automation rules: {str(exc)}")
        raise self.retry(exc=exc, countdown=30 * (2 ** self.request.retries))

@celery_app.task(bind=True, max_retries=2)
def generate_ai_recommendations(self, room_id: str):
    """Generate AI-powered recommendations for room optimization"""
    try:
        room = Room.query.get(room_id)
        if not room:
            logger.error(f"Room {room_id} not found")
            return {'status': 'error', 'message': 'Room not found'}
        
        # Get recent sensor data (last 24 hours)
        recent_data = SensorData.query.filter(
            SensorData.room_id == room_id,
            SensorData.recorded_at >= datetime.utcnow() - timedelta(hours=24)
        ).order_by(SensorData.recorded_at.desc()).limit(100).all()
        
        if not recent_data:
            logger.warning(f"No recent sensor data for room {room_id}")
            return {'status': 'warning', 'message': 'No recent sensor data'}
        
        # Prepare data for AI analysis
        sensor_readings = []
        for data in recent_data:
            reading = {
                'timestamp': data.recorded_at.isoformat(),
                'temperature_c': data.temperature_c,
                'humidity_pct': data.humidity_pct,
                'co2_ppm': data.co2_ppm,
                'light_lux': data.light_lux,
                'substrate_moisture': data.substrate_moisture
            }
            sensor_readings.append(reading)
        
        # Generate recommendations using Bedrock
        recommendations = bedrock_service.analyze_room_conditions(
            room_id=room_id,
            mushroom_type=room.mushroom_type,
            growth_stage=room.stage,
            sensor_data=sensor_readings
        )
        
        # Save recommendations to database
        for rec in recommendations:
            recommendation = Recommendation(
                room_id=room_id,
                farm_id=room.farm_id,
                recommendation_type=rec.get('type', 'general'),
                title=rec.get('title'),
                description=rec.get('description'),
                priority=rec.get('priority', 'medium'),
                suggested_actions=rec.get('actions', []),
                confidence_score=rec.get('confidence', 0.8),
                created_at=datetime.utcnow()
            )
            db.session.add(recommendation)
        
        db.session.commit()
        
        # Send notification about new recommendations
        if recommendations:
            send_notification.delay(
                room_id=room_id,
                title="New AI Recommendations Available",
                message=f"{len(recommendations)} new recommendations generated for {room.name}",
                notification_type='ai_recommendation'
            )
        
        return {
            'status': 'success',
            'recommendations_count': len(recommendations),
            'room_id': room_id
        }
        
    except Exception as exc:
        logger.error(f"Error generating AI recommendations: {str(exc)}")
        raise self.retry(exc=exc, countdown=120 * (2 ** self.request.retries))

@celery_app.task(bind=True, max_retries=3)
def send_notification(self, room_id: str, title: str, message: str, 
                     notification_type: str = 'general', user_id: str = None):
    """Send notification to users"""
    try:
        room = Room.query.get(room_id)
        if not room:
            logger.error(f"Room {room_id} not found")
            return {'status': 'error', 'message': 'Room not found'}
        
        # Get users who should receive notifications for this room
        if user_id:
            # Send to specific user
            target_users = [user_id]
        else:
            # Send to all users with access to this room
            from models import UserRoom
            user_rooms = UserRoom.query.filter_by(room_id=room_id).all()
            target_users = [ur.user_id for ur in user_rooms]
        
        notifications_created = []
        
        for user_id in target_users:
            notification = Notification(
                user_id=user_id,
                room_id=room_id,
                farm_id=room.farm_id,
                title=title,
                message=message,
                notification_type=notification_type,
                is_read=False,
                created_at=datetime.utcnow()
            )
            db.session.add(notification)
            notifications_created.append(str(notification.notification_id))
        
        db.session.commit()
        
        # TODO: Send push notifications, emails, etc.
        # This could be extended to integrate with SNS, FCM, etc.
        
        return {
            'status': 'success',
            'notifications_created': notifications_created,
            'user_count': len(target_users)
        }
        
    except Exception as exc:
        logger.error(f"Error sending notification: {str(exc)}")
        raise self.retry(exc=exc, countdown=30 * (2 ** self.request.retries))

@celery_app.task(bind=True)
def cleanup_old_data(self, days_to_keep: int = 90):
    """Clean up old sensor data and logs"""
    try:
        cutoff_date = datetime.utcnow() - timedelta(days=days_to_keep)
        
        # Delete old sensor data
        old_sensor_data = SensorData.query.filter(
            SensorData.recorded_at < cutoff_date
        ).delete()
        
        # Delete old notifications (keep for 30 days)
        notification_cutoff = datetime.utcnow() - timedelta(days=30)
        old_notifications = Notification.query.filter(
            Notification.created_at < notification_cutoff,
            Notification.is_read == True
        ).delete()
        
        # Delete old recommendations (keep for 60 days)
        recommendation_cutoff = datetime.utcnow() - timedelta(days=60)
        old_recommendations = Recommendation.query.filter(
            Recommendation.created_at < recommendation_cutoff
        ).delete()
        
        db.session.commit()
        
        logger.info(f"Cleanup completed: {old_sensor_data} sensor records, "
                   f"{old_notifications} notifications, {old_recommendations} recommendations deleted")
        
        return {
            'status': 'success',
            'sensor_data_deleted': old_sensor_data,
            'notifications_deleted': old_notifications,
            'recommendations_deleted': old_recommendations
        }
        
    except Exception as exc:
        logger.error(f"Error during cleanup: {str(exc)}")
        raise self.retry(exc=exc, countdown=300)  # Retry after 5 minutes

@celery_app.task(bind=True, max_retries=2)
def predict_yield(self, room_id: str):
    """Predict mushroom yield based on current conditions and historical data"""
    try:
        room = Room.query.get(room_id)
        if not room:
            return {'status': 'error', 'message': 'Room not found'}
        
        # Get historical data for yield prediction
        historical_data = SensorData.query.filter(
            SensorData.room_id == room_id,
            SensorData.recorded_at >= datetime.utcnow() - timedelta(days=30)
        ).order_by(SensorData.recorded_at.desc()).all()
        
        if len(historical_data) < 100:  # Need sufficient data
            return {'status': 'warning', 'message': 'Insufficient historical data'}
        
        # Prepare data for AI prediction
        sensor_data = []
        for data in historical_data:
            sensor_data.append({
                'timestamp': data.recorded_at.isoformat(),
                'temperature_c': data.temperature_c,
                'humidity_pct': data.humidity_pct,
                'co2_ppm': data.co2_ppm,
                'light_lux': data.light_lux,
                'substrate_moisture': data.substrate_moisture
            })
        
        # Use Bedrock for yield prediction
        prediction = bedrock_service.predict_yield(
            room_id=room_id,
            mushroom_type=room.mushroom_type,
            growth_stage=room.stage,
            sensor_data=sensor_data,
            current_cycle_start=room.current_cycle_start
        )
        
        # Save prediction as a recommendation
        recommendation = Recommendation(
            room_id=room_id,
            farm_id=room.farm_id,
            recommendation_type='yield_prediction',
            title='Yield Prediction Update',
            description=f"Predicted yield: {prediction.get('estimated_yield', 'N/A')} kg",
            priority='low',
            suggested_actions=prediction.get('optimization_suggestions', []),
            confidence_score=prediction.get('confidence', 0.7),
            metadata=prediction,
            created_at=datetime.utcnow()
        )
        
        db.session.add(recommendation)
        db.session.commit()
        
        return {
            'status': 'success',
            'prediction': prediction,
            'room_id': room_id
        }
        
    except Exception as exc:
        logger.error(f"Error predicting yield: {str(exc)}")
        raise self.retry(exc=exc, countdown=180 * (2 ** self.request.retries))

# Periodic tasks (configured in celery beat)
@celery_app.task
def periodic_ai_analysis():
    """Periodic task to run AI analysis on all active rooms"""
    try:
        active_rooms = Room.query.filter_by(is_active=True).all()
        
        for room in active_rooms:
            # Generate recommendations every 4 hours
            generate_ai_recommendations.delay(room.room_id)
            
            # Predict yield once per day
            if room.stage in ['fruiting', 'harvesting']:
                predict_yield.delay(room.room_id)
        
        return {
            'status': 'success',
            'rooms_processed': len(active_rooms)
        }
        
    except Exception as exc:
        logger.error(f"Error in periodic AI analysis: {str(exc)}")
        return {'status': 'error', 'message': str(exc)}

@celery_app.task
def daily_cleanup():
    """Daily cleanup task"""
    return cleanup_old_data.delay(days_to_keep=90)