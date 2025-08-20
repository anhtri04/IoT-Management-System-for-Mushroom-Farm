package com.smartfarm.controller;

import com.smartfarm.dto.DeviceDto;
import com.smartfarm.dto.DeviceCreateDto;
import com.smartfarm.dto.CommandDto;
import com.smartfarm.dto.CommandCreateDto;
import com.smartfarm.dto.SensorDataDto;
import com.smartfarm.service.DeviceService;
import com.smartfarm.service.CommandService;
import com.smartfarm.service.SensorDataService;
import com.smartfarm.service.RoomService;
import com.smartfarm.exception.ResourceNotFoundException;
import com.smartfarm.exception.ValidationException;
import com.smartfarm.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/**
 * REST controller for device management operations.
 * Handles device registration, commands, telemetry data, and device status management.
 */
@RestController
@RequestMapping("/api/devices")
@Validated
@Tag(name = "Devices", description = "Device management operations")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DeviceController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceController.class);

    private final DeviceService deviceService;
    private final CommandService commandService;
    private final SensorDataService sensorDataService;
    private final RoomService roomService;

    @Autowired
    public DeviceController(DeviceService deviceService, CommandService commandService, 
                           SensorDataService sensorDataService, RoomService roomService) {
        this.deviceService = deviceService;
        this.commandService = commandService;
        this.sensorDataService = sensorDataService;
        this.roomService = roomService;
    }

    /**
     * Register a new device.
     *
     * @param deviceDto device registration data
     * @param authHeader authorization header
     * @return registered device
     */
    @PostMapping
    @Operation(summary = "Register device", description = "Register a new IoT device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Device registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> registerDevice(
            @Valid @RequestBody DeviceCreateDto deviceDto,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Register device request: {} in room: {}", deviceDto.getName(), deviceDto.getRoomId());

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to the room
            if (!roomService.hasUserAccessToRoom(currentUserId, deviceDto.getRoomId())) {
                throw new UnauthorizedException("Access denied to room");
            }

            DeviceDto registeredDevice = deviceService.createDevice(deviceDto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Device registered successfully");
            response.put("data", registeredDevice);

            logger.info("Device registered successfully: {}", registeredDevice.getDeviceId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (UnauthorizedException e) {
            logger.warn("Access denied to register device in room: {}", deviceDto.getRoomId());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to room");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (ValidationException e) {
            logger.warn("Device registration validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "validation_error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to register device: {}", deviceDto.getName(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "registration_failed");
            errorResponse.put("message", "Failed to register device");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get device by ID.
     *
     * @param deviceId the device ID
     * @param authHeader authorization header
     * @return device details
     */
    @GetMapping("/{deviceId}")
    @Operation(summary = "Get device by ID", description = "Get detailed information about a specific device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Device not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getDeviceById(
            @PathVariable @Parameter(description = "Device ID") UUID deviceId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get device by ID request: {}", deviceId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            DeviceDto device = deviceService.getDeviceById(deviceId);
            
            // Check if user has access to the device's room
            if (!roomService.hasUserAccessToRoom(currentUserId, device.getRoomId())) {
                throw new UnauthorizedException("Access denied to device");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", device);

            logger.info("Device retrieved successfully: {}", deviceId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Device not found: {}", deviceId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Device not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to device: {}", deviceId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to device");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve device: {}", deviceId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve device");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Update an existing device.
     *
     * @param deviceId the device ID
     * @param updateDto device update data
     * @param authHeader authorization header
     * @return updated device
     */
    @PutMapping("/{deviceId}")
    @Operation(summary = "Update device", description = "Update an existing device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device updated successfully"),
            @ApiResponse(responseCode = "404", description = "Device not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> updateDevice(
            @PathVariable UUID deviceId,
            @Valid @RequestBody DeviceCreateDto updateDto,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Update device request: {}", deviceId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            DeviceDto existingDevice = deviceService.getDeviceById(deviceId);
            
            // Check if user has access to the device's room
            if (!roomService.hasUserAccessToRoom(currentUserId, existingDevice.getRoomId())) {
                throw new UnauthorizedException("Access denied to device");
            }

            DeviceDto updatedDevice = deviceService.updateDevice(deviceId, updateDto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Device updated successfully");
            response.put("data", updatedDevice);

            logger.info("Device updated successfully: {}", deviceId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Device not found for update: {}", deviceId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Device not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to update device: {}", deviceId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to device");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (ValidationException e) {
            logger.warn("Device update validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "validation_error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to update device: {}", deviceId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "update_failed");
            errorResponse.put("message", "Failed to update device");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Delete a device.
     *
     * @param deviceId the device ID
     * @param authHeader authorization header
     * @return deletion confirmation
     */
    @DeleteMapping("/{deviceId}")
    @Operation(summary = "Delete device", description = "Delete a device and all associated data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Device not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> deleteDevice(
            @PathVariable UUID deviceId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Delete device request: {}", deviceId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            DeviceDto existingDevice = deviceService.getDeviceById(deviceId);
            
            // Check if user has access to the device's room
            if (!roomService.hasUserAccessToRoom(currentUserId, existingDevice.getRoomId())) {
                throw new UnauthorizedException("Access denied to device");
            }

            deviceService.deleteDevice(deviceId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Device deleted successfully");

            logger.info("Device deleted successfully: {}", deviceId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Device not found for deletion: {}", deviceId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Device not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to delete device: {}", deviceId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to device");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to delete device: {}", deviceId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "deletion_failed");
            errorResponse.put("message", "Failed to delete device");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Send command to device.
     *
     * @param deviceId the device ID
     * @param commandDto command data
     * @param authHeader authorization header
     * @return command confirmation
     */
    @PostMapping("/{deviceId}/commands")
    @Operation(summary = "Send command to device", description = "Send a control command to a specific device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Command sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid command data"),
            @ApiResponse(responseCode = "404", description = "Device not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> sendCommand(
            @PathVariable UUID deviceId,
            @Valid @RequestBody CommandCreateDto commandDto,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Send command to device request: {}, command: {}", deviceId, commandDto.getCommand());

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            DeviceDto device = deviceService.getDeviceById(deviceId);
            
            // Check if user has access to the device's room
            if (!roomService.hasUserAccessToRoom(currentUserId, device.getRoomId())) {
                throw new UnauthorizedException("Access denied to device");
            }

            commandDto.setDeviceId(deviceId);
            commandDto.setIssuedBy(currentUserId);
            CommandDto sentCommand = commandService.sendCommand(commandDto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Command sent successfully");
            response.put("data", sentCommand);

            logger.info("Command sent successfully to device: {}, command ID: {}", deviceId, sentCommand.getCommandId());
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Device not found for command: {}", deviceId);
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
     * Get device commands history.
     *
     * @param deviceId the device ID
     * @param status filter by command status
     * @param page page number
     * @param size page size
     * @param authHeader authorization header
     * @return paginated list of commands
     */
    @GetMapping("/{deviceId}/commands")
    @Operation(summary = "Get device commands", description = "Get paginated list of commands for a specific device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Commands retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Device not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getDeviceCommands(
            @PathVariable UUID deviceId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get device commands request: {}, status: {}", deviceId, status);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            DeviceDto device = deviceService.getDeviceById(deviceId);
            
            // Check if user has access to the device's room
            if (!roomService.hasUserAccessToRoom(currentUserId, device.getRoomId())) {
                throw new UnauthorizedException("Access denied to device");
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "issuedAt"));
            Page<CommandDto> commands = commandService.getCommandsByDevice(deviceId, status, pageable);

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
            errorResponse.put("message", "Failed to retrieve commands");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get latest telemetry data for device.
     *
     * @param deviceId the device ID
     * @param authHeader authorization header
     * @return latest telemetry data
     */
    @GetMapping("/{deviceId}/latest")
    @Operation(summary = "Get latest device telemetry", description = "Get the most recent telemetry data from a device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Latest telemetry retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Device not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getLatestTelemetry(
            @PathVariable UUID deviceId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get latest telemetry request: {}", deviceId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            DeviceDto device = deviceService.getDeviceById(deviceId);
            
            // Check if user has access to the device's room
            if (!roomService.hasUserAccessToRoom(currentUserId, device.getRoomId())) {
                throw new UnauthorizedException("Access denied to device");
            }

            SensorDataDto latestData = sensorDataService.getLatestSensorData(deviceId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", latestData);

            logger.info("Latest telemetry retrieved successfully for device: {}", deviceId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Device not found or no telemetry data: {}", deviceId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Device not found or no telemetry data available");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to device telemetry: {}", deviceId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to device");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve latest telemetry for device: {}", deviceId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve latest telemetry");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get device telemetry history.
     *
     * @param deviceId the device ID
     * @param from start date/time
     * @param to end date/time
     * @param page page number
     * @param size page size
     * @param authHeader authorization header
     * @return paginated telemetry data
     */
    @GetMapping("/{deviceId}/telemetry")
    @Operation(summary = "Get device telemetry history", description = "Get historical telemetry data for a specific device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Telemetry data retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Device not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getDeviceTelemetry(
            @PathVariable UUID deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(1000) int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get device telemetry request: {}, from: {}, to: {}", deviceId, from, to);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            DeviceDto device = deviceService.getDeviceById(deviceId);
            
            // Check if user has access to the device's room
            if (!roomService.hasUserAccessToRoom(currentUserId, device.getRoomId())) {
                throw new UnauthorizedException("Access denied to device");
            }

            // Set default time range if not provided (last 24 hours)
            if (from == null) {
                from = LocalDateTime.now().minusDays(1);
            }
            if (to == null) {
                to = LocalDateTime.now();
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "recordedAt"));
            Page<SensorDataDto> telemetryData = sensorDataService.getSensorDataByDevice(deviceId, from, to, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", telemetryData.getContent());
            response.put("pagination", Map.of(
                    "page", telemetryData.getNumber(),
                    "size", telemetryData.getSize(),
                    "totalElements", telemetryData.getTotalElements(),
                    "totalPages", telemetryData.getTotalPages(),
                    "first", telemetryData.isFirst(),
                    "last", telemetryData.isLast()
            ));
            response.put("timeRange", Map.of(
                    "from", from,
                    "to", to
            ));

            logger.info("Retrieved {} telemetry records for device: {}", telemetryData.getContent().size(), deviceId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Device not found: {}", deviceId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Device not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to device telemetry: {}", deviceId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to device");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (ValidationException e) {
            logger.warn("Telemetry request validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "validation_error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve telemetry for device: {}", deviceId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve telemetry data");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Update device status (for internal use by MQTT service).
     *
     * @param deviceId the device ID
     * @param status new device status
     * @param authHeader authorization header
     * @return status update confirmation
     */
    @PutMapping("/{deviceId}/status")
    @Operation(summary = "Update device status", description = "Update the status of a device (internal use)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Device not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> updateDeviceStatus(
            @PathVariable UUID deviceId,
            @RequestParam String status,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Update device status request: {}, status: {}", deviceId, status);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            DeviceDto device = deviceService.getDeviceById(deviceId);
            
            // Check if user has access to the device's room
            if (!roomService.hasUserAccessToRoom(currentUserId, device.getRoomId())) {
                throw new UnauthorizedException("Access denied to device");
            }

            deviceService.updateDeviceStatus(deviceId, status);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Device status updated successfully");

            logger.info("Device status updated successfully: {} -> {}", deviceId, status);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Device not found for status update: {}", deviceId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Device not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to update device status: {}", deviceId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to device");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (ValidationException e) {
            logger.warn("Device status validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "validation_error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to update device status: {}", deviceId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "update_failed");
            errorResponse.put("message", "Failed to update device status");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}