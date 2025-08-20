You are an expert full-stack IoT engineer. Build a complete, production-ready Mushroom Farm IoT system that supports multiple farms, multiple rooms (aka blocks/houses), and devices per room. The system includes: ESP32/ESP8266 devices, AWS IoT Core (MQTT), AWS Cognito (auth), a Flask backend with a PostgreSQL database, a React web dashboard, and an Expo React Native mobile app. The web and mobile apps share the same backend & auth; mobile is scoped to assigned room(s).

Deliver production-quality code, migrations, docs, tests, and deployment scripts (IaC skeleton). Provide clear setup/run instructions and sample test data/dev device simulator.

Key concepts & naming
Farm: top-level grouping (farm_id). One user may own multiple farms.

Room (Farm Zone / Block / House): physical grow area inside a Farm (room_id). Devices belong to rooms.

Device: physical controller/sensor (device_id), belongs to a room.

Sensor data: time-series telemetry produced by devices.

Command: control messages from backend to devices.

MQTT topic design (required)
All devices use TLS & X.509 or secure keys for AWS IoT Core.

Telemetry (device → cloud):

farm/{farm_id}/room/{room_id}/device/{device_id}/telemetry
Control (cloud → device):

farm/{farm_id}/room/{room_id}/device/{device_id}/command
Status / ack:

farm/{farm_id}/room/{room_id}/device/{device_id}/status
Devices must use ISO8601 UTC timestamps, small JSON payloads, and support local buffering when offline.

Database (PostgreSQL) — CREATE TABLE scripts
Use UUIDs for primary keys. Include indexes for time-series reads.

-- users
CREATE TABLE users (
user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
cognito_sub VARCHAR(255) UNIQUE NOT NULL,
email VARCHAR(255) UNIQUE NOT NULL,
full_name VARCHAR(200),
role VARCHAR(20) NOT NULL DEFAULT 'viewer', -- admin, manager, viewer
created_at TIMESTAMPTZ DEFAULT now()
);

-- farms
CREATE TABLE farms (
farm_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
owner_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
name VARCHAR(200) NOT NULL,
location TEXT,
created_at TIMESTAMPTZ DEFAULT now()
);

