#!/usr/bin/env python3
"""
Mushroom Farm IoT Simulator

Simulates multiple farms with rooms and devices, publishing telemetry data
to AWS IoT Core and subscribing to command topics.
"""

import json
import logging
import random
import threading
import time
import uuid
from datetime import datetime, timezone
from typing import Dict, List, Any

import yaml
from AWSIoTPythonSDK.MQTTLib import AWSIoTMQTTClient
import coloredlogs
from faker import Faker
import numpy as np


class DeviceSimulator:
    """Simulates a single IoT device with sensors and actuators."""
    
    def __init__(self, device_config: Dict[str, Any], farm_id: str, room_id: str):
        self.device_id = str(uuid.uuid4())
        self.farm_id = farm_id
        self.room_id = room_id
        self.config = device_config
        self.name = device_config['name']
        self.category = device_config['category']
        self.device_type = device_config['device_type']
        self.sensors = device_config.get('sensors', {})
        
        # Current sensor values
        self.current_values = {}
        self.initialize_sensor_values()
        
        # Device status
        self.status = 'online'
        self.firmware_version = f"v{random.randint(1, 3)}.{random.randint(0, 9)}.{random.randint(0, 9)}"
        self.last_command = None
        
        self.logger = logging.getLogger(f"Device-{self.name}")
    
    def initialize_sensor_values(self):
        """Initialize sensor values to optimal ranges."""
        for sensor_name, sensor_config in self.sensors.items():
            optimal = sensor_config.get('optimal', sensor_config.get('min', 0))
            variance = sensor_config.get('variance', 1)
            # Start near optimal with small random variation
            self.current_values[sensor_name] = optimal + random.uniform(-variance/2, variance/2)
    
    def generate_telemetry(self) -> Dict[str, Any]:
        """Generate realistic telemetry data with gradual changes."""
        timestamp = datetime.now(timezone.utc).isoformat()
        
        # Update sensor values with realistic drift
        for sensor_name, sensor_config in self.sensors.items():
            current = self.current_values[sensor_name]
            optimal = sensor_config['optimal']
            variance = sensor_config['variance']
            min_val = sensor_config['min']
            max_val = sensor_config['max']
            
            # Gradual drift towards optimal with random walk
            drift_to_optimal = (optimal - current) * 0.1
            random_walk = random.uniform(-variance/10, variance/10)
            
            new_value = current + drift_to_optimal + random_walk
            
            # Keep within bounds
            new_value = max(min_val, min(max_val, new_value))
            self.current_values[sensor_name] = new_value
        
        # Create telemetry payload
        telemetry = {
            'device_id': self.device_id,
            'timestamp': timestamp,
            'status': self.status,
            'firmware_version': self.firmware_version,
            **{k: round(v, 2) for k, v in self.current_values.items()}
        }
        
        return telemetry
    
    def get_mqtt_topic(self) -> str:
        """Get the MQTT topic for this device's telemetry."""
        return f"farm/{self.farm_id}/room/{self.room_id}/device/{self.device_id}/telemetry"
    
    def get_command_topic(self) -> str:
        """Get the MQTT topic for this device's commands."""
        return f"farm/{self.farm_id}/room/{self.room_id}/device/{self.device_id}/command"
    
    def get_status_topic(self) -> str:
        """Get the MQTT topic for this device's status/ack."""
        return f"farm/{self.farm_id}/room/{self.room_id}/device/{self.device_id}/status"
    
    def handle_command(self, command_data: Dict[str, Any]):
        """Handle incoming command and send acknowledgment."""
        self.logger.info(f"Received command: {command_data}")
        self.last_command = command_data
        
        # Simulate command execution
        command = command_data.get('command', '')
        params = command_data.get('params', {})
        
        # Simulate different command responses
        if 'turn_on' in command.lower():
            self.status = 'active'
        elif 'turn_off' in command.lower():
            self.status = 'idle'
        elif 'calibrate' in command.lower():
            self.initialize_sensor_values()  # Reset to optimal
        
        # Send acknowledgment
        ack_payload = {
            'device_id': self.device_id,
            'command_id': command_data.get('command_id', str(uuid.uuid4())),
            'status': 'acked',
            'timestamp': datetime.now(timezone.utc).isoformat(),
            'response': f"Command '{command}' executed successfully"
        }
        
        return ack_payload


