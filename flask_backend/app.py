from flask import Flask, request, jsonify, g
from flask_sqlalchemy import SQLAlchemy
from flask_migrate import Migrate
from flask_cors import CORS
from flask_jwt_extended import JWTManager
from flask_socketio import SocketIO, emit
from flasgger import Swagger
import os
from datetime import datetime
import logging
from logging.handlers import RotatingFileHandler

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(name)s] %(levelname)s: %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)

# App logger
app_logger = logging.getLogger('app')
db_logger = logging.getLogger('database')

# Import configuration
from config import Config, DevelopmentConfig, ProductionConfig, TestingConfig

# Import models and services
from models import db, User, Farm, Room, Device, SensorData, Notification
from auth import CognitoAuth
from mqtt_service import mqtt_service
from bedrock_service import bedrock_service

# Import API routes
from api_routes import api

def create_app(config_class=None):
    """Application factory pattern"""
    app = Flask(__name__)
    
    # Load configuration
    if config_class is None:
        env = os.environ.get('FLASK_ENV', 'development')
        if env == 'production':
            config_class = ProductionConfig
        elif env == 'testing':
            config_class = TestingConfig
        else:
            config_class = DevelopmentConfig
    
    app.config.from_object(config_class)
    
    # Initialize extensions
    db.init_app(app)
    migrate = Migrate(app, db)
    db_logger.info("Database and migration initialized successfully")
    
    # Initialize CORS
    CORS(app, 
         origins=app.config['CORS_ORIGINS'],
         supports_credentials=True,
         allow_headers=['Content-Type', 'Authorization'],
         methods=['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'])
    
    # Initialize JWT
    jwt = JWTManager(app)
    
    # Initialize SocketIO
    socketio = SocketIO(app, 
                       cors_allowed_origins=app.config['CORS_ORIGINS'],
                       async_mode='threading',
                       logger=app.config['DEBUG'],
                       engineio_logger=app.config['DEBUG'])
    
    # Store socketio instance for access from other modules
    app.socketio = socketio
    
    # Initialize Swagger for API documentation
    swagger_config = {
        "headers": [],
        "specs": [
            {
                "endpoint": 'apispec',
                "route": '/apispec.json',
                "rule_filter": lambda rule: True,
                "model_filter": lambda tag: True,
            }
        ],
        "static_url_path": "/flasgger_static",
        "swagger_ui": True,
        "specs_route": "/docs/"
    }
    
    swagger_template = {
        "swagger": "2.0",
        "info": {
            "title": "IoT Smart Farm API",
            "description": "REST API for IoT Smart Mushroom Farm Management System",
            "version": "1.0.0",
            "contact": {
                "name": "Smart Farm Team",
                "email": "support@smartfarm.com"
            }
        },
        "host": app.config.get('API_HOST', 'localhost:5000'),
        "basePath": "/",
        "schemes": ["http", "https"],
        "securityDefinitions": {
            "Bearer": {
                "type": "apiKey",
                "name": "Authorization",
                "in": "header",
                "description": "JWT Authorization header using the Bearer scheme. Example: 'Authorization: Bearer {token}'"
            }
        }
    }
    
    swagger = Swagger(app, config=swagger_config, template=swagger_template)
    
    # Initialize Cognito Auth
    cognito_auth = CognitoAuth(app)
    app.cognito_auth = cognito_auth
    
    # Configure logging
    if not app.debug and not app.testing:
        if not os.path.exists('logs'):
            os.mkdir('logs')
        
        file_handler = RotatingFileHandler(
            'logs/smartfarm.log', 
            maxBytes=10240000, 
            backupCount=10
        )
        file_handler.setFormatter(logging.Formatter(
            '%(asctime)s %(levelname)s: %(message)s [in %(pathname)s:%(lineno)d]'
        ))
        file_handler.setLevel(logging.INFO)
        app.logger.addHandler(file_handler)
        
        app.logger.setLevel(logging.INFO)
        app.logger.info('Smart Farm startup')
    
    # Register blueprints
    app.register_blueprint(api)
    
    # Health check endpoint
    @app.route('/health')
    def health_check():
        """Health check endpoint"""
        return jsonify({
            'status': 'healthy',
            'timestamp': datetime.utcnow().isoformat(),
            'version': '1.0.0',
            'environment': app.config['ENV']
        })
    
    # Root endpoint
    @app.route('/')
    def index():
        """Root endpoint with API information"""
        return jsonify({
            'message': 'IoT Smart Farm API',
            'version': '1.0.0',
            'documentation': '/docs/',
            'health': '/health',
            'api_base': '/api'
        })
    
    # Error handlers
    @app.errorhandler(400)
    def bad_request(error):
        return jsonify({'error': 'Bad request'}), 400
    
    @app.errorhandler(401)
    def unauthorized(error):
        return jsonify({'error': 'Unauthorized'}), 401
    
    @app.errorhandler(403)
    def forbidden(error):
        return jsonify({'error': 'Forbidden'}), 403
    
    @app.errorhandler(404)
    def not_found(error):
        return jsonify({'error': 'Not found'}), 404
    
    @app.errorhandler(500)
    def internal_error(error):
        db.session.rollback()
        app.logger.error(f'Server Error: {error}')
        return jsonify({'error': 'Internal server error'}), 500
    
    # JWT error handlers
    @jwt.expired_token_loader
    def expired_token_callback(jwt_header, jwt_payload):
        return jsonify({'error': 'Token has expired'}), 401
    
    @jwt.invalid_token_loader
    def invalid_token_callback(error):
        return jsonify({'error': 'Invalid token'}), 401
    
    @jwt.unauthorized_loader
    def missing_token_callback(error):
        return jsonify({'error': 'Authorization token is required'}), 401
    
    # SocketIO event handlers
    @socketio.on('connect')
    def handle_connect(auth):
        """Handle client connection"""
        try:
            # Optionally validate auth token here
            app.logger.info(f'Client connected: {request.sid}')
            emit('connected', {'status': 'Connected to Smart Farm'})
        except Exception as e:
            app.logger.error(f'Connection error: {str(e)}')
            return False
    
    @socketio.on('disconnect')
    def handle_disconnect():
        """Handle client disconnection"""
        app.logger.info(f'Client disconnected: {request.sid}')
    
    @socketio.on('join_farm')
    def handle_join_farm(data):
        """Join farm room for real-time updates"""
        try:
            farm_id = data.get('farm_id')
            if farm_id:
                # TODO: Validate user access to farm
                socketio.join_room(f'farm_{farm_id}')
                emit('joined_farm', {'farm_id': farm_id})
                app.logger.info(f'Client {request.sid} joined farm {farm_id}')
        except Exception as e:
            app.logger.error(f'Error joining farm: {str(e)}')
            emit('error', {'message': 'Failed to join farm'})
    
    @socketio.on('leave_farm')
    def handle_leave_farm(data):
        """Leave farm room"""
        try:
            farm_id = data.get('farm_id')
            if farm_id:
                socketio.leave_room(f'farm_{farm_id}')
                emit('left_farm', {'farm_id': farm_id})
                app.logger.info(f'Client {request.sid} left farm {farm_id}')
        except Exception as e:
            app.logger.error(f'Error leaving farm: {str(e)}')
    
    @socketio.on('join_room')
    def handle_join_room(data):
        """Join room for real-time updates"""
        try:
            room_id = data.get('room_id')
            if room_id:
                # TODO: Validate user access to room
                socketio.join_room(f'room_{room_id}')
                emit('joined_room', {'room_id': room_id})
                app.logger.info(f'Client {request.sid} joined room {room_id}')
        except Exception as e:
            app.logger.error(f'Error joining room: {str(e)}')
            emit('error', {'message': 'Failed to join room'})
    
    @socketio.on('leave_room')
    def handle_leave_room(data):
        """Leave room"""
        try:
            room_id = data.get('room_id')
            if room_id:
                socketio.leave_room(f'room_{room_id}')
                emit('left_room', {'room_id': room_id})
                app.logger.info(f'Client {request.sid} left room {room_id}')
        except Exception as e:
            app.logger.error(f'Error leaving room: {str(e)}')
    
    # Initialize services with app context
    with app.app_context():
        try:
            # Initialize MQTT service
            mqtt_service.init_app(app, socketio)
            app.logger.info('MQTT service initialized')
            
            # Create database tables
            db_logger.info("Creating database tables...")
            db.create_all()
            db_logger.info('Database tables created successfully')
            
            # Check if tables exist and log their status
            from models import Farm, Room, User
            
            farm_count = Farm.query.count()
            room_count = Room.query.count()
            user_count = User.query.count()
            
            db_logger.info(f"Database status - Farms: {farm_count}, Rooms: {room_count}, Users: {user_count}")
            
        except Exception as e:
            db_logger.error(f'Error initializing services: {str(e)}')
            app.logger.error(f'Error initializing services: {str(e)}')
    
    # CLI commands for database management
    @app.cli.command()
    def init_db():
        """Initialize the database"""
        db.create_all()
        print('Database initialized.')
    
    @app.cli.command()
    def reset_db():
        """Reset the database"""
        db.drop_all()
        db.create_all()
        print('Database reset.')
    
    @app.cli.command()
    def seed_db():
        """Seed the database with sample data"""
        from datetime import datetime, timedelta
        import uuid
        
        try:
            # Create sample farm
            farm = Farm(
                owner_id='admin-user-id',
                name='Demo Mushroom Farm',
                location='Demo Location'
            )
            db.session.add(farm)
            db.session.flush()
            
            # Create sample room
            room = Room(
                farm_id=farm.farm_id,
                name='Oyster Mushroom Room 1',
                description='Primary oyster mushroom growing room',
                mushroom_type='oyster',
                stage='fruiting'
            )
            db.session.add(room)
            db.session.flush()
            
            # Create sample devices
            devices = [
                Device(
                    room_id=room.room_id,
                    name='Temperature/Humidity Sensor',
                    device_type='sensor',
                    category='environmental',
                    mqtt_topic=f'farm/{farm.farm_id}/room/{room.room_id}/sensor/env'
                ),
                Device(
                    room_id=room.room_id,
                    name='CO2 Sensor',
                    device_type='sensor',
                    category='air_quality',
                    mqtt_topic=f'farm/{farm.farm_id}/room/{room.room_id}/sensor/co2'
                ),
                Device(
                    room_id=room.room_id,
                    name='Humidifier',
                    device_type='actuator',
                    category='climate_control',
                    mqtt_topic=f'farm/{farm.farm_id}/room/{room.room_id}/actuator/humidifier'
                )
            ]
            
            for device in devices:
                db.session.add(device)
            
            db.session.flush()
            
            # Create sample sensor data
            base_time = datetime.utcnow() - timedelta(hours=24)
            
            for i in range(144):  # 24 hours of 10-minute intervals
                timestamp = base_time + timedelta(minutes=i * 10)
                
                # Simulate realistic sensor data
                temp_base = 18.0 + (i % 24) * 0.2  # Temperature variation
                humidity_base = 85.0 + (i % 12) * 2  # Humidity variation
                
                sensor_data = SensorData(
                    device_id=devices[0].device_id,
                    room_id=room.room_id,
                    farm_id=farm.farm_id,
                    temperature_c=temp_base + (i % 3 - 1) * 0.5,
                    humidity_pct=humidity_base + (i % 5 - 2) * 1.0,
                    co2_ppm=800 + (i % 10) * 50,
                    light_lux=10 + (i % 6) * 5,
                    substrate_moisture=65.0 + (i % 8 - 4) * 0.5,
                    battery_v=3.7 - (i * 0.001),
                    recorded_at=timestamp
                )
                db.session.add(sensor_data)
            
            db.session.commit()
            print('Database seeded with sample data.')
            
        except Exception as e:
            db.session.rollback()
            print(f'Error seeding database: {str(e)}')
    
    return app, socketio

# Create app instance
app, socketio = create_app()

if __name__ == '__main__':
    # Development server
    socketio.run(app, 
                debug=app.config['DEBUG'],
                host='0.0.0.0',
                port=int(os.environ.get('PORT', 5000)),
                use_reloader=False)  # Disable reloader to prevent MQTT connection issues