-- rooms (farm zones / blocks)
CREATE TABLE rooms (
room_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
farm_id UUID REFERENCES farms(farm_id) ON DELETE CASCADE,
name VARCHAR(200) NOT NULL,
description TEXT,
mushroom_type VARCHAR(100),
stage VARCHAR(50), -- incubation, fruiting, maintenance
created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_rooms_farm ON rooms(farm_id);

-- user_room mapping (which rooms a user can access)
CREATE TABLE user_rooms (
user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
room_id UUID REFERENCES rooms(room_id) ON DELETE CASCADE,
role VARCHAR(20) DEFAULT 'operator', -- owner, operator, viewer
assigned_at TIMESTAMPTZ DEFAULT now(),
PRIMARY KEY (user_id, room_id)
);

-- devices
CREATE TABLE devices (
device_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
room_id UUID REFERENCES rooms(room_id) ON DELETE CASCADE,
name VARCHAR(200) NOT NULL,
device_type VARCHAR(50), -- sensor, actuator, hybrid
category VARCHAR(50), -- temperature, humidity, co2, light, fan, humidifier...
mqtt_topic VARCHAR(512) UNIQUE NOT NULL,
status VARCHAR(20) DEFAULT 'offline',
last_seen TIMESTAMPTZ,
firmware_version VARCHAR(50),
created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_devices_room ON devices(room_id);

-- sensor_data (time-series)
CREATE TABLE sensor_data (
reading_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
device_id UUID REFERENCES devices(device_id) ON DELETE CASCADE,
room_id UUID REFERENCES rooms(room_id) ON DELETE CASCADE,
farm_id UUID REFERENCES farms(farm_id) ON DELETE CASCADE,
temperature_c REAL,
humidity_pct REAL,
co2_ppm REAL,
light_lux REAL,
substrate_moisture REAL,
battery_v REAL,
recorded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_sensor_room_time ON sensor_data(room_id, recorded_at DESC);
CREATE INDEX idx_sensor_device_time ON sensor_data(device_id, recorded_at DESC);

-- commands (control history & pending commands)
CREATE TABLE commands (
command_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
device_id UUID REFERENCES devices(device_id) ON DELETE CASCADE,
room_id UUID REFERENCES rooms(room_id),
farm_id UUID REFERENCES farms(farm_id),
command TEXT NOT NULL,
params JSONB,
issued_by UUID REFERENCES users(user_id),
issued_at TIMESTAMPTZ DEFAULT now(),
status VARCHAR(30) DEFAULT 'pending' -- pending, sent, acked, failed
);
CREATE INDEX idx_commands_device_time ON commands(device_id, issued_at DESC);

-- automation rules
CREATE TABLE automation_rules (
rule_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
room_id UUID REFERENCES rooms(room_id),
name VARCHAR(200),
parameter VARCHAR(50), -- temperature, humidity, co2, light
comparator VARCHAR(2), -- <, >, <=, >=, ==
threshold REAL,
action_device UUID REFERENCES devices(device_id),
action_command TEXT, -- e.g., {"cmd":"turn_on","duration_s":300}
enabled BOOLEAN DEFAULT TRUE,
created_by UUID REFERENCES users(user_id),
created_at TIMESTAMPTZ DEFAULT now()
);

-- farming cycles (batches)
CREATE TABLE farming_cycles (
cycle_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
room_id UUID REFERENCES rooms(room_id),
start_date DATE NOT NULL,
expected_harvest_date DATE,
status VARCHAR(50) DEFAULT 'growing', -- growing, harvested, aborted
mushroom_variety VARCHAR(200),
notes TEXT,
created_at TIMESTAMPTZ DEFAULT now()
);

-- notifications / alerts
CREATE TABLE notifications (
notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
farm_id UUID REFERENCES farms(farm_id),
room_id UUID REFERENCES rooms(room_id),
device_id UUID REFERENCES devices(device_id),
level VARCHAR(20), -- info, warning, critical
message TEXT NOT NULL,
created_at TIMESTAMPTZ DEFAULT now(),
acknowledged_by UUID REFERENCES users(user_id),
acknowledged_at TIMESTAMPTZ
);

-- bedrock recommendations / AI outputs
CREATE TABLE recommendations (
rec_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
farm_id UUID REFERENCES farms(farm_id),
room_id UUID REFERENCES rooms(room_id),
payload JSONB NOT NULL, -- model output JSON
confidence REAL,
model_id VARCHAR(200),
created_at TIMESTAMPTZ DEFAULT now()
);
Backend (Flask) — API endpoints (summary)
All protected endpoints require Cognito JWT validation (middleware). Use role-based authorization.

Auth

POST /auth/register — create user in Cognito + optional DB user row

POST /auth/login — return Cognito tokens (or redirect to hosted UI)

Farms & Rooms

GET /api/farms — list farms user owns or has access to

POST /api/farms — create farm

GET /api/farms/{farm_id} — farm detail

GET /api/farms/{farm_id}/rooms — list rooms in farm

POST /api/farms/{farm_id}/rooms — create room (name, mushroom_type, default_rules)

GET /api/rooms/{room_id} — room detail

PUT /api/rooms/{room_id} — update room (stage, name, etc.)

POST /api/rooms/{room_id}/assign — assign user(s) to room (user_id + role)

Devices & Telemetry

POST /api/devices — register device (room_id, name, category, mqtt_topic)

GET /api/rooms/{room_id}/devices — list devices in room

GET /api/devices/{device_id} — device detail

GET /api/rooms/{room_id}/telemetry?from=&to=&agg=minute|hour — timeseries query

GET /api/devices/{device_id}/latest — latest readings

Commands & Controls

POST /api/devices/{device_id}/commands — send command (body: command, params)

Backend publishes to AWS IoT Core topic farm/{farm}/room/{room}/device/{device}/command

Create a commands DB record with status

GET /api/rooms/{room_id}/commands — recent commands

Automation & AI

GET /api/rooms/{room_id}/rules

POST /api/rooms/{room_id}/rules

POST /api/rooms/{room_id}/recommend — force an AI recommendation (triggers Bedrock)

GET /api/rooms/{room_id}/recommendations — list recommended actions

Notifications

GET /api/notifications

POST /api/notifications/{id}/ack

Real-time

Implement real-time updates using Flask-SocketIO or a WebSocket gateway.

Ingest path: AWS IoT Rule → Lambda → HTTP POST to backend /internal/ingest (authenticated by internal token). Backend writes DB and emits WebSocket event to subscribed clients.

IoT ingestion flow (recommended)
Device publishes telemetry to AWS IoT Core topic farm/{farm}/room/{room}/device/{device}/telemetry.

AWS IoT Rule triggers a Lambda:

Validates & normalizes payload

Writes to sensor_data

Evaluates automation rules for that room (or enqueues to rules worker)

Posts event to backend internal endpoint to push real-time WebSocket updates

Optionally enqueue a Bedrock job (SQS) for batch AI analysis

Lambda pseudocode should be provided by the coding AI.

Amazon Bedrock integration (AI)
Periodically (configurable per room, e.g., hourly) aggregate last N hours of telemetry and send to Bedrock model for analysis & recommendations.

Store results in recommendations.

Do not auto-execute high-risk actions without owner approval; allow auto-exec only for low-risk actions with a configurable confidence threshold.

Log all model inputs/outputs (redact PII).

Frontend: Web (React)
Full farm/room management: farms list, rooms list, room detail, device list, telemetry graphs, automation rules, recommendations panel, user management.

Real-time dashboard: WebSocket subscriptions for farms/rooms the user has access to.

Admin pages: create farm/room, register devices, assign user-room roles.

Graphing: use Recharts or Chart.js with server-side aggregation endpoints.

Frontend: Mobile (React Native / Expo)
Single-app codebase but feature-flag UI for mobile-friendly layout.

On login, show the user’s assigned rooms (via user_rooms). If multiple, allow selection — otherwise show the single assigned room by default.

Room detail screen: live readings, quick controls, notifications, ability to accept/reject AI suggestions.

Offline: cache most recent readings; queue commands if offline and retry.

Device firmware (ESP32/ESP8266)
Secure MQTT to AWS IoT Core with device certs or Cognito/IoT-thing provisioning.

Publish telemetry JSON to farm/.../device/.../telemetry at configurable intervals (default 30–60s).

Subscribe to its .../command topic and execute commands; send ack to .../status.

Support OTA updates via S3-signed URLs and verify signature before applying.

Implement local retry/backoff and local storage of last X samples while offline.

Deliverables (explicit)
Full Flask backend repo (Dockerized), with:

OpenAPI (Swagger) spec

migrations (Alembic) and seed data

Cognito JWT middleware

AWS IoT publish helper

WebSocket server and /internal/ingest receiver

Bedrock invoker worker (or Lambda integration)

Unit & integration tests

PostgreSQL schema SQL & migration scripts (above).

React web dashboard repo (Dockerized), with components for farms, rooms, devices, telemetry, automation, recommendations.

Expo React Native mobile app repo (Docker-friendly build/CI).

ESP32/ESP8266 firmware sample with build instructions and device simulator script.

AWS IaC skeleton (Terraform): IoT Core things/policies, Lambda (ingest + bedrock trigger), SQS, SNS, Cognito user pool, IAM roles, and minimal ECS or Lambda deployment for backend.

README / runbook: deployment steps, env variables, testing guide, and sample Postman collection.

Acceptance tests (must pass)
Onboard new farm → create room → register device → telemetry from simulator reaches backend and appears in room detail UI within 5s.

Assign a user to a room → mobile login shows only assigned room(s).

Post a command via UI → backend stores command and publishes to correct MQTT topic → simulator receives and ACKs → command row marked acked.

Trigger automation rule via simulated telemetry → an action command is issued and logged.

Bedrock integration returns a recommendation and recommendation is stored; UI shows it.

Implementation notes / constraints
Use UTC for all timestamps.

Keep prompt size for Bedrock small — send aggregated time-series or summary, not raw high-frequency points.

Use role-based access checks in backend endpoints (user must have user_rooms entry to control devices in a room).

Keep secrets in environment variables and use AWS KMS where required.

Provide clear CI scripts to run tests and deploy to an AWS account (staging and prod).
