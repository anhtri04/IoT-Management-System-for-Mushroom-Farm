package com.smartfarm.service;

import com.smartfarm.entity.AutomationRule;
import com.smartfarm.entity.Room;
import com.smartfarm.entity.Device;
import com.smartfarm.entity.SensorData;
import com.smartfarm.repository.AutomationRuleRepository;
import com.smartfarm.repository.RoomRepository;
import com.smartfarm.repository.DeviceRepository;
import com.smartfarm.exception.ResourceNotFoundException;
import com.smartfarm.exception.UnauthorizedException;
import com.smartfarm.exception.ValidationException;
import com.smartfarm.dto.AutomationRuleDto;
import com.smartfarm.dto.AutomationRuleCreateDto;
import com.smartfarm.dto.CommandCreateDto;
import com.smartfarm.enums.Comparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

/**
 * Service class for AutomationRule entity operations.
 * Handles business logic for automation rules, triggers, and rule evaluation.
 */
@Service
@Transactional
public class AutomationRuleService {

    private static final Logger logger = LoggerFactory.getLogger(AutomationRuleService.class);

    private final AutomationRuleRepository automationRuleRepository;
    private final RoomRepository roomRepository;
    private final DeviceRepository deviceRepository;
    private final RoomService roomService;
    private final CommandService commandService;

    @Autowired
    public AutomationRuleService(AutomationRuleRepository automationRuleRepository, RoomRepository roomRepository,
                                DeviceRepository deviceRepository, RoomService roomService, CommandService commandService) {
        this.automationRuleRepository = automationRuleRepository;
        this.roomRepository = roomRepository;
        this.deviceRepository = deviceRepository;
        this.roomService = roomService;
        this.commandService = commandService;
    }

