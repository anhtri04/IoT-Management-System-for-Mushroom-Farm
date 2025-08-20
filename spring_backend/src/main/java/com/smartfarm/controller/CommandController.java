package com.smartfarm.controller;

import com.smartfarm.dto.CommandDto;
import com.smartfarm.dto.CommandCreateDto;
import com.smartfarm.service.CommandService;
import com.smartfarm.service.RoomService;
import com.smartfarm.service.DeviceService;
import com.smartfarm.exception.ResourceNotFoundException;
import com.smartfarm.exception.ValidationException;
import com.smartfarm.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/**
 * REST controller for device command operations.
 * Handles command creation, execution, and status tracking.
 */
@RestController
@RequestMapping("/api/commands")
@Validated
@Tag(name = "Commands", description = "Device command operations")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CommandController {

    private static final Logger logger = LoggerFactory.getLogger(CommandController.class);

    private final CommandService commandService;
    private final RoomService roomService;
    private final DeviceService deviceService;

    @Autowired
    public CommandController(CommandService commandService, RoomService roomService, DeviceService deviceService) {
        this.commandService = commandService;
        this.roomService = roomService;
        this.deviceService = deviceService;
    }

    /**
     * Send a command to a device.
     *
     * @param deviceId the device ID
     * @param commandDto command details
     * @param authHeader authorization header
     * @return command confirmation
     */
    @PostMapping("/devices/{deviceId}")
    @Operation(summary = "Send command to device", description = "Send a control command to a specific device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Command sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid command data"),
            @ApiResponse(responseCode = "404", description = "Device not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> sendDeviceCommand(
            @PathVariable @Parameter(description = "Device ID") UUID deviceId,
            @Valid @RequestBody CommandCreateDto commandDto,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Send command to device request: {}, command: {}", deviceId, commandDto.getCommand());

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Set the device ID and issuer
            commandDto.setDeviceId(deviceId);
            commandDto.setIssuedBy(currentUserId);

            CommandDto sentCommand = commandService.createCommand(commandDto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Command sent successfully");
            response.put("data", sentCommand);

            logger.info("Command sent successfully: {}", sentCommand.getCommandId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Device not found: {}", deviceId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Device not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to send command to device: {}", deviceId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to device");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (ValidationException e) {
            logger.warn("Command validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "validation_error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to send command to device: {}", deviceId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "command_failed");
            errorResponse.put("message", "Failed to send command");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get command history for a device.
     *
     * @param deviceId the device ID
     * @param status filter by command status
     * @param page page number
     * @param size page size
     * @param authHeader authorization header
     * @return paginated command history
     */
    @GetMapping("/devices/{deviceId}")
    @Operation(summary = "Get device command history", description = "Get command history for a specific device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Command history retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Device not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getDeviceCommandHistory(
            @PathVariable UUID deviceId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get device command history request: {}, status: {}", deviceId, status);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to the device (through room access)
            if (!deviceService.hasUserAccessToDevice(currentUserId, deviceId)) {
                throw new UnauthorizedException("Access denied to device");
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "issuedAt"));
            Page<CommandDto> commands;

            if (status != null && !status.trim().isEmpty()) {
                commands = commandService.getCommandsByDeviceAndStatus(deviceId, status, pageable);
            } else {
                commands = commandService.getCommandsByDevice(deviceId, pageable);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", commands.getContent());
            response.put("pagination", Map.of(
                    "page", commands.getNumber(),
                    "size", commands.getSize(),
                    "totalElements", commands.getTotalElements(),
                    "totalPages", commands.getTotalPages(),
                    "first", commands.isFirst(),
                    "last", commands.isLast()
            ));

            logger.info("Retrieved {} commands for device: {}", commands.getContent().size(), deviceId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Device not found: {}", deviceId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Device not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to device commands: {}", deviceId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to device");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve commands for device: {}", deviceId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve command history");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get command history for a room.
     *
     * @param roomId the room ID
     * @param status filter by command status
     * @param page page number
     * @param size page size
     * @param authHeader authorization header
     * @return paginated command history
     */
    @GetMapping("/rooms/{roomId}")
    @Operation(summary = "Get room command history", description = "Get command history for all devices in a room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Command history retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getRoomCommandHistory(
            @PathVariable UUID roomId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get room command history request: {}, status: {}", roomId, status);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to the room
            if (!roomService.hasUserAccessToRoom(currentUserId, roomId)) {
                throw new UnauthorizedException("Access denied to room");
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "issuedAt"));
            Page<CommandDto> commands;

            if (status != null && !status.trim().isEmpty()) {
                commands = commandService.getCommandsByRoomAndStatus(roomId, status, pageable);
            } else {
                commands = commandService.getCommandsByRoom(roomId, pageable);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", commands.getContent());
            response.put("pagination", Map.of(
                    "page", commands.getNumber(),
                    "size", commands.getSize(),
                    "totalElements", commands.getTotalElements(),
                    "totalPages", commands.getTotalPages(),
                    "first", commands.isFirst(),
                    "last", commands.isLast()
            ));

            logger.info("Retrieved {} commands for room: {}", commands.getContent().size(), roomId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Room not found: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Room not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to room commands: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to room");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve commands for room: {}", roomId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve command history");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get a specific command by ID.
     *
     * @param commandId the command ID
     * @param authHeader authorization header
     * @return command details
     */
    @GetMapping("/{commandId}")
    @Operation(summary = "Get command details", description = "Get details of a specific command")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Command retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Command not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getCommand(
            @PathVariable UUID commandId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get command details request: {}", commandId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            CommandDto command = commandService.getCommandById(commandId);

            // Check if user has access to the command (through device/room access)
            if (!deviceService.hasUserAccessToDevice(currentUserId, command.getDeviceId())) {
                throw new UnauthorizedException("Access denied to command");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", command);

            logger.info("Command details retrieved successfully: {}", commandId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Command not found: {}", commandId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Command not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to command: {}", commandId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to command");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve command: {}", commandId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve command");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Update command status (for device acknowledgments).
     *
     * @param commandId the command ID
     * @param statusUpdate status update data
     * @param internalToken internal service token
     * @return update confirmation
     */
    @PutMapping("/{commandId}/status")
    @Operation(summary = "Update command status", description = "Update command status (internal endpoint for device acknowledgments)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Command status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Command not found"),
            @ApiResponse(responseCode = "401", description = "Invalid internal token")
    })
    public ResponseEntity<Map<String, Object>> updateCommandStatus(
            @PathVariable UUID commandId,
            @RequestBody Map<String, String> statusUpdate,
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken) {
        
        logger.info("Update command status request: {}, status: {}", commandId, statusUpdate.get("status"));

        try {
            // TODO: Validate internal token for MQTT service authentication
            if (internalToken == null || !"internal-service-token".equals(internalToken)) {
                logger.warn("Invalid or missing internal token for command status update");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "unauthorized");
                errorResponse.put("message", "Invalid internal token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            String newStatus = statusUpdate.get("status");
            if (newStatus == null || newStatus.trim().isEmpty()) {
                throw new ValidationException("Status is required");
            }

            CommandDto updatedCommand = commandService.updateCommandStatus(commandId, newStatus);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Command status updated successfully");
            response.put("data", updatedCommand);

            logger.info("Command status updated successfully: {} -> {}", commandId, newStatus);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Command not found: {}", commandId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Command not found");
            return ResponseEntity.notFound().build();
            
        } catch (ValidationException e) {
            logger.warn("Command status update validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "validation_error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to update command status: {}", commandId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "update_failed");
            errorResponse.put("message", "Failed to update command status");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get pending commands for a device (for device polling).
     *
     * @param deviceId the device ID
     * @param internalToken internal service token
     * @return pending commands
     */
    @GetMapping("/devices/{deviceId}/pending")
    @Operation(summary = "Get pending device commands", description = "Get pending commands for a device (internal endpoint)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pending commands retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Device not found"),
            @ApiResponse(responseCode = "401", description = "Invalid internal token")
    })
    public ResponseEntity<Map<String, Object>> getPendingDeviceCommands(
            @PathVariable UUID deviceId,
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken) {
        
        logger.info("Get pending device commands request: {}", deviceId);

        try {
            // TODO: Validate internal token for device authentication
            if (internalToken == null || !"internal-service-token".equals(internalToken)) {
                logger.warn("Invalid or missing internal token for pending commands");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "unauthorized");
                errorResponse.put("message", "Invalid internal token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            List<CommandDto> pendingCommands = commandService.getPendingCommandsByDevice(deviceId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", pendingCommands);
            response.put("count", pendingCommands.size());

            logger.info("Retrieved {} pending commands for device: {}", pendingCommands.size(), deviceId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Device not found: {}", deviceId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Device not found");
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            logger.error("Failed to retrieve pending commands for device: {}", deviceId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve pending commands");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get command statistics for a room.
     *
     * @param roomId the room ID
     * @param authHeader authorization header
     * @return command statistics
     */
    @GetMapping("/rooms/{roomId}/statistics")
    @Operation(summary = "Get room command statistics", description = "Get command execution statistics for a room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getRoomCommandStatistics(
            @PathVariable UUID roomId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get room command statistics request: {}", roomId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to the room
            if (!roomService.hasUserAccessToRoom(currentUserId, roomId)) {
                throw new UnauthorizedException("Access denied to room");
            }

            Map<String, Object> statistics = commandService.getRoomCommandStatistics(roomId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", statistics);

            logger.info("Command statistics retrieved successfully for room: {}", roomId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Room not found: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Room not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to room command statistics: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to room");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve command statistics for room: {}", roomId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve command statistics");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}