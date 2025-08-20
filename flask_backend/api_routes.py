from flask import Blueprint, request, jsonify, g, current_app
from sqlalchemy import func, desc, and_, or_
from sqlalchemy.exc import SQLAlchemyError
from datetime import datetime, timedelta
import uuid
import json
import logging

# API logging utility
api_logger = logging.getLogger('api_routes')
api_logger.setLevel(logging.INFO)

# Create console handler if not exists
if not api_logger.handlers:
    console_handler = logging.StreamHandler()
    console_handler.setLevel(logging.INFO)
    formatter = logging.Formatter('[%(name)s] %(levelname)s: %(message)s')
    console_handler.setFormatter(formatter)
    api_logger.addHandler(console_handler)

from models import (
    db, User, Farm, Room, UserRoom, Device, SensorData, 
    Command, AutomationRule, FarmingCycle, Notification, Recommendation
)
from auth import (
    require_auth, require_role, require_room_access, require_internal_auth,
    get_user_accessible_rooms, get_user_accessible_farms, check_farm_access
)
from mqtt_service import mqtt_service
from bedrock_service import bedrock_service

# Create API blueprint
api = Blueprint('api', __name__, url_prefix='/api')

# ============================================================================
# AUTHENTICATION ENDPOINTS
# ============================================================================

@api.route('/auth/register', methods=['POST'])
def register():
    """Register a new user with Cognito"""
    try:
        data = request.get_json()
        
        # Validate required fields
        required_fields = ['email', 'password', 'full_name']
        for field in required_fields:
            if not data.get(field):
                api_logger.warning(f"Registration failed: Missing {field}")
                return jsonify({'error': f'Missing required field: {field}'}), 400
        
        email = data['email']
        password = data['password']
        full_name = data['full_name']
        role = data.get('role', 'viewer')  # Default role
        
        api_logger.info(f"Attempting to register user: {email}")
        
        # Create user in Cognito
        cognito_auth = current_app.cognito_auth
        result = cognito_auth.create_user(email, password, full_name)
        
        if result.get('success'):
            # Create user record in database
            user = User(
                cognito_sub=result['user_sub'],
                email=email,
                full_name=full_name,
                role=role
            )
            
            db.session.add(user)
            db.session.commit()
            
            api_logger.info(f"User registered successfully: {email}")
            return jsonify({
                'message': 'User registered successfully',
                'user_id': str(user.user_id),
                'email': email,
                'role': role
            }), 201
        else:
            api_logger.error(f"Cognito registration failed for {email}: {result.get('error')}")
            return jsonify({'error': result.get('error', 'Registration failed')}), 400
            
    except Exception as e:
        api_logger.error(f"Registration error: {str(e)}")
        return jsonify({'error': 'Internal server error'}), 500

@api.route('/auth/login', methods=['POST'])
def login():
    """Authenticate user with Cognito"""
    try:
        data = request.get_json()
        
        # Validate required fields
        if not data.get('email') or not data.get('password'):
            api_logger.warning("Login failed: Missing email or password")
            return jsonify({'error': 'Email and password are required'}), 400
        
        email = data['email']
        password = data['password']
        
        api_logger.info(f"Attempting login for user: {email}")
        
        # Authenticate with Cognito
        cognito_auth = current_app.cognito_auth
        result = cognito_auth.authenticate_user(email, password)
        
        # Check if authentication was successful
        if result is not None:
            api_logger.info(f"Login successful for user: {email}")
            return jsonify({
                'message': 'Login successful',
                'tokens': {
                    'AccessToken': result['AccessToken'],
                    'RefreshToken': result['RefreshToken'],
                    'IdToken': result['IdToken']
                }
            }), 200
        else:
            api_logger.warning(f"Login failed for user: {email}")
            return jsonify({'error': 'Invalid email or password'}), 401
            
    except Exception as e:
        api_logger.error(f"Login error: {str(e)}")
        return jsonify({'error': 'Internal server error'}), 500

