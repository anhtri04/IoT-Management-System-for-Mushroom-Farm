package com.smartfarm.service;

import com.smartfarm.entity.Command;
import com.smartfarm.entity.Device;
import com.smartfarm.entity.Room;
import com.smartfarm.repository.CommandRepository;
import com.smartfarm.repository.DeviceRepository;
import com.smartfarm.repository.RoomRepository;
import com.smartfarm.exception.ResourceNotFoundException;
import com.smartfarm.exception.UnauthorizedException;
import com.smartfarm.exception.ValidationException;
import com.smartfarm.dto.CommandDto;
import com.smartfarm.dto.CommandCreateDto;
import com.smartfarm.enums.CommandStatus;
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
 * Service class for Command entity operations.
 * Handles business logic for device command management and MQTT communication.
 */
@Service
@Transactional
public class CommandService {

    private static final Logger logger = LoggerFactory.getLogger(CommandService.class);

    private final CommandRepository commandRepository;
    private final DeviceRepository deviceRepository;
    private final RoomRepository roomRepository;
    private final RoomService roomService;
    // Note: MqttService would be injected here in a complete implementation
    // private final MqttService mqttService;

    @Autowired
    public CommandService(CommandRepository commandRepository, DeviceRepository deviceRepository,
                         RoomRepository roomRepository, RoomService roomService) {
        this.commandRepository = commandRepository;
        this.deviceRepository = deviceRepository;
        this.roomRepository = roomRepository;
        this.roomService = roomService;
    }

