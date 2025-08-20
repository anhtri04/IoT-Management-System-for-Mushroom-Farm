-- IoT Smart Farm Database Schema
-- Migration V1: Create initial schema with all required tables

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Users table
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cognito_sub VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(200),
    role VARCHAR(20) NOT NULL DEFAULT 'viewer', -- admin, manager, viewer
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_users_cognito_sub ON users(cognito_sub);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- Farms table
CREATE TABLE farms (
    farm_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    name VARCHAR(200) NOT NULL,
    location TEXT,
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_farms_owner ON farms(owner_id);
CREATE INDEX idx_farms_name ON farms(name);

-- Rooms (farm zones / blocks) table
CREATE TABLE rooms (
    room_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    farm_id UUID REFERENCES farms(farm_id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    mushroom_type VARCHAR(100),
    stage VARCHAR(50), -- incubation, fruiting, maintenance
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_rooms_farm ON rooms(farm_id);
CREATE INDEX idx_rooms_stage ON rooms(stage);
CREATE INDEX idx_rooms_mushroom_type ON rooms(mushroom_type);

-- User-room mapping (which rooms a user can access)
CREATE TABLE user_rooms (
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    room_id UUID REFERENCES rooms(room_id) ON DELETE CASCADE,
    role VARCHAR(20) DEFAULT 'operator', -- owner, operator, viewer
    assigned_at TIMESTAMPTZ DEFAULT now(),
    PRIMARY KEY (user_id, room_id)
);

CREATE INDEX idx_user_rooms_user ON user_rooms(user_id);
CREATE INDEX idx_user_rooms_room ON user_rooms(room_id);

-- Devices table
CREATE TABLE devices (
    device_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id UUID REFERENCES rooms(room_id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    device_type VARCHAR(50), -- sensor, actuator, hybrid
    category VARCHAR(50), -- temperature, humidity, co2, light, fan, humidifier
    mqtt_topic VARCHAR(512) UNIQUE NOT NULL,
    status VARCHAR(20) DEFAULT 'offline', -- online, offline, error
    last_seen TIMESTAMPTZ,
    firmware_version VARCHAR(50),
    battery_level REAL,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_devices_room ON devices(room_id);
CREATE INDEX idx_devices_type ON devices(device_type);
CREATE INDEX idx_devices_category ON devices(category);
CREATE INDEX idx_devices_status ON devices(status);
CREATE UNIQUE INDEX idx_devices_mqtt_topic ON devices(mqtt_topic);

-- Sensor data (time-series) table
CREATE TABLE sensor_data (
    reading_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID REFERENCES devices(device_id) ON DELETE CASCADE,
    room_id UUID REFERENCES rooms(room_id) ON DELETE CASCADE,
    farm_id UUID REFERENCES farms(farm_id) ON DELETE CASCADE,
    temperature_c REAL,
    humidity_pct REAL,
    co2_ppm REAL,
    light_lux REAL,
    ph_level REAL,
    substrate_moisture REAL,
    battery_v REAL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Optimized indexes for time-series queries
CREATE INDEX idx_sensor_room_time ON sensor_data(room_id, recorded_at DESC);
CREATE INDEX idx_sensor_device_time ON sensor_data(device_id, recorded_at DESC);
CREATE INDEX idx_sensor_farm_time ON sensor_data(farm_id, recorded_at DESC);
CREATE INDEX idx_sensor_recorded_at ON sensor_data(recorded_at DESC);

-- Commands (control history & pending commands) table
CREATE TABLE commands (
    command_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID REFERENCES devices(device_id) ON DELETE CASCADE,
    room_id UUID REFERENCES rooms(room_id),
    farm_id UUID REFERENCES farms(farm_id),
    command TEXT NOT NULL,
    params JSONB,
    issued_by UUID REFERENCES users(user_id),
    issued_at TIMESTAMPTZ DEFAULT now(),
    executed_at TIMESTAMPTZ,
    status VARCHAR(30) DEFAULT 'pending', -- pending, sent, acked, failed, timeout
    response JSONB,
    error_message TEXT
);

CREATE INDEX idx_commands_device_time ON commands(device_id, issued_at DESC);
CREATE INDEX idx_commands_room_time ON commands(room_id, issued_at DESC);
CREATE INDEX idx_commands_status ON commands(status);
CREATE INDEX idx_commands_issued_by ON commands(issued_by);

-- Automation rules table
CREATE TABLE automation_rules (
    rule_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id UUID REFERENCES rooms(room_id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    parameter VARCHAR(50) NOT NULL, -- temperature, humidity, co2, light, ph, moisture
    comparator VARCHAR(2) NOT NULL, -- <, >, <=, >=, ==, !=
    threshold REAL NOT NULL,
    action_device UUID REFERENCES devices(device_id),
    action_command TEXT NOT NULL, -- e.g., {"cmd":"turn_on","duration_s":300}
    enabled BOOLEAN DEFAULT TRUE,
    priority INTEGER DEFAULT 1, -- 1=highest, 10=lowest
    cooldown_minutes INTEGER DEFAULT 5, -- minimum time between rule executions
    last_triggered TIMESTAMPTZ,
    created_by UUID REFERENCES users(user_id),
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_automation_rules_room ON automation_rules(room_id);
CREATE INDEX idx_automation_rules_enabled ON automation_rules(enabled);
CREATE INDEX idx_automation_rules_parameter ON automation_rules(parameter);
CREATE INDEX idx_automation_rules_priority ON automation_rules(priority);

-- Farming cycles (batches) table
CREATE TABLE farming_cycles (
    cycle_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id UUID REFERENCES rooms(room_id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    start_date DATE NOT NULL,
    expected_harvest_date DATE,
    actual_harvest_date DATE,
    status VARCHAR(50) DEFAULT 'growing', -- planning, growing, harvesting, harvested, aborted
    mushroom_variety VARCHAR(200),
    substrate_type VARCHAR(100),
    inoculation_date DATE,
    notes TEXT,
    yield_kg REAL,
    created_by UUID REFERENCES users(user_id),
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_farming_cycles_room ON farming_cycles(room_id);
CREATE INDEX idx_farming_cycles_status ON farming_cycles(status);
CREATE INDEX idx_farming_cycles_start_date ON farming_cycles(start_date);
CREATE INDEX idx_farming_cycles_variety ON farming_cycles(mushroom_variety);

-- Notifications / alerts table
CREATE TABLE notifications (
    notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    farm_id UUID REFERENCES farms(farm_id) ON DELETE CASCADE,
    room_id UUID REFERENCES rooms(room_id) ON DELETE CASCADE,
    device_id UUID REFERENCES devices(device_id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    level VARCHAR(20) NOT NULL, -- info, warning, critical, error
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    category VARCHAR(50), -- system, device, automation, security, maintenance
    read_status BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT now(),
    acknowledged_by UUID REFERENCES users(user_id),
    acknowledged_at TIMESTAMPTZ
);

CREATE INDEX idx_notifications_farm ON notifications(farm_id);
CREATE INDEX idx_notifications_room ON notifications(room_id);
CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_level ON notifications(level);
CREATE INDEX idx_notifications_read_status ON notifications(read_status);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);

-- Recommendations / AI outputs table (for future AI integration)
CREATE TABLE recommendations (
    rec_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    farm_id UUID REFERENCES farms(farm_id) ON DELETE CASCADE,
    room_id UUID REFERENCES rooms(room_id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    payload JSONB NOT NULL, -- model output JSON
    confidence REAL,
    model_id VARCHAR(200),
    category VARCHAR(50), -- optimization, alert, maintenance, harvest
    status VARCHAR(20) DEFAULT 'pending', -- pending, accepted, rejected, expired
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now(),
    reviewed_by UUID REFERENCES users(user_id),
    reviewed_at TIMESTAMPTZ
);

CREATE INDEX idx_recommendations_farm ON recommendations(farm_id);
CREATE INDEX idx_recommendations_room ON recommendations(room_id);
CREATE INDEX idx_recommendations_status ON recommendations(status);
CREATE INDEX idx_recommendations_category ON recommendations(category);
CREATE INDEX idx_recommendations_created_at ON recommendations(created_at DESC);

-- System configuration table
CREATE TABLE system_config (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value TEXT NOT NULL,
    description TEXT,
    config_type VARCHAR(20) DEFAULT 'string', -- string, number, boolean, json
    updated_by UUID REFERENCES users(user_id),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Insert default system configuration
INSERT INTO system_config (config_key, config_value, description, config_type) VALUES
('telemetry_retention_days', '90', 'Number of days to retain sensor telemetry data', 'number'),
('notification_retention_days', '30', 'Number of days to retain notifications', 'number'),
('max_devices_per_room', '50', 'Maximum number of devices allowed per room', 'number'),
('default_automation_cooldown', '5', 'Default cooldown period for automation rules in minutes', 'number'),
('mqtt_keepalive_seconds', '60', 'MQTT keep-alive interval in seconds', 'number'),
('websocket_heartbeat_seconds', '30', 'WebSocket heartbeat interval in seconds', 'number');

-- Create views for common queries

-- Latest sensor readings per device
CREATE VIEW latest_sensor_readings AS
SELECT DISTINCT ON (device_id) 
    device_id,
    room_id,
    farm_id,
    temperature_c,
    humidity_pct,
    co2_ppm,
    light_lux,
    ph_level,
    substrate_moisture,
    battery_v,
    recorded_at
FROM sensor_data
ORDER BY device_id, recorded_at DESC;

-- Room summary with device counts and latest readings
CREATE VIEW room_summary AS
SELECT 
    r.room_id,
    r.farm_id,
    r.name,
    r.description,
    r.mushroom_type,
    r.stage,
    COUNT(d.device_id) as device_count,
    COUNT(CASE WHEN d.status = 'online' THEN 1 END) as online_devices,
    MAX(sd.recorded_at) as last_telemetry_at,
    r.created_at,
    r.updated_at
FROM rooms r
LEFT JOIN devices d ON r.room_id = d.room_id
LEFT JOIN sensor_data sd ON r.room_id = sd.room_id
GROUP BY r.room_id, r.farm_id, r.name, r.description, r.mushroom_type, r.stage, r.created_at, r.updated_at;

-- Device status summary
CREATE VIEW device_status_summary AS
SELECT 
    d.device_id,
    d.room_id,
    d.name,
    d.device_type,
    d.category,
    d.status,
    d.last_seen,
    d.firmware_version,
    d.battery_level,
    lsr.temperature_c,
    lsr.humidity_pct,
    lsr.co2_ppm,
    lsr.light_lux,
    lsr.ph_level,
    lsr.substrate_moisture,
    lsr.recorded_at as last_reading_at
FROM devices d
LEFT JOIN latest_sensor_readings lsr ON d.device_id = lsr.device_id;