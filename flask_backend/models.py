from flask_sqlalchemy import SQLAlchemy
from sqlalchemy import func, Index, String, Text
from sqlalchemy.dialects.postgresql import UUID as PostgresUUID, JSONB as PostgresJSONB
from sqlalchemy.types import TypeDecorator, CHAR
from datetime import datetime
import uuid
import json

# Custom UUID type that works with both SQLite and PostgreSQL
class UUID(TypeDecorator):
    impl = CHAR
    cache_ok = True
    
    def __init__(self, as_uuid=True, *args, **kwargs):
        self.as_uuid = as_uuid
        super(UUID, self).__init__(*args, **kwargs)
    
    def load_dialect_impl(self, dialect):
        if dialect.name == 'postgresql':
            return dialect.type_descriptor(PostgresUUID(as_uuid=self.as_uuid))
        else:
            return dialect.type_descriptor(CHAR(36))
    
    def process_bind_param(self, value, dialect):
        if value is None:
            return value
        elif dialect.name == 'postgresql':
            return str(value)
        else:
            if not isinstance(value, uuid.UUID):
                return str(value)
            else:
                return str(value)
    
    def process_result_value(self, value, dialect):
        if value is None:
            return value
        else:
            if not isinstance(value, uuid.UUID):
                try:
                    return uuid.UUID(value)
                except (ValueError, TypeError):
                    return value
            return value

# Custom JSONB type that works with both SQLite and PostgreSQL
class JSONB(TypeDecorator):
    impl = Text
    cache_ok = True
    
    def load_dialect_impl(self, dialect):
        if dialect.name == 'postgresql':
            return dialect.type_descriptor(PostgresJSONB())
        else:
            return dialect.type_descriptor(Text())
    
    def process_bind_param(self, value, dialect):
        if value is None:
            return value
        elif dialect.name == 'postgresql':
            return value
        else:
            return json.dumps(value)
    
    def process_result_value(self, value, dialect):
        if value is None:
            return value
        elif dialect.name == 'postgresql':
            return value
        else:
            return json.loads(value)

db = SQLAlchemy()

class User(db.Model):
    """User model for authentication and authorization"""
    __tablename__ = 'users'
    
    user_id = db.Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    cognito_sub = db.Column(db.String(255), unique=True, nullable=False)
    email = db.Column(db.String(255), unique=True, nullable=False)
    full_name = db.Column(db.String(200))
    role = db.Column(db.String(20), nullable=False, default='viewer')  # admin, manager, viewer
    created_at = db.Column(db.DateTime(timezone=True), default=func.now())
    
    # Relationships
    owned_farms = db.relationship('Farm', backref='owner', lazy=True)
    user_rooms = db.relationship('UserRoom', backref='user', lazy=True, cascade='all, delete-orphan')
    issued_commands = db.relationship('Command', backref='issuer', lazy=True)
    created_rules = db.relationship('AutomationRule', backref='creator', lazy=True)
    acknowledged_notifications = db.relationship('Notification', backref='acknowledger', lazy=True)
    
    def __repr__(self):
        return f'<User {self.email}>'
    
    def to_dict(self):
        return {
            'user_id': str(self.user_id),
            'email': self.email,
            'full_name': self.full_name,
            'role': self.role,
            'created_at': self.created_at.isoformat() if self.created_at else None
        }

class Farm(db.Model):
    """Farm model representing top-level grouping"""
    __tablename__ = 'farms'
    
    farm_id = db.Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    owner_id = db.Column(UUID(as_uuid=True), db.ForeignKey('users.user_id', ondelete='SET NULL'))
    name = db.Column(db.String(200), nullable=False)
    location = db.Column(db.Text)
    created_at = db.Column(db.DateTime(timezone=True), default=func.now())
    
    # Relationships
    rooms = db.relationship('Room', backref='farm', lazy=True, cascade='all, delete-orphan')
    notifications = db.relationship('Notification', backref='farm', lazy=True)
    recommendations = db.relationship('Recommendation', backref='farm', lazy=True)
    
    def __repr__(self):
        return f'<Farm {self.name}>'
    
    def to_dict(self):
        return {
            'farm_id': str(self.farm_id),
            'owner_id': str(self.owner_id) if self.owner_id else None,
            'name': self.name,
            'location': self.location,
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'rooms_count': len(self.rooms)
        }

