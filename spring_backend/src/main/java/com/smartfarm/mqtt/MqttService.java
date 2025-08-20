package com.smartfarm.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfarm.dto.CommandDto;
import com.smartfarm.entity.Command;
import com.smartfarm.entity.Device;
import com.smartfarm.service.CommandService;
import com.smartfarm.service.DeviceService;
import com.smartfarm.service.SensorDataService;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MQTT service for IoT device communication with AWS IoT Core.
 * Handles device telemetry ingestion, command publishing, and status updates.
 */
@Service
public class MqttService {

    private static final Logger logger = LoggerFactory.getLogger(MqttService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // Connection tracking
    private MqttClient mqttClient;
    private final Map<String, LocalDateTime> deviceLastSeen = new ConcurrentHashMap<>();
    
    @Value("${aws.iot.endpoint:}")
    private String iotEndpoint;
    
    @Value("${aws.iot.clientId:smartfarm-backend}")
    private String clientId;
    
    @Value("${aws.iot.certificatePath:}")
    private String certificatePath;
    
    @Value("${aws.iot.privateKeyPath:}")
    private String privateKeyPath;
    
    @Value("${aws.iot.caCertPath:}")
    private String caCertPath;
    
    @Value("${mqtt.qos:1}")
    private int defaultQos;
    
    @Value("${mqtt.keepAlive:60}")
    private int keepAliveInterval;
    
    @Value("${mqtt.connectionTimeout:30}")
    private int connectionTimeout;
    
    @Value("${mqtt.development.enabled:true}")
    private boolean developmentMode;
    
    private final SensorDataService sensorDataService;
    private final CommandService commandService;
    private final DeviceService deviceService;
    
    @Autowired
    public MqttService(SensorDataService sensorDataService,
                      CommandService commandService,
                      DeviceService deviceService) {
        this.sensorDataService = sensorDataService;
        this.commandService = commandService;
        this.deviceService = deviceService;
    }

    @PostConstruct
    public void initialize() {
        try {
            logger.info("Initializing MQTT service...");
            
            if (developmentMode) {
                logger.warn("MQTT service running in development mode - using mock connections");
                initializeDevelopmentMode();
            } else {
                initializeProductionMode();
            }
            
            // Start device status monitoring
            startDeviceStatusMonitoring();
            
            logger.info("MQTT service initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize MQTT service", e);
            throw new RuntimeException("MQTT service initialization failed", e);
        }
    }
    
    /**
     * Initialize MQTT service in development mode (mock connections).
     */
    private void initializeDevelopmentMode() {
        logger.info("MQTT service initialized in development mode");
        // In development mode, we don't establish real MQTT connections
        // Commands and telemetry are handled through REST endpoints for testing
    }
    
    /**
     * Initialize MQTT service in production mode with AWS IoT Core.
     */
    private void initializeProductionMode() throws MqttException {
        if (iotEndpoint == null || iotEndpoint.trim().isEmpty()) {
            throw new IllegalStateException("AWS IoT endpoint not configured");
        }
        
        String brokerUrl = "ssl://" + iotEndpoint + ":8883";
        logger.info("Connecting to AWS IoT Core: {}", brokerUrl);
        
        // Create MQTT client
        mqttClient = new MqttClient(brokerUrl, clientId + "-" + System.currentTimeMillis());
        
        // Configure connection options
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setKeepAliveInterval(keepAliveInterval);
        options.setConnectionTimeout(connectionTimeout);
        options.setAutomaticReconnect(true);
        
        // Set SSL properties for AWS IoT Core
        configureSslProperties(options);
        
        // Set callback for connection events
        mqttClient.setCallback(new MqttCallbackHandler());
        
        // Connect to broker
        mqttClient.connect(options);
        logger.info("Connected to AWS IoT Core successfully");
        
        // Subscribe to telemetry and status topics
        subscribeToTopics();
    }
    
    /**
     * Configure SSL properties for AWS IoT Core connection.
     */
    private void configureSslProperties(MqttConnectOptions options) {
        try {
            // TODO: Implement SSL configuration with device certificates
            // This would involve:
            // 1. Loading device certificate from certificatePath
            // 2. Loading private key from privateKeyPath
            // 3. Loading CA certificate from caCertPath
            // 4. Creating SSL context with certificates
            // 5. Setting SSL properties on connection options
            
            logger.warn("SSL configuration not fully implemented - using default settings");
            
        } catch (Exception e) {
            logger.error("Failed to configure SSL properties", e);
            throw new RuntimeException("SSL configuration failed", e);
        }
    }
    
    /**
     * Subscribe to MQTT topics for telemetry and device status.
     */
    private void subscribeToTopics() throws MqttException {
        // Subscribe to all telemetry topics
        String telemetryTopic = "farm/+/room/+/device/+/telemetry";
        mqttClient.subscribe(telemetryTopic, defaultQos);
        logger.info("Subscribed to telemetry topic: {}", telemetryTopic);
        
        // Subscribe to all status topics
        String statusTopic = "farm/+/room/+/device/+/status";
        mqttClient.subscribe(statusTopic, defaultQos);
        logger.info("Subscribed to status topic: {}", statusTopic);
    }
    
    /**
     * Publish command to device via MQTT.
     */
    public void publishCommand(UUID deviceId, CommandDto command) {
        try {
            Device device = deviceService.getDeviceById(deviceId);
            String topic = buildCommandTopic(device.getRoom().getFarm().getFarmId(),
                                           device.getRoom().getRoomId(),
                                           deviceId);
            
            String payload = objectMapper.writeValueAsString(command);
            
            if (developmentMode) {
                logger.info("[DEV MODE] Would publish command to topic: {} - Payload: {}", topic, payload);
                // In development mode, simulate immediate acknowledgment
                simulateCommandAcknowledgment(command.getCommandId());
            } else {
                MqttMessage message = new MqttMessage(payload.getBytes());
                message.setQos(defaultQos);
                message.setRetained(false);
                
                mqttClient.publish(topic, message);
                logger.info("Published command to device {} on topic: {}", deviceId, topic);
            }
            
        } catch (Exception e) {
            logger.error("Failed to publish command to device: {}", deviceId, e);
            throw new RuntimeException("Failed to publish command", e);
        }
    }
    
    /**
     * Build command topic for a device.
     */
    private String buildCommandTopic(UUID farmId, UUID roomId, UUID deviceId) {
        return String.format("farm/%s/room/%s/device/%s/command", farmId, roomId, deviceId);
    }
    
    /**
     * Build telemetry topic for a device.
     */
    private String buildTelemetryTopic(UUID farmId, UUID roomId, UUID deviceId) {
        return String.format("farm/%s/room/%s/device/%s/telemetry", farmId, roomId, deviceId);
    }
    
    /**
     * Build status topic for a device.
     */
    private String buildStatusTopic(UUID farmId, UUID roomId, UUID deviceId) {
        return String.format("farm/%s/room/%s/device/%s/status", farmId, roomId, deviceId);
    }
    
    /**
     * Simulate command acknowledgment in development mode.
     */
    private void simulateCommandAcknowledgment(UUID commandId) {
        scheduler.schedule(() -> {
            try {
                commandService.updateCommandStatus(commandId, "acknowledged", "Simulated ACK");
                logger.debug("Simulated acknowledgment for command: {}", commandId);
            } catch (Exception e) {
                logger.error("Failed to simulate command acknowledgment", e);
            }
        }, 2, TimeUnit.SECONDS);
    }
    
    /**
     * Start monitoring device status and update last seen timestamps.
     */
    private void startDeviceStatusMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                updateDeviceStatuses();
            } catch (Exception e) {
                logger.error("Error during device status monitoring", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
        
        logger.info("Device status monitoring started");
    }
    
    /**
     * Update device statuses based on last seen timestamps.
     */
    private void updateDeviceStatuses() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime offlineThreshold = now.minusMinutes(5); // 5 minutes offline threshold
        
        deviceLastSeen.entrySet().removeIf(entry -> {
            String deviceId = entry.getKey();
            LocalDateTime lastSeen = entry.getValue();
            
            if (lastSeen.isBefore(offlineThreshold)) {
                try {
                    deviceService.updateDeviceStatus(UUID.fromString(deviceId), "offline");
                    logger.debug("Marked device {} as offline (last seen: {})", deviceId, lastSeen);
                    return true; // Remove from tracking
                } catch (Exception e) {
                    logger.error("Failed to update device status for: {}", deviceId, e);
                }
            }
            return false;
        });
    }
    
