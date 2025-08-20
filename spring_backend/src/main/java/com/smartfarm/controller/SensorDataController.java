package com.smartfarm.controller;

import com.smartfarm.dto.SensorDataDto;
import com.smartfarm.dto.SensorDataCreateDto;
import com.smartfarm.service.SensorDataService;
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
 * REST controller for sensor data and telemetry operations.
 * Handles telemetry data ingestion, querying, and aggregation.
 */
@RestController
@RequestMapping("/api/telemetry")
@Validated
@Tag(name = "Sensor Data", description = "Telemetry and sensor data operations")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SensorDataController {

    private static final Logger logger = LoggerFactory.getLogger(SensorDataController.class);

    private final SensorDataService sensorDataService;
    private final RoomService roomService;
    private final DeviceService deviceService;

    @Autowired
    public SensorDataController(SensorDataService sensorDataService, RoomService roomService, DeviceService deviceService) {
        this.sensorDataService = sensorDataService;
        this.roomService = roomService;
        this.deviceService = deviceService;
    }

    /**
     * Ingest telemetry data (internal endpoint for MQTT service).
     *
     * @param sensorDataDto telemetry data
     * @param internalToken internal service token
     * @return ingestion confirmation
     */
    @PostMapping("/ingest")
    @Operation(summary = "Ingest telemetry data", description = "Internal endpoint for ingesting telemetry data from IoT devices")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Telemetry data ingested successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid telemetry data"),
            @ApiResponse(responseCode = "401", description = "Invalid internal token")
    })
    public ResponseEntity<Map<String, Object>> ingestTelemetryData(
            @Valid @RequestBody SensorDataCreateDto sensorDataDto,
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken) {
        
        logger.info("Ingest telemetry data request for device: {}", sensorDataDto.getDeviceId());

        try {
            // TODO: Validate internal token for MQTT service authentication
            if (internalToken == null || !"internal-service-token".equals(internalToken)) {
                logger.warn("Invalid or missing internal token for telemetry ingestion");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "unauthorized");
                errorResponse.put("message", "Invalid internal token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            SensorDataDto ingestedData = sensorDataService.createSensorData(sensorDataDto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Telemetry data ingested successfully");
            response.put("data", ingestedData);

            logger.info("Telemetry data ingested successfully: {}", ingestedData.getReadingId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (ValidationException e) {
            logger.warn("Telemetry data validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "validation_error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to ingest telemetry data for device: {}", sensorDataDto.getDeviceId(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "ingestion_failed");
            errorResponse.put("message", "Failed to ingest telemetry data");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get telemetry data by room with time range and aggregation.
     *
     * @param roomId the room ID
     * @param from start date/time
     * @param to end date/time
     * @param aggregation aggregation type (minute, hour, day)
     * @param page page number
     * @param size page size
     * @param authHeader authorization header
     * @return paginated telemetry data
     */
    @GetMapping("/rooms/{roomId}")
    @Operation(summary = "Get room telemetry data", description = "Get aggregated telemetry data for all devices in a room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Telemetry data retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getRoomTelemetryData(
            @PathVariable @Parameter(description = "Room ID") UUID roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "hour") String aggregation,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(1000) int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get room telemetry data request: {}, aggregation: {}", roomId, aggregation);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to the room
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

            // Validate aggregation type
            if (!List.of("minute", "hour", "day").contains(aggregation.toLowerCase())) {
                throw new ValidationException("Invalid aggregation type. Must be: minute, hour, or day");
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "recordedAt"));
            Page<SensorDataDto> telemetryData;

            if ("minute".equals(aggregation.toLowerCase())) {
                telemetryData = sensorDataService.getSensorDataByRoom(roomId, from, to, pageable);
            } else {
                // For hour/day aggregation, use aggregated data service
                telemetryData = sensorDataService.getAggregatedSensorDataByRoom(roomId, from, to, aggregation, pageable);
            }

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
                    "aggregation", aggregation
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
     * Get current environmental conditions for a room.
     *
     * @param roomId the room ID
     * @param authHeader authorization header
     * @return current environmental conditions
     */
    @GetMapping("/rooms/{roomId}/current")
    @Operation(summary = "Get current room conditions", description = "Get the latest environmental conditions for a room")
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

            // Check if user has access to the room
            if (!roomService.hasUserAccessToRoom(currentUserId, roomId)) {
                throw new UnauthorizedException("Access denied to room");
            }

            Map<String, Object> currentConditions = sensorDataService.getCurrentRoomConditions(roomId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", currentConditions);

            logger.info("Current conditions retrieved successfully for room: {}", roomId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Room not found or no data available: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Room not found or no data available");
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

    /**
     * Get telemetry statistics for a room.
     *
     * @param roomId the room ID
     * @param from start date/time
     * @param to end date/time
     * @param authHeader authorization header
     * @return telemetry statistics
     */
    @GetMapping("/rooms/{roomId}/statistics")
    @Operation(summary = "Get room telemetry statistics", description = "Get statistical analysis of telemetry data for a room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getRoomTelemetryStatistics(
            @PathVariable UUID roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get room telemetry statistics request: {}", roomId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to the room
            if (!roomService.hasUserAccessToRoom(currentUserId, roomId)) {
                throw new UnauthorizedException("Access denied to room");
            }

            // Set default time range if not provided (last 7 days)
            if (from == null) {
                from = LocalDateTime.now().minusDays(7);
            }
            if (to == null) {
                to = LocalDateTime.now();
            }

            Map<String, Object> statistics = sensorDataService.getRoomTelemetryStatistics(roomId, from, to);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", statistics);
            response.put("timeRange", Map.of(
                    "from", from,
                    "to", to
            ));

            logger.info("Telemetry statistics retrieved successfully for room: {}", roomId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Room not found: {}", roomId);
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
            logger.error("Failed to retrieve statistics for room: {}", roomId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve statistics");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get telemetry data by farm with aggregation.
     *
     * @param farmId the farm ID
     * @param from start date/time
     * @param to end date/time
     * @param aggregation aggregation type (hour, day)
     * @param page page number
     * @param size page size
     * @param authHeader authorization header
     * @return paginated aggregated telemetry data
     */
    @GetMapping("/farms/{farmId}")
    @Operation(summary = "Get farm telemetry data", description = "Get aggregated telemetry data for all rooms in a farm")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Telemetry data retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Farm not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getFarmTelemetryData(
            @PathVariable UUID farmId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "day") String aggregation,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(1000) int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get farm telemetry data request: {}, aggregation: {}", farmId, aggregation);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // TODO: Check if user has access to the farm
            // This would require a FarmService method to check farm access

            // Set default time range if not provided (last 7 days)
            if (from == null) {
                from = LocalDateTime.now().minusDays(7);
            }
            if (to == null) {
                to = LocalDateTime.now();
            }

            // Validate aggregation type
            if (!List.of("hour", "day").contains(aggregation.toLowerCase())) {
                throw new ValidationException("Invalid aggregation type for farm data. Must be: hour or day");
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "recordedAt"));
            Page<SensorDataDto> telemetryData = sensorDataService.getAggregatedSensorDataByFarm(farmId, from, to, aggregation, pageable);

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
                    "aggregation", aggregation
            ));

            logger.info("Retrieved {} telemetry records for farm: {}", telemetryData.getContent().size(), farmId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Farm not found: {}", farmId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Farm not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to farm telemetry: {}", farmId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to farm");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (ValidationException e) {
            logger.warn("Farm telemetry request validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "validation_error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve telemetry for farm: {}", farmId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve telemetry data");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Delete old telemetry data (cleanup endpoint).
     *
     * @param olderThanDays delete data older than specified days
     * @param internalToken internal service token
     * @return cleanup confirmation
     */
    @DeleteMapping("/cleanup")
    @Operation(summary = "Cleanup old telemetry data", description = "Delete telemetry data older than specified days (internal use)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cleanup completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "401", description = "Invalid internal token")
    })
    public ResponseEntity<Map<String, Object>> cleanupOldTelemetryData(
            @RequestParam @Min(1) int olderThanDays,
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken) {
        
        logger.info("Cleanup old telemetry data request: older than {} days", olderThanDays);

        try {
            // TODO: Validate internal token for cleanup service authentication
            if (internalToken == null || !"internal-service-token".equals(internalToken)) {
                logger.warn("Invalid or missing internal token for telemetry cleanup");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "unauthorized");
                errorResponse.put("message", "Invalid internal token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(olderThanDays);
            long deletedCount = sensorDataService.deleteOldSensorData(cutoffDate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Telemetry data cleanup completed successfully");
            response.put("deletedRecords", deletedCount);
            response.put("cutoffDate", cutoffDate);

            logger.info("Telemetry data cleanup completed: {} records deleted", deletedCount);
            return ResponseEntity.ok(response);

        } catch (ValidationException e) {
            logger.warn("Cleanup request validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "validation_error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to cleanup telemetry data", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "cleanup_failed");
            errorResponse.put("message", "Failed to cleanup telemetry data");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}