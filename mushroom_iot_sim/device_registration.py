#!/usr/bin/env python3
"""
Device Registration Script

Registers simulated devices with the backend API to ensure they exist
in the database before starting telemetry simulation.
"""

import json
import logging
import requests
import yaml
from typing import Dict, List, Any
from simulator import MQTTSimulator


class DeviceRegistrar:
    """Handles device registration with the backend API."""
    
    def __init__(self, backend_url: str, auth_token: str = None):
        self.backend_url = backend_url.rstrip('/')
        self.auth_token = auth_token
        self.session = requests.Session()
        
        if auth_token:
            self.session.headers.update({
                'Authorization': f'Bearer {auth_token}',
                'Content-Type': 'application/json'
            })
        
        self.logger = logging.getLogger('DeviceRegistrar')
    
    def register_farm(self, farm_data: Dict[str, Any]) -> str:
        """Register a farm and return its ID."""
        try:
            response = self.session.post(
                f"{self.backend_url}/api/farms",
                json={
                    'name': farm_data['name'],
                    'location': farm_data.get('location', 'Simulated Location')
                }
            )
            
            if response.status_code == 201:
                farm_id = response.json()['farm_id']
                self.logger.info(f"Registered farm: {farm_data['name']} ({farm_id})")
                return farm_id
            else:
                self.logger.error(f"Failed to register farm: {response.status_code} - {response.text}")
                return None
        
        except Exception as e:
            self.logger.error(f"Error registering farm: {e}")
            return None
    
    def register_room(self, farm_id: str, room_data: Dict[str, Any]) -> str:
        """Register a room and return its ID."""
        try:
            response = self.session.post(
                f"{self.backend_url}/api/farms/{farm_id}/rooms",
                json={
                    'name': room_data['name'],
                    'mushroom_type': room_data.get('mushroom_type', 'Unknown'),
                    'stage': room_data.get('stage', 'growing'),
                    'description': f"Simulated room for {room_data.get('mushroom_type', 'mushroom')} cultivation"
                }
            )
            
            if response.status_code == 201:
                room_id = response.json()['room_id']
                self.logger.info(f"Registered room: {room_data['name']} ({room_id})")
                return room_id
            else:
                self.logger.error(f"Failed to register room: {response.status_code} - {response.text}")
                return None
        
        except Exception as e:
            self.logger.error(f"Error registering room: {e}")
            return None
    
    def register_device(self, room_id: str, device_data: Dict[str, Any]) -> bool:
        """Register a device."""
        try:
            mqtt_topic = f"farm/{device_data['farm_id']}/room/{room_id}/device/{device_data['device_id']}/telemetry"
            
            response = self.session.post(
                f"{self.backend_url}/api/devices",
                json={
                    'room_id': room_id,
                    'name': device_data['name'],
                    'device_type': device_data.get('device_type', 'sensor'),
                    'category': device_data.get('category', 'environmental'),
                    'mqtt_topic': mqtt_topic,
                    'firmware_version': device_data.get('firmware_version', 'v1.0.0')
                }
            )
            
            if response.status_code == 201:
                self.logger.info(f"Registered device: {device_data['name']} ({device_data['device_id']})")
                return True
            else:
                self.logger.error(f"Failed to register device: {response.status_code} - {response.text}")
                return False
        
        except Exception as e:
            self.logger.error(f"Error registering device: {e}")
            return False
    
    def register_all_from_simulator(self, simulator: MQTTSimulator) -> Dict[str, Any]:
        """Register all farms, rooms, and devices from a simulator instance."""
        registration_map = {
            'farms': {},
            'rooms': {},
            'devices': {}
        }
        
        for farm in simulator.farms:
            # Register farm
            farm_data = {
                'name': farm.name,
                'location': farm.location
            }
            
            registered_farm_id = self.register_farm(farm_data)
            if not registered_farm_id:
                self.logger.error(f"Failed to register farm {farm.name}, skipping...")
                continue
            
            registration_map['farms'][farm.farm_id] = registered_farm_id
            
            # Register rooms
            for room_id, room_info in farm.rooms.items():
                room_data = {
                    'name': room_info['name'],
                    'mushroom_type': room_info['mushroom_type'],
                    'stage': room_info['stage']
                }
                
                registered_room_id = self.register_room(registered_farm_id, room_data)
                if not registered_room_id:
                    self.logger.error(f"Failed to register room {room_info['name']}, skipping...")
                    continue
                
                registration_map['rooms'][room_id] = registered_room_id
                
                # Register devices in this room
                room_devices = [d for d in farm.devices if d.room_id == room_id]
                for device in room_devices:
                    device_data = {
                        'farm_id': farm.farm_id,
                        'device_id': device.device_id,
                        'name': device.name,
                        'device_type': device.device_type,
                        'category': device.category,
                        'firmware_version': device.firmware_version
                    }
                    
                    if self.register_device(registered_room_id, device_data):
                        registration_map['devices'][device.device_id] = {
                            'room_id': registered_room_id,
                            'farm_id': registered_farm_id
                        }
        
        return registration_map
    
    def save_registration_map(self, registration_map: Dict[str, Any], filename: str = 'registration_map.json'):
        """Save the registration mapping to a file."""
        try:
            with open(filename, 'w') as f:
                json.dump(registration_map, f, indent=2)
            self.logger.info(f"Registration map saved to {filename}")
        except Exception as e:
            self.logger.error(f"Failed to save registration map: {e}")
    
    def load_registration_map(self, filename: str = 'registration_map.json') -> Dict[str, Any]:
        """Load the registration mapping from a file."""
        try:
            with open(filename, 'r') as f:
                registration_map = json.load(f)
            self.logger.info(f"Registration map loaded from {filename}")
            return registration_map
        except FileNotFoundError:
            self.logger.warning(f"Registration map file {filename} not found")
            return {'farms': {}, 'rooms': {}, 'devices': {}}
        except Exception as e:
            self.logger.error(f"Failed to load registration map: {e}")
            return {'farms': {}, 'rooms': {}, 'devices': {}}


