package com.smartfarm.controller;

import com.smartfarm.dto.FarmDto;
import com.smartfarm.dto.FarmCreateDto;
import com.smartfarm.dto.RoomDto;
import com.smartfarm.dto.RoomCreateDto;
import com.smartfarm.service.FarmService;
import com.smartfarm.service.RoomService;
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
 * REST controller for farm management operations.
 * Handles farm CRUD operations, room management, and user access control.
 */
@RestController
@RequestMapping("/api/farms")
@Validated
@Tag(name = "Farms", description = "Farm management operations")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FarmController {

    private static final Logger logger = LoggerFactory.getLogger(FarmController.class);

    private final FarmService farmService;
    private final RoomService roomService;

    @Autowired
    public FarmController(FarmService farmService, RoomService roomService) {
        this.farmService = farmService;
        this.roomService = roomService;
    }

    /**
     * Get all farms accessible to the current user.
     *
     * @param page page number (0-based)
     * @param size page size
     * @param sortBy field to sort by
     * @param sortDir sort direction (asc/desc)
     * @param authHeader authorization header
     * @return paginated list of farms
     */
    @GetMapping
    @Operation(summary = "Get all farms", description = "Get paginated list of farms accessible to current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Farms retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, Object>> getAllFarms(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get all farms request - page: {}, size: {}, sortBy: {}, sortDir: {}", page, size, sortBy, sortDir);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<FarmDto> farms = farmService.getFarmsByUser(currentUserId, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", farms.getContent());
            response.put("pagination", Map.of(
                    "page", farms.getNumber(),
                    "size", farms.getSize(),
                    "totalElements", farms.getTotalElements(),
                    "totalPages", farms.getTotalPages(),
                    "first", farms.isFirst(),
                    "last", farms.isLast()
            ));

            logger.info("Retrieved {} farms for user: {}", farms.getContent().size(), currentUserId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to retrieve farms", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve farms");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get farm by ID.
     *
     * @param farmId the farm ID
     * @param authHeader authorization header
     * @return farm details
     */
    @GetMapping("/{farmId}")
    @Operation(summary = "Get farm by ID", description = "Get detailed information about a specific farm")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Farm retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Farm not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getFarmById(
            @PathVariable @Parameter(description = "Farm ID") UUID farmId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get farm by ID request: {}", farmId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            FarmDto farm = farmService.getFarmById(farmId);
            
            // Check if user has access to this farm
            if (!farmService.hasUserAccessToFarm(currentUserId, farmId)) {
                throw new UnauthorizedException("Access denied to farm");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", farm);

            logger.info("Farm retrieved successfully: {}", farmId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Farm not found: {}", farmId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Farm not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to farm: {}", farmId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to farm");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve farm: {}", farmId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve farm");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Create a new farm.
     *
     * @param createDto farm creation data
     * @param authHeader authorization header
     * @return created farm
     */
    @PostMapping
    @Operation(summary = "Create new farm", description = "Create a new farm for the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Farm created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, Object>> createFarm(
            @Valid @RequestBody FarmCreateDto createDto,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Create farm request: {}", createDto.getName());

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now
            
            createDto.setOwnerId(currentUserId);
            FarmDto createdFarm = farmService.createFarm(createDto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Farm created successfully");
            response.put("data", createdFarm);

            logger.info("Farm created successfully: {}", createdFarm.getFarmId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (ValidationException e) {
            logger.warn("Farm creation validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "validation_error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to create farm: {}", createDto.getName(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "creation_failed");
            errorResponse.put("message", "Failed to create farm");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Update an existing farm.
     *
     * @param farmId the farm ID
     * @param updateDto farm update data
     * @param authHeader authorization header
     * @return updated farm
     */
    @PutMapping("/{farmId}")
    @Operation(summary = "Update farm", description = "Update an existing farm")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Farm updated successfully"),
            @ApiResponse(responseCode = "404", description = "Farm not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> updateFarm(
            @PathVariable UUID farmId,
            @Valid @RequestBody FarmCreateDto updateDto,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Update farm request: {}", farmId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to this farm
            if (!farmService.hasUserAccessToFarm(currentUserId, farmId)) {
                throw new UnauthorizedException("Access denied to farm");
            }

            FarmDto updatedFarm = farmService.updateFarm(farmId, updateDto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Farm updated successfully");
            response.put("data", updatedFarm);

            logger.info("Farm updated successfully: {}", farmId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Farm not found for update: {}", farmId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Farm not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to update farm: {}", farmId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to farm");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (ValidationException e) {
            logger.warn("Farm update validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "validation_error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to update farm: {}", farmId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "update_failed");
            errorResponse.put("message", "Failed to update farm");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Delete a farm.
     *
     * @param farmId the farm ID
     * @param authHeader authorization header
     * @return deletion confirmation
     */
    @DeleteMapping("/{farmId}")
    @Operation(summary = "Delete farm", description = "Delete a farm and all associated data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Farm deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Farm not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> deleteFarm(
            @PathVariable UUID farmId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Delete farm request: {}", farmId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to this farm
            if (!farmService.hasUserAccessToFarm(currentUserId, farmId)) {
                throw new UnauthorizedException("Access denied to farm");
            }

            farmService.deleteFarm(farmId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Farm deleted successfully");

            logger.info("Farm deleted successfully: {}", farmId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Farm not found for deletion: {}", farmId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Farm not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to delete farm: {}", farmId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to farm");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to delete farm: {}", farmId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "deletion_failed");
            errorResponse.put("message", "Failed to delete farm");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get all rooms in a farm.
     *
     * @param farmId the farm ID
     * @param page page number
     * @param size page size
     * @param stage filter by room stage
     * @param authHeader authorization header
     * @return paginated list of rooms
     */
    @GetMapping("/{farmId}/rooms")
    @Operation(summary = "Get rooms in farm", description = "Get paginated list of rooms in a specific farm")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rooms retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Farm not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getRoomsInFarm(
            @PathVariable UUID farmId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String stage,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get rooms in farm request: {}, stage: {}", farmId, stage);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to this farm
            if (!farmService.hasUserAccessToFarm(currentUserId, farmId)) {
                throw new UnauthorizedException("Access denied to farm");
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
            Page<RoomDto> rooms;
            
            if (stage != null && !stage.trim().isEmpty()) {
                rooms = roomService.getRoomsByFarmAndStage(farmId, stage, pageable);
            } else {
                rooms = roomService.getRoomsByFarm(farmId, pageable);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", rooms.getContent());
            response.put("pagination", Map.of(
                    "page", rooms.getNumber(),
                    "size", rooms.getSize(),
                    "totalElements", rooms.getTotalElements(),
                    "totalPages", rooms.getTotalPages(),
                    "first", rooms.isFirst(),
                    "last", rooms.isLast()
            ));

            logger.info("Retrieved {} rooms for farm: {}", rooms.getContent().size(), farmId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Farm not found: {}", farmId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Farm not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to farm rooms: {}", farmId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to farm");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve rooms for farm: {}", farmId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve rooms");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Create a new room in a farm.
     *
     * @param farmId the farm ID
     * @param createDto room creation data
     * @param authHeader authorization header
     * @return created room
     */
    @PostMapping("/{farmId}/rooms")
    @Operation(summary = "Create room in farm", description = "Create a new room in a specific farm")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Room created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Farm not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> createRoomInFarm(
            @PathVariable UUID farmId,
            @Valid @RequestBody RoomCreateDto createDto,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Create room in farm request: {}, room: {}", farmId, createDto.getName());

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to this farm
            if (!farmService.hasUserAccessToFarm(currentUserId, farmId)) {
                throw new UnauthorizedException("Access denied to farm");
            }

            createDto.setFarmId(farmId);
            RoomDto createdRoom = roomService.createRoom(createDto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Room created successfully");
            response.put("data", createdRoom);

            logger.info("Room created successfully: {}", createdRoom.getRoomId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Farm not found for room creation: {}", farmId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Farm not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to create room in farm: {}", farmId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to farm");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (ValidationException e) {
            logger.warn("Room creation validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "validation_error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to create room in farm: {}", farmId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "creation_failed");
            errorResponse.put("message", "Failed to create room");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get farm statistics.
     *
     * @param farmId the farm ID
     * @param authHeader authorization header
     * @return farm statistics
     */
    @GetMapping("/{farmId}/stats")
    @Operation(summary = "Get farm statistics", description = "Get statistical information about a farm")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Farm not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getFarmStatistics(
            @PathVariable UUID farmId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get farm statistics request: {}", farmId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to this farm
            if (!farmService.hasUserAccessToFarm(currentUserId, farmId)) {
                throw new UnauthorizedException("Access denied to farm");
            }

            Map<String, Object> stats = farmService.getFarmStatistics(farmId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            logger.info("Farm statistics retrieved successfully: {}", farmId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Farm not found for statistics: {}", farmId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Farm not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to farm statistics: {}", farmId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to farm");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve farm statistics: {}", farmId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve statistics");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}