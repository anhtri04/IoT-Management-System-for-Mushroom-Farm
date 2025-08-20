# Mushroom Farm IoT Simulator

A comprehensive Python application that simulates IoT devices for mushroom farms, publishing realistic telemetry data to AWS IoT Core via MQTT and handling command subscriptions.

## Features

- **Multi-Farm Simulation**: Simulate multiple farms with configurable rooms and devices
- **Realistic Sensor Data**: Generate realistic environmental data with gradual changes and drift
- **MQTT Integration**: Full AWS IoT Core integration with secure MQTT connections
- **Command Handling**: Subscribe to command topics and send acknowledgments
- **Device Registration**: Automatic device registration with backend API
- **Flexible Configuration**: YAML-based configuration for easy customization
- **Demo Mode**: Run without AWS IoT Core for local testing

## Project Structure

```
mushroom_iot_sim/
├── simulator.py              # Main simulator application
├── device_registration.py    # Device registration with backend API
├── config.yaml              # Configuration file
├── requirements.txt         # Python dependencies
├── README.md               # This file
├── .env.example            # Environment variables template
└── scripts/
    ├── start_simulator.py   # Convenience startup script
    └── test_mqtt.py        # MQTT connection testing
```

## Installation

1. **Clone the repository** (if not already done):
   ```bash
   cd mushroom_iot_sim
   ```

2. **Create a virtual environment**:
   ```bash
   python -m venv venv
   
   # On Windows
   venv\Scripts\activate
   
   # On macOS/Linux
   source venv/bin/activate
   ```

3. **Install dependencies**:
   ```bash
   pip install -r requirements.txt
   ```

4. **Configure the simulator**:
   ```bash
   cp .env.example .env
   # Edit .env with your AWS IoT Core settings
   ```

## Configuration

### Basic Configuration (config.yaml)

The simulator uses a YAML configuration file to define:

- **AWS IoT Core settings**: Endpoint, certificates, regions
- **Simulation parameters**: Number of farms, rooms, devices, intervals
- **Farm templates**: Mushroom types, growth stages, room layouts
- **Device types**: Sensor ranges, optimal values, variance
- **Logging settings**: Log levels, output formats

### Environment Variables (.env)

Create a `.env` file for sensitive configuration:

```env
# AWS IoT Core Configuration
AWS_IOT_ENDPOINT=your-iot-endpoint.iot.us-east-1.amazonaws.com
AWS_IOT_CA_CERT_PATH=./certs/AmazonRootCA1.pem
AWS_IOT_CERT_PATH=./certs/device-certificate.pem.crt
AWS_IOT_PRIVATE_KEY_PATH=./certs/private.pem.key
AWS_REGION=us-east-1

# Backend API Configuration
BACKEND_URL=http://localhost:5000
API_AUTH_TOKEN=your-jwt-token-here

# Logging
LOG_LEVEL=INFO
LOG_FILE=simulator.log
```

## Usage

### 1. Basic Simulation (Demo Mode)

Run the simulator without AWS IoT Core connection:

```bash
python simulator.py
```

This will:
- Create simulated farms, rooms, and devices
- Generate and log telemetry data
- Show MQTT topics and payloads in console
- Run indefinitely until stopped with Ctrl+C

### 2. View Device Information

See what devices will be simulated:

```bash
python simulator.py --info
```

### 3. Custom Configuration

Use a different configuration file:

```bash
python simulator.py --config my_config.yaml
```

### 4. Device Registration

Register simulated devices with the backend API:

```bash
# Dry run to see what would be registered
python device_registration.py --dry-run

# Actually register devices
python device_registration.py --backend-url http://localhost:5000

# With authentication
python device_registration.py --backend-url http://localhost:5000 --auth-token "your-jwt-token"
```

### 5. Full AWS IoT Core Integration

1. **Setup AWS IoT Core**:
   - Create IoT Things for each device
   - Generate certificates
   - Create IoT policies
   - Configure IoT rules for data ingestion

2. **Configure certificates**:
   ```bash
   mkdir certs
   # Copy your AWS IoT certificates to the certs/ directory
   ```

3. **Update configuration**:
   Edit `config.yaml` with your AWS IoT endpoint and certificate paths.

4. **Run simulator**:
   ```bash
   python simulator.py --config config.yaml
   ```

## MQTT Topic Structure

The simulator follows the defined MQTT topic structure:

### Telemetry (Device → Cloud)
```
farm/{farm_id}/room/{room_id}/device/{device_id}/telemetry
```

