# IoT Smart Farm System

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.java.net/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19.1.1-blue.svg)](https://reactjs.org/)
[![React Native](https://img.shields.io/badge/React%20Native-0.74.5-blue.svg)](https://reactnative.dev/)

A comprehensive, production-ready IoT management system for mushroom farming operations. This system integrates multiple technologies to provide end-to-end farm monitoring and control capabilities with support for multiple farms, rooms (growing zones), and IoT devices per room.

## 🌟 Features

- **Multi-Tenant Farm Management**: Support for multiple farms, rooms, and devices
- **Real-Time Monitoring**: Live sensor data streaming via WebSocket and MQTT
- **Automated Control**: Threshold-based automation rules and remote device control
- **Cross-Platform**: Web dashboard and React Native mobile app
- **AI Integration**: Amazon Bedrock for recommendations and analytics
- **Secure IoT**: TLS-encrypted MQTT with X.509 certificates
- **Production Ready**: Enterprise-grade security, scalability, and monitoring

## 🏗️ Architecture

### System Components

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Web Dashboard │    │   Mobile App    │    │  IoT Devices    │
│   (React/Vite)  │    │ (React Native)  │    │ (ESP8266/32)    │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
          ┌─────────────────────────────────────────────┐
          │           Spring Boot Backend               │
          │     (REST API + WebSocket + MQTT)          │
          └─────────────────┬───────────────────────────┘
                            │
          ┌─────────────────┼───────────────────────────┐
          │                 │                           │
    ┌─────▼─────┐    ┌─────▼─────┐              ┌─────▼─────┐
    │PostgreSQL │    │AWS IoT    │              │AWS Cognito│
    │ Database  │    │   Core    │              │   Auth    │
    └───────────┘    └───────────┘              └───────────┘
```

### Technology Stack

**Backend**
- Spring Boot 3.2.1 with Java 17
- PostgreSQL database with JPA/Hibernate
- Spring Security with OAuth2 JWT
- AWS IoT Core for MQTT communication
- WebSocket for real-time updates

**Frontend**
- **Web**: React 19.1.1 + Vite + TailwindCSS
- **Mobile**: React Native + Expo SDK 51
- AWS Amplify for authentication
- Socket.IO for real-time communication

**IoT**
- ESP8266/ESP32 microcontrollers
- Arduino framework
- Secure MQTT over TLS
- Multi-sensor support (DHT11, light, moisture)

## 🚀 Quick Start

### Prerequisites

- **Java 17+** (for Spring Boot backend)
- **Node.js 18+** (for React web dashboard)
- **PostgreSQL 13+** (database)
- **AWS Account** (for IoT Core, Cognito, Bedrock)
- **Arduino IDE** (for IoT device firmware)

### 1. Clone the Repository

```bash
git clone https://github.com/anhtri04/IoT-Management-System-for-Mushroom-Farm.git
cd IoT_SmartFarm_System
```

### 2. Database Setup

```bash
# Create PostgreSQL database
createdb mushroom_farm_db

# Database migrations will run automatically on first startup
```

### 3. Backend Setup (Spring Boot)

```bash
cd spring_backend

# Configure application.yml with your AWS and database credentials
cp src/main/resources/application.yml.example src/main/resources/application.yml

# Build and run
./mvnw clean install
./mvnw spring-boot:run
```

The backend will be available at `http://localhost:8080`

### 4. Web Dashboard Setup

```bash
cd mushroom_iot_web_dashboard

# Install dependencies
npm install

# Configure environment
cp .env.example .env
# Edit .env with your backend URL and AWS configuration

# Start development server
npm run dev
```

The web dashboard will be available at `http://localhost:5173`

### 5. Mobile App Setup

```bash
cd mobile/SmartFarmMobileApp

# Install dependencies
npm install

# Configure environment
cp .env.example .env
# Edit .env with your backend URL and AWS configuration

# Start Expo development server
npx expo start
```

### 6. IoT Simulator (Optional)

```bash
cd mushroom_iot_sim

# Install Python dependencies
pip install -r requirements.txt

# Configure simulation
cp .env.example .env
# Edit .env with your AWS IoT Core credentials

# Run simulator
python simulator.py
```

## 📱 Usage

### Web Dashboard

1. **Farm Management**: Create and manage multiple farms
2. **Room Configuration**: Set up growing rooms with specific parameters
3. **Device Registration**: Add and configure IoT devices
4. **Real-Time Monitoring**: View live sensor data and device status
5. **Automation Rules**: Configure threshold-based automation
6. **AI Recommendations**: Get optimization suggestions

### Mobile App

1. **Room-Specific Access**: View assigned rooms only
2. **Quick Controls**: Fast device control interface
3. **Real-Time Data**: Live sensor monitoring
4. **Offline Support**: Cached data for offline viewing
5. **Push Notifications**: Alerts and status updates

### IoT Devices

1. **Sensor Monitoring**: Temperature, humidity, CO2, light, moisture
2. **Actuator Control**: Fans, lights, irrigation, heating
3. **Secure Communication**: TLS-encrypted MQTT
4. **OTA Updates**: Remote firmware updates
5. **Local Buffering**: Offline data storage

## 🔧 Configuration

### Environment Variables

**Spring Boot Backend** (`application.yml`):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mushroom_farm_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

aws:
  iot:
    endpoint: ${AWS_IOT_ENDPOINT}
  cognito:
    user-pool-id: ${AWS_COGNITO_USER_POOL_ID}
    client-id: ${AWS_COGNITO_CLIENT_ID}
```

**Web Dashboard** (`.env`):
```env
VITE_API_BASE_URL=http://localhost:8080
VITE_AWS_REGION=us-east-1
VITE_AWS_COGNITO_USER_POOL_ID=your_user_pool_id
VITE_AWS_COGNITO_CLIENT_ID=your_client_id
```

**Mobile App** (`.env`):
```env
EXPO_PUBLIC_API_BASE_URL=http://localhost:8080
EXPO_PUBLIC_AWS_REGION=us-east-1
EXPO_PUBLIC_AWS_COGNITO_USER_POOL_ID=your_user_pool_id
EXPO_PUBLIC_AWS_COGNITO_CLIENT_ID=your_client_id
```

### AWS Services Setup

1. **AWS IoT Core**:
   - Create IoT Thing Types for devices
   - Configure IoT policies for device access
   - Generate device certificates

2. **AWS Cognito**:
   - Create User Pool for authentication
   - Configure App Client settings
   - Set up OAuth flows

3. **Amazon Bedrock** (Optional):
   - Enable AI models for recommendations
   - Configure IAM roles for access

## 📊 API Documentation

Once the Spring Boot backend is running, access the interactive API documentation at:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI Spec**: `http://localhost:8080/v3/api-docs`

### Key Endpoints

- `GET /api/farms` - List user's farms
- `POST /api/farms` - Create new farm
- `GET /api/farms/{farm_id}/rooms` - List rooms in farm
- `POST /api/devices` - Register new device
- `GET /api/rooms/{room_id}/telemetry` - Get sensor data
- `POST /api/devices/{device_id}/commands` - Send device command

## 🧪 Testing

### Backend Tests
```bash
cd spring_backend
./mvnw test
```

### Frontend Tests
```bash
cd mushroom_iot_web_dashboard
npm test
```

### Integration Tests
```bash
cd spring_backend
./mvnw test -Dtest=*IntegrationTest
```

## 🐳 Docker Deployment

### Using Docker Compose

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Individual Services

```bash
# Backend
cd spring_backend
docker build -t smartfarm-backend .
docker run -p 8080:8080 smartfarm-backend

# Web Dashboard
cd mushroom_iot_web_dashboard
docker build -t smartfarm-web .
docker run -p 5173:5173 smartfarm-web
```

## 📁 Project Structure

```
IoT_SmartFarm_System/
├── spring_backend/                 # Spring Boot backend
│   ├── src/main/java/com/smartfarm/
│   │   ├── controllers/           # REST API controllers
│   │   ├── entities/             # JPA entities
│   │   ├── repositories/         # Data access layer
│   │   ├── services/            # Business logic
│   │   ├── security/            # Authentication & authorization
│   │   ├── mqtt/               # MQTT integration
│   │   └── websocket/          # WebSocket configuration
│   └── pom.xml                # Maven dependencies
├── mushroom_iot_web_dashboard/    # React web application
│   ├── src/
│   │   ├── components/         # Reusable UI components
│   │   ├── pages/             # Application pages
│   │   ├── contexts/          # React contexts
│   │   └── services/          # API services
│   └── package.json
├── mobile/SmartFarmMobileApp/     # React Native mobile app
│   ├── components/            # Mobile UI components
│   ├── screens/              # Application screens
│   ├── navigation/           # Navigation setup
│   └── services/             # API and real-time services
├── mushroom_iot_sim/             # IoT device simulator
│   ├── simulator.py          # Main simulation engine
│   ├── config.yaml           # Simulation configuration
│   └── scripts/              # Utility scripts
├── arduino/                      # ESP8266/ESP32 firmware
│   └── esp82xx.ino           # Arduino firmware code
└── flask_backend/               # Legacy Flask backend (reference)
```

## 🔒 Security

### Authentication & Authorization
- AWS Cognito for user management
- JWT tokens for stateless authentication
- Role-based access control (RBAC)
- API endpoint protection

### IoT Security
- X.509 certificate authentication
- TLS encryption for all MQTT communications
- Topic-level access control
- Secure credential storage

### Data Security
- Database connection encryption
- Input validation and sanitization
- CORS configuration
- Secrets management with AWS KMS

## 📈 Monitoring & Observability

### Application Monitoring
- Spring Boot Actuator health checks
- Structured logging with correlation IDs
- Performance metrics collection
- Error tracking and alerting

### IoT Monitoring
- Real-time device status tracking
- MQTT connection monitoring
- Sensor data quality validation
- Performance metrics

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- Follow existing code style and conventions
- Write comprehensive tests for new features
- Update documentation for API changes
- Ensure all tests pass before submitting PR

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

### Documentation
- [Project Report](PROJECT_REPORT.md) - Comprehensive technical documentation
- [Migration Guide](Flask_to_SpringBoot_Migration_Guide.md) - Flask to Spring Boot migration details
- [API Documentation](http://localhost:8080/swagger-ui.html) - Interactive API docs

### Getting Help

If you encounter any issues or have questions:

1. Check the [Project Report](PROJECT_REPORT.md) for detailed technical information
2. Review the API documentation at `http://localhost:8080/swagger-ui.html`
3. Check existing issues in the repository
4. Create a new issue with detailed information about your problem

### Common Issues

**Database Connection Issues**
- Ensure PostgreSQL is running and accessible
- Verify database credentials in `application.yml`
- Check if the database exists and migrations have run

**AWS Configuration Issues**
- Verify AWS credentials and permissions
- Check IoT Core endpoint and certificate configuration
- Ensure Cognito User Pool is properly configured

**Device Connection Issues**
- Verify MQTT certificates are valid and not expired
- Check IoT policies allow device access to required topics
- Ensure device firmware has correct AWS IoT endpoint

## 🚀 Deployment

### Production Deployment

For production deployment, consider:

1. **Infrastructure as Code**: Use Terraform or CloudFormation
2. **Container Orchestration**: Deploy with Kubernetes or ECS
3. **Load Balancing**: Use Application Load Balancer
4. **Database**: Use managed PostgreSQL (RDS)
5. **Monitoring**: Implement comprehensive monitoring with CloudWatch
6. **Security**: Enable WAF, use VPC, implement proper IAM roles

### Environment-Specific Configuration

- **Development**: Local database, mock AWS services
- **Staging**: Shared AWS resources, test data
- **Production**: Dedicated AWS resources, real devices

---

**Built with ❤️ for modern mushroom farming operations**