class FarmSimulator:
    """Simulates an entire farm with multiple rooms and devices."""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.farm_id = str(uuid.uuid4())
        self.name = config['name']
        self.location = config['location']
        self.rooms = {}
        self.devices = []
        
        self.logger = logging.getLogger(f"Farm-{self.name}")
        
        # Initialize rooms and devices
        self.setup_rooms_and_devices()
    
    def setup_rooms_and_devices(self):
        """Set up rooms and devices based on configuration."""
        device_types = self.config.get('device_types', {})
        
        for room_config in self.config['rooms']:
            room_id = str(uuid.uuid4())
            self.rooms[room_id] = {
                'id': room_id,
                'name': room_config['name'],
                'mushroom_type': room_config['mushroom_type'],
                'stage': room_config['stage']
            }
            
            # Create devices for this room
            devices_per_room = self.config.get('devices_per_room', 4)
            device_type_names = list(device_types.keys())
            
            for i in range(devices_per_room):
                device_type_name = device_type_names[i % len(device_type_names)]
                device_type_config = device_types[device_type_name].copy()
                
                device_config = {
                    'name': f"{room_config['name']}-{device_type_name}-{i+1}",
                    **device_type_config
                }
                
                device = DeviceSimulator(device_config, self.farm_id, room_id)
                self.devices.append(device)
                
                self.logger.info(f"Created device: {device.name} ({device.device_id})")
    
    def get_all_devices(self) -> List[DeviceSimulator]:
        """Get all devices in this farm."""
        return self.devices


