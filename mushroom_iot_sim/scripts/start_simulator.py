#!/usr/bin/env python3
"""
Convenience script to start the IoT simulator with common configurations.
"""

import os
import sys
import argparse
import subprocess
from pathlib import Path

# Add parent directory to path to import simulator
sys.path.insert(0, str(Path(__file__).parent.parent))

from simulator import MQTTSimulator


def check_dependencies():
    """Check if all required dependencies are installed."""
    required_packages = [
        'AWSIoTPythonSDK',
        'PyYAML',
        'coloredlogs',
        'faker',
        'numpy'
    ]
    
    missing_packages = []
    
    for package in required_packages:
        try:
            __import__(package)
        except ImportError:
            missing_packages.append(package)
    
    if missing_packages:
        print(f"Missing required packages: {', '.join(missing_packages)}")
        print("Please install them with: pip install -r requirements.txt")
        return False
    
    return True


def check_config_file(config_path):
    """Check if configuration file exists and is valid."""
    if not os.path.exists(config_path):
        print(f"Configuration file not found: {config_path}")
        print("Please create a config.yaml file or specify a different path.")
        return False
    
    try:
        import yaml
        with open(config_path, 'r') as f:
            config = yaml.safe_load(f)
        
        # Basic validation
        required_sections = ['simulation', 'farm_templates', 'device_types']
        for section in required_sections:
            if section not in config:
                print(f"Missing required section in config: {section}")
                return False
        
        return True
    
    except Exception as e:
        print(f"Error reading configuration file: {e}")
        return False


def setup_environment():
    """Setup environment variables from .env file if it exists."""
    env_file = Path(__file__).parent.parent / '.env'
    
    if env_file.exists():
        try:
            from dotenv import load_dotenv
            load_dotenv(env_file)
            print(f"Loaded environment variables from {env_file}")
        except ImportError:
            print("python-dotenv not installed. Skipping .env file loading.")
            print("Install with: pip install python-dotenv")
    else:
        print(f"No .env file found at {env_file}")
        print("You can create one from .env.example for custom configuration.")


def main():
    parser = argparse.ArgumentParser(
        description='Start the Mushroom Farm IoT Simulator',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python start_simulator.py                    # Start with default config
  python start_simulator.py --demo             # Start in demo mode
  python start_simulator.py --info             # Show device info only
  python start_simulator.py --config custom.yaml  # Use custom config
  python start_simulator.py --register         # Register devices first
"""
    )
    
    parser.add_argument('--config', '-c', default='../config.yaml',
                       help='Configuration file path (default: ../config.yaml)')
    parser.add_argument('--demo', action='store_true',
                       help='Force demo mode (no AWS IoT connection)')
    parser.add_argument('--info', '-i', action='store_true',
                       help='Show device information and exit')
    parser.add_argument('--register', '-r', action='store_true',
                       help='Register devices with backend API before starting')
    parser.add_argument('--backend-url', default='http://localhost:5000',
                       help='Backend API URL for device registration')
    parser.add_argument('--check-deps', action='store_true',
                       help='Check dependencies and exit')
    parser.add_argument('--no-env', action='store_true',
                       help='Skip loading .env file')
    
    args = parser.parse_args()
    
    # Check dependencies if requested
    if args.check_deps:
        if check_dependencies():
            print("All dependencies are installed.")
            return 0
        else:
            return 1
    
    # Setup environment
    if not args.no_env:
        setup_environment()
    
    # Check dependencies
    if not check_dependencies():
        return 1
    
    # Resolve config path
    config_path = Path(__file__).parent / args.config
    if not config_path.is_absolute():
        config_path = Path(__file__).parent.parent / args.config
    
    # Check configuration
    if not check_config_file(config_path):
        return 1
    
    print(f"Using configuration: {config_path}")
    
    try:
        # Create simulator instance
        simulator = MQTTSimulator(str(config_path))
        
        # Force demo mode if requested
        if args.demo:
            simulator.demo_mode = True
            print("Running in demo mode (no AWS IoT connection)")
        
        # Show device info and exit if requested
        if args.info:
            simulator.print_device_info()
            return 0
        
        # Register devices if requested
        if args.register:
            print("Registering devices with backend API...")
            try:
                from device_registration import DeviceRegistrar
                
                auth_token = os.getenv('API_AUTH_TOKEN')
                registrar = DeviceRegistrar(args.backend_url, auth_token)
                registration_map = registrar.register_all_from_simulator(simulator)
                registrar.save_registration_map(registration_map)
                
                print("Device registration completed.")
            except Exception as e:
                print(f"Device registration failed: {e}")
                print("Continuing with simulation...")
        
        # Start the simulator
        print("Starting IoT simulator...")
        print("Press Ctrl+C to stop")
        
        simulator.start()
        
    except KeyboardInterrupt:
        print("\nSimulator stopped by user.")
        return 0
    except Exception as e:
        print(f"Error starting simulator: {e}")
        return 1


if __name__ == '__main__':
    sys.exit(main())