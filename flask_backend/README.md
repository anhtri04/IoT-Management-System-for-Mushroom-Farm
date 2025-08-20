# IoT Smart Mushroom Farm - Flask Backend

A comprehensive Flask-based backend system for managing IoT smart mushroom farms with real-time monitoring, automation, and AI-powered recommendations.

## Features

### Core Functionality
- **Multi-Farm Management**: Support for multiple farms with hierarchical room organization
- **Real-time Monitoring**: Live sensor data collection and visualization via WebSocket
- **Device Control**: Remote control of actuators (humidifiers, fans, lights, etc.)
- **Automation Rules**: Configurable automation based on sensor thresholds
- **AI Recommendations**: AWS Bedrock-powered insights and optimization suggestions
- **User Management**: AWS Cognito integration for authentication and authorization
- **Historical Analytics**: Comprehensive data storage and analysis

### Technical Features
- **MQTT Integration**: Secure communication with IoT devices via AWS IoT Core
- **PostgreSQL Database**: Robust data storage with SQLAlchemy ORM
- **Background Tasks**: Celery-based asynchronous processing
- **API Documentation**: Swagger/OpenAPI documentation
- **Real-time Updates**: Socket.IO for live data streaming
- **Microservices Ready**: Modular architecture for scalability

## Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   IoT Devices   │────│   AWS IoT Core   │────│  Flask Backend  │
│  (ESP32/ESP8266)│    │     (MQTT)       │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                         │
                       ┌─────────────────────────────────┼─────────────────┐
                       │                                 │                 │
                ┌──────▼──────┐    ┌──────────────┐    ┌▼──────────┐    ┌─▼─────────┐
                │ PostgreSQL  │    │    Redis     │    │   AWS     │    │  Socket.IO│
                │  Database   │    │   (Celery)   │    │  Bedrock  │    │  (WebSocket)│
                └─────────────┘    └──────────────┘    └───────────┘    └───────────┘
```

## Installation

### Prerequisites
- Python 3.8+
- PostgreSQL 12+
- Redis 6+
- AWS Account with IoT Core, Cognito, and Bedrock access

### Setup

1. **Clone and Navigate**
   ```bash
   cd flask_backend
   ```

2. **Create Virtual Environment**
   ```bash
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   ```

3. **Install Dependencies**
   ```bash
   pip install -r requirements.txt
   ```

4. **Environment Configuration**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

5. **Database Setup**
   ```bash
   # Initialize database
   flask db init
   flask db migrate -m "Initial migration"
   flask db upgrade
   
   # Seed with sample data (optional)
   flask seed-db
   ```

6. **Start Services**
   ```bash
   # Terminal 1: Redis
   redis-server
   
   # Terminal 2: Celery Worker
   celery -A celery_app.celery_app worker --loglevel=info
   
   # Terminal 3: Celery Beat (for periodic tasks)
   celery -A celery_app.celery_app beat --loglevel=info
   
   # Terminal 4: Flask Application
   python app.py
   ```

## Configuration

### Environment Variables

Create a `.env` file based on `.env.example`:

```env
# Database
DATABASE_URL=postgresql://username:password@localhost:5432/smartfarm

# Flask
FLASK_ENV=development
FLASK_DEBUG=True
SECRET_KEY=your-secret-key-here
JWT_SECRET_KEY=your-jwt-secret-key

# AWS Services
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key

# AWS IoT Core
IOT_ENDPOINT=your-iot-endpoint.iot.region.amazonaws.com
IOT_CA_CERT_PATH=certs/AmazonRootCA1.pem
IOT_CERT_PATH=certs/device-cert.pem.crt
IOT_PRIVATE_KEY_PATH=certs/device-private.pem.key

# AWS Cognito
COGNITO_USER_POOL_ID=your-user-pool-id
COGNITO_CLIENT_ID=your-client-id
COGNITO_REGION=us-east-1

# Redis & Celery
REDIS_URL=redis://localhost:6379/0
CELERY_BROKER_URL=redis://localhost:6379/0
CELERY_RESULT_BACKEND=redis://localhost:6379/0
```

### AWS Setup

1. **IoT Core**
   - Create IoT Thing for each device
   - Generate certificates and policies
   - Configure MQTT topics: `farm/{farm_id}/room/{room_id}/{device_type}/{device_id}`

2. **Cognito**
   - Create User Pool for authentication
   - Configure app client settings
   - Set up user groups and permissions

3. **Bedrock**
   - Enable Claude or other AI models
   - Configure IAM permissions for model access

## API Documentation

Once running, visit:
- **Swagger UI**: `http://localhost:5000/docs/`
- **API Spec**: `http://localhost:5000/apispec.json`

### Key Endpoints

#### Farms & Rooms
```
GET    /api/farms                    # List user's farms
POST   /api/farms                    # Create new farm
GET    /api/farms/{farm_id}/rooms    # List rooms in farm
POST   /api/farms/{farm_id}/rooms    # Create new room
```

#### Devices & Telemetry
```
GET    /api/rooms/{room_id}/devices           # List room devices
POST   /api/rooms/{room_id}/devices           # Register new device
GET    /api/rooms/{room_id}/telemetry         # Get sensor data
GET    /api/devices/{device_id}/latest        # Latest device reading
```