    /**
     * Record device activity (called when telemetry or status is received).
     */
    private void recordDeviceActivity(String deviceId) {
        deviceLastSeen.put(deviceId, LocalDateTime.now());
        
        // Update device status to online
        try {
            deviceService.updateDeviceStatus(UUID.fromString(deviceId), "online");
        } catch (Exception e) {
            logger.error("Failed to update device status to online: {}", deviceId, e);
        }
    }
    
    /**
     * Check if MQTT client is connected.
     */
    public boolean isConnected() {
        if (developmentMode) {
            return true; // Always "connected" in development mode
        }
        return mqttClient != null && mqttClient.isConnected();
    }
    
    /**
     * Get connection status information.
     */
    public Map<String, Object> getConnectionStatus() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("connected", isConnected());
        status.put("developmentMode", developmentMode);
        status.put("clientId", clientId);
        status.put("endpoint", iotEndpoint);
        status.put("activeDevices", deviceLastSeen.size());
        
        if (mqttClient != null) {
            status.put("serverURI", mqttClient.getServerURI());
        }
        
        return status;
    }
    
    @PreDestroy
    public void cleanup() {
        logger.info("Shutting down MQTT service...");
        
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                mqttClient.close();
                logger.info("MQTT client disconnected successfully");
            } catch (MqttException e) {
                logger.error("Error disconnecting MQTT client", e);
            }
        }
        
        logger.info("MQTT service shutdown completed");
    }
    
    /**
     * MQTT callback handler for connection events and message processing.
     */
    private class MqttCallbackHandler implements MqttCallback {
        
        @Override
        public void connectionLost(Throwable cause) {
            logger.error("MQTT connection lost", cause);
            // Automatic reconnection is enabled in connection options
        }
        
        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            try {
                String payload = new String(message.getPayload());
                logger.debug("Received message on topic: {} - Payload: {}", topic, payload);
                
                // Parse topic to extract device information
                String[] topicParts = topic.split("/");
                if (topicParts.length >= 6) {
                    String deviceId = topicParts[5];
                    String messageType = topicParts[6]; // telemetry or status
                    
                    recordDeviceActivity(deviceId);
                    
                    if ("telemetry".equals(messageType)) {
                        processTelemetryMessage(deviceId, payload);
                    } else if ("status".equals(messageType)) {
                        processStatusMessage(deviceId, payload);
                    }
                }
                
            } catch (Exception e) {
                logger.error("Error processing MQTT message from topic: {}", topic, e);
            }
        }
        
        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            logger.debug("Message delivery completed: {}", token.getMessageId());
        }
    }
    
    /**
     * Process telemetry message from device.
     */
    private void processTelemetryMessage(String deviceId, String payload) {
        try {
            // Delegate to sensor data service for processing
            sensorDataService.processTelemetryData(UUID.fromString(deviceId), payload);
            logger.debug("Processed telemetry data for device: {}", deviceId);
            
        } catch (Exception e) {
            logger.error("Failed to process telemetry data for device: {}", deviceId, e);
        }
    }
    
    /**
     * Process status message from device (command acknowledgments, etc.).
     */
    private void processStatusMessage(String deviceId, String payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> statusData = objectMapper.readValue(payload, Map.class);
            
            // Handle command acknowledgments
            if (statusData.containsKey("commandId")) {
                String commandId = (String) statusData.get("commandId");
                String status = (String) statusData.get("status");
                String message = (String) statusData.get("message");
                
                commandService.updateCommandStatus(UUID.fromString(commandId), status, message);
                logger.debug("Updated command status: {} -> {}", commandId, status);
            }
            
            // Handle other status updates (firmware version, battery level, etc.)
            if (statusData.containsKey("firmwareVersion")) {
                String firmwareVersion = (String) statusData.get("firmwareVersion");
                deviceService.updateDeviceFirmware(UUID.fromString(deviceId), firmwareVersion);
            }
            
        } catch (Exception e) {
            logger.error("Failed to process status message for device: {}", deviceId, e);
        }
    }
}