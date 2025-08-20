package com.smartfarm.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfarm.entity.*;
import com.smartfarm.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles processing of MQTT messages from IoT devices.
 * Processes telemetry data, device status updates, and command acknowledgments.
 */
@Component
public class MqttMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(MqttMessageHandler.class);
    
    // Topic pattern: farm/{farm_id}/room/{room_id}/device/{device_id}/{message_type}
    private static final Pattern TOPIC_PATTERN = Pattern.compile(
        "farm/([^/]+)/room/([^/]+)/device/([^/]+)/(telemetry|status|command)"
    );
    
    private final ObjectMapper objectMapper;
    private final DeviceService deviceService;
    private final SensorDataService sensorDataService;
    private final CommandService commandService;
    private final AutomationRuleService automationRuleService;
    private final NotificationService notificationService;
    
    @Autowired
    public MqttMessageHandler(
            ObjectMapper objectMapper,
            DeviceService deviceService,
            SensorDataService sensorDataService,
            CommandService commandService,
            AutomationRuleService automationRuleService,
            NotificationService notificationService) {
        this.objectMapper = objectMapper;
        this.deviceService = deviceService;
        this.sensorDataService = sensorDataService;
        this.commandService = commandService;
        this.automationRuleService = automationRuleService;
        this.notificationService = notificationService;
    }

    /**
     * Process incoming MQTT message based on topic and payload.
     */
    @Transactional
    public void handleMessage(String topic, String payload) {
        try {
            logger.debug("Processing MQTT message - Topic: {}, Payload: {}", topic, payload);
            
            // Parse topic to extract IDs and message type
            TopicInfo topicInfo = parseTopic(topic);
            if (topicInfo == null) {
                logger.warn("Invalid topic format: {}", topic);
                return;
            }
            
            // Parse JSON payload
            JsonNode messageNode = objectMapper.readTree(payload);
            
            // Route message based on type
            switch (topicInfo.messageType) {
                case "telemetry":
                    handleTelemetryMessage(topicInfo, messageNode);
                    break;
                case "status":
                    handleStatusMessage(topicInfo, messageNode);
                    break;
                case "command":
                    handleCommandAcknowledgment(topicInfo, messageNode);
                    break;
                default:
                    logger.warn("Unknown message type: {}", topicInfo.messageType);
            }
            
        } catch (Exception e) {
            logger.error("Error processing MQTT message - Topic: {}, Payload: {}", topic, payload, e);
        }
    }

    /**
     * Handle telemetry data from devices.
     */
    private void handleTelemetryMessage(TopicInfo topicInfo, JsonNode messageNode) {
        try {
            logger.debug("Processing telemetry message for device: {}", topicInfo.deviceId);
            
            // Find device
            Optional<Device> deviceOpt = deviceService.findById(UUID.fromString(topicInfo.deviceId));
            if (!deviceOpt.isPresent()) {
                logger.warn("Device not found: {}", topicInfo.deviceId);
                return;
            }
            
            Device device = deviceOpt.get();
            
            // Update device last seen
            device.setLastSeen(LocalDateTime.now());
            device.setStatus("online");
            deviceService.save(device);
            
            // Create sensor data record
            SensorData sensorData = new SensorData();
            sensorData.setDevice(device);
            sensorData.setRoom(device.getRoom());
            sensorData.setFarm(device.getRoom().getFarm());
            
            // Parse timestamp
            LocalDateTime recordedAt = parseTimestamp(messageNode.get("timestamp"));
            sensorData.setRecordedAt(recordedAt);
            
            // Extract sensor readings
            if (messageNode.has("temperature")) {
                sensorData.setTemperatureC(messageNode.get("temperature").asDouble());
            }
            if (messageNode.has("humidity")) {
                sensorData.setHumidityPct(messageNode.get("humidity").asDouble());
            }
            if (messageNode.has("co2")) {
                sensorData.setCo2Ppm(messageNode.get("co2").asDouble());
            }
            if (messageNode.has("light")) {
                sensorData.setLightLux(messageNode.get("light").asDouble());
            }
            if (messageNode.has("substrate_moisture")) {
                sensorData.setSubstrateMoisture(messageNode.get("substrate_moisture").asDouble());
            }
            if (messageNode.has("battery")) {
                sensorData.setBatteryV(messageNode.get("battery").asDouble());
            }
            
            // Save sensor data
            sensorDataService.save(sensorData);
            
            logger.info("Telemetry data saved for device: {} at {}", device.getName(), recordedAt);
            
            // Trigger automation rule evaluation
            try {
                automationRuleService.evaluateRulesForRoom(device.getRoom().getRoomId());
            } catch (Exception e) {
                logger.error("Error evaluating automation rules for room: {}", device.getRoom().getRoomId(), e);
            }
            
        } catch (Exception e) {
            logger.error("Error processing telemetry message", e);
        }
    }

    /**
     * Handle device status updates.
     */
    private void handleStatusMessage(TopicInfo topicInfo, JsonNode messageNode) {
        try {
            logger.debug("Processing status message for device: {}", topicInfo.deviceId);
            
            // Find device
            Optional<Device> deviceOpt = deviceService.findById(UUID.fromString(topicInfo.deviceId));
            if (!deviceOpt.isPresent()) {
                logger.warn("Device not found: {}", topicInfo.deviceId);
                return;
            }
            
            Device device = deviceOpt.get();
            
            // Update device status
            if (messageNode.has("status")) {
                String status = messageNode.get("status").asText();
                device.setStatus(status);
                logger.info("Device {} status updated to: {}", device.getName(), status);
            }
            
            // Update firmware version
            if (messageNode.has("firmware_version")) {
                String firmwareVersion = messageNode.get("firmware_version").asText();
                device.setFirmwareVersion(firmwareVersion);
                logger.info("Device {} firmware version updated to: {}", device.getName(), firmwareVersion);
            }
            
            // Update last seen
            device.setLastSeen(LocalDateTime.now());
            deviceService.save(device);
            
            // Check for error conditions
            if (messageNode.has("error")) {
                String errorMessage = messageNode.get("error").asText();
                createErrorNotification(device, errorMessage);
            }
            
        } catch (Exception e) {
            logger.error("Error processing status message", e);
        }
    }

    /**
     * Handle command acknowledgments from devices.
     */
    private void handleCommandAcknowledgment(TopicInfo topicInfo, JsonNode messageNode) {
        try {
            logger.debug("Processing command acknowledgment for device: {}", topicInfo.deviceId);
            
            if (!messageNode.has("command_id")) {
                logger.warn("Command acknowledgment missing command_id");
                return;
            }
            
            String commandIdStr = messageNode.get("command_id").asText();
            UUID commandId = UUID.fromString(commandIdStr);
            
            // Find and update command
            Optional<Command> commandOpt = commandService.findById(commandId);
            if (!commandOpt.isPresent()) {
                logger.warn("Command not found: {}", commandId);
                return;
            }
            
            Command command = commandOpt.get();
            
            // Update command status
            if (messageNode.has("status")) {
                String status = messageNode.get("status").asText();
                command.setStatus(status);
                
                logger.info("Command {} status updated to: {}", commandId, status);
                
                // If command failed, create notification
                if ("failed".equals(status) && messageNode.has("error")) {
                    String errorMessage = messageNode.get("error").asText();
                    createCommandFailureNotification(command, errorMessage);
                }
            }
            
            commandService.save(command);
            
        } catch (Exception e) {
            logger.error("Error processing command acknowledgment", e);
        }
    }

    /**
     * Parse MQTT topic to extract farm, room, device IDs and message type.
     */
    private TopicInfo parseTopic(String topic) {
        Matcher matcher = TOPIC_PATTERN.matcher(topic);
        if (!matcher.matches()) {
            return null;
        }
        
        TopicInfo info = new TopicInfo();
        info.farmId = matcher.group(1);
        info.roomId = matcher.group(2);
        info.deviceId = matcher.group(3);
        info.messageType = matcher.group(4);
        
        return info;
    }

    /**
     * Parse timestamp from message, defaulting to current time if not present or invalid.
     */
    private LocalDateTime parseTimestamp(JsonNode timestampNode) {
        if (timestampNode == null || timestampNode.isNull()) {
            return LocalDateTime.now();
        }
        
        try {
            String timestampStr = timestampNode.asText();
            
            // Try parsing as ISO8601 UTC timestamp
            if (timestampStr.endsWith("Z")) {
                Instant instant = Instant.parse(timestampStr);
                return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
            }
            
            // Try parsing as epoch milliseconds
            if (timestampStr.matches("\\d+")) {
                long epochMilli = Long.parseLong(timestampStr);
                Instant instant = Instant.ofEpochMilli(epochMilli);
                return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
            }
            
            // Try parsing as ISO local datetime
            return LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
        } catch (Exception e) {
            logger.warn("Failed to parse timestamp: {}, using current time", timestampNode.asText());
            return LocalDateTime.now();
        }
    }

    /**
     * Create error notification for device issues.
     */
    private void createErrorNotification(Device device, String errorMessage) {
        try {
            Notification notification = new Notification();
            notification.setFarm(device.getRoom().getFarm());
            notification.setRoom(device.getRoom());
            notification.setDevice(device);
            notification.setLevel("critical");
            notification.setMessage(String.format("Device %s reported error: %s", device.getName(), errorMessage));
            notification.setCreatedAt(LocalDateTime.now());
            
            notificationService.save(notification);
            
            logger.info("Error notification created for device: {}", device.getName());
            
        } catch (Exception e) {
            logger.error("Failed to create error notification", e);
        }
    }

    /**
     * Create notification for command failures.
     */
    private void createCommandFailureNotification(Command command, String errorMessage) {
        try {
            Notification notification = new Notification();
            notification.setFarm(command.getFarm());
            notification.setRoom(command.getRoom());
            notification.setDevice(command.getDevice());
            notification.setLevel("warning");
            notification.setMessage(String.format("Command failed on device %s: %s", 
                command.getDevice().getName(), errorMessage));
            notification.setCreatedAt(LocalDateTime.now());
            
            notificationService.save(notification);
            
            logger.info("Command failure notification created for device: {}", command.getDevice().getName());
            
        } catch (Exception e) {
            logger.error("Failed to create command failure notification", e);
        }
    }

    /**
     * Helper class to hold parsed topic information.
     */
    private static class TopicInfo {
        String farmId;
        String roomId;
        String deviceId;
        String messageType;
    }
}