**Example payload**:
```json
{
  "device_id": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-01-15T10:30:00.000Z",
  "status": "online",
  "firmware_version": "v1.2.3",
  "temperature_c": 22.5,
  "humidity_pct": 85.2,
  "co2_ppm": 1200,
  "light_lux": 150.0,
  "substrate_moisture": 75.8,
  "battery_v": 3.7
}
```

### Commands (Cloud → Device)
```
farm/{farm_id}/room/{room_id}/device/{device_id}/command
```

**Example payload**:
```json
{
  "command_id": "cmd-123",
  "command": "turn_on_fan",
  "params": {
    "duration_s": 300,
    "speed": "medium"
  },
  "timestamp": "2024-01-15T10:30:00.000Z"
}
```

### Status/Acknowledgment (Device → Cloud)
```
farm/{farm_id}/room/{room_id}/device/{device_id}/status
```

**Example payload**:
```json
{
  "device_id": "550e8400-e29b-41d4-a716-446655440000",
  "command_id": "cmd-123",
  "status": "acked",
  "timestamp": "2024-01-15T10:30:05.000Z",
  "response": "Command 'turn_on_fan' executed successfully"
}
```

## Device Types and Sensors

The simulator supports various device types with realistic sensor ranges:

### Environmental Sensors
- **Temperature**: 18-28°C (optimal: 22°C)
- **Humidity**: 75-95% (optimal: 85%)
- **CO2**: 800-2000 ppm (optimal: 1200 ppm)
- **Light**: 50-500 lux (optimal: 200 lux)

### Substrate Monitoring
- **Moisture**: 60-90% (optimal: 75%)
- **pH**: 6.0-7.5 (optimal: 6.5)
- **Temperature**: 20-26°C (optimal: 23°C)

### System Monitoring
- **Battery Voltage**: 3.0-4.2V (optimal: 3.7V)
- **Signal Strength**: -100 to -30 dBm
- **Memory Usage**: 10-90%

## Customization

### Adding New Device Types

Edit `config.yaml` to add new device types:

```yaml
device_types:
  custom_sensor:
    category: 'environmental'
    device_type: 'sensor'
    sensors:
      custom_parameter:
        min: 0.0
        max: 100.0
        optimal: 50.0
        variance: 5.0
```

### Modifying Farm Templates

Add new farm configurations:

```yaml
farm_templates:
  - name: 'Large Commercial Farm'
    location: 'Industrial District'
    rooms:
      - name: 'Incubation Room A'
        mushroom_type: 'Shiitake'
        stage: 'incubation'
      - name: 'Fruiting Room B'
        mushroom_type: 'Oyster'
        stage: 'fruiting'
```

### Adjusting Simulation Parameters

```yaml
simulation:
  num_farms: 2
  rooms_per_farm: 4
  devices_per_room: 6
  telemetry_interval: 30  # seconds
  interval_jitter: 5      # ±5 seconds
```

## Troubleshooting

### Common Issues

1. **MQTT Connection Failed**:
   - Check AWS IoT endpoint URL
   - Verify certificate files exist and are readable
   - Ensure IoT policy allows required actions
   - Check network connectivity

2. **Device Registration Failed**:
   - Verify backend API is running
   - Check authentication token
   - Ensure API endpoints are accessible

3. **High CPU Usage**:
   - Increase telemetry interval
   - Reduce number of simulated devices
   - Check for infinite loops in custom code

### Debug Mode

Enable debug logging:

```bash
LOG_LEVEL=DEBUG python simulator.py
```

### Testing MQTT Connection

Test MQTT connectivity separately:

```bash
python scripts/test_mqtt.py
```

## Integration with Backend

The simulator is designed to work with the Flask backend:

1. **Device Registration**: Use `device_registration.py` to register devices
2. **Telemetry Ingestion**: Backend receives data via AWS IoT Rules → Lambda
3. **Command Sending**: Backend publishes commands via AWS IoT Core
4. **Real-time Updates**: WebSocket integration for live dashboard updates

## Performance Considerations

- **Memory Usage**: ~10-50MB depending on number of devices
- **CPU Usage**: Low, mostly I/O bound
- **Network**: ~1-10KB/s per device depending on telemetry interval
- **Scalability**: Tested with up to 1000 simulated devices

## Security

- Uses AWS IoT Core X.509 certificates for authentication
- All MQTT connections use TLS encryption
- No hardcoded credentials (uses environment variables)
- Supports IAM roles for AWS service integration

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## License

This project is part of the Mushroom Farm IoT System and follows the same license terms.

## Support

For issues and questions:
1. Check the troubleshooting section
2. Review logs for error messages
3. Create an issue in the project repository
4. Include configuration and log files (redact sensitive information)