class Room(db.Model):
    """Room model representing farm zones/blocks/houses"""
    __tablename__ = 'rooms'
    
    room_id = db.Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    farm_id = db.Column(UUID(as_uuid=True), db.ForeignKey('farms.farm_id', ondelete='CASCADE'), nullable=False)
    name = db.Column(db.String(200), nullable=False)
    description = db.Column(db.Text)
    mushroom_type = db.Column(db.String(100))
    stage = db.Column(db.String(50))  # incubation, fruiting, maintenance
    created_at = db.Column(db.DateTime(timezone=True), default=func.now())
    
    # Relationships
    devices = db.relationship('Device', backref='room', lazy=True, cascade='all, delete-orphan')
    user_rooms = db.relationship('UserRoom', backref='room', lazy=True, cascade='all, delete-orphan')
    sensor_data = db.relationship('SensorData', backref='room', lazy=True)
    commands = db.relationship('Command', backref='room', lazy=True)
    automation_rules = db.relationship('AutomationRule', backref='room', lazy=True)
    farming_cycles = db.relationship('FarmingCycle', backref='room', lazy=True)
    notifications = db.relationship('Notification', backref='room', lazy=True)
    recommendations = db.relationship('Recommendation', backref='room', lazy=True)
    
    __table_args__ = (
        Index('idx_rooms_farm', 'farm_id'),
    )
    
    def __repr__(self):
        return f'<Room {self.name}>'
    
    def to_dict(self):
        return {
            'room_id': str(self.room_id),
            'farm_id': str(self.farm_id),
            'name': self.name,
            'description': self.description,
            'mushroom_type': self.mushroom_type,
            'stage': self.stage,
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'devices_count': len(self.devices)
        }

class UserRoom(db.Model):
    """User-Room mapping for access control"""
    __tablename__ = 'user_rooms'
    
    user_id = db.Column(UUID(as_uuid=True), db.ForeignKey('users.user_id', ondelete='CASCADE'), primary_key=True)
    room_id = db.Column(UUID(as_uuid=True), db.ForeignKey('rooms.room_id', ondelete='CASCADE'), primary_key=True)
    role = db.Column(db.String(20), default='operator')  # owner, operator, viewer
    assigned_at = db.Column(db.DateTime(timezone=True), default=func.now())
    
    def __repr__(self):
        return f'<UserRoom {self.user_id}-{self.room_id}>'
    
    def to_dict(self):
        return {
            'user_id': str(self.user_id),
            'room_id': str(self.room_id),
            'role': self.role,
            'assigned_at': self.assigned_at.isoformat() if self.assigned_at else None
        }

class Device(db.Model):
    """Device model for IoT devices"""
    __tablename__ = 'devices'
    
    device_id = db.Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    room_id = db.Column(UUID(as_uuid=True), db.ForeignKey('rooms.room_id', ondelete='CASCADE'), nullable=False)
    name = db.Column(db.String(200), nullable=False)
    device_type = db.Column(db.String(50))  # sensor, actuator, hybrid
    category = db.Column(db.String(50))  # temperature, humidity, co2, light, fan, humidifier
    mqtt_topic = db.Column(db.String(512), unique=True, nullable=False)
    status = db.Column(db.String(20), default='offline')
    last_seen = db.Column(db.DateTime(timezone=True))
    firmware_version = db.Column(db.String(50))
    created_at = db.Column(db.DateTime(timezone=True), default=func.now())
    
    # Relationships
    sensor_data = db.relationship('SensorData', backref='device', lazy=True)
    commands = db.relationship('Command', backref='device', lazy=True)
    automation_rules = db.relationship('AutomationRule', backref='action_device_ref', lazy=True)
    notifications = db.relationship('Notification', backref='device', lazy=True)
    
    __table_args__ = (
        Index('idx_devices_room', 'room_id'),
    )
    
    def __repr__(self):
        return f'<Device {self.name}>'
    
    def to_dict(self):
        return {
            'device_id': str(self.device_id),
            'room_id': str(self.room_id),
            'name': self.name,
            'device_type': self.device_type,
            'category': self.category,
            'mqtt_topic': self.mqtt_topic,
            'status': self.status,
            'last_seen': self.last_seen.isoformat() if self.last_seen else None,
            'firmware_version': self.firmware_version,
            'created_at': self.created_at.isoformat() if self.created_at else None
        }

