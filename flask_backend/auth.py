import jwt
import requests
from functools import wraps
from flask import request, jsonify, current_app, g
from flask_jwt_extended import JWTManager, verify_jwt_in_request, get_jwt_identity, get_jwt
import boto3
from botocore.exceptions import ClientError
from models import User, UserRoom, db
import json
from datetime import datetime

# Initialize JWT manager
jwt_manager = JWTManager()

class CognitoAuth:
    """AWS Cognito authentication handler"""
    
    def __init__(self, app=None):
        self.app = app
        self.cognito_client = None
        self.jwks = None
        
        if app is not None:
            self.init_app(app)
    
    def init_app(self, app):
        """Initialize Cognito authentication"""
        self.app = app
        
        # Initialize Cognito client
        try:
            self.cognito_client = boto3.client(
                'cognito-idp',
                region_name=app.config['COGNITO_REGION'],
                aws_access_key_id=app.config.get('AWS_ACCESS_KEY_ID'),
                aws_secret_access_key=app.config.get('AWS_SECRET_ACCESS_KEY')
            )
        except Exception as e:
            app.logger.error(f"Failed to initialize Cognito client: {e}")
        
        # Fetch JWKS for token verification
        self._fetch_jwks()
    
    def _fetch_jwks(self):
        """Fetch JSON Web Key Set from Cognito"""
        try:
            region = self.app.config['COGNITO_REGION']
            user_pool_id = self.app.config['COGNITO_USER_POOL_ID']
            jwks_url = f"https://cognito-idp.{region}.amazonaws.com/{user_pool_id}/.well-known/jwks.json"
            
            response = requests.get(jwks_url)
            response.raise_for_status()
            self.jwks = response.json()
            
        except Exception as e:
            self.app.logger.error(f"Failed to fetch JWKS: {e}")
            self.jwks = None
    
    def verify_token(self, token):
        """Verify Cognito JWT token"""
        try:
            if not self.jwks:
                self._fetch_jwks()
            
            # Decode token header to get kid
            unverified_header = jwt.get_unverified_header(token)
            kid = unverified_header['kid']
            
            # Find the correct key
            key = None
            for jwk in self.jwks['keys']:
                if jwk['kid'] == kid:
                    key = jwt.algorithms.RSAAlgorithm.from_jwk(json.dumps(jwk))
                    break
            
            if not key:
                raise ValueError("Unable to find appropriate key")
            
            # Verify token
            payload = jwt.decode(
                token,
                key,
                algorithms=['RS256'],
                audience=self.app.config['COGNITO_CLIENT_ID'],
                issuer=f"https://cognito-idp.{self.app.config['COGNITO_REGION']}.amazonaws.com/{self.app.config['COGNITO_USER_POOL_ID']}"
            )
            
            return payload
            
        except Exception as e:
            self.app.logger.error(f"Token verification failed: {e}")
            return None
    
    def create_user(self, email, password, full_name=None):
        """Create user in Cognito"""
        try:
            response = self.cognito_client.admin_create_user(
                UserPoolId=self.app.config['COGNITO_USER_POOL_ID'],
                Username=email,
                UserAttributes=[
                    {'Name': 'email', 'Value': email},
                    {'Name': 'email_verified', 'Value': 'true'}
                ] + ([{'Name': 'name', 'Value': full_name}] if full_name else []),
                TemporaryPassword=password,
                MessageAction='SUPPRESS'
            )
            
            # Set permanent password
            self.cognito_client.admin_set_user_password(
                UserPoolId=self.app.config['COGNITO_USER_POOL_ID'],
                Username=email,
                Password=password,
                Permanent=True
            )
            
            return response
            
        except ClientError as e:
            self.app.logger.error(f"Failed to create Cognito user: {e}")
            raise e
    
    def authenticate_user(self, email, password):
        """Authenticate user with Cognito"""
        try:
            response = self.cognito_client.admin_initiate_auth(
                UserPoolId=self.app.config['COGNITO_USER_POOL_ID'],
                ClientId=self.app.config['COGNITO_CLIENT_ID'],
                AuthFlow='ADMIN_NO_SRP_AUTH',
                AuthParameters={
                    'USERNAME': email,
                    'PASSWORD': password
                }
            )
            
            return response['AuthenticationResult']
            
        except ClientError as e:
            self.app.logger.error(f"Authentication failed: {e}")
            return None

# Initialize Cognito auth
cognito_auth = CognitoAuth()

def require_auth(f):
    """Decorator to require authentication"""
    @wraps(f)
    def decorated_function(*args, **kwargs):
        # Check for Authorization header
        auth_header = request.headers.get('Authorization')
        if not auth_header or not auth_header.startswith('Bearer '):
            return jsonify({'error': 'Missing or invalid authorization header'}), 401
        
        token = auth_header.split(' ')[1]
        
        # Verify token
        payload = cognito_auth.verify_token(token)
        if not payload:
            return jsonify({'error': 'Invalid or expired token'}), 401
        
        # Get or create user in database
        cognito_sub = payload.get('sub')
        email = payload.get('email')
        
        user = User.query.filter_by(cognito_sub=cognito_sub).first()
        if not user:
            # Create user if doesn't exist
            user = User(
                cognito_sub=cognito_sub,
                email=email,
                full_name=payload.get('name')
            )
            db.session.add(user)
            db.session.commit()
        
        # Store user in Flask g object
        g.current_user = user
        g.token_payload = payload
        
        return f(*args, **kwargs)
    
    return decorated_function

