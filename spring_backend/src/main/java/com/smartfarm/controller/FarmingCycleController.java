package com.smartfarm.controller;

import com.smartfarm.dto.FarmingCycleDto;
import com.smartfarm.dto.FarmingCycleCreateDto;
import com.smartfarm.dto.FarmingCycleUpdateDto;
import com.smartfarm.service.FarmingCycleService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/**
 * REST controller for farming cycle operations.
 * Handles farming cycle creation, management, and tracking.
 */
@RestController
@RequestMapping("/api/farming-cycles")
@Validated
@Tag(name = "Farming Cycles", description = "Farming cycle operations")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FarmingCycleController {

    private static final Logger logger = LoggerFactory.getLogger(FarmingCycleController.class);

    private final FarmingCycleService farmingCycleService;
    private final RoomService roomService;

    @Autowired
    public FarmingCycleController(FarmingCycleService farmingCycleService, RoomService roomService) {
        this.farmingCycleService = farmingCycleService;
        this.roomService = roomService;
    }

    /**
     * Create a new farming cycle.
     *
     * @param cycleDto farming cycle data
     * @param authHeader authorization header
     * @return created farming cycle
     */
    @PostMapping
    @Operation(summary = "Create farming cycle", description = "Create a new farming cycle for a room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Farming cycle created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid cycle data"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> createFarmingCycle(
            @Valid @RequestBody FarmingCycleCreateDto cycleDto,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Create farming cycle request for room: {}", cycleDto.getRoomId());

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to the room
            if (!roomService.hasUserAccessToRoom(currentUserId, cycleDto.getRoomId())) {
                throw new UnauthorizedException("Access denied to room");
            }

            FarmingCycleDto createdCycle = farmingCycleService.createFarmingCycle(cycleDto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Farming cycle created successfully");
            response.put("data", createdCycle);

            logger.info("Farming cycle created successfully: {}", createdCycle.getCycleId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (ValidationException e) {
            logger.warn("Farming cycle validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "validation_error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to create farming cycle for room: {}", cycleDto.getRoomId());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to room");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to create farming cycle for room: {}", cycleDto.getRoomId(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "creation_failed");
            errorResponse.put("message", "Failed to create farming cycle");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get farming cycles for a room.
     *
     * @param roomId the room ID
     * @param status filter by status
     * @param variety filter by mushroom variety
     * @param page page number
     * @param size page size
     * @param authHeader authorization header
     * @return paginated farming cycles
     */
    @GetMapping("/rooms/{roomId}")
    @Operation(summary = "Get room farming cycles", description = "Get farming cycles for a specific room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Farming cycles retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getRoomFarmingCycles(
            @PathVariable @Parameter(description = "Room ID") UUID roomId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String variety,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get room farming cycles request: {}, status: {}, variety: {}", roomId, status, variety);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to the room
            if (!roomService.hasUserAccessToRoom(currentUserId, roomId)) {
                throw new UnauthorizedException("Access denied to room");
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startDate"));
            Page<FarmingCycleDto> cycles = farmingCycleService.getFarmingCyclesByRoom(roomId, status, variety, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", cycles.getContent());
            response.put("pagination", Map.of(
                    "page", cycles.getNumber(),
                    "size", cycles.getSize(),
                    "totalElements", cycles.getTotalElements(),
                    "totalPages", cycles.getTotalPages(),
                    "first", cycles.isFirst(),
                    "last", cycles.isLast()
            ));

            logger.info("Retrieved {} farming cycles for room: {}", cycles.getContent().size(), roomId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Room not found: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Room not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to room farming cycles: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to room");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve farming cycles for room: {}", roomId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve farming cycles");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get a specific farming cycle by ID.
     *
     * @param cycleId the cycle ID
     * @param authHeader authorization header
     * @return farming cycle details
     */
    @GetMapping("/{cycleId}")
    @Operation(summary = "Get farming cycle details", description = "Get details of a specific farming cycle")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Farming cycle retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Cycle not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getFarmingCycle(
            @PathVariable UUID cycleId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get farming cycle details request: {}", cycleId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            FarmingCycleDto cycle = farmingCycleService.getFarmingCycleById(cycleId);

            // Check if user has access to the cycle (through room access)
            if (!roomService.hasUserAccessToRoom(currentUserId, cycle.getRoomId())) {
                throw new UnauthorizedException("Access denied to farming cycle");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", cycle);

            logger.info("Farming cycle details retrieved successfully: {}", cycleId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Farming cycle not found: {}", cycleId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Farming cycle not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to farming cycle: {}", cycleId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to farming cycle");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve farming cycle: {}", cycleId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve farming cycle");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Update a farming cycle.
     *
     * @param cycleId the cycle ID
     * @param updateDto cycle update data
     * @param authHeader authorization header
     * @return updated farming cycle
     */
    @PutMapping("/{cycleId}")
    @Operation(summary = "Update farming cycle", description = "Update an existing farming cycle")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Farming cycle updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid cycle data"),
            @ApiResponse(responseCode = "404", description = "Cycle not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> updateFarmingCycle(
            @PathVariable UUID cycleId,
            @Valid @RequestBody FarmingCycleUpdateDto updateDto,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Update farming cycle request: {}", cycleId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            FarmingCycleDto updatedCycle = farmingCycleService.updateFarmingCycle(cycleId, updateDto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Farming cycle updated successfully");
            response.put("data", updatedCycle);

            logger.info("Farming cycle updated successfully: {}", cycleId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Farming cycle not found: {}", cycleId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Farming cycle not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to update farming cycle: {}", cycleId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to farming cycle");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (ValidationException e) {
            logger.warn("Farming cycle update validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "validation_error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to update farming cycle: {}", cycleId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "update_failed");
            errorResponse.put("message", "Failed to update farming cycle");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Delete a farming cycle.
     *
     * @param cycleId the cycle ID
     * @param authHeader authorization header
     * @return deletion confirmation
     */
    @DeleteMapping("/{cycleId}")
    @Operation(summary = "Delete farming cycle", description = "Delete a farming cycle")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Farming cycle deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Cycle not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> deleteFarmingCycle(
            @PathVariable UUID cycleId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Delete farming cycle request: {}", cycleId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            farmingCycleService.deleteFarmingCycle(cycleId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Farming cycle deleted successfully");

            logger.info("Farming cycle deleted successfully: {}", cycleId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Farming cycle not found: {}", cycleId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Farming cycle not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to delete farming cycle: {}", cycleId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to farming cycle");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to delete farming cycle: {}", cycleId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "deletion_failed");
            errorResponse.put("message", "Failed to delete farming cycle");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get current active farming cycle for a room.
     *
     * @param roomId the room ID
     * @param authHeader authorization header
     * @return current active farming cycle
     */
    @GetMapping("/rooms/{roomId}/current")
    @Operation(summary = "Get current farming cycle", description = "Get the current active farming cycle for a room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Current farming cycle retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No active cycle found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getCurrentFarmingCycle(
            @PathVariable UUID roomId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get current farming cycle request for room: {}", roomId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to the room
            if (!roomService.hasUserAccessToRoom(currentUserId, roomId)) {
                throw new UnauthorizedException("Access denied to room");
            }

            FarmingCycleDto currentCycle = farmingCycleService.getCurrentFarmingCycle(roomId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", currentCycle);

            logger.info("Current farming cycle retrieved successfully for room: {}", roomId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("No active farming cycle found for room: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "No active farming cycle found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to room farming cycle: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to room");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve current farming cycle for room: {}", roomId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve current farming cycle");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Complete a farming cycle (mark as harvested).
     *
     * @param cycleId the cycle ID
     * @param authHeader authorization header
     * @return completed farming cycle
     */
    @PutMapping("/{cycleId}/complete")
    @Operation(summary = "Complete farming cycle", description = "Mark a farming cycle as completed/harvested")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Farming cycle completed successfully"),
            @ApiResponse(responseCode = "404", description = "Cycle not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> completeFarmingCycle(
            @PathVariable UUID cycleId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Complete farming cycle request: {}", cycleId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            FarmingCycleDto completedCycle = farmingCycleService.completeFarmingCycle(cycleId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Farming cycle completed successfully");
            response.put("data", completedCycle);

            logger.info("Farming cycle completed successfully: {}", cycleId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Farming cycle not found: {}", cycleId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Farming cycle not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to complete farming cycle: {}", cycleId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to farming cycle");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to complete farming cycle: {}", cycleId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "completion_failed");
            errorResponse.put("message", "Failed to complete farming cycle");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get farming cycle statistics for a room.
     *
     * @param roomId the room ID
     * @param authHeader authorization header
     * @return farming cycle statistics
     */
    @GetMapping("/rooms/{roomId}/statistics")
    @Operation(summary = "Get farming cycle statistics", description = "Get farming cycle statistics for a room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getRoomFarmingStatistics(
            @PathVariable UUID roomId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get room farming statistics request: {}", roomId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to the room
            if (!roomService.hasUserAccessToRoom(currentUserId, roomId)) {
                throw new UnauthorizedException("Access denied to room");
            }

            Map<String, Object> statistics = farmingCycleService.getRoomFarmingStatistics(roomId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", statistics);

            logger.info("Farming statistics retrieved successfully for room: {}", roomId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Room not found: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Room not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to room farming statistics: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to room");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve farming statistics for room: {}", roomId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve farming statistics");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}