class MQTTSimulator:
    """Main MQTT simulator that manages all farms and devices."""
    
    def __init__(self, config_path: str = 'config.yaml'):
        self.config = self.load_config(config_path)
        self.setup_logging()
        
        self.logger = logging.getLogger('MQTTSimulator')
        self.farms = []
        self.mqtt_client = None
        self.running = False
        
        # Initialize farms
        self.setup_farms()
        
        # Setup MQTT client
        self.setup_mqtt_client()
    
    def load_config(self, config_path: str) -> Dict[str, Any]:
        """Load configuration from YAML file."""
        try:
            with open(config_path, 'r') as f:
                return yaml.safe_load(f)
        except FileNotFoundError:
            print(f"Config file {config_path} not found. Using default configuration.")
            return self.get_default_config()
    
    def get_default_config(self) -> Dict[str, Any]:
        """Get default configuration if config file is not found."""
        return {
            'aws_iot': {
                'endpoint': 'localhost',
                'region': 'us-east-1',
                'client_id_prefix': 'mushroom_sim'
            },
            'simulation': {
                'num_farms': 1,
                'rooms_per_farm': 2,
                'devices_per_room': 3,
                'telemetry_interval': 30,
                'interval_jitter': 5
            },
            'farm_templates': [
                {
                    'name': 'Demo Farm',
                    'location': 'Local',
                    'rooms': [
                        {'name': 'Room 1', 'mushroom_type': 'Shiitake', 'stage': 'fruiting'},
                        {'name': 'Room 2', 'mushroom_type': 'Oyster', 'stage': 'incubation'}
                    ]
                }
            ],
            'device_types': {
                'multi_sensor': {
                    'category': 'environmental',
                    'device_type': 'hybrid',
                    'sensors': {
                        'temperature_c': {'min': 18.0, 'max': 28.0, 'optimal': 22.0, 'variance': 2.0},
                        'humidity_pct': {'min': 75.0, 'max': 95.0, 'optimal': 85.0, 'variance': 5.0},
                        'battery_v': {'min': 3.0, 'max': 4.2, 'optimal': 3.7, 'variance': 0.3}
                    }
                }
            }
        }
    
    def setup_logging(self):
        """Setup logging configuration."""
        log_config = self.config.get('logging', {})
        level = log_config.get('level', 'INFO')
        log_format = log_config.get('format', '%(asctime)s - %(name)s - %(levelname)s - %(message)s')
        
        # Setup colored logs for console
        coloredlogs.install(
            level=level,
            fmt=log_format,
            level_styles={
                'debug': {'color': 'cyan'},
                'info': {'color': 'green'},
                'warning': {'color': 'yellow'},
                'error': {'color': 'red'},
                'critical': {'color': 'red', 'bold': True}
            }
        )
        
        # Also log to file if specified
        log_file = log_config.get('file')
        if log_file:
            file_handler = logging.FileHandler(log_file)
            file_handler.setFormatter(logging.Formatter(log_format))
            logging.getLogger().addHandler(file_handler)
    
    def setup_farms(self):
        """Initialize all farms based on configuration."""
        farm_templates = self.config.get('farm_templates', [])
        device_types = self.config.get('device_types', {})
        
        for template in farm_templates:
            farm_config = template.copy()
            farm_config['device_types'] = device_types
            farm_config['devices_per_room'] = self.config['simulation']['devices_per_room']
            
            farm = FarmSimulator(farm_config)
            self.farms.append(farm)
            
            self.logger.info(f"Created farm: {farm.name} with {len(farm.devices)} devices")
    
    def setup_mqtt_client(self):
        """Setup AWS IoT MQTT client."""
        aws_config = self.config['aws_iot']
        client_id = f"{aws_config['client_id_prefix']}_{uuid.uuid4().hex[:8]}"
        
        self.mqtt_client = AWSIoTMQTTClient(client_id)
        
        # Configure MQTT client
        endpoint = aws_config.get('endpoint', 'localhost')
        if endpoint == 'localhost' or 'your-iot-endpoint' in endpoint:
            self.logger.warning("Using localhost/demo mode - MQTT will not actually connect")
            self.demo_mode = True
            return
        
        self.demo_mode = False
        
        # Configure for AWS IoT
        self.mqtt_client.configureEndpoint(endpoint, 8883)
        
        # Configure credentials
        ca_cert = aws_config.get('ca_cert_path')
        cert_file = aws_config.get('cert_path')
        key_file = aws_config.get('private_key_path')
        
        if all([ca_cert, cert_file, key_file]):
            self.mqtt_client.configureCredentials(ca_cert, key_file, cert_file)
        else:
            self.logger.warning("AWS IoT credentials not configured. Running in demo mode.")
            self.demo_mode = True
            return
        
        # Configure connection
        self.mqtt_client.configureAutoReconnectBackoffTime(1, 32, 20)
        self.mqtt_client.configureOfflinePublishQueueing(-1)
        self.mqtt_client.configureDrainingFrequency(2)
        self.mqtt_client.configureConnectDisconnectTimeout(10)
        self.mqtt_client.configureMQTTOperationTimeout(5)
    
    def connect_mqtt(self):
        """Connect to MQTT broker."""
        if self.demo_mode:
            self.logger.info("Demo mode - skipping MQTT connection")
            return True
        
        try:
            self.mqtt_client.connect()
            self.logger.info("Connected to AWS IoT Core")
            
            # Subscribe to all command topics
            self.subscribe_to_commands()
            return True
        except Exception as e:
            self.logger.error(f"Failed to connect to MQTT: {e}")
            return False
    
    def subscribe_to_commands(self):
        """Subscribe to command topics for all devices."""
        for farm in self.farms:
            for device in farm.get_all_devices():
                command_topic = device.get_command_topic()
                self.mqtt_client.subscribe(command_topic, 1, self.command_callback)
                self.logger.info(f"Subscribed to: {command_topic}")
    
    def command_callback(self, client, userdata, message):
        """Handle incoming command messages."""
        try:
            topic = message.topic
            payload = json.loads(message.payload.decode())
            
            self.logger.info(f"Received command on {topic}: {payload}")
            
            # Extract device info from topic
            topic_parts = topic.split('/')
            if len(topic_parts) >= 6:
                farm_id = topic_parts[1]
                room_id = topic_parts[3]
                device_id = topic_parts[5]
                
                # Find the device and handle command
                device = self.find_device(farm_id, room_id, device_id)
                if device:
                    ack_payload = device.handle_command(payload)
                    
                    # Publish acknowledgment
                    status_topic = device.get_status_topic()
                    self.publish_message(status_topic, ack_payload)
                else:
                    self.logger.warning(f"Device not found: {device_id}")
        
        except Exception as e:
            self.logger.error(f"Error handling command: {e}")
    
    def find_device(self, farm_id: str, room_id: str, device_id: str) -> DeviceSimulator:
        """Find a device by its identifiers."""
        for farm in self.farms:
            if farm.farm_id == farm_id:
                for device in farm.get_all_devices():
                    if device.device_id == device_id and device.room_id == room_id:
                        return device
        return None
    
    def publish_message(self, topic: str, payload: Dict[str, Any]):
        """Publish a message to MQTT topic."""
        if self.demo_mode:
            self.logger.info(f"[DEMO] Would publish to {topic}: {json.dumps(payload, indent=2)}")
            return
        
        try:
            message_json = json.dumps(payload)
            self.mqtt_client.publish(topic, message_json, 1)
            self.logger.debug(f"Published to {topic}: {message_json}")
        except Exception as e:
            self.logger.error(f"Failed to publish to {topic}: {e}")
    
    def publish_telemetry(self):
        """Publish telemetry data for all devices."""
        for farm in self.farms:
            for device in farm.get_all_devices():
                telemetry = device.generate_telemetry()
                topic = device.get_mqtt_topic()
                self.publish_message(topic, telemetry)
    
    def telemetry_loop(self):
        """Main telemetry publishing loop."""
        interval = self.config['simulation']['telemetry_interval']
        jitter = self.config['simulation']['interval_jitter']
        
        while self.running:
            try:
                self.publish_telemetry()
                
                # Sleep with jitter
                sleep_time = interval + random.uniform(-jitter, jitter)
                time.sleep(max(1, sleep_time))
                
            except KeyboardInterrupt:
                self.logger.info("Telemetry loop interrupted")
                break
            except Exception as e:
                self.logger.error(f"Error in telemetry loop: {e}")
                time.sleep(5)  # Wait before retrying
    
    def start(self):
        """Start the simulator."""
        self.logger.info("Starting Mushroom Farm IoT Simulator")
        
        # Connect to MQTT
        if not self.connect_mqtt():
            self.logger.error("Failed to connect to MQTT. Exiting.")
            return
        
        # Start telemetry publishing
        self.running = True
        telemetry_thread = threading.Thread(target=self.telemetry_loop, daemon=True)
        telemetry_thread.start()
        
        self.logger.info(f"Simulator started with {len(self.farms)} farms")
        
        try:
            # Keep main thread alive
            while self.running:
                time.sleep(1)
        except KeyboardInterrupt:
            self.logger.info("Shutting down simulator...")
            self.stop()
    
    def stop(self):
        """Stop the simulator."""
        self.running = False
        
        if not self.demo_mode and self.mqtt_client:
            self.mqtt_client.disconnect()
        
        self.logger.info("Simulator stopped")
    
    def print_device_info(self):
        """Print information about all simulated devices."""
        print("\n" + "="*80)
        print("MUSHROOM FARM IOT SIMULATOR - DEVICE INFORMATION")
        print("="*80)
        
        for farm in self.farms:
            print(f"\nFarm: {farm.name} ({farm.farm_id})")
            print(f"Location: {farm.location}")
            
            for room_id, room_info in farm.rooms.items():
                print(f"\n  Room: {room_info['name']} ({room_id})")
                print(f"  Mushroom Type: {room_info['mushroom_type']}")
                print(f"  Stage: {room_info['stage']}")
                
                room_devices = [d for d in farm.devices if d.room_id == room_id]
                for device in room_devices:
                    print(f"\n    Device: {device.name} ({device.device_id})")
                    print(f"    Category: {device.category}")
                    print(f"    Type: {device.device_type}")
                    print(f"    Telemetry Topic: {device.get_mqtt_topic()}")
                    print(f"    Command Topic: {device.get_command_topic()}")
                    print(f"    Status Topic: {device.get_status_topic()}")
                    
                    if device.sensors:
                        print(f"    Sensors: {', '.join(device.sensors.keys())}")
        
        print("\n" + "="*80)


def main():
    """Main entry point."""
    import argparse
    
    parser = argparse.ArgumentParser(description='Mushroom Farm IoT Simulator')
    parser.add_argument('--config', '-c', default='config.yaml',
                       help='Configuration file path (default: config.yaml)')
    parser.add_argument('--info', '-i', action='store_true',
                       help='Print device information and exit')
    
    args = parser.parse_args()
    
    # Create simulator
    simulator = MQTTSimulator(args.config)
    
    if args.info:
        simulator.print_device_info()
        return
    
    # Start simulation
    try:
        simulator.start()
    except KeyboardInterrupt:
        print("\nShutting down...")
    finally:
        simulator.stop()


if __name__ == '__main__':
    main()