    /**
     * Create a new automation rule.
     *
     * @param roomId the room ID
     * @param createDto the automation rule creation data
     * @param userId the user ID creating the rule
     * @return the created automation rule
     * @throws ResourceNotFoundException if room or action device not found
     * @throws UnauthorizedException if user doesn't have access to room
     * @throws ValidationException if rule data is invalid
     */
    public AutomationRuleDto createAutomationRule(UUID roomId, AutomationRuleCreateDto createDto, UUID userId) {
        logger.debug("Creating automation rule for room: {} by user: {}", roomId, userId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        Device actionDevice = deviceRepository.findById(createDto.getActionDeviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Action device not found with ID: " + createDto.getActionDeviceId()));

        if (!actionDevice.getRoom().getRoomId().equals(roomId)) {
            throw new ValidationException("Action device must be in the same room as the automation rule");
        }

        validateAutomationRule(createDto);

        AutomationRule rule = new AutomationRule();
        rule.setRoom(room);
        rule.setName(createDto.getName());
        rule.setParameter(createDto.getParameter());
        rule.setComparator(createDto.getComparator());
        rule.setThreshold(createDto.getThreshold());
        rule.setActionDevice(actionDevice);
        rule.setActionCommand(createDto.getActionCommand());
        rule.setEnabled(createDto.getEnabled() != null ? createDto.getEnabled() : true);
        rule.setCreatedBy(userId);
        rule.setCreatedAt(LocalDateTime.now());

        AutomationRule savedRule = automationRuleRepository.save(rule);
        logger.debug("Automation rule created with ID: {}", savedRule.getRuleId());

        return convertToDto(savedRule);
    }

    /**
     * Get automation rule by ID.
     *
     * @param ruleId the rule ID
     * @param userId the user ID (for access control)
     * @return the automation rule
     * @throws ResourceNotFoundException if rule not found
     * @throws UnauthorizedException if user doesn't have access
     */
    @Transactional(readOnly = true)
    public AutomationRuleDto getAutomationRule(UUID ruleId, UUID userId) {
        logger.debug("Fetching automation rule: {} by user: {}", ruleId, userId);

        AutomationRule rule = automationRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Automation rule not found with ID: " + ruleId));

        if (!roomService.hasAccessToRoom(rule.getRoom().getRoomId(), userId)) {
            throw new UnauthorizedException("User does not have access to automation rule: " + ruleId);
        }

        return convertToDto(rule);
    }

    /**
     * Update an automation rule.
     *
     * @param ruleId the rule ID
     * @param createDto the updated rule data
     * @param userId the user ID updating the rule
     * @return the updated automation rule
     * @throws ResourceNotFoundException if rule or action device not found
     * @throws UnauthorizedException if user doesn't have access
     * @throws ValidationException if rule data is invalid
     */
    public AutomationRuleDto updateAutomationRule(UUID ruleId, AutomationRuleCreateDto createDto, UUID userId) {
        logger.debug("Updating automation rule: {} by user: {}", ruleId, userId);

        AutomationRule rule = automationRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Automation rule not found with ID: " + ruleId));

        if (!roomService.hasAccessToRoom(rule.getRoom().getRoomId(), userId)) {
            throw new UnauthorizedException("User does not have access to automation rule: " + ruleId);
        }

        Device actionDevice = deviceRepository.findById(createDto.getActionDeviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Action device not found with ID: " + createDto.getActionDeviceId()));

        if (!actionDevice.getRoom().getRoomId().equals(rule.getRoom().getRoomId())) {
            throw new ValidationException("Action device must be in the same room as the automation rule");
        }

        validateAutomationRule(createDto);

        rule.setName(createDto.getName());
        rule.setParameter(createDto.getParameter());
        rule.setComparator(createDto.getComparator());
        rule.setThreshold(createDto.getThreshold());
        rule.setActionDevice(actionDevice);
        rule.setActionCommand(createDto.getActionCommand());
        if (createDto.getEnabled() != null) {
            rule.setEnabled(createDto.getEnabled());
        }

        AutomationRule savedRule = automationRuleRepository.save(rule);
        logger.debug("Automation rule updated: {}", ruleId);

        return convertToDto(savedRule);
    }

    /**
     * Delete an automation rule.
     *
     * @param ruleId the rule ID
     * @param userId the user ID deleting the rule
     * @throws ResourceNotFoundException if rule not found
     * @throws UnauthorizedException if user doesn't have access
     */
    public void deleteAutomationRule(UUID ruleId, UUID userId) {
        logger.debug("Deleting automation rule: {} by user: {}", ruleId, userId);

        AutomationRule rule = automationRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Automation rule not found with ID: " + ruleId));

        if (!roomService.hasAccessToRoom(rule.getRoom().getRoomId(), userId)) {
            throw new UnauthorizedException("User does not have access to automation rule: " + ruleId);
        }

        automationRuleRepository.delete(rule);
        logger.debug("Automation rule deleted: {}", ruleId);
    }

    /**
     * Get automation rules for a room.
     *
     * @param roomId the room ID
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of automation rules
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public Page<AutomationRuleDto> getRoomAutomationRules(UUID roomId, UUID userId, Pageable pageable) {
        logger.debug("Fetching automation rules for room: {} by user: {}", roomId, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        return automationRuleRepository.findByRoomOrderByCreatedAtDesc(roomId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get enabled automation rules for a room.
     *
     * @param roomId the room ID
     * @param userId the user ID (for access control)
     * @return list of enabled automation rules
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public List<AutomationRuleDto> getEnabledRules(UUID roomId, UUID userId) {
        logger.debug("Fetching enabled automation rules for room: {} by user: {}", roomId, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        List<AutomationRule> enabledRules = automationRuleRepository.findEnabledRulesByRoom(roomId);
        return enabledRules.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     * Get automation rules by parameter.
     *
     * @param roomId the room ID
     * @param parameter the parameter (temperature, humidity, etc.)
     * @param userId the user ID (for access control)
     * @return list of automation rules for the parameter
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public List<AutomationRuleDto> getRulesByParameter(UUID roomId, String parameter, UUID userId) {
        logger.debug("Fetching automation rules for parameter {} in room: {} by user: {}", parameter, roomId, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        List<AutomationRule> rules = automationRuleRepository.findByRoomAndParameter(roomId, parameter);
        return rules.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     * Enable or disable an automation rule.
     *
     * @param ruleId the rule ID
     * @param enabled whether to enable or disable the rule
     * @param userId the user ID making the change
     * @return the updated automation rule
     * @throws ResourceNotFoundException if rule not found
     * @throws UnauthorizedException if user doesn't have access
     */
    public AutomationRuleDto toggleRule(UUID ruleId, boolean enabled, UUID userId) {
        logger.debug("Toggling automation rule: {} to {} by user: {}", ruleId, enabled, userId);

        AutomationRule rule = automationRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Automation rule not found with ID: " + ruleId));

        if (!roomService.hasAccessToRoom(rule.getRoom().getRoomId(), userId)) {
            throw new UnauthorizedException("User does not have access to automation rule: " + ruleId);
        }

        rule.setEnabled(enabled);
        AutomationRule savedRule = automationRuleRepository.save(rule);
        logger.debug("Automation rule {} {}", ruleId, enabled ? "enabled" : "disabled");

        return convertToDto(savedRule);
    }

    /**
     * Evaluate automation rules against sensor data.
     * This method is typically called when new sensor data is received.
     *
     * @param sensorData the sensor data to evaluate
     * @return list of triggered rule IDs
     */
    public List<UUID> evaluateRules(SensorData sensorData) {
        logger.debug("Evaluating automation rules for room: {} with sensor data from device: {}", 
                    sensorData.getRoom().getRoomId(), sensorData.getDevice().getDeviceId());

        List<AutomationRule> enabledRules = automationRuleRepository.findEnabledRulesByRoom(sensorData.getRoom().getRoomId());
        List<UUID> triggeredRules = new java.util.ArrayList<>();

        for (AutomationRule rule : enabledRules) {
            if (evaluateRule(rule, sensorData)) {
                triggeredRules.add(rule.getRuleId());
                executeRuleAction(rule, sensorData);
            }
        }

        if (!triggeredRules.isEmpty()) {
            logger.info("Triggered {} automation rules for room: {}", triggeredRules.size(), sensorData.getRoom().getRoomId());
        }

        return triggeredRules;
    }

    /**
     * Test an automation rule against current sensor data.
     *
     * @param ruleId the rule ID
     * @param userId the user ID testing the rule
     * @return test result with evaluation details
     * @throws ResourceNotFoundException if rule not found
     * @throws UnauthorizedException if user doesn't have access
     */
    @Transactional(readOnly = true)
    public RuleTestResult testRule(UUID ruleId, UUID userId) {
        logger.debug("Testing automation rule: {} by user: {}", ruleId, userId);

        AutomationRule rule = automationRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Automation rule not found with ID: " + ruleId));

        if (!roomService.hasAccessToRoom(rule.getRoom().getRoomId(), userId)) {
            throw new UnauthorizedException("User does not have access to automation rule: " + ruleId);
        }

        // Get latest sensor data for the room
        // This would typically use SensorDataService to get the latest reading
        // For now, we'll return a placeholder result
        return new RuleTestResult(
            rule.getRuleId(),
            rule.getName(),
            false, // would be actual evaluation result
            "No recent sensor data available for testing",
            null,
            null
        );
    }

    /**
     * Get automation rule statistics for a room.
     *
     * @param roomId the room ID
     * @param startTime the start time
     * @param endTime the end time
     * @param userId the user ID (for access control)
     * @return automation rule statistics
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public AutomationRuleStatistics getRuleStatistics(UUID roomId, LocalDateTime startTime, LocalDateTime endTime, UUID userId) {
        logger.debug("Fetching automation rule statistics for room: {} from {} to {} by user: {}", roomId, startTime, endTime, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        var stats = automationRuleRepository.getAutomationRuleStatistics(roomId, startTime, endTime)
                .orElse(new AutomationRuleRepository.AutomationRuleStatistics() {
                    @Override public Long getTotalRules() { return 0L; }
                    @Override public Long getEnabledRules() { return 0L; }
                    @Override public Long getTriggeredRules() { return 0L; }
                    @Override public Long getSuccessfulExecutions() { return 0L; }
                    @Override public Double getSuccessRate() { return 0.0; }
                });

        return new AutomationRuleStatistics(
            stats.getTotalRules(),
            stats.getEnabledRules(),
            stats.getTriggeredRules(),
            stats.getSuccessfulExecutions(),
            stats.getSuccessRate()
        );
    }

    /**
     * Validate automation rule data.
     *
     * @param createDto the automation rule creation data
     * @throws ValidationException if rule data is invalid
     */
    private void validateAutomationRule(AutomationRuleCreateDto createDto) {
        if (!StringUtils.hasText(createDto.getName())) {
            throw new ValidationException("Rule name cannot be empty");
        }

        if (!StringUtils.hasText(createDto.getParameter())) {
            throw new ValidationException("Parameter cannot be empty");
        }

        if (createDto.getComparator() == null) {
            throw new ValidationException("Comparator cannot be null");
        }

        if (createDto.getThreshold() == null) {
            throw new ValidationException("Threshold cannot be null");
        }

        if (createDto.getActionDeviceId() == null) {
            throw new ValidationException("Action device ID cannot be null");
        }

        if (!StringUtils.hasText(createDto.getActionCommand())) {
            throw new ValidationException("Action command cannot be empty");
        }

        // Validate parameter values
        List<String> validParameters = List.of("temperature", "humidity", "co2", "light", "substrate_moisture");
        if (!validParameters.contains(createDto.getParameter().toLowerCase())) {
            throw new ValidationException("Invalid parameter: " + createDto.getParameter());
        }

        // Validate threshold ranges based on parameter
        validateThresholdRange(createDto.getParameter(), createDto.getThreshold());
    }

    /**
     * Validate threshold range based on parameter type.
     *
     * @param parameter the parameter name
     * @param threshold the threshold value
     * @throws ValidationException if threshold is out of valid range
     */
    private void validateThresholdRange(String parameter, Double threshold) {
        switch (parameter.toLowerCase()) {
            case "temperature":
                if (threshold < -50 || threshold > 100) {
                    throw new ValidationException("Temperature threshold must be between -50°C and 100°C");
                }
                break;
            case "humidity":
                if (threshold < 0 || threshold > 100) {
                    throw new ValidationException("Humidity threshold must be between 0% and 100%");
                }
                break;
            case "co2":
                if (threshold < 0 || threshold > 10000) {
                    throw new ValidationException("CO2 threshold must be between 0 and 10000 ppm");
                }
                break;
            case "light":
                if (threshold < 0 || threshold > 100000) {
                    throw new ValidationException("Light threshold must be between 0 and 100000 lux");
                }
                break;
            case "substrate_moisture":
                if (threshold < 0 || threshold > 100) {
                    throw new ValidationException("Substrate moisture threshold must be between 0% and 100%");
                }
                break;
        }
    }

    /**
     * Evaluate a single rule against sensor data.
     *
     * @param rule the automation rule
     * @param sensorData the sensor data
     * @return true if rule is triggered
     */
    private boolean evaluateRule(AutomationRule rule, SensorData sensorData) {
        Double sensorValue = getSensorValue(rule.getParameter(), sensorData);
        if (sensorValue == null) {
            return false;
        }

        return switch (rule.getComparator()) {
            case LESS_THAN -> sensorValue < rule.getThreshold();
            case LESS_THAN_OR_EQUAL -> sensorValue <= rule.getThreshold();
            case GREATER_THAN -> sensorValue > rule.getThreshold();
            case GREATER_THAN_OR_EQUAL -> sensorValue >= rule.getThreshold();
            case EQUAL -> Math.abs(sensorValue - rule.getThreshold()) < 0.01; // Small epsilon for double comparison
        };
    }

    /**
     * Get sensor value for a specific parameter.
     *
     * @param parameter the parameter name
     * @param sensorData the sensor data
     * @return the sensor value or null if not available
     */
    private Double getSensorValue(String parameter, SensorData sensorData) {
        return switch (parameter.toLowerCase()) {
            case "temperature" -> sensorData.getTemperatureC();
            case "humidity" -> sensorData.getHumidityPct();
            case "co2" -> sensorData.getCo2Ppm();
            case "light" -> sensorData.getLightLux();
            case "substrate_moisture" -> sensorData.getSubstrateMoisture();
            default -> null;
        };
    }

    /**
     * Execute the action for a triggered rule.
     *
     * @param rule the triggered automation rule
     * @param sensorData the sensor data that triggered the rule
     */
    private void executeRuleAction(AutomationRule rule, SensorData sensorData) {
        logger.info("Executing action for triggered rule: {} in room: {}", rule.getRuleId(), rule.getRoom().getRoomId());

        try {
            // Parse action command (assuming it's JSON format)
            Map<String, Object> actionParams = parseActionCommand(rule.getActionCommand());
            
            CommandCreateDto commandDto = new CommandCreateDto();
            commandDto.setCommand(actionParams.get("command").toString());
            commandDto.setParams(actionParams);

            // Send command using CommandService
            // Note: We need a system user ID for automated actions
            UUID systemUserId = rule.getCreatedBy(); // Use rule creator as fallback
            commandService.sendCommand(rule.getActionDevice().getDeviceId(), commandDto, systemUserId);

            logger.info("Successfully executed automation rule action: {}", rule.getRuleId());
        } catch (Exception e) {
            logger.error("Failed to execute automation rule action: {}", rule.getRuleId(), e);
        }
    }

    /**
     * Parse action command string to extract command and parameters.
     *
     * @param actionCommand the action command string
     * @return parsed command parameters
     */
    private Map<String, Object> parseActionCommand(String actionCommand) {
        // Simple parsing - in a real implementation, you might use JSON parsing
        Map<String, Object> params = new HashMap<>();
        
        // Example: "turn_on" or "{\"command\":\"set_temperature\",\"temperature\":25}"
        if (actionCommand.startsWith("{")) {
            // JSON format - would use ObjectMapper in real implementation
            params.put("command", "turn_on"); // Placeholder
        } else {
            // Simple command
            params.put("command", actionCommand);
        }
        
        return params;
    }

    /**
     * Convert AutomationRule entity to AutomationRuleDto.
     *
     * @param rule the automation rule entity
     * @return the automation rule DTO
     */
    private AutomationRuleDto convertToDto(AutomationRule rule) {
        AutomationRuleDto dto = new AutomationRuleDto();
        dto.setRuleId(rule.getRuleId());
        dto.setRoomId(rule.getRoom().getRoomId());
        dto.setRoomName(rule.getRoom().getName());
        dto.setName(rule.getName());
        dto.setParameter(rule.getParameter());
        dto.setComparator(rule.getComparator());
        dto.setThreshold(rule.getThreshold());
        dto.setActionDeviceId(rule.getActionDevice().getDeviceId());
        dto.setActionDeviceName(rule.getActionDevice().getName());
        dto.setActionCommand(rule.getActionCommand());
        dto.setEnabled(rule.getEnabled());
        dto.setCreatedBy(rule.getCreatedBy());
        dto.setCreatedAt(rule.getCreatedAt());
        return dto;
    }

    /**
     * Rule test result data class.
     */
    public static class RuleTestResult {
        private final UUID ruleId;
        private final String ruleName;
        private final boolean triggered;
        private final String message;
        private final Double sensorValue;
        private final Double threshold;

        public RuleTestResult(UUID ruleId, String ruleName, boolean triggered, String message,
                             Double sensorValue, Double threshold) {
            this.ruleId = ruleId;
            this.ruleName = ruleName;
            this.triggered = triggered;
            this.message = message;
            this.sensorValue = sensorValue;
            this.threshold = threshold;
        }

        public UUID getRuleId() { return ruleId; }
        public String getRuleName() { return ruleName; }
        public boolean isTriggered() { return triggered; }
        public String getMessage() { return message; }
        public Double getSensorValue() { return sensorValue; }
        public Double getThreshold() { return threshold; }
    }

    /**
     * Automation rule statistics data class.
     */
    public static class AutomationRuleStatistics {
        private final long totalRules;
        private final long enabledRules;
        private final long triggeredRules;
        private final long successfulExecutions;
        private final double successRate;

        public AutomationRuleStatistics(long totalRules, long enabledRules, long triggeredRules,
                                       long successfulExecutions, double successRate) {
            this.totalRules = totalRules;
            this.enabledRules = enabledRules;
            this.triggeredRules = triggeredRules;
            this.successfulExecutions = successfulExecutions;
            this.successRate = successRate;
        }

        public long getTotalRules() { return totalRules; }
        public long getEnabledRules() { return enabledRules; }
        public long getTriggeredRules() { return triggeredRules; }
        public long getSuccessfulExecutions() { return successfulExecutions; }
        public double getSuccessRate() { return successRate; }
    }
}