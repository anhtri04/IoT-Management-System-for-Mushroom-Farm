#!/usr/bin/env python3
"""
IoT Farm Simulator Installation Script

Automated setup script for the mushroom farm IoT simulator.
Handles dependency installation, configuration setup, and initial testing.
"""

import os
import sys
import subprocess
import shutil
import json
from pathlib import Path
from typing import List, Dict, Optional


class SimulatorInstaller:
    """Handles installation and setup of the IoT simulator."""
    
    def __init__(self):
        self.project_root = Path(__file__).parent
        self.python_executable = sys.executable
        self.errors = []
        self.warnings = []
    
    def print_header(self, title: str):
        """Print a formatted header."""
        print("\n" + "=" * 60)
        print(f" {title}")
        print("=" * 60)
    
    def print_step(self, step: str):
        """Print a step indicator."""
        print(f"\n[STEP] {step}")
    
    def print_success(self, message: str):
        """Print a success message."""
        print(f"✓ {message}")
    
    def print_warning(self, message: str):
        """Print a warning message."""
        print(f"⚠ WARNING: {message}")
        self.warnings.append(message)
    
    def print_error(self, message: str):
        """Print an error message."""
        print(f"✗ ERROR: {message}")
        self.errors.append(message)
    
    def run_command(self, command: List[str], description: str = None) -> bool:
        """Run a command and return success status."""
        if description:
            print(f"  Running: {description}")
        
        try:
            result = subprocess.run(
                command,
                capture_output=True,
                text=True,
                check=True
            )
            return True
        except subprocess.CalledProcessError as e:
            self.print_error(f"Command failed: {' '.join(command)}")
            if e.stdout:
                print(f"  STDOUT: {e.stdout}")
            if e.stderr:
                print(f"  STDERR: {e.stderr}")
            return False
        except FileNotFoundError:
            self.print_error(f"Command not found: {command[0]}")
            return False
    
    def check_python_version(self) -> bool:
        """Check if Python version is compatible."""
        self.print_step("Checking Python version")
        
        version = sys.version_info
        if version.major < 3 or (version.major == 3 and version.minor < 8):
            self.print_error(f"Python 3.8+ required, found {version.major}.{version.minor}")
            return False
        
        self.print_success(f"Python {version.major}.{version.minor}.{version.micro} is compatible")
        return True
    
    def check_pip(self) -> bool:
        """Check if pip is available."""
        self.print_step("Checking pip availability")
        
        try:
            result = subprocess.run(
                [self.python_executable, "-m", "pip", "--version"],
                capture_output=True,
                text=True,
                check=True
            )
            self.print_success(f"pip is available: {result.stdout.strip()}")
            return True
        except (subprocess.CalledProcessError, FileNotFoundError):
            self.print_error("pip is not available")
            return False
    
    def install_dependencies(self) -> bool:
        """Install Python dependencies."""
        self.print_step("Installing Python dependencies")
        
        requirements_file = self.project_root / "requirements.txt"
        if not requirements_file.exists():
            self.print_error(f"Requirements file not found: {requirements_file}")
            return False
        
        # Upgrade pip first
        if not self.run_command(
            [self.python_executable, "-m", "pip", "install", "--upgrade", "pip"],
            "Upgrading pip"
        ):
            self.print_warning("Failed to upgrade pip, continuing anyway")
        
        # Install requirements
        success = self.run_command(
            [self.python_executable, "-m", "pip", "install", "-r", str(requirements_file)],
            "Installing requirements"
        )
        
        if success:
            self.print_success("Dependencies installed successfully")
        else:
            self.print_error("Failed to install dependencies")
        
        return success
    
    def install_optional_dependencies(self) -> bool:
        """Install optional dependencies for analysis features."""
        self.print_step("Installing optional dependencies for data analysis")
        
        optional_packages = [
            "pandas",
            "matplotlib",
            "seaborn",
            "numpy"
        ]
        
        success = True
        for package in optional_packages:
            if not self.run_command(
                [self.python_executable, "-m", "pip", "install", package],
                f"Installing {package}"
            ):
                self.print_warning(f"Failed to install optional package: {package}")
                success = False
        
        if success:
            self.print_success("Optional dependencies installed successfully")
        else:
            self.print_warning("Some optional dependencies failed to install")
        
        return success
    
    def create_config_files(self) -> bool:
        """Create configuration files from templates."""
        self.print_step("Creating configuration files")
        
        success = True
        
        # Create .env file from template
        env_example = self.project_root / ".env.example"
        env_file = self.project_root / ".env"
        
        if env_example.exists() and not env_file.exists():
            try:
                shutil.copy2(env_example, env_file)
                self.print_success(f"Created .env file from template")
                self.print_warning("Please edit .env file with your AWS credentials and settings")
            except Exception as e:
                self.print_error(f"Failed to create .env file: {e}")
                success = False
        elif env_file.exists():
            self.print_success(".env file already exists")
        else:
            self.print_warning(".env.example template not found")
        
        # Check config.yaml
        config_file = self.project_root / "config.yaml"
        if config_file.exists():
            self.print_success("config.yaml file found")
        else:
            self.print_warning("config.yaml file not found - using default configuration")
        
        return success
    
    def create_directories(self) -> bool:
        """Create necessary directories."""
        self.print_step("Creating directories")
        
        directories = [
            "logs",
            "data",
            "certificates",
            "analysis_output"
        ]
        
        success = True
        for directory in directories:
            dir_path = self.project_root / directory
            try:
                dir_path.mkdir(exist_ok=True)
                self.print_success(f"Created directory: {directory}")
            except Exception as e:
                self.print_error(f"Failed to create directory {directory}: {e}")
                success = False
        
        return success
    
    def test_imports(self) -> bool:
        """Test if all required modules can be imported."""
        self.print_step("Testing module imports")
        
        required_modules = [
            "yaml",
            "requests",
            "jsonschema",
            "coloredlogs",
            "dateutil",
            "cryptography",
            "dotenv",
            "faker",
            "uuid",
            "threading"
        ]
        
        optional_modules = [
            "pandas",
            "matplotlib",
            "seaborn",
            "numpy"
        ]
        
        success = True
        
        # Test required modules
        for module in required_modules:
            try:
                __import__(module)
                self.print_success(f"✓ {module}")
            except ImportError:
                self.print_error(f"Required module not available: {module}")
                success = False
        
        # Test optional modules
        for module in optional_modules:
            try:
                __import__(module)
                self.print_success(f"✓ {module} (optional)")
            except ImportError:
                self.print_warning(f"Optional module not available: {module}")
        
        return success
    
    def test_simulator(self) -> bool:
        """Test if the simulator can be imported and basic functionality works."""
        self.print_step("Testing simulator functionality")
        
        try:
            # Test importing the main simulator module
            sys.path.insert(0, str(self.project_root))
            
            from simulator import MQTTSimulator, FarmSimulator, DeviceSimulator
            self.print_success("Simulator modules imported successfully")
            
            # Test basic configuration loading
            import yaml
            config_file = self.project_root / "config.yaml"
            if config_file.exists():
                with open(config_file, 'r') as f:
                    config = yaml.safe_load(f)
                self.print_success("Configuration file loaded successfully")
            
            # Test device simulator creation
            device_config = {
                'name': 'Test Device',
                'device_type': 'sensor',
                'category': 'temperature',
                'sensors': {
                    'temperature_c': {
                        'min': 18.0,
                        'max': 28.0,
                        'optimal': 22.0,
                        'variance': 2.0
                    }
                }
            }
            device = DeviceSimulator(
                device_config=device_config,
                room_id="test-room",
                farm_id="test-farm"
            )
            telemetry = device.generate_telemetry()
            if telemetry and 'timestamp' in telemetry:
                self.print_success("Device simulator test passed")
            else:
                self.print_error("Device simulator test failed")
                return False
            
            return True
            
        except Exception as e:
            self.print_error(f"Simulator test failed: {e}")
            return False
    
    def print_next_steps(self):
        """Print next steps for the user."""
        self.print_header("NEXT STEPS")
        
        print("1. Configure your settings:")
        print("   - Edit .env file with your AWS IoT Core credentials")
        print("   - Modify config.yaml if needed")
        print("")
        print("2. Test the simulator:")
        print("   python simulator.py --demo")
        print("")
        print("3. Run with AWS IoT Core:")
        print("   python simulator.py --farms 2 --devices-per-room 3")
        print("")
        print("4. Analyze generated data:")
        print("   python scripts/analyze_data.py --sample-data")
        print("")
        print("5. Register devices with backend:")
        print("   python device_registration.py")
        print("")
        print("For more information, see README.md")
    
    def print_summary(self):
        """Print installation summary."""
        self.print_header("INSTALLATION SUMMARY")
        
        if not self.errors:
            print("✓ Installation completed successfully!")
        else:
            print(f"✗ Installation completed with {len(self.errors)} error(s):")
            for error in self.errors:
                print(f"  - {error}")
        
        if self.warnings:
            print(f"\n⚠ {len(self.warnings)} warning(s):")
            for warning in self.warnings:
                print(f"  - {warning}")
    
    def install(self, skip_optional: bool = False) -> bool:
        """Run the complete installation process."""
        self.print_header("IoT FARM SIMULATOR INSTALLATION")
        
        steps = [
            self.check_python_version,
            self.check_pip,
            self.install_dependencies,
            self.create_directories,
            self.create_config_files,
            self.test_imports,
            self.test_simulator
        ]
        
        if not skip_optional:
            steps.insert(3, self.install_optional_dependencies)
        
        success = True
        for step in steps:
            if not step():
                success = False
                # Continue with other steps even if one fails
        
        self.print_summary()
        
        if success:
            self.print_next_steps()
        
        return success


def main():
    import argparse
    
    parser = argparse.ArgumentParser(description='Install IoT Farm Simulator')
    parser.add_argument('--skip-optional', action='store_true',
                       help='Skip installation of optional dependencies')
    parser.add_argument('--test-only', action='store_true',
                       help='Only run tests, skip installation')
    
    args = parser.parse_args()
    
    installer = SimulatorInstaller()
    
    if args.test_only:
        # Only run tests
        installer.print_header("TESTING SIMULATOR")
        success = installer.test_imports() and installer.test_simulator()
        installer.print_summary()
    else:
        # Run full installation
        success = installer.install(skip_optional=args.skip_optional)
    
    return 0 if success else 1


if __name__ == '__main__':
    sys.exit(main())