#!/usr/bin/env python3
"""
Test script to verify Flask backend setup and dependencies
"""

import sys
import os
import importlib
from datetime import datetime

def test_imports():
    """Test all critical imports"""
    print("Testing imports...")
    
    required_modules = [
        'flask',
        'flask_sqlalchemy',
        'flask_migrate',
        'flask_cors',
        'flask_jwt_extended',
        'flask_socketio',
        'flasgger',
        'paho.mqtt.client',
        'boto3',
        'celery',
        'redis',
        'marshmallow',
        'werkzeug'
    ]
    
    failed_imports = []
    
    for module in required_modules:
        try:
            importlib.import_module(module)
            print(f"✓ {module}")
        except ImportError as e:
            print(f"✗ {module} - {str(e)}")
            failed_imports.append(module)
    
    if failed_imports:
        print(f"\nFailed imports: {', '.join(failed_imports)}")
        print("Please install missing dependencies with: pip install -r requirements.txt")
        return False
    
    print("\nAll imports successful!")
    return True

def test_local_imports():
    """Test local module imports"""
    print("\nTesting local module imports...")
    
    # Add current directory to path
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    
    local_modules = [
        'config',
        'models',
        'auth',
        'mqtt_service',
        'bedrock_service',
        'api_routes',
        'tasks',
        'celery_app'
    ]
    
    failed_imports = []
    
    for module in local_modules:
        try:
            importlib.import_module(module)
            print(f"✓ {module}")
        except ImportError as e:
            print(f"✗ {module} - {str(e)}")
            failed_imports.append(module)
        except Exception as e:
            print(f"⚠ {module} - {str(e)} (may need environment setup)")
    
    if failed_imports:
        print(f"\nFailed local imports: {', '.join(failed_imports)}")
        return False
    
    print("\nLocal imports successful!")
    return True

def test_environment():
    """Test environment configuration"""
    print("\nTesting environment configuration...")
    
    # Check for .env file
    env_file = '.env'
    if os.path.exists(env_file):
        print(f"✓ {env_file} exists")
    else:
        print(f"⚠ {env_file} not found - copy from .env.example")
    
    # Check critical environment variables
    critical_vars = [
        'SECRET_KEY',
        'DATABASE_URL',
        'AWS_REGION'
    ]
    
    missing_vars = []
    for var in critical_vars:
        if os.environ.get(var):
            print(f"✓ {var} is set")
        else:
            print(f"⚠ {var} not set")
            missing_vars.append(var)
    
    if missing_vars:
        print(f"\nMissing environment variables: {', '.join(missing_vars)}")
        print("Please configure your .env file")
    
    return len(missing_vars) == 0

def test_app_creation():
    """Test Flask app creation"""
    print("\nTesting Flask app creation...")
    
    try:
        # Set minimal environment for testing
        os.environ.setdefault('SECRET_KEY', 'test-key')
        os.environ.setdefault('DATABASE_URL', 'sqlite:///test.db')
        os.environ.setdefault('AWS_REGION', 'us-east-1')
        os.environ.setdefault('FLASK_ENV', 'testing')
        
        from app import create_app
        app, socketio = create_app()
        
        print("✓ Flask app created successfully")
        print(f"✓ App name: {app.name}")
        print(f"✓ Debug mode: {app.debug}")
        print(f"✓ SocketIO initialized: {socketio is not None}")
        
        # Test app context
        with app.app_context():
            print("✓ App context works")
        
        return True
        
    except Exception as e:
        print(f"✗ App creation failed: {str(e)}")
        return False

def test_database_models():
    """Test database model definitions"""
    print("\nTesting database models...")
    
    try:
        from models import (
            User, Farm, Room, Device, SensorData, 
            Command, AutomationRule, Notification, Recommendation
        )
        
        models = [
            User, Farm, Room, Device, SensorData,
            Command, AutomationRule, Notification, Recommendation
        ]
        
        for model in models:
            print(f"✓ {model.__name__} model defined")
        
        print("✓ All database models loaded successfully")
        return True
        
    except Exception as e:
        print(f"✗ Model loading failed: {str(e)}")
        return False

def main():
    """Run all tests"""
    print("IoT Smart Farm Backend - Setup Verification")
    print("=" * 50)
    print(f"Test started at: {datetime.now()}")
    print(f"Python version: {sys.version}")
    print(f"Working directory: {os.getcwd()}")
    print("=" * 50)
    
    tests = [
        ("External Dependencies", test_imports),
        ("Local Modules", test_local_imports),
        ("Environment Configuration", test_environment),
        ("Database Models", test_database_models),
        ("Flask App Creation", test_app_creation)
    ]
    
    results = []
    
    for test_name, test_func in tests:
        print(f"\n{'='*20} {test_name} {'='*20}")
        try:
            result = test_func()
            results.append((test_name, result))
        except Exception as e:
            print(f"✗ Test failed with exception: {str(e)}")
            results.append((test_name, False))
    
    # Summary
    print("\n" + "="*50)
    print("TEST SUMMARY")
    print("="*50)
    
    passed = 0
    total = len(results)
    
    for test_name, result in results:
        status = "PASS" if result else "FAIL"
        icon = "✓" if result else "✗"
        print(f"{icon} {test_name}: {status}")
        if result:
            passed += 1
    
    print(f"\nResults: {passed}/{total} tests passed")
    
    if passed == total:
        print("\n🎉 All tests passed! Your Flask backend is ready to run.")
        print("\nNext steps:")
        print("1. Configure your .env file with actual values")
        print("2. Set up PostgreSQL database")
        print("3. Run database migrations: flask db upgrade")
        print("4. Start the application: python app.py")
    else:
        print(f"\n⚠ {total - passed} test(s) failed. Please fix the issues before running the application.")
    
    return passed == total

if __name__ == '__main__':
    success = main()
    sys.exit(0 if success else 1)