# ============================================================================
# FARMS & ROOMS ENDPOINTS
# ============================================================================

@api.route('/farms', methods=['GET'])
@require_auth
def get_farms():
    """Get farms user owns or has access to"""
    try:
        api_logger.info(f"Fetching farms for user {g.current_user.user_id} (role: {g.current_user.role})")
        
        if g.current_user.role == 'admin':
            # Admin can see all farms
            farms = Farm.query.all()
            api_logger.info(f"Admin user - fetched {len(farms)} total farms")
        else:
            # Get farms through accessible rooms
            farms = get_user_accessible_farms(g.current_user.user_id)
            api_logger.info(f"Regular user - fetched {len(farms)} accessible farms")
        
        farm_data = [farm.to_dict() for farm in farms]
        api_logger.info(f"Returning farms data: {[f['name'] for f in farm_data]}")
        
        return jsonify({
            'status': 'success',
            'farms': farm_data
        })
        
    except Exception as e:
        api_logger.error(f"Error fetching farms: {str(e)}")
        return jsonify({'error': str(e)}), 500

@api.route('/farms', methods=['POST'])
# @require_auth  # Temporarily disabled for testing
# @require_role('manager')  # Temporarily disabled for testing
def create_farm():
    """Create new farm"""
    api_logger.info("[CREATE FARM] Starting farm creation process")
    
    try:
        data = request.get_json()
        api_logger.info(f"[CREATE FARM] Received data: {data}")
        
        if not data or not data.get('name'):
            api_logger.error("[CREATE FARM] Validation failed: Farm name is required")
            return jsonify({'error': 'Farm name is required'}), 400
        
        # Get current user from token (temporarily using test user)
        # current_user = g.current_user
        api_logger.info(f"[CREATE FARM] Using test user for farm creation")
        
        farm = Farm(
            owner_id=None,  # Temporarily set to None for testing
            name=data['name'],
            location=data.get('location')
        )
        api_logger.info(f"[CREATE FARM] Farm object created: {farm.name}")
        
        api_logger.info(f"[CREATE FARM] Adding farm to database: {farm.name} (owner: None - testing)")
        db.session.add(farm)
        api_logger.info("[CREATE FARM] Farm added to session")
        
        db.session.commit()
        api_logger.info(f"[CREATE FARM] Database commit successful - Farm ID: {farm.farm_id}")
        
        api_logger.info(f"Farm created successfully: {farm.farm_id} - {farm.name}")
        return jsonify({
            'status': 'success',
            'farm': farm.to_dict()
        }), 201
        
    except SQLAlchemyError as e:
        db.session.rollback()
        api_logger.error(f"[CREATE FARM] SQLAlchemy error: {type(e).__name__}: {str(e)}")
        api_logger.error(f"[CREATE FARM] SQLAlchemy error details: {repr(e)}")
        import traceback
        api_logger.error(f"[CREATE FARM] Full traceback: {traceback.format_exc()}")
        return jsonify({'error': f'Database error: {str(e)}'}), 500
    except Exception as e:
        api_logger.error(f"[CREATE FARM] Exception occurred: {type(e).__name__}: {str(e)}")
        api_logger.error(f"[CREATE FARM] Exception details: {repr(e)}")
        import traceback
        api_logger.error(f"[CREATE FARM] Full traceback: {traceback.format_exc()}")
        return jsonify({'error': f'Unexpected error: {str(e)}'}), 500