class SensorData(db.Model):
    """Time-series sensor data model"""
    __tablename__ = 'sensor_data'
    
    reading_id = db.Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    device_id = db.Column(UUID(as_uuid=True), db.ForeignKey('devices.device_id', ondelete='CASCADE'), nullable=False)
    room_id = db.Column(UUID(as_uuid=True), db.ForeignKey('rooms.room_id', ondelete='CASCADE'), nullable=False)
    farm_id = db.Column(UUID(as_uuid=True), db.ForeignKey('farms.farm_id', ondelete='CASCADE'), nullable=False)
    temperature_c = db.Column(db.Float)
    humidity_pct = db.Column(db.Float)
    co2_ppm = db.Column(db.Float)
    light_lux = db.Column(db.Float)
    substrate_moisture = db.Column(db.Float)
    battery_v = db.Column(db.Float)
    recorded_at = db.Column(db.DateTime(timezone=True), nullable=False, default=func.now())
    
    __table_args__ = (
        Index('idx_sensor_room_time', 'room_id', 'recorded_at'),
        Index('idx_sensor_device_time', 'device_id', 'recorded_at'),
    )
    
    def __repr__(self):
        return f'<SensorData {self.device_id} at {self.recorded_at}>'
    
    def to_dict(self):
        return {
            'reading_id': str(self.reading_id),
            'device_id': str(self.device_id),
            'room_id': str(self.room_id),
            'farm_id': str(self.farm_id),
            'temperature_c': self.temperature_c,
            'humidity_pct': self.humidity_pct,
            'co2_ppm': self.co2_ppm,
            'light_lux': self.light_lux,
            'substrate_moisture': self.substrate_moisture,
            'battery_v': self.battery_v,
            'recorded_at': self.recorded_at.isoformat() if self.recorded_at else None
        }

class Command(db.Model):
    """Command model for device control history"""
    __tablename__ = 'commands'
    
    command_id = db.Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    device_id = db.Column(UUID(as_uuid=True), db.ForeignKey('devices.device_id', ondelete='CASCADE'), nullable=False)
    room_id = db.Column(UUID(as_uuid=True), db.ForeignKey('rooms.room_id'))
    farm_id = db.Column(UUID(as_uuid=True), db.ForeignKey('farms.farm_id'))
    command = db.Column(db.Text, nullable=False)
    params = db.Column(JSONB)
    issued_by = db.Column(UUID(as_uuid=True), db.ForeignKey('users.user_id'))
    issued_at = db.Column(db.DateTime(timezone=True), default=func.now())
    status = db.Column(db.String(30), default='pending')  # pending, sent, acked, failed
    
    __table_args__ = (
        Index('idx_commands_device_time', 'device_id', 'issued_at'),
    )
    
    def __repr__(self):
        return f'<Command {self.command} for {self.device_id}>'
    
    def to_dict(self):
        return {
            'command_id': str(self.command_id),
            'device_id': str(self.device_id),
            'room_id': str(self.room_id) if self.room_id else None,
            'farm_id': str(self.farm_id) if self.farm_id else None,
            'command': self.command,
            'params': self.params,
            'issued_by': str(self.issued_by) if self.issued_by else None,
            'issued_at': self.issued_at.isoformat() if self.issued_at else None,
            'status': self.status
        }

class AutomationRule(db.Model):
    """Automation rules for automatic device control"""
    __tablename__ = 'automation_rules'
    
    rule_id = db.Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    room_id = db.Column(UUID(as_uuid=True), db.ForeignKey('rooms.room_id'))
    name = db.Column(db.String(200))
    parameter = db.Column(db.String(50))  # temperature, humidity, co2, light
    comparator = db.Column(db.String(2))  # <, >, <=, >=, ==
    threshold = db.Column(db.Float)
    action_device = db.Column(UUID(as_uuid=True), db.ForeignKey('devices.device_id'))
    action_command = db.Column(db.Text)  # JSON string
    enabled = db.Column(db.Boolean, default=True)
    created_by = db.Column(UUID(as_uuid=True), db.ForeignKey('users.user_id'))
    created_at = db.Column(db.DateTime(timezone=True), default=func.now())
    
    def __repr__(self):
        return f'<AutomationRule {self.name}>'
    
    def to_dict(self):
        return {
            'rule_id': str(self.rule_id),
            'room_id': str(self.room_id) if self.room_id else None,
            'name': self.name,
            'parameter': self.parameter,
            'comparator': self.comparator,
            'threshold': self.threshold,
            'action_device': str(self.action_device) if self.action_device else None,
            'action_command': self.action_command,
            'enabled': self.enabled,
            'created_by': str(self.created_by) if self.created_by else None,
            'created_at': self.created_at.isoformat() if self.created_at else None
        }

