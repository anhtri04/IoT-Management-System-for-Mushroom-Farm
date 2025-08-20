import paho.mqtt.client as mqtt
import ssl
import json
import threading
import time
from datetime import datetime
from flask import current_app
from models import Device, SensorData, Command, db
from sqlalchemy.exc import SQLAlchemyError
import uuid

class MQTTService:
    """MQTT service for AWS IoT Core communication"""
    
    def __init__(self, app=None, socketio=None):
        self.app = app
        self.socketio = socketio
        self.client = None
        self.connected = False
        self.reconnect_delay = 5
        self.max_reconnect_delay = 300
        self.reconnect_attempts = 0
        
        if app is not None:
            self.init_app(app, socketio)
    
    def init_app(self, app, socketio=None):
        """Initialize MQTT service"""
        self.app = app
        self.socketio = socketio
        
        # Initialize MQTT client
        self.client = mqtt.Client(client_id=f"mushroom_farm_backend_{uuid.uuid4().hex[:8]}")
        
        # Configure TLS
        try:
            self.client.tls_set(
                ca_certs=app.config['CA_PATH'],
                certfile=app.config['CERT_PATH'],
                keyfile=app.config['KEY_PATH'],
                tls_version=ssl.PROTOCOL_TLSv1_2
            )
        except Exception as e:
            app.logger.error(f"Failed to configure MQTT TLS: {e}")
            return
        
        # Set callbacks
        self.client.on_connect = self._on_connect
        self.client.on_disconnect = self._on_disconnect
        self.client.on_message = self._on_message
        self.client.on_log = self._on_log
        
        # Start connection in background thread
        self.connection_thread = threading.Thread(target=self._connect_loop, daemon=True)
        self.connection_thread.start()
    
    def _connect_loop(self):
        """Main connection loop with reconnection logic"""
        while True:
            try:
                if not self.connected:
                    self.app.logger.info("Attempting to connect to MQTT broker...")
                    self.client.connect(
                        self.app.config['MQTT_BROKER'],
                        self.app.config['MQTT_PORT'],
                        60
                    )
                    self.client.loop_forever()
                else:
                    time.sleep(1)
                    
            except Exception as e:
                self.app.logger.error(f"MQTT connection error: {e}")
                self.connected = False
                
                # Exponential backoff for reconnection
                delay = min(self.reconnect_delay * (2 ** self.reconnect_attempts), self.max_reconnect_delay)
                self.app.logger.info(f"Reconnecting in {delay} seconds...")
                time.sleep(delay)
                self.reconnect_attempts += 1
    
    def _on_connect(self, client, userdata, flags, rc):
        """Callback for successful MQTT connection"""
        if rc == 0:
            self.connected = True
            self.reconnect_attempts = 0
            self.app.logger.info("Connected to MQTT broker successfully")
            
            # Subscribe to all telemetry and status topics
            self._subscribe_to_topics()
            
            # Emit connection status via SocketIO
            if self.socketio:
                self.socketio.emit('mqtt_status', {'connected': True})
        else:
            self.app.logger.error(f"Failed to connect to MQTT broker. Return code: {rc}")
            self.connected = False
    
    def _on_disconnect(self, client, userdata, rc):
        """Callback for MQTT disconnection"""
        self.connected = False
        self.app.logger.warning(f"Disconnected from MQTT broker. Return code: {rc}")
        
        # Emit disconnection status via SocketIO
        if self.socketio:
            self.socketio.emit('mqtt_status', {'connected': False})
    
    def _on_message(self, client, userdata, msg):
        """Callback for received MQTT messages"""
        try:
            topic = msg.topic
            payload = json.loads(msg.payload.decode())
            
            self.app.logger.debug(f"Received MQTT message: {topic} -> {payload}")
            
            # Parse topic to extract farm_id, room_id, device_id
            topic_parts = topic.split('/')
            if len(topic_parts) >= 6:
                farm_id = topic_parts[1]
                room_id = topic_parts[3]
                device_id = topic_parts[5]
                message_type = topic_parts[6] if len(topic_parts) > 6 else 'unknown'
                
                # Handle different message types
                if message_type == 'telemetry':
                    self._handle_telemetry(farm_id, room_id, device_id, payload)
                elif message_type == 'status':
                    self._handle_status(farm_id, room_id, device_id, payload)
                else:
                    self.app.logger.warning(f"Unknown message type: {message_type}")
            else:
                self.app.logger.warning(f"Invalid topic format: {topic}")
                
        except json.JSONDecodeError as e:
            self.app.logger.error(f"Failed to decode MQTT message JSON: {e}")
        except Exception as e:
            self.app.logger.error(f"Error processing MQTT message: {e}")
    
    def _handle_telemetry(self, farm_id, room_id, device_id, payload):
        """Handle telemetry data from devices"""
        try:
            with self.app.app_context():
                # Verify device exists
                device = Device.query.filter_by(device_id=device_id).first()
                if not device:
                    self.app.logger.warning(f"Received telemetry from unknown device: {device_id}")
                    return
                
                # Update device last seen
                device.last_seen = datetime.utcnow()
                device.status = 'online'
                
                # Create sensor data record
                sensor_data = SensorData(
                    device_id=device_id,
                    room_id=room_id,
                    farm_id=farm_id,
                    temperature_c=payload.get('temperature_c'),
                    humidity_pct=payload.get('humidity_pct'),
                    co2_ppm=payload.get('co2_ppm'),
                    light_lux=payload.get('light_lux'),
                    substrate_moisture=payload.get('substrate_moisture'),
                    battery_v=payload.get('battery_v'),
                    recorded_at=datetime.fromisoformat(payload.get('timestamp', datetime.utcnow().isoformat()))
                )
                
                db.session.add(sensor_data)
                db.session.commit()
                
                # Emit real-time data via SocketIO
                if self.socketio:
                    self.socketio.emit('telemetry_data', {
                        'farm_id': farm_id,
                        'room_id': room_id,
                        'device_id': device_id,
                        'data': sensor_data.to_dict()
                    })
                
                # Check automation rules
                self._check_automation_rules(room_id, sensor_data)
                
                self.app.logger.debug(f"Processed telemetry from device {device_id}")
                
        except SQLAlchemyError as e:
            db.session.rollback()
            self.app.logger.error(f"Database error processing telemetry: {e}")
        except Exception as e:
            self.app.logger.error(f"Error processing telemetry: {e}")
    
    def _handle_status(self, farm_id, room_id, device_id, payload):
        """Handle status messages from devices"""
        try:
            with self.app.app_context():
                # Update device status
                device = Device.query.filter_by(device_id=device_id).first()
                if device:
                    device.last_seen = datetime.utcnow()
                    device.status = payload.get('status', 'online')
                    
                    # Update firmware version if provided
                    if 'firmware_version' in payload:
                        device.firmware_version = payload['firmware_version']
                    
                    db.session.commit()
                
                # Handle command acknowledgments
                if 'command_id' in payload:
                    command = Command.query.filter_by(command_id=payload['command_id']).first()
                    if command:
                        command.status = payload.get('ack_status', 'acked')
                        db.session.commit()
                        
                        # Emit command status update
                        if self.socketio:
                            self.socketio.emit('command_status', {
                                'command_id': str(command.command_id),
                                'status': command.status
                            })
                
                # Emit device status update
                if self.socketio:
                    self.socketio.emit('device_status', {
                        'farm_id': farm_id,
                        'room_id': room_id,
                        'device_id': device_id,
                        'status': payload
                    })
                
        except SQLAlchemyError as e:
            db.session.rollback()
            self.app.logger.error(f"Database error processing status: {e}")
        except Exception as e:
            self.app.logger.error(f"Error processing status: {e}")
    
    def _check_automation_rules(self, room_id, sensor_data):
        """Check and execute automation rules based on sensor data"""
        try:
            from models import AutomationRule
            
            # Get active automation rules for this room
            rules = AutomationRule.query.filter_by(
                room_id=room_id,
                enabled=True
            ).all()
            
            for rule in rules:
                # Get sensor value based on parameter
                sensor_value = None
                if rule.parameter == 'temperature':
                    sensor_value = sensor_data.temperature_c
                elif rule.parameter == 'humidity':
                    sensor_value = sensor_data.humidity_pct
                elif rule.parameter == 'co2':
                    sensor_value = sensor_data.co2_ppm
                elif rule.parameter == 'light':
                    sensor_value = sensor_data.light_lux
                elif rule.parameter == 'substrate_moisture':
                    sensor_value = sensor_data.substrate_moisture
                
                if sensor_value is None:
                    continue
                
                # Check rule condition
                condition_met = False
                if rule.comparator == '<':
                    condition_met = sensor_value < rule.threshold
                elif rule.comparator == '>':
                    condition_met = sensor_value > rule.threshold
                elif rule.comparator == '<=':
                    condition_met = sensor_value <= rule.threshold
                elif rule.comparator == '>=':
                    condition_met = sensor_value >= rule.threshold
                elif rule.comparator == '==':
                    condition_met = abs(sensor_value - rule.threshold) < 0.01
                
                if condition_met:
                    # Execute automation action
                    self._execute_automation_action(rule, sensor_data)
                    
        except Exception as e:
            self.app.logger.error(f"Error checking automation rules: {e}")
    
    def _execute_automation_action(self, rule, sensor_data):
        """Execute automation rule action"""
        try:
            # Parse action command
            action_command = json.loads(rule.action_command)
            
            # Send command to device
            success = self.send_command(
                rule.action_device,
                action_command.get('command'),
                action_command.get('params', {}),
                issued_by=None  # Automated command
            )
            
            if success:
                self.app.logger.info(f"Executed automation rule '{rule.name}' for device {rule.action_device}")
                
                # Create notification
                from models import Notification
                notification = Notification(
                    room_id=rule.room_id,
                    level='info',
                    message=f"Automation rule '{rule.name}' triggered: {rule.parameter} {rule.comparator} {rule.threshold}"
                )
                db.session.add(notification)
                db.session.commit()
            
        except Exception as e:
            self.app.logger.error(f"Error executing automation action: {e}")
    
    def _subscribe_to_topics(self):
        """Subscribe to all relevant MQTT topics"""
        try:
            # Subscribe to telemetry topics for all farms/rooms/devices
            self.client.subscribe("farm/+/room/+/device/+/telemetry")
            self.client.subscribe("farm/+/room/+/device/+/status")
            
            self.app.logger.info("Subscribed to MQTT topics")
            
        except Exception as e:
            self.app.logger.error(f"Error subscribing to MQTT topics: {e}")
    
    def _on_log(self, client, userdata, level, buf):
        """MQTT client logging callback"""
        self.app.logger.debug(f"MQTT Log: {buf}")
    
    def send_command(self, device_id, command, params=None, issued_by=None):
        """Send command to device via MQTT"""
        try:
            with self.app.app_context():
                # Get device info
                device = Device.query.filter_by(device_id=device_id).first()
                if not device:
                    self.app.logger.error(f"Device not found: {device_id}")
                    return False
                
                # Create command record
                command_record = Command(
                    device_id=device_id,
                    room_id=device.room_id,
                    farm_id=device.room.farm_id,
                    command=command,
                    params=params,
                    issued_by=issued_by,
                    status='pending'
                )
                
                db.session.add(command_record)
                db.session.commit()
                
                # Construct MQTT topic
                topic = f"farm/{device.room.farm_id}/room/{device.room_id}/device/{device_id}/command"
                
                # Prepare payload
                payload = {
                    'command_id': str(command_record.command_id),
                    'command': command,
                    'params': params or {},
                    'timestamp': datetime.utcnow().isoformat()
                }
                
                # Publish command
                if self.connected:
                    result = self.client.publish(topic, json.dumps(payload))
                    
                    if result.rc == mqtt.MQTT_ERR_SUCCESS:
                        command_record.status = 'sent'
                        db.session.commit()
                        
                        self.app.logger.info(f"Command sent to device {device_id}: {command}")
                        return True
                    else:
                        command_record.status = 'failed'
                        db.session.commit()
                        
                        self.app.logger.error(f"Failed to publish MQTT command. Return code: {result.rc}")
                        return False
                else:
                    command_record.status = 'failed'
                    db.session.commit()
                    
                    self.app.logger.error("MQTT client not connected")
                    return False
                    
        except SQLAlchemyError as e:
            db.session.rollback()
            self.app.logger.error(f"Database error sending command: {e}")
            return False
        except Exception as e:
            self.app.logger.error(f"Error sending command: {e}")
            return False
    
    def get_connection_status(self):
        """Get MQTT connection status"""
        return {
            'connected': self.connected,
            'broker': self.app.config.get('MQTT_BROKER'),
            'port': self.app.config.get('MQTT_PORT')
        }
    
    def disconnect(self):
        """Disconnect from MQTT broker"""
        if self.client and self.connected:
            self.client.disconnect()
            self.connected = False
            self.app.logger.info("Disconnected from MQTT broker")

# Global MQTT service instance
mqtt_service = MQTTService()