@api.route('/farms/<farm_id>', methods=['GET'])
@require_auth
def get_farm(farm_id):
    """Get farm details"""
    try:
        farm = Farm.query.get_or_404(farm_id)
        
        # Check access
        if g.current_user.role != 'admin' and not check_farm_access(g.current_user.user_id, farm_id):
            return jsonify({'error': 'Access denied'}), 403
        
        return jsonify({
            'status': 'success',
            'farm': farm.to_dict()
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@api.route('/farms/<farm_id>/rooms', methods=['GET'])
@require_auth
def get_farm_rooms(farm_id):
    """Get rooms in farm"""
    try:
        api_logger.info(f"Fetching rooms for farm {farm_id} by user {g.current_user.user_id}")
        
        # Check farm access
        if g.current_user.role != 'admin' and not check_farm_access(g.current_user.user_id, farm_id):
            api_logger.error(f"Access denied for user {g.current_user.user_id} to farm {farm_id}")
            return jsonify({'error': 'Access denied'}), 403
        
        if g.current_user.role == 'admin':
            # Admin can see all rooms in farm
            rooms = Room.query.filter_by(farm_id=farm_id).all()
            api_logger.info(f"Admin user - fetched {len(rooms)} total rooms in farm {farm_id}")
        else:
            # Get only accessible rooms
            accessible_room_ids = get_user_accessible_rooms(g.current_user.user_id)
            rooms = Room.query.filter(
                Room.farm_id == farm_id,
                Room.room_id.in_(accessible_room_ids)
            ).all()
            api_logger.info(f"Regular user - fetched {len(rooms)} accessible rooms in farm {farm_id}")
        
        room_data = [room.to_dict() for room in rooms]
        api_logger.info(f"Returning rooms data: {[r['name'] for r in room_data]}")
        
        return jsonify({
            'status': 'success',
            'rooms': room_data
        })
        
    except Exception as e:
        api_logger.error(f"Error fetching rooms for farm {farm_id}: {str(e)}")
        return jsonify({'error': str(e)}), 500

@api.route('/farms/<farm_id>/rooms', methods=['POST'])
@require_auth
@require_role('manager')
def create_room(farm_id):
    """Create room in farm"""
    try:
        data = request.get_json()
        api_logger.info(f"Creating room in farm {farm_id} with data: {data}")
        
        # Check farm access
        if g.current_user.role != 'admin' and not check_farm_access(g.current_user.user_id, farm_id):
            api_logger.error(f"Access denied for user {g.current_user.user_id} to farm {farm_id}")
            return jsonify({'error': 'Access denied'}), 403
        
        if not data or not data.get('name'):
            api_logger.error("Room creation failed: name is required")
            return jsonify({'error': 'Room name is required'}), 400
        
        room = Room(
            farm_id=farm_id,
            name=data['name'],
            description=data.get('description'),
            mushroom_type=data.get('mushroom_type'),
            stage=data.get('stage', 'incubation')
        )
        
        api_logger.info(f"Adding room to database: {room.name} in farm {farm_id}")
        db.session.add(room)
        db.session.flush()  # Get room_id
        
        api_logger.info(f"Room created with ID: {room.room_id}, assigning owner role")
        
        # Assign creator as room owner
        user_room = UserRoom(
            user_id=g.current_user.user_id,
            room_id=room.room_id,
            role='owner'
        )
        
        db.session.add(user_room)
        db.session.commit()
        
        api_logger.info(f"Room created successfully: {room.room_id} - {room.name} in farm {farm_id}")
        return jsonify({
            'status': 'success',
            'room': room.to_dict()
        }), 201
        
    except SQLAlchemyError as e:
        db.session.rollback()
        api_logger.error(f"Database error creating room: {str(e)}")
        return jsonify({'error': 'Database error'}), 500
    except Exception as e:
        api_logger.error(f"Unexpected error creating room: {str(e)}")
        return jsonify({'error': str(e)}), 500

@api.route('/rooms/<room_id>', methods=['GET'])
@require_auth
@require_room_access()
def get_room(room_id):
    """Get room details"""
    try:
        room = Room.query.get_or_404(room_id)
        
        return jsonify({
            'status': 'success',
            'room': room.to_dict()
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@api.route('/rooms/<room_id>', methods=['PUT'])
@require_auth
@require_room_access(required_role='operator')
def update_room(room_id):
    """Update room details"""
    try:
        room = Room.query.get_or_404(room_id)
        data = request.get_json()
        
        if 'name' in data:
            room.name = data['name']
        if 'description' in data:
            room.description = data['description']
        if 'mushroom_type' in data:
            room.mushroom_type = data['mushroom_type']
        if 'stage' in data:
            room.stage = data['stage']
        
        db.session.commit()
        
        return jsonify({
            'status': 'success',
            'room': room.to_dict()
        })
        
    except SQLAlchemyError as e:
        db.session.rollback()
        return jsonify({'error': 'Database error'}), 500
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@api.route('/rooms/<room_id>/assign', methods=['POST'])
@require_auth
@require_room_access(required_role='owner')
def assign_user_to_room(room_id):
    """Assign user to room"""
    try:
        data = request.get_json()
        
        if not data or not data.get('user_id') or not data.get('role'):
            return jsonify({'error': 'user_id and role are required'}), 400
        
        user_id = data['user_id']
        role = data['role']
        
        # Validate role
        if role not in ['owner', 'operator', 'viewer']:
            return jsonify({'error': 'Invalid role'}), 400
        
        # Check if user exists
        user = User.query.get(user_id)
        if not user:
            return jsonify({'error': 'User not found'}), 404
        
        # Check if assignment already exists
        existing = UserRoom.query.filter_by(user_id=user_id, room_id=room_id).first()
        if existing:
            existing.role = role
        else:
            user_room = UserRoom(
                user_id=user_id,
                room_id=room_id,
                role=role
            )
            db.session.add(user_room)
        
        db.session.commit()
        
        return jsonify({
            'status': 'success',
            'message': f'User assigned to room with role {role}'
        })
        
    except SQLAlchemyError as e:
        db.session.rollback()
        return jsonify({'error': 'Database error'}), 500
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# ============================================================================
# DEVICES & TELEMETRY ENDPOINTS
# ============================================================================

@api.route('/devices', methods=['POST'])
@require_auth
@require_role('operator')
def register_device():
    """Register new device"""
    try:
        data = request.get_json()
        
        required_fields = ['room_id', 'name', 'category', 'mqtt_topic']
        if not data or not all(field in data for field in required_fields):
            return jsonify({'error': 'Missing required fields'}), 400
        
        room_id = data['room_id']
        
        # Check room access
        if g.current_user.role != 'admin':
            user_room = UserRoom.query.filter_by(
                user_id=g.current_user.user_id,
                room_id=room_id
            ).first()
            
            if not user_room or user_room.role == 'viewer':
                return jsonify({'error': 'Insufficient permissions'}), 403
        
        device = Device(
            room_id=room_id,
            name=data['name'],
            device_type=data.get('device_type', 'sensor'),
            category=data['category'],
            mqtt_topic=data['mqtt_topic'],
            firmware_version=data.get('firmware_version')
        )
        
        db.session.add(device)
        db.session.commit()
        
        return jsonify({
            'status': 'success',
            'device': device.to_dict()
        }), 201
        
    except SQLAlchemyError as e:
        db.session.rollback()
        return jsonify({'error': 'Database error'}), 500
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@api.route('/rooms/<room_id>/devices', methods=['GET'])
@require_auth
@require_room_access()
def get_room_devices(room_id):
    """Get devices in room"""
    try:
        devices = Device.query.filter_by(room_id=room_id).all()
        
        return jsonify({
            'status': 'success',
            'devices': [device.to_dict() for device in devices]
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@api.route('/devices/<device_id>', methods=['GET'])
@require_auth
def get_device(device_id):
    """Get device details"""
    try:
        device = Device.query.get_or_404(device_id)
        
        # Check room access
        if g.current_user.role != 'admin':
            user_room = UserRoom.query.filter_by(
                user_id=g.current_user.user_id,
                room_id=device.room_id
            ).first()
            
            if not user_room:
                return jsonify({'error': 'Access denied'}), 403
        
        return jsonify({
            'status': 'success',
            'device': device.to_dict()
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@api.route('/rooms/<room_id>/telemetry', methods=['GET'])
@require_auth
@require_room_access()
def get_room_telemetry(room_id):
    """Get telemetry data for room"""
    try:
        # Parse query parameters
        from_time = request.args.get('from')
        to_time = request.args.get('to')
        agg = request.args.get('agg', 'minute')  # minute, hour
        limit = min(int(request.args.get('limit', 1000)), 10000)
        
        # Default time range (last 24 hours)
        if not to_time:
            to_time = datetime.utcnow()
        else:
            to_time = datetime.fromisoformat(to_time.replace('Z', '+00:00'))
        
        if not from_time:
            from_time = to_time - timedelta(hours=24)
        else:
            from_time = datetime.fromisoformat(from_time.replace('Z', '+00:00'))
        
        # Build query
        query = SensorData.query.filter(
            SensorData.room_id == room_id,
            SensorData.recorded_at >= from_time,
            SensorData.recorded_at <= to_time
        )
        
        # Apply aggregation if requested
        if agg in ['minute', 'hour']:
            # Group by time intervals
            if agg == 'minute':
                time_trunc = func.date_trunc('minute', SensorData.recorded_at)
            else:
                time_trunc = func.date_trunc('hour', SensorData.recorded_at)
            
            query = db.session.query(
                time_trunc.label('time_bucket'),
                SensorData.device_id,
                func.avg(SensorData.temperature_c).label('avg_temperature_c'),
                func.avg(SensorData.humidity_pct).label('avg_humidity_pct'),
                func.avg(SensorData.co2_ppm).label('avg_co2_ppm'),
                func.avg(SensorData.light_lux).label('avg_light_lux'),
                func.avg(SensorData.substrate_moisture).label('avg_substrate_moisture'),
                func.count().label('sample_count')
            ).filter(
                SensorData.room_id == room_id,
                SensorData.recorded_at >= from_time,
                SensorData.recorded_at <= to_time
            ).group_by(
                time_trunc,
                SensorData.device_id
            ).order_by(time_trunc.desc()).limit(limit)
            
            results = query.all()
            
            telemetry_data = []
            for row in results:
                telemetry_data.append({
                    'timestamp': row.time_bucket.isoformat(),
                    'device_id': str(row.device_id),
                    'temperature_c': float(row.avg_temperature_c) if row.avg_temperature_c else None,
                    'humidity_pct': float(row.avg_humidity_pct) if row.avg_humidity_pct else None,
                    'co2_ppm': float(row.avg_co2_ppm) if row.avg_co2_ppm else None,
                    'light_lux': float(row.avg_light_lux) if row.avg_light_lux else None,
                    'substrate_moisture': float(row.avg_substrate_moisture) if row.avg_substrate_moisture else None,
                    'sample_count': row.sample_count
                })
        else:
            # Raw data
            query = query.order_by(SensorData.recorded_at.desc()).limit(limit)
            results = query.all()
            
            telemetry_data = [reading.to_dict() for reading in results]
        
        return jsonify({
            'status': 'success',
            'data': telemetry_data,
            'count': len(telemetry_data),
            'aggregation': agg,
            'from': from_time.isoformat(),
            'to': to_time.isoformat()
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@api.route('/devices/<device_id>/latest', methods=['GET'])
@require_auth
def get_device_latest(device_id):
    """Get latest readings from device"""
    try:
        device = Device.query.get_or_404(device_id)
        
        # Check room access
        if g.current_user.role != 'admin':
            user_room = UserRoom.query.filter_by(
                user_id=g.current_user.user_id,
                room_id=device.room_id
            ).first()
            
            if not user_room:
                return jsonify({'error': 'Access denied'}), 403
        
        # Get latest sensor data
        latest_reading = SensorData.query.filter_by(
            device_id=device_id
        ).order_by(SensorData.recorded_at.desc()).first()
        
        return jsonify({
            'status': 'success',
            'device': device.to_dict(),
            'latest_reading': latest_reading.to_dict() if latest_reading else None
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# ============================================================================
# COMMANDS & CONTROLS ENDPOINTS
# ============================================================================

@api.route('/devices/<device_id>/commands', methods=['POST'])
@require_auth
def send_device_command(device_id):
    """Send command to device"""
    try:
        device = Device.query.get_or_404(device_id)
        
        # Check room access
        if g.current_user.role != 'admin':
            user_room = UserRoom.query.filter_by(
                user_id=g.current_user.user_id,
                room_id=device.room_id
            ).first()
            
            if not user_room or user_room.role == 'viewer':
                return jsonify({'error': 'Insufficient permissions'}), 403
        
        data = request.get_json()
        
        if not data or not data.get('command'):
            return jsonify({'error': 'Command is required'}), 400
        
        command = data['command']
        params = data.get('params', {})
        
        # Send command via MQTT
        success = mqtt_service.send_command(
            device_id=device_id,
            command=command,
            params=params,
            issued_by=g.current_user.user_id
        )
        
        if success:
            return jsonify({
                'status': 'success',
                'message': 'Command sent successfully'
            })
        else:
            return jsonify({'error': 'Failed to send command'}), 500
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@api.route('/rooms/<room_id>/commands', methods=['GET'])
@require_auth
@require_room_access()
def get_room_commands(room_id):
    """Get recent commands for room"""
    try:
        limit = min(int(request.args.get('limit', 100)), 1000)
        
        commands = Command.query.filter_by(
            room_id=room_id
        ).order_by(Command.issued_at.desc()).limit(limit).all()
        
        return jsonify({
            'status': 'success',
            'commands': [cmd.to_dict() for cmd in commands]
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# ============================================================================
# AUTOMATION & AI ENDPOINTS
# ============================================================================

@api.route('/rooms/<room_id>/rules', methods=['GET'])
@require_auth
@require_room_access()
def get_automation_rules(room_id):
    """Get automation rules for room"""
    try:
        rules = AutomationRule.query.filter_by(room_id=room_id).all()
        
        return jsonify({
            'status': 'success',
            'rules': [rule.to_dict() for rule in rules]
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@api.route('/rooms/<room_id>/rules', methods=['POST'])
@require_auth
@require_room_access(required_role='operator')
def create_automation_rule(room_id):
    """Create automation rule"""
    try:
        data = request.get_json()
        
        required_fields = ['name', 'parameter', 'comparator', 'threshold', 'action_device', 'action_command']
        if not data or not all(field in data for field in required_fields):
            return jsonify({'error': 'Missing required fields'}), 400
        
        # Validate comparator
        if data['comparator'] not in ['<', '>', '<=', '>=', '==']:
            return jsonify({'error': 'Invalid comparator'}), 400
        
        # Validate parameter
        valid_parameters = ['temperature', 'humidity', 'co2', 'light', 'substrate_moisture']
        if data['parameter'] not in valid_parameters:
            return jsonify({'error': 'Invalid parameter'}), 400
        
        # Validate action command is valid JSON
        try:
            json.loads(data['action_command'])
        except json.JSONDecodeError:
            return jsonify({'error': 'Invalid action_command JSON'}), 400
        
        rule = AutomationRule(
            room_id=room_id,
            name=data['name'],
            parameter=data['parameter'],
            comparator=data['comparator'],
            threshold=float(data['threshold']),
            action_device=data['action_device'],
            action_command=data['action_command'],
            enabled=data.get('enabled', True),
            created_by=g.current_user.user_id
        )
        
        db.session.add(rule)
        db.session.commit()
        
        return jsonify({
            'status': 'success',
            'rule': rule.to_dict()
        }), 201
        
    except SQLAlchemyError as e:
        db.session.rollback()
        return jsonify({'error': 'Database error'}), 500
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@api.route('/rooms/<room_id>/recommend', methods=['POST'])
@require_auth
@require_room_access(required_role='operator')
def force_ai_recommendation(room_id):
    """Force AI recommendation for room"""
    try:
        # Trigger Bedrock analysis
        recommendation = bedrock_service.analyze_room(room_id)
        
        if recommendation:
            return jsonify({
                'status': 'success',
                'recommendation': recommendation.to_dict()
            })
        else:
            return jsonify({'error': 'Failed to generate recommendation'}), 500
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@api.route('/rooms/<room_id>/recommendations', methods=['GET'])
@require_auth
@require_room_access()
def get_room_recommendations(room_id):
    """Get AI recommendations for room"""
    try:
        limit = min(int(request.args.get('limit', 50)), 500)
        
        recommendations = Recommendation.query.filter_by(
            room_id=room_id
        ).order_by(Recommendation.created_at.desc()).limit(limit).all()
        
        return jsonify({
            'status': 'success',
            'recommendations': [rec.to_dict() for rec in recommendations]
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# ============================================================================
# NOTIFICATIONS ENDPOINTS
# ============================================================================

@api.route('/notifications', methods=['GET'])
@require_auth
def get_notifications():
    """Get user notifications"""
    try:
        limit = min(int(request.args.get('limit', 100)), 1000)
        level = request.args.get('level')  # info, warning, critical
        
        # Get accessible room IDs
        if g.current_user.role == 'admin':
            # Admin sees all notifications
            query = Notification.query
        else:
            accessible_room_ids = get_user_accessible_rooms(g.current_user.user_id)
            query = Notification.query.filter(
                or_(
                    Notification.room_id.in_(accessible_room_ids),
                    Notification.room_id.is_(None)  # System-wide notifications
                )
            )
        
        if level:
            query = query.filter(Notification.level == level)
        
        notifications = query.order_by(
            Notification.created_at.desc()
        ).limit(limit).all()
        
        return jsonify({
            'status': 'success',
            'notifications': [notif.to_dict() for notif in notifications]
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@api.route('/notifications/<notification_id>/ack', methods=['POST'])
@require_auth
def acknowledge_notification(notification_id):
    """Acknowledge notification"""
    try:
        notification = Notification.query.get_or_404(notification_id)
        
        # Check access to notification
        if g.current_user.role != 'admin' and notification.room_id:
            user_room = UserRoom.query.filter_by(
                user_id=g.current_user.user_id,
                room_id=notification.room_id
            ).first()
            
            if not user_room:
                return jsonify({'error': 'Access denied'}), 403
        
        notification.acknowledged_by = g.current_user.user_id
        notification.acknowledged_at = datetime.utcnow()
        
        db.session.commit()
        
        return jsonify({
            'status': 'success',
            'message': 'Notification acknowledged'
        })
        
    except SQLAlchemyError as e:
        db.session.rollback()
        return jsonify({'error': 'Database error'}), 500
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@api.route('/sensor-history', methods=['GET'])
def get_sensor_history():
    """Get sensor history data for React dashboard compatibility"""
    try:
        # Parse query parameters
        from_time = request.args.get('from')
        to_time = request.args.get('to')
        limit = min(int(request.args.get('limit', 1000)), 10000)
        
        # Default time range (last 24 hours)
        if not to_time:
            to_time = datetime.utcnow()
        else:
            to_time = datetime.fromisoformat(to_time.replace('Z', '+00:00'))
        
        if not from_time:
            from_time = to_time - timedelta(hours=24)
        else:
            from_time = datetime.fromisoformat(from_time.replace('Z', '+00:00'))
        
        # Get sensor data from all rooms (for demo purposes)
        # In production, you might want to filter by user access
        query = SensorData.query.filter(
            SensorData.recorded_at >= from_time,
            SensorData.recorded_at <= to_time
        ).order_by(SensorData.recorded_at.desc()).limit(limit)
        
        results = query.all()
        
        # Format data for React dashboard
        sensor_data = []
        for reading in results:
            sensor_data.append({
                'timestamp': reading.recorded_at.isoformat(),
                'temperature': reading.temperature_c,
                'humidity': reading.humidity_pct,
                'light': reading.light_lux,
                'co2': reading.co2_ppm,
                'device_id': str(reading.device_id),
                'room_id': str(reading.room_id)
            })
        
        return jsonify({
            'success': True,
            'data': sensor_data,
            'count': len(sensor_data),
            'from': from_time.isoformat(),
            'to': to_time.isoformat()
        })
        
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e),
            'data': []
        }), 500

@api.route('/control', methods=['POST'])
def device_control():
    """Device control endpoint for React dashboard compatibility"""
    try:
        data = request.get_json()
        command = data.get('command')
        
        if not command:
            return jsonify({'error': 'Command is required'}), 400
        
        # For demo purposes, just return success
        # In production, this would send MQTT commands
        print(f"Received command: {command}")
        
        return jsonify({
            'success': True,
            'message': f'Command {command} sent successfully'
        })
        
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@api.route('/device_states', methods=['GET'])
def get_device_states():
    """Get current device states for React dashboard compatibility"""
    try:
        # Return demo device states
        # In production, this would query actual device states
        device_states = {
            'humidifier1': {'state': 'off', 'level': 0, 'type': 'humidifier', 'name': 'Main Humidifier'},
            'ventilation1': {'state': 'off', 'speed': 0, 'type': 'ventilation', 'name': 'Air Circulation'},
            'irrigation1': {'state': 'off', 'flow': 0, 'type': 'irrigation', 'name': 'Watering System'},
            'co2_control1': {'state': 'off', 'level': 0, 'type': 'co2_control', 'name': 'CO2 Injection'},
            'substrate_mixer1': {'state': 'off', 'speed': 0, 'type': 'substrate_mixer', 'name': 'Substrate Mixer'}
        }
        
        return jsonify(device_states)
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# ============================================================================
# INTERNAL ENDPOINTS (for Lambda/IoT ingestion)
# ============================================================================

@api.route('/internal/ingest', methods=['POST'])
@require_internal_auth
def internal_ingest():
    """Internal endpoint for IoT data ingestion from Lambda"""
    try:
        data = request.get_json()
        
        if not data or 'type' not in data:
            return jsonify({'error': 'Invalid payload'}), 400
        
        if data['type'] == 'telemetry':
            # Process telemetry data
            sensor_data = SensorData(
                device_id=data['device_id'],
                room_id=data['room_id'],
                farm_id=data['farm_id'],
                temperature_c=data.get('temperature_c'),
                humidity_pct=data.get('humidity_pct'),
                co2_ppm=data.get('co2_ppm'),
                light_lux=data.get('light_lux'),
                substrate_moisture=data.get('substrate_moisture'),
                battery_v=data.get('battery_v'),
                recorded_at=datetime.fromisoformat(data.get('timestamp', datetime.utcnow().isoformat()))
            )
            
            db.session.add(sensor_data)
            db.session.commit()
            
            # Emit real-time update
            if hasattr(current_app, 'socketio'):
                current_app.socketio.emit('telemetry_data', {
                    'farm_id': data['farm_id'],
                    'room_id': data['room_id'],
                    'device_id': data['device_id'],
                    'data': sensor_data.to_dict()
                })
        
        elif data['type'] == 'notification':
            # Create notification
            notification = Notification(
                farm_id=data.get('farm_id'),
                room_id=data.get('room_id'),
                device_id=data.get('device_id'),
                level=data.get('level', 'info'),
                message=data['message']
            )
            
            db.session.add(notification)
            db.session.commit()
            
            # Emit notification
            if hasattr(current_app, 'socketio'):
                current_app.socketio.emit('notification', notification.to_dict())
        
        return jsonify({'status': 'success'})
        
    except SQLAlchemyError as e:
        db.session.rollback()
        return jsonify({'error': 'Database error'}), 500
    except Exception as e:
        return jsonify({'error': str(e)}), 500