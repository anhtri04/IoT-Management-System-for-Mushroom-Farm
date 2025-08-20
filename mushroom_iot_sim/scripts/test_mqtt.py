#!/usr/bin/env python3
"""
MQTT Connection Testing Script

Tests MQTT connectivity to AWS IoT Core and validates certificates.
"""

import json
import logging
import os
import sys
import time
import uuid
from pathlib import Path

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent))

try:
    from AWSIoTPythonSDK.MQTTLib import AWSIoTMQTTClient
except ImportError:
    print("AWSIoTPythonSDK not installed. Please install with:")
    print("pip install AWSIoTPythonSDK")
    sys.exit(1)

try:
    import yaml
except ImportError:
    print("PyYAML not installed. Please install with:")
    print("pip install PyYAML")
    sys.exit(1)


class MQTTTester:
    """Test MQTT connectivity and functionality."""
    
    def __init__(self, config_path='../config.yaml'):
        self.config = self.load_config(config_path)
        self.client = None
        self.connected = False
        self.messages_received = []
        
        # Setup logging
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
        )
        self.logger = logging.getLogger('MQTTTester')
    
    def load_config(self, config_path):
        """Load configuration from YAML file."""
        config_file = Path(__file__).parent.parent / config_path
        
        try:
            with open(config_file, 'r') as f:
                return yaml.safe_load(f)
        except FileNotFoundError:
            self.logger.error(f"Config file not found: {config_file}")
            return self.get_default_config()
        except Exception as e:
            self.logger.error(f"Error loading config: {e}")
            return self.get_default_config()
    
    def get_default_config(self):
        """Get default configuration for testing."""
        return {
            'aws_iot': {
                'endpoint': os.getenv('AWS_IOT_ENDPOINT', 'localhost'),
                'region': os.getenv('AWS_REGION', 'us-east-1'),
                'ca_cert_path': os.getenv('AWS_IOT_CA_CERT_PATH', './certs/AmazonRootCA1.pem'),
                'cert_path': os.getenv('AWS_IOT_CERT_PATH', './certs/device-certificate.pem.crt'),
                'private_key_path': os.getenv('AWS_IOT_PRIVATE_KEY_PATH', './certs/private.pem.key'),
                'client_id_prefix': 'mqtt_tester'
            }
        }
    
    def check_certificates(self):
        """Check if certificate files exist and are readable."""
        aws_config = self.config['aws_iot']
        cert_files = {
            'CA Certificate': aws_config.get('ca_cert_path'),
            'Device Certificate': aws_config.get('cert_path'),
            'Private Key': aws_config.get('private_key_path')
        }
        
        print("\n" + "="*50)
        print("CERTIFICATE CHECK")
        print("="*50)
        
        all_exist = True
        
        for name, path in cert_files.items():
            if not path:
                print(f"❌ {name}: Not configured")
                all_exist = False
                continue
            
            # Resolve relative paths
            if not os.path.isabs(path):
                path = Path(__file__).parent.parent / path
            
            if os.path.exists(path):
                try:
                    with open(path, 'r') as f:
                        content = f.read()
                    
                    if content.strip():
                        print(f"✅ {name}: {path} (OK)")
                    else:
                        print(f"❌ {name}: {path} (Empty file)")
                        all_exist = False
                except Exception as e:
                    print(f"❌ {name}: {path} (Read error: {e})")
                    all_exist = False
            else:
                print(f"❌ {name}: {path} (Not found)")
                all_exist = False
        
        return all_exist
    
    def setup_client(self):
        """Setup MQTT client with configuration."""
        aws_config = self.config['aws_iot']
        client_id = f"{aws_config['client_id_prefix']}_{uuid.uuid4().hex[:8]}"
        
        self.client = AWSIoTMQTTClient(client_id)
        
        # Configure endpoint
        endpoint = aws_config.get('endpoint', 'localhost')
        if endpoint == 'localhost' or 'your-iot-endpoint' in endpoint:
            print("❌ AWS IoT endpoint not configured properly")
            print("Please set AWS_IOT_ENDPOINT in your .env file or config.yaml")
            return False
        
        print(f"📡 Endpoint: {endpoint}")
        self.client.configureEndpoint(endpoint, 8883)
        
        # Configure credentials
        ca_cert = aws_config.get('ca_cert_path')
        cert_file = aws_config.get('cert_path')
        key_file = aws_config.get('private_key_path')
        
        # Resolve relative paths
        base_path = Path(__file__).parent.parent
        if ca_cert and not os.path.isabs(ca_cert):
            ca_cert = base_path / ca_cert
        if cert_file and not os.path.isabs(cert_file):
            cert_file = base_path / cert_file
        if key_file and not os.path.isabs(key_file):
            key_file = base_path / key_file
        
        try:
            self.client.configureCredentials(str(ca_cert), str(key_file), str(cert_file))
        except Exception as e:
            print(f"❌ Failed to configure credentials: {e}")
            return False
        
        # Configure connection settings
        self.client.configureAutoReconnectBackoffTime(1, 32, 20)
        self.client.configureOfflinePublishQueueing(-1)
        self.client.configureDrainingFrequency(2)
        self.client.configureConnectDisconnectTimeout(10)
        self.client.configureMQTTOperationTimeout(5)
        
        print(f"🔧 Client ID: {client_id}")
        return True
    
    def test_connection(self):
        """Test MQTT connection to AWS IoT Core."""
        print("\n" + "="*50)
        print("CONNECTION TEST")
        print("="*50)
        
        if not self.client:
            print("❌ Client not configured")
            return False
        
        try:
            print("🔄 Connecting to AWS IoT Core...")
            self.client.connect()
            self.connected = True
            print("✅ Connected successfully!")
            return True
        
        except Exception as e:
            print(f"❌ Connection failed: {e}")
            
            # Provide troubleshooting hints
            print("\n💡 Troubleshooting tips:")
            print("   - Check your internet connection")
            print("   - Verify AWS IoT endpoint URL")
            print("   - Ensure certificates are valid and not expired")
            print("   - Check IoT policy permissions")
            print("   - Verify security group settings if using VPC")
            
            return False
    
    def message_callback(self, client, userdata, message):
        """Handle received MQTT messages."""
        try:
            topic = message.topic
            payload = json.loads(message.payload.decode())
            
            self.messages_received.append({
                'topic': topic,
                'payload': payload,
                'timestamp': time.time()
            })
            
            print(f"📨 Received message on {topic}:")
            print(f"   {json.dumps(payload, indent=2)}")
        
        except Exception as e:
            print(f"❌ Error processing message: {e}")
    
    def test_publish_subscribe(self):
        """Test publish and subscribe functionality."""
        print("\n" + "="*50)
        print("PUBLISH/SUBSCRIBE TEST")
        print("="*50)
        
        if not self.connected:
            print("❌ Not connected to MQTT broker")
            return False
        
        # Test topics
        test_topic = "test/mqtt_tester"
        response_topic = "test/mqtt_tester/response"
        
        try:
            # Subscribe to response topic
            print(f"📡 Subscribing to {response_topic}...")
            self.client.subscribe(response_topic, 1, self.message_callback)
            time.sleep(1)  # Wait for subscription to be established
            
            # Publish test message
            test_message = {
                'test_id': str(uuid.uuid4()),
                'timestamp': time.time(),
                'message': 'Hello from MQTT tester!',
                'client_info': {
                    'version': '1.0.0',
                    'platform': sys.platform
                }
            }
            
            print(f"📤 Publishing to {test_topic}...")
            self.client.publish(test_topic, json.dumps(test_message), 1)
            
            # Also publish to response topic to test our own subscription
            response_message = {
                'response_to': test_message['test_id'],
                'status': 'received',
                'timestamp': time.time()
            }
            
            time.sleep(1)  # Wait a moment
            print(f"📤 Publishing response to {response_topic}...")
            self.client.publish(response_topic, json.dumps(response_message), 1)
            
            # Wait for messages
            print("⏳ Waiting for messages...")
            time.sleep(3)
            
            if self.messages_received:
                print(f"✅ Received {len(self.messages_received)} message(s)")
                return True
            else:
                print("❌ No messages received")
                print("💡 This might indicate:")
                print("   - Subscription didn't work")
                print("   - Publishing failed")
                print("   - Network issues")
                print("   - IoT policy restrictions")
                return False
        
        except Exception as e:
            print(f"❌ Publish/Subscribe test failed: {e}")
            return False
    
    def test_farm_topics(self):
        """Test with actual farm topic structure."""
        print("\n" + "="*50)
        print("FARM TOPIC STRUCTURE TEST")
        print("="*50)
        
        if not self.connected:
            print("❌ Not connected to MQTT broker")
            return False
        
        # Simulate farm structure
        farm_id = str(uuid.uuid4())
        room_id = str(uuid.uuid4())
        device_id = str(uuid.uuid4())
        
        telemetry_topic = f"farm/{farm_id}/room/{room_id}/device/{device_id}/telemetry"
        command_topic = f"farm/{farm_id}/room/{room_id}/device/{device_id}/command"
        status_topic = f"farm/{farm_id}/room/{room_id}/device/{device_id}/status"
        
        try:
            # Subscribe to command topic (device would listen for commands)
            print(f"📡 Subscribing to command topic...")
            self.client.subscribe(command_topic, 1, self.message_callback)
            time.sleep(1)
            
            # Publish telemetry (device sends data)
            telemetry_data = {
                'device_id': device_id,
                'timestamp': time.time(),
                'temperature_c': 22.5,
                'humidity_pct': 85.2,
                'co2_ppm': 1200,
                'status': 'online'
            }
            
            print(f"📤 Publishing telemetry data...")
            self.client.publish(telemetry_topic, json.dumps(telemetry_data), 1)
            
            # Simulate command from backend
            command_data = {
                'command_id': str(uuid.uuid4()),
                'command': 'turn_on_fan',
                'params': {'duration_s': 300},
                'timestamp': time.time()
            }
            
            time.sleep(1)
            print(f"📤 Publishing command...")
            self.client.publish(command_topic, json.dumps(command_data), 1)
            
            # Wait for command to be received
            time.sleep(2)
            
            # Send status acknowledgment
            status_data = {
                'device_id': device_id,
                'command_id': command_data['command_id'],
                'status': 'acked',
                'timestamp': time.time(),
                'response': 'Command executed successfully'
            }
            
            print(f"📤 Publishing status acknowledgment...")
            self.client.publish(status_topic, json.dumps(status_data), 1)
            
            time.sleep(2)
            
            print(f"\n📊 Test Summary:")
            print(f"   Farm ID: {farm_id}")
            print(f"   Room ID: {room_id}")
            print(f"   Device ID: {device_id}")
            print(f"   Messages received: {len(self.messages_received)}")
            
            return True
        
        except Exception as e:
            print(f"❌ Farm topic test failed: {e}")
            return False
    
    def disconnect(self):
        """Disconnect from MQTT broker."""
        if self.client and self.connected:
            try:
                self.client.disconnect()
                print("🔌 Disconnected from MQTT broker")
            except Exception as e:
                print(f"❌ Error disconnecting: {e}")
    
    def run_all_tests(self):
        """Run all MQTT tests."""
        print("🧪 MQTT Connection Tester")
        print("Testing AWS IoT Core connectivity and functionality")
        
        # Check certificates
        if not self.check_certificates():
            print("\n❌ Certificate check failed. Please fix certificate issues before continuing.")
            return False
        
        # Setup client
        if not self.setup_client():
            print("\n❌ Client setup failed.")
            return False
        
        # Test connection
        if not self.test_connection():
            print("\n❌ Connection test failed.")
            return False
        
        # Test publish/subscribe
        if not self.test_publish_subscribe():
            print("\n❌ Publish/Subscribe test failed.")
            self.disconnect()
            return False
        
        # Test farm topic structure
        if not self.test_farm_topics():
            print("\n❌ Farm topic test failed.")
            self.disconnect()
            return False
        
        # Disconnect
        self.disconnect()
        
        print("\n" + "="*50)
        print("✅ ALL TESTS PASSED!")
        print("Your MQTT configuration is working correctly.")
        print("="*50)
        
        return True


def main():
    import argparse
    
    parser = argparse.ArgumentParser(description='Test MQTT connectivity to AWS IoT Core')
    parser.add_argument('--config', '-c', default='../config.yaml',
                       help='Configuration file path')
    parser.add_argument('--certs-only', action='store_true',
                       help='Only check certificates, skip connection tests')
    
    args = parser.parse_args()
    
    # Load environment variables if available
    try:
        from dotenv import load_dotenv
        env_file = Path(__file__).parent.parent / '.env'
        if env_file.exists():
            load_dotenv(env_file)
    except ImportError:
        pass
    
    # Create tester
    tester = MQTTTester(args.config)
    
    if args.certs_only:
        success = tester.check_certificates()
        return 0 if success else 1
    
    # Run all tests
    success = tester.run_all_tests()
    return 0 if success else 1


if __name__ == '__main__':
    sys.exit(main())