def require_internal_auth(f):
    """Decorator to require internal API authentication"""
    @wraps(f)
    def decorated_function(*args, **kwargs):
        # Check for internal API token
        auth_header = request.headers.get('Authorization')
        if not auth_header or not auth_header.startswith('Internal '):
            return jsonify({'error': 'Missing or invalid internal authorization'}), 401
        
        token = auth_header.split(' ')[1]
        
        if token != current_app.config['INTERNAL_API_TOKEN']:
            return jsonify({'error': 'Invalid internal token'}), 401
        
        return f(*args, **kwargs)
    
    return decorated_function

def require_role(required_role):
    """Decorator to require specific user role"""
    def decorator(f):
        @wraps(f)
        def decorated_function(*args, **kwargs):
            if not hasattr(g, 'current_user'):
                return jsonify({'error': 'Authentication required'}), 401
            
            user_role = g.current_user.role
            
            # Role hierarchy: admin > manager > viewer
            role_hierarchy = {'admin': 3, 'manager': 2, 'viewer': 1}
            
            if role_hierarchy.get(user_role, 0) < role_hierarchy.get(required_role, 0):
                return jsonify({'error': 'Insufficient permissions'}), 403
            
            return f(*args, **kwargs)
        
        return decorated_function
    return decorator

def require_room_access(room_id_param='room_id', required_role='viewer'):
    """Decorator to require access to specific room"""
    def decorator(f):
        @wraps(f)
        def decorated_function(*args, **kwargs):
            if not hasattr(g, 'current_user'):
                return jsonify({'error': 'Authentication required'}), 401
            
            # Get room_id from URL parameters or request data
            room_id = kwargs.get(room_id_param) or request.json.get(room_id_param)
            
            if not room_id:
                return jsonify({'error': 'Room ID required'}), 400
            
            # Check if user has access to this room
            user_room = UserRoom.query.filter_by(
                user_id=g.current_user.user_id,
                room_id=room_id
            ).first()
            
            if not user_room:
                # Check if user is admin (has access to all rooms)
                if g.current_user.role != 'admin':
                    return jsonify({'error': 'Access denied to this room'}), 403
            else:
                # Check role hierarchy
                role_hierarchy = {'owner': 3, 'operator': 2, 'viewer': 1}
                
                if role_hierarchy.get(user_room.role, 0) < role_hierarchy.get(required_role, 0):
                    return jsonify({'error': 'Insufficient room permissions'}), 403
            
            # Store room access info in g
            g.room_access = user_room
            
            return f(*args, **kwargs)
        
        return decorated_function
    return decorator

def get_user_accessible_rooms(user_id):
    """Get list of rooms accessible to user"""
    user_rooms = UserRoom.query.filter_by(user_id=user_id).all()
    return [ur.room_id for ur in user_rooms]

def check_farm_access(user_id, farm_id):
    """Check if user has access to farm through any room"""
    from models import Room
    
    user_room_ids = get_user_accessible_rooms(user_id)
    
    # Check if any accessible room belongs to this farm
    accessible_farm_rooms = Room.query.filter(
        Room.room_id.in_(user_room_ids),
        Room.farm_id == farm_id
    ).first()
    
    return accessible_farm_rooms is not None

def get_user_accessible_farms(user_id):
    """Get list of farms accessible to user"""
    from models import Room, Farm
    
    user_room_ids = get_user_accessible_rooms(user_id)
    
    # Get farms that contain accessible rooms
    accessible_farms = db.session.query(Farm).join(Room).filter(
        Room.room_id.in_(user_room_ids)
    ).distinct().all()
    
    return accessible_farms

# JWT token blacklist (for logout functionality)
blacklisted_tokens = set()

@jwt_manager.token_in_blocklist_loader
def check_if_token_revoked(jwt_header, jwt_payload):
    """Check if JWT token is blacklisted"""
    jti = jwt_payload['jti']
    return jti in blacklisted_tokens

def revoke_token(jti):
    """Add token to blacklist"""
    blacklisted_tokens.add(jti)

# Error handlers
@jwt_manager.expired_token_loader
def expired_token_callback(jwt_header, jwt_payload):
    return jsonify({'error': 'Token has expired'}), 401

@jwt_manager.invalid_token_loader
def invalid_token_callback(error):
    return jsonify({'error': 'Invalid token'}), 401

@jwt_manager.unauthorized_loader
def missing_token_callback(error):
    return jsonify({'error': 'Authorization token required'}), 401