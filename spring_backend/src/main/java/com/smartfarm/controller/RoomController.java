package com.smartfarm.controller;

import com.smartfarm.dto.RoomDto;
import com.smartfarm.dto.RoomCreateDto;
import com.smartfarm.dto.DeviceDto;
import com.smartfarm.dto.UserRoomAssignmentDto;
import com.smartfarm.dto.SensorDataDto;
import com.smartfarm.service.RoomService;
import com.smartfarm.service.DeviceService;
import com.smartfarm.service.SensorDataService;
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
 * REST controller for room management operations.
 * Handles room CRUD operations, device management, telemetry data, and user assignments.
 */
@RestController
@RequestMapping("/api/rooms")
@Validated
@Tag(name = "Rooms", description = "Room management operations")
@CrossOrigin(origins = "*", maxAge = 3600)
public class RoomController {

    private static final Logger logger = LoggerFactory.getLogger(RoomController.class);

    private final RoomService roomService;
    private final DeviceService deviceService;
    private final SensorDataService sensorDataService;

    @Autowired
    public RoomController(RoomService roomService, DeviceService deviceService, SensorDataService sensorDataService) {
        this.roomService = roomService;
        this.deviceService = deviceService;
        this.sensorDataService = sensorDataService;
    }

    /**
     * Get room by ID.
     *
     * @param roomId the room ID
     * @param authHeader authorization header
     * @return room details
     */
    @GetMapping("/{roomId}")
    @Operation(summary = "Get room by ID", description = "Get detailed information about a specific room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Room retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getRoomById(
            @PathVariable @Parameter(description = "Room ID") UUID roomId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get room by ID request: {}", roomId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to this room
            if (!roomService.hasUserAccessToRoom(currentUserId, roomId)) {
                throw new UnauthorizedException("Access denied to room");
            }

            RoomDto room = roomService.getRoomById(roomId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", room);

            logger.info("Room retrieved successfully: {}", roomId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Room not found: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Room not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to room: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to room");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve room: {}", roomId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve room");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Update an existing room.
     *
     * @param roomId the room ID
     * @param updateDto room update data
     * @param authHeader authorization header
     * @return updated room
     */
    @PutMapping("/{roomId}")
    @Operation(summary = "Update room", description = "Update an existing room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Room updated successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> updateRoom(
            @PathVariable UUID roomId,
            @Valid @RequestBody RoomCreateDto updateDto,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Update room request: {}", roomId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to this room
            if (!roomService.hasUserAccessToRoom(currentUserId, roomId)) {
                throw new UnauthorizedException("Access denied to room");
            }

            RoomDto updatedRoom = roomService.updateRoom(roomId, updateDto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Room updated successfully");
            response.put("data", updatedRoom);

            logger.info("Room updated successfully: {}", roomId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Room not found for update: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Room not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to update room: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to room");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (ValidationException e) {
            logger.warn("Room update validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "validation_error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to update room: {}", roomId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "update_failed");
            errorResponse.put("message", "Failed to update room");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Delete a room.
     *
     * @param roomId the room ID
     * @param authHeader authorization header
     * @return deletion confirmation
     */
    @DeleteMapping("/{roomId}")
    @Operation(summary = "Delete room", description = "Delete a room and all associated data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Room deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> deleteRoom(
            @PathVariable UUID roomId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Delete room request: {}", roomId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to this room
            if (!roomService.hasUserAccessToRoom(currentUserId, roomId)) {
                throw new UnauthorizedException("Access denied to room");
            }

            roomService.deleteRoom(roomId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Room deleted successfully");

            logger.info("Room deleted successfully: {}", roomId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Room not found for deletion: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Room not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to delete room: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to room");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to delete room: {}", roomId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "deletion_failed");
            errorResponse.put("message", "Failed to delete room");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get all devices in a room.
     *
     * @param roomId the room ID
     * @param page page number
     * @param size page size
     * @param deviceType filter by device type
     * @param status filter by device status
     * @param authHeader authorization header
     * @return paginated list of devices
     */
    @GetMapping("/{roomId}/devices")
    @Operation(summary = "Get devices in room", description = "Get paginated list of devices in a specific room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Devices retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getDevicesInRoom(
            @PathVariable UUID roomId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String status,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get devices in room request: {}, type: {}, status: {}", roomId, deviceType, status);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to this room
            if (!roomService.hasUserAccessToRoom(currentUserId, roomId)) {
                throw new UnauthorizedException("Access denied to room");
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
            Page<DeviceDto> devices = deviceService.getDevicesByRoom(roomId, deviceType, status, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", devices.getContent());
            response.put("pagination", Map.of(
                    "page", devices.getNumber(),
                    "size", devices.getSize(),
                    "totalElements", devices.getTotalElements(),
                    "totalPages", devices.getTotalPages(),
                    "first", devices.isFirst(),
                    "last", devices.isLast()
            ));

            logger.info("Retrieved {} devices for room: {}", devices.getContent().size(), roomId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Room not found: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Room not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to room devices: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to room");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve devices for room: {}", roomId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve devices");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get telemetry data for a room.
     *
     * @param roomId the room ID
     * @param from start date/time
     * @param to end date/time
     * @param agg aggregation level (minute, hour, day)
     * @param page page number
     * @param size page size
     * @param authHeader authorization header
     * @return paginated telemetry data
     */
    @GetMapping("/{roomId}/telemetry")
    @Operation(summary = "Get room telemetry", description = "Get time-series telemetry data for a room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Telemetry data retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getRoomTelemetry(
            @PathVariable UUID roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "hour") String agg,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(1000) int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get room telemetry request: {}, from: {}, to: {}, agg: {}", roomId, from, to, agg);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to this room
            if (!roomService.hasUserAccessToRoom(currentUserId, roomId)) {
                throw new UnauthorizedException("Access denied to room");
            }

            // Set default time range if not provided (last 24 hours)
            if (from == null) {
                from = LocalDateTime.now().minusDays(1);
            }
            if (to == null) {
                to = LocalDateTime.now();
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "recordedAt"));
            Page<SensorDataDto> telemetryData = sensorDataService.getSensorDataByRoom(roomId, from, to, pageable);

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
                    "to", to,
                    "aggregation", agg
            ));

            logger.info("Retrieved {} telemetry records for room: {}", telemetryData.getContent().size(), roomId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Room not found: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Room not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to room telemetry: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to room");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (ValidationException e) {
            logger.warn("Telemetry request validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "validation_error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve telemetry for room: {}", roomId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve telemetry data");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Assign user to room.
     *
     * @param roomId the room ID
     * @param assignmentDto user assignment data
     * @param authHeader authorization header
     * @return assignment confirmation
     */
    @PostMapping("/{roomId}/assign")
    @Operation(summary = "Assign user to room", description = "Assign a user to a room with specific role")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User assigned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Room not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> assignUserToRoom(
            @PathVariable UUID roomId,
            @Valid @RequestBody UserRoomAssignmentDto assignmentDto,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Assign user to room request: {}, user: {}, role: {}", roomId, assignmentDto.getUserId(), assignmentDto.getRole());

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to this room
            if (!roomService.hasUserAccessToRoom(currentUserId, roomId)) {
                throw new UnauthorizedException("Access denied to room");
            }

            assignmentDto.setRoomId(roomId);
            roomService.assignUserToRoom(assignmentDto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User assigned to room successfully");

            logger.info("User assigned to room successfully: {} -> {}", assignmentDto.getUserId(), roomId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Room or user not found for assignment: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Room or user not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to assign user to room: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to room");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (ValidationException e) {
            logger.warn("User assignment validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "validation_error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to assign user to room: {}", roomId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "assignment_failed");
            errorResponse.put("message", "Failed to assign user to room");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get room statistics.
     *
     * @param roomId the room ID
     * @param authHeader authorization header
     * @return room statistics
     */
    @GetMapping("/{roomId}/stats")
    @Operation(summary = "Get room statistics", description = "Get statistical information about a room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getRoomStatistics(
            @PathVariable UUID roomId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get room statistics request: {}", roomId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to this room
            if (!roomService.hasUserAccessToRoom(currentUserId, roomId)) {
                throw new UnauthorizedException("Access denied to room");
            }

            Map<String, Object> stats = roomService.getRoomStatistics(roomId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            logger.info("Room statistics retrieved successfully: {}", roomId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Room not found for statistics: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Room not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to room statistics: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to room");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve room statistics: {}", roomId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve statistics");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get current environmental conditions for a room.
     *
     * @param roomId the room ID
     * @param authHeader authorization header
     * @return current environmental conditions
     */
    @GetMapping("/{roomId}/current-conditions")
    @Operation(summary = "Get current room conditions", description = "Get current environmental conditions for a room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Current conditions retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getCurrentRoomConditions(
            @PathVariable UUID roomId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get current room conditions request: {}", roomId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to this room
            if (!roomService.hasUserAccessToRoom(currentUserId, roomId)) {
                throw new UnauthorizedException("Access denied to room");
            }

            Map<String, Object> conditions = sensorDataService.getCurrentConditions(roomId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", conditions);

            logger.info("Current room conditions retrieved successfully: {}", roomId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Room not found for current conditions: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Room not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to room conditions: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to room");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve current conditions for room: {}", roomId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve current conditions");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}