class FarmingCycle(db.Model):
    """Farming cycles/batches model"""
    __tablename__ = 'farming_cycles'
    
    cycle_id = db.Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    room_id = db.Column(UUID(as_uuid=True), db.ForeignKey('rooms.room_id'))
    start_date = db.Column(db.Date, nullable=False)
    expected_harvest_date = db.Column(db.Date)
    status = db.Column(db.String(50), default='growing')  # growing, harvested, aborted
    mushroom_variety = db.Column(db.String(200))
    notes = db.Column(db.Text)
    created_at = db.Column(db.DateTime(timezone=True), default=func.now())
    
    def __repr__(self):
        return f'<FarmingCycle {self.mushroom_variety} in {self.room_id}>'
    
    def to_dict(self):
        return {
            'cycle_id': str(self.cycle_id),
            'room_id': str(self.room_id) if self.room_id else None,
            'start_date': self.start_date.isoformat() if self.start_date else None,
            'expected_harvest_date': self.expected_harvest_date.isoformat() if self.expected_harvest_date else None,
            'status': self.status,
            'mushroom_variety': self.mushroom_variety,
            'notes': self.notes,
            'created_at': self.created_at.isoformat() if self.created_at else None
        }

class Notification(db.Model):
    """Notifications and alerts model"""
    __tablename__ = 'notifications'
    
    notification_id = db.Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    farm_id = db.Column(UUID(as_uuid=True), db.ForeignKey('farms.farm_id'))
    room_id = db.Column(UUID(as_uuid=True), db.ForeignKey('rooms.room_id'))
    device_id = db.Column(UUID(as_uuid=True), db.ForeignKey('devices.device_id'))
    level = db.Column(db.String(20))  # info, warning, critical
    message = db.Column(db.Text, nullable=False)
    created_at = db.Column(db.DateTime(timezone=True), default=func.now())
    acknowledged_by = db.Column(UUID(as_uuid=True), db.ForeignKey('users.user_id'))
    acknowledged_at = db.Column(db.DateTime(timezone=True))
    
    def __repr__(self):
        return f'<Notification {self.level}: {self.message[:50]}>'
    
    def to_dict(self):
        return {
            'notification_id': str(self.notification_id),
            'farm_id': str(self.farm_id) if self.farm_id else None,
            'room_id': str(self.room_id) if self.room_id else None,
            'device_id': str(self.device_id) if self.device_id else None,
            'level': self.level,
            'message': self.message,
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'acknowledged_by': str(self.acknowledged_by) if self.acknowledged_by else None,
            'acknowledged_at': self.acknowledged_at.isoformat() if self.acknowledged_at else None
        }

class Recommendation(db.Model):
    """AI recommendations from Bedrock model"""
    __tablename__ = 'recommendations'
    
    rec_id = db.Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    farm_id = db.Column(UUID(as_uuid=True), db.ForeignKey('farms.farm_id'))
    room_id = db.Column(UUID(as_uuid=True), db.ForeignKey('rooms.room_id'))
    payload = db.Column(JSONB, nullable=False)  # Model output JSON
    confidence = db.Column(db.Float)
    model_id = db.Column(db.String(200))
    created_at = db.Column(db.DateTime(timezone=True), default=func.now())
    
    def __repr__(self):
        return f'<Recommendation {self.model_id} for {self.room_id}>'
    
    def to_dict(self):
        return {
            'rec_id': str(self.rec_id),
            'farm_id': str(self.farm_id) if self.farm_id else None,
            'room_id': str(self.room_id) if self.room_id else None,
            'payload': self.payload,
            'confidence': self.confidence,
            'model_id': self.model_id,
            'created_at': self.created_at.isoformat() if self.created_at else None
        }