    /**
     * Send a command to a device.
     *
     * @param deviceId the device ID
     * @param createDto the command creation data
     * @param userId the user ID issuing the command
     * @return the created command
     * @throws ResourceNotFoundException if device not found
     * @throws UnauthorizedException if user doesn't have access
     * @throws ValidationException if command is invalid
     */
    public CommandDto sendCommand(UUID deviceId, CommandCreateDto createDto, UUID userId) {
        logger.debug("Sending command to device: {} by user: {}", deviceId, userId);

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));

        if (!roomService.hasAccessToRoom(device.getRoom().getRoomId(), userId)) {
            throw new UnauthorizedException("User does not have access to device: " + deviceId);
        }

        validateCommand(createDto.getCommand(), createDto.getParams());

        Command command = new Command();
        command.setDevice(device);
        command.setRoom(device.getRoom());
        command.setFarm(device.getRoom().getFarm());
        command.setCommand(createDto.getCommand());
        command.setParams(createDto.getParams());
        command.setIssuedBy(userId);
        command.setIssuedAt(LocalDateTime.now());
        command.setStatus(CommandStatus.PENDING);

        Command savedCommand = commandRepository.save(command);
        logger.debug("Command created with ID: {}", savedCommand.getCommandId());

        // Publish command to MQTT topic
        try {
            publishCommandToMqtt(device, savedCommand);
            savedCommand.setStatus(CommandStatus.SENT);
            commandRepository.save(savedCommand);
            logger.debug("Command sent to MQTT topic for device: {}", deviceId);
        } catch (Exception e) {
            logger.error("Failed to send command to MQTT for device: {}", deviceId, e);
            savedCommand.setStatus(CommandStatus.FAILED);
            commandRepository.save(savedCommand);
            throw new RuntimeException("Failed to send command to device", e);
        }

        return convertToDto(savedCommand);
    }

    /**
     * Get commands for a device.
     *
     * @param deviceId the device ID
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of commands
     * @throws ResourceNotFoundException if device not found
     * @throws UnauthorizedException if user doesn't have access
     */
    @Transactional(readOnly = true)
    public Page<CommandDto> getDeviceCommands(UUID deviceId, UUID userId, Pageable pageable) {
        logger.debug("Fetching commands for device: {} by user: {}", deviceId, userId);

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));

        if (!roomService.hasAccessToRoom(device.getRoom().getRoomId(), userId)) {
            throw new UnauthorizedException("User does not have access to device: " + deviceId);
        }

        return commandRepository.findByDeviceOrderByIssuedAtDesc(deviceId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get commands for a room.
     *
     * @param roomId the room ID
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of commands
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public Page<CommandDto> getRoomCommands(UUID roomId, UUID userId, Pageable pageable) {
        logger.debug("Fetching commands for room: {} by user: {}", roomId, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        return commandRepository.findByRoomOrderByIssuedAtDesc(roomId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get commands by status.
     *
     * @param roomId the room ID
     * @param status the command status
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of commands
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public Page<CommandDto> getCommandsByStatus(UUID roomId, CommandStatus status, UUID userId, Pageable pageable) {
        logger.debug("Fetching commands with status {} for room: {} by user: {}", status, roomId, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        return commandRepository.findByRoomAndStatus(roomId, status, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get pending commands for a device.
     *
     * @param deviceId the device ID
     * @param userId the user ID (for access control)
     * @return list of pending commands
     * @throws ResourceNotFoundException if device not found
     * @throws UnauthorizedException if user doesn't have access
     */
    @Transactional(readOnly = true)
    public List<CommandDto> getPendingCommands(UUID deviceId, UUID userId) {
        logger.debug("Fetching pending commands for device: {} by user: {}", deviceId, userId);

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));

        if (!roomService.hasAccessToRoom(device.getRoom().getRoomId(), userId)) {
            throw new UnauthorizedException("User does not have access to device: " + deviceId);
        }

        List<Command> pendingCommands = commandRepository.findPendingCommandsByDevice(deviceId);
        return pendingCommands.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     * Acknowledge a command (typically called when device responds).
     *
     * @param commandId the command ID
     * @param success whether the command was successful
     * @param response optional response from device
     * @return the updated command
     * @throws ResourceNotFoundException if command not found
     */
    public CommandDto acknowledgeCommand(UUID commandId, boolean success, String response) {
        logger.debug("Acknowledging command: {} with success: {}", commandId, success);

        Command command = commandRepository.findById(commandId)
                .orElseThrow(() -> new ResourceNotFoundException("Command not found with ID: " + commandId));

        command.setStatus(success ? CommandStatus.ACKNOWLEDGED : CommandStatus.FAILED);
        if (StringUtils.hasText(response)) {
            // In a complete implementation, you might store the response in a separate field
            // For now, we'll add it to the params as a response field
            Map<String, Object> params = command.getParams() != null ? new HashMap<>(command.getParams()) : new HashMap<>();
            params.put("device_response", response);
            command.setParams(params);
        }

        Command savedCommand = commandRepository.save(command);
        logger.debug("Command acknowledged: {}", commandId);

        return convertToDto(savedCommand);
    }

    /**
     * Retry a failed command.
     *
     * @param commandId the command ID
     * @param userId the user ID retrying the command
     * @return the updated command
     * @throws ResourceNotFoundException if command not found
     * @throws UnauthorizedException if user doesn't have access
     * @throws ValidationException if command cannot be retried
     */
    public CommandDto retryCommand(UUID commandId, UUID userId) {
        logger.debug("Retrying command: {} by user: {}", commandId, userId);

        Command command = commandRepository.findById(commandId)
                .orElseThrow(() -> new ResourceNotFoundException("Command not found with ID: " + commandId));

        if (!roomService.hasAccessToRoom(command.getRoom().getRoomId(), userId)) {
            throw new UnauthorizedException("User does not have access to command: " + commandId);
        }

        if (command.getStatus() != CommandStatus.FAILED) {
            throw new ValidationException("Only failed commands can be retried");
        }

        command.setStatus(CommandStatus.PENDING);
        command.setIssuedAt(LocalDateTime.now());
        Command savedCommand = commandRepository.save(command);

        // Retry publishing to MQTT
        try {
            publishCommandToMqtt(command.getDevice(), savedCommand);
            savedCommand.setStatus(CommandStatus.SENT);
            commandRepository.save(savedCommand);
            logger.debug("Command retried and sent to MQTT: {}", commandId);
        } catch (Exception e) {
            logger.error("Failed to retry command to MQTT: {}", commandId, e);
            savedCommand.setStatus(CommandStatus.FAILED);
            commandRepository.save(savedCommand);
            throw new RuntimeException("Failed to retry command", e);
        }

        return convertToDto(savedCommand);
    }

    /**
     * Cancel a pending command.
     *
     * @param commandId the command ID
     * @param userId the user ID canceling the command
     * @return the updated command
     * @throws ResourceNotFoundException if command not found
     * @throws UnauthorizedException if user doesn't have access
     * @throws ValidationException if command cannot be canceled
     */
    public CommandDto cancelCommand(UUID commandId, UUID userId) {
        logger.debug("Canceling command: {} by user: {}", commandId, userId);

        Command command = commandRepository.findById(commandId)
                .orElseThrow(() -> new ResourceNotFoundException("Command not found with ID: " + commandId));

        if (!roomService.hasAccessToRoom(command.getRoom().getRoomId(), userId)) {
            throw new UnauthorizedException("User does not have access to command: " + commandId);
        }

        if (command.getStatus() != CommandStatus.PENDING && command.getStatus() != CommandStatus.SENT) {
            throw new ValidationException("Only pending or sent commands can be canceled");
        }

        command.setStatus(CommandStatus.FAILED);
        Command savedCommand = commandRepository.save(command);
        logger.debug("Command canceled: {}", commandId);

        return convertToDto(savedCommand);
    }

    /**
     * Get command statistics for a room.
     *
     * @param roomId the room ID
     * @param startTime the start time
     * @param endTime the end time
     * @param userId the user ID (for access control)
     * @return command statistics
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public CommandStatistics getCommandStatistics(UUID roomId, LocalDateTime startTime, LocalDateTime endTime, UUID userId) {
        logger.debug("Fetching command statistics for room: {} from {} to {} by user: {}", roomId, startTime, endTime, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        var stats = commandRepository.getCommandStatistics(roomId, startTime, endTime)
                .orElse(new CommandRepository.CommandStatistics() {
                    @Override public Long getTotalCommands() { return 0L; }
                    @Override public Long getSuccessfulCommands() { return 0L; }
                    @Override public Long getFailedCommands() { return 0L; }
                    @Override public Long getPendingCommands() { return 0L; }
                    @Override public Double getSuccessRate() { return 0.0; }
                });

        return new CommandStatistics(
            stats.getTotalCommands(),
            stats.getSuccessfulCommands(),
            stats.getFailedCommands(),
            stats.getPendingCommands(),
            stats.getSuccessRate()
        );
    }

    /**
     * Get recent commands for a room.
     *
     * @param roomId the room ID
     * @param hours the number of hours to look back
     * @param userId the user ID (for access control)
     * @return list of recent commands
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public List<CommandDto> getRecentCommands(UUID roomId, int hours, UUID userId) {
        logger.debug("Fetching recent commands for room: {} in last {} hours by user: {}", roomId, hours, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
        List<Command> recentCommands = commandRepository.findRecentCommandsByRoom(roomId, cutoffTime);
        return recentCommands.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     * Bulk update command status (typically used for cleanup or batch operations).
     *
     * @param roomId the room ID
     * @param oldStatus the old status
     * @param newStatus the new status
     * @param userId the user ID performing the update
     * @return number of commands updated
     * @throws UnauthorizedException if user doesn't have access to room
     */
    public long bulkUpdateCommandStatus(UUID roomId, CommandStatus oldStatus, CommandStatus newStatus, UUID userId) {
        logger.debug("Bulk updating commands from {} to {} for room: {} by user: {}", oldStatus, newStatus, roomId, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        long updatedCount = commandRepository.bulkUpdateCommandStatus(roomId, oldStatus, newStatus);
        logger.debug("Updated {} commands from {} to {} for room: {}", updatedCount, oldStatus, newStatus, roomId);

        return updatedCount;
    }

    /**
     * Delete old commands.
     *
     * @param cutoffDate the cutoff date (commands older than this will be deleted)
     * @param userId the user ID making the deletion (must be admin)
     * @return number of commands deleted
     * @throws UnauthorizedException if user is not admin
     */
    public long deleteOldCommands(LocalDateTime cutoffDate, UUID userId) {
        logger.info("Deleting commands older than {} by user: {}", cutoffDate, userId);

        // Note: In a real implementation, you would check if the user is an admin
        // This is a placeholder for the admin check
        // if (!userService.isAdmin(userId)) {
        //     throw new UnauthorizedException("Only administrators can delete old commands");
        // }

        long deletedCount = commandRepository.deleteOldCommands(cutoffDate);
        logger.info("Deleted {} old commands", deletedCount);

        return deletedCount;
    }

    /**
     * Validate command before sending.
     *
     * @param command the command string
     * @param params the command parameters
     * @throws ValidationException if command is invalid
     */
    private void validateCommand(String command, Map<String, Object> params) {
        if (!StringUtils.hasText(command)) {
            throw new ValidationException("Command cannot be empty");
        }

        // Add more command validation logic here
        // For example, validate against allowed commands, parameter types, etc.
        List<String> allowedCommands = List.of(
            "turn_on", "turn_off", "set_temperature", "set_humidity", 
            "set_light_intensity", "start_irrigation", "stop_irrigation",
            "get_status", "restart", "calibrate"
        );

        if (!allowedCommands.contains(command.toLowerCase())) {
            throw new ValidationException("Invalid command: " + command);
        }

        // Validate specific command parameters
        switch (command.toLowerCase()) {
            case "set_temperature":
                if (params == null || !params.containsKey("temperature")) {
                    throw new ValidationException("Temperature parameter is required for set_temperature command");
                }
                break;
            case "set_humidity":
                if (params == null || !params.containsKey("humidity")) {
                    throw new ValidationException("Humidity parameter is required for set_humidity command");
                }
                break;
            case "set_light_intensity":
                if (params == null || !params.containsKey("intensity")) {
                    throw new ValidationException("Intensity parameter is required for set_light_intensity command");
                }
                break;
        }
    }

    /**
     * Publish command to MQTT topic.
     * This is a placeholder implementation - in a real system, this would use MqttService.
     *
     * @param device the target device
     * @param command the command to send
     */
    private void publishCommandToMqtt(Device device, Command command) {
        // Placeholder for MQTT publishing
        // In a real implementation, this would use MqttService to publish to:
        // farm/{farm_id}/room/{room_id}/device/{device_id}/command
        
        String topic = String.format("farm/%s/room/%s/device/%s/command",
            device.getRoom().getFarm().getFarmId(),
            device.getRoom().getRoomId(),
            device.getDeviceId());
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("command_id", command.getCommandId().toString());
        payload.put("command", command.getCommand());
        payload.put("params", command.getParams());
        payload.put("timestamp", command.getIssuedAt().toString());
        
        logger.debug("Publishing command to MQTT topic: {} with payload: {}", topic, payload);
        
        // mqttService.publish(topic, payload);
        // For now, we'll just log the action
        logger.info("Command would be published to MQTT topic: {}", topic);
    }

    /**
     * Convert Command entity to CommandDto.
     *
     * @param command the command entity
     * @return the command DTO
     */
    private CommandDto convertToDto(Command command) {
        CommandDto dto = new CommandDto();
        dto.setCommandId(command.getCommandId());
        dto.setDeviceId(command.getDevice().getDeviceId());
        dto.setDeviceName(command.getDevice().getName());
        dto.setRoomId(command.getRoom().getRoomId());
        dto.setRoomName(command.getRoom().getName());
        dto.setFarmId(command.getFarm().getFarmId());
        dto.setFarmName(command.getFarm().getName());
        dto.setCommand(command.getCommand());
        dto.setParams(command.getParams());
        dto.setIssuedBy(command.getIssuedBy());
        dto.setIssuedAt(command.getIssuedAt());
        dto.setStatus(command.getStatus());
        return dto;
    }

    /**
     * Command statistics data class.
     */
    public static class CommandStatistics {
        private final long totalCommands;
        private final long successfulCommands;
        private final long failedCommands;
        private final long pendingCommands;
        private final double successRate;

        public CommandStatistics(long totalCommands, long successfulCommands, long failedCommands,
                               long pendingCommands, double successRate) {
            this.totalCommands = totalCommands;
            this.successfulCommands = successfulCommands;
            this.failedCommands = failedCommands;
            this.pendingCommands = pendingCommands;
            this.successRate = successRate;
        }

        public long getTotalCommands() { return totalCommands; }
        public long getSuccessfulCommands() { return successfulCommands; }
        public long getFailedCommands() { return failedCommands; }
        public long getPendingCommands() { return pendingCommands; }
        public double getSuccessRate() { return successRate; }
    }
}