def main():
    """Main entry point for device registration."""
    import argparse
    
    parser = argparse.ArgumentParser(description='Register simulated devices with backend API')
    parser.add_argument('--config', '-c', default='config.yaml',
                       help='Simulator configuration file (default: config.yaml)')
    parser.add_argument('--backend-url', '-u', default='http://localhost:5000',
                       help='Backend API URL (default: http://localhost:5000)')
    parser.add_argument('--auth-token', '-t',
                       help='Authentication token for API requests')
    parser.add_argument('--output', '-o', default='registration_map.json',
                       help='Output file for registration mapping (default: registration_map.json)')
    parser.add_argument('--dry-run', action='store_true',
                       help='Show what would be registered without actually doing it')
    
    args = parser.parse_args()
    
    # Setup logging
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    # Create simulator to get device information
    simulator = MQTTSimulator(args.config)
    
    if args.dry_run:
        print("\nDRY RUN - Would register the following:")
        simulator.print_device_info()
        return
    
    # Create registrar and register devices
    registrar = DeviceRegistrar(args.backend_url, args.auth_token)
    
    print(f"\nRegistering devices with backend at {args.backend_url}...")
    registration_map = registrar.register_all_from_simulator(simulator)
    
    # Save registration mapping
    registrar.save_registration_map(registration_map, args.output)
    
    # Print summary
    farms_count = len(registration_map['farms'])
    rooms_count = len(registration_map['rooms'])
    devices_count = len(registration_map['devices'])
    
    print(f"\nRegistration complete:")
    print(f"  Farms: {farms_count}")
    print(f"  Rooms: {rooms_count}")
    print(f"  Devices: {devices_count}")
    print(f"\nRegistration mapping saved to: {args.output}")


if __name__ == '__main__':
    main()