#### Commands & Control
```
POST   /api/rooms/{room_id}/commands          # Send device command
GET    /api/rooms/{room_id}/commands          # List recent commands
```

#### Automation & AI
```
GET    /api/rooms/{room_id}/automation        # List automation rules
POST   /api/rooms/{room_id}/automation        # Create automation rule
POST   /api/rooms/{room_id}/ai/recommend      # Force AI recommendations
GET    /api/rooms/{room_id}/ai/recommendations # Get AI recommendations
```

## Database Schema

### Core Tables
- **farms**: Farm information and ownership
- **rooms**: Individual growing rooms within farms
- **devices**: IoT devices (sensors/actuators)
- **sensor_data**: Time-series sensor readings
- **commands**: Device control commands
- **automation_rules**: Automated control rules
- **recommendations**: AI-generated suggestions
- **notifications**: User notifications

### Relationships
```
Farm (1) ──── (N) Room (1) ──── (N) Device
                │                     │
                └── (N) SensorData ───┘
                │
                ├── (N) AutomationRule
                ├── (N) Recommendation
                └── (N) Notification
```

## MQTT Topics

### Device Communication
```
# Telemetry (Device → Cloud)
farm/{farm_id}/room/{room_id}/telemetry/{device_id}

# Commands (Cloud → Device)
farm/{farm_id}/room/{room_id}/command/{device_id}

# Status Updates (Device → Cloud)
farm/{farm_id}/room/{room_id}/status/{device_id}

# Acknowledgments (Device → Cloud)
farm/{farm_id}/room/{room_id}/ack/{device_id}
```

### Message Formats

**Telemetry Message**:
```json
{
  "device_id": "sensor-001",
  "timestamp": "2024-01-15T10:30:00Z",
  "data": {
    "temperature_c": 18.5,
    "humidity_pct": 85.2,
    "co2_ppm": 800,
    "light_lux": 15,
    "substrate_moisture": 65.0,
    "battery_v": 3.7
  }
}
```

**Command Message**:
```json
{
  "command_id": "cmd-123",
  "device_id": "actuator-001",
  "action": "set_humidity",
  "value": 90,
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## Background Tasks

### Celery Workers

**Available Tasks**:
- `process_sensor_data`: Process incoming telemetry
- `check_automation_rules`: Execute automation logic
- `generate_ai_recommendations`: AI analysis and suggestions
- `send_notification`: User notifications
- `cleanup_old_data`: Data retention management
- `predict_yield`: Yield prediction analysis

**Periodic Tasks**:
- AI analysis every 4 hours
- Daily data cleanup
- Yield predictions for fruiting rooms

## Development

### Running Tests
```bash
pytest tests/ -v
```

### Database Migrations
```bash
# Create migration
flask db migrate -m "Description of changes"

# Apply migration
flask db upgrade

# Rollback migration
flask db downgrade
```

### Adding New Features

1. **New API Endpoint**:
   - Add route to `api_routes.py`
   - Update Swagger documentation
   - Add authentication/authorization

2. **New Background Task**:
   - Add task to `tasks.py`
   - Register in `celery_app.py`
   - Configure periodic execution if needed

3. **New Database Model**:
   - Add model to `models.py`
   - Create migration: `flask db migrate`
   - Apply migration: `flask db upgrade`

## Monitoring & Logging

### Application Logs
- Location: `logs/smartfarm.log`
- Rotation: 10MB files, 10 backups
- Levels: INFO, WARNING, ERROR

### Health Checks
- **Application**: `GET /health`
- **Database**: Connection status in health endpoint
- **Redis**: Celery worker status
- **MQTT**: Connection status monitoring

### Metrics
- Request/response times
- Database query performance
- MQTT message throughput
- Background task execution times

## Security

### Authentication
- AWS Cognito JWT tokens
- Role-based access control
- Room-level permissions

### Data Protection
- TLS encryption for all communications
- Database connection encryption
- Secure credential storage

### API Security
- Rate limiting
- Input validation
- SQL injection prevention
- XSS protection

## Deployment

### Production Checklist
- [ ] Set `FLASK_ENV=production`
- [ ] Configure production database
- [ ] Set up SSL certificates
- [ ] Configure reverse proxy (nginx)
- [ ] Set up monitoring and alerting
- [ ] Configure backup strategy
- [ ] Set up log aggregation

### Docker Deployment
```bash
# Build image
docker build -t smartfarm-backend .

# Run with docker-compose
docker-compose up -d
```

## Troubleshooting

### Common Issues

1. **MQTT Connection Failed**
   - Check AWS IoT certificates
   - Verify endpoint URL
   - Check security policies

2. **Database Connection Error**
   - Verify PostgreSQL is running
   - Check connection string
   - Ensure database exists

3. **Celery Tasks Not Running**
   - Check Redis connection
   - Verify worker is running
   - Check task registration

4. **AI Recommendations Not Generated**
   - Verify AWS Bedrock access
   - Check IAM permissions
   - Ensure sufficient sensor data

### Debug Mode
```bash
# Enable debug logging
export FLASK_DEBUG=True
export LOG_LEVEL=DEBUG

# Run with verbose output
python app.py
```

## Contributing

1. Fork the repository
2. Create feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit pull request

## License

MIT License - see LICENSE file for details

## Support

For issues and questions:
- Create GitHub issue
- Check documentation
- Review logs for error details