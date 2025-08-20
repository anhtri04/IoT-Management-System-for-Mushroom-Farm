package com.smartfarm.controller;

import com.smartfarm.dto.RecommendationDto;
import com.smartfarm.service.RecommendationService;
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

import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/**
 * REST controller for AI recommendation operations.
 * Handles recommendation retrieval, generation, and management.
 */
@RestController
@RequestMapping("/api/recommendations")
@Validated
@Tag(name = "Recommendations", description = "AI recommendation operations")
@CrossOrigin(origins = "*", maxAge = 3600)
public class RecommendationController {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationController.class);

    private final RecommendationService recommendationService;
    private final FarmService farmService;
    private final RoomService roomService;

    @Autowired
    public RecommendationController(RecommendationService recommendationService, 
                                  FarmService farmService, 
                                  RoomService roomService) {
        this.recommendationService = recommendationService;
        this.farmService = farmService;
        this.roomService = roomService;
    }

    /**
     * Get recommendations for a specific farm.
     *
     * @param farmId the farm ID
     * @param page page number
     * @param size page size
     * @param authHeader authorization header
     * @return paginated farm recommendations
     */
    @GetMapping("/farms/{farmId}")
    @Operation(summary = "Get farm recommendations", description = "Get AI recommendations for a specific farm")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Farm recommendations retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Farm not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getFarmRecommendations(
            @PathVariable @Parameter(description = "Farm ID") UUID farmId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get farm recommendations request: {}", farmId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to the farm
            if (!farmService.hasUserAccessToFarm(currentUserId, farmId)) {
                throw new UnauthorizedException("Access denied to farm");
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<RecommendationDto> recommendations = recommendationService.getFarmRecommendations(farmId, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", recommendations.getContent());
            response.put("pagination", Map.of(
                    "page", recommendations.getNumber(),
                    "size", recommendations.getSize(),
                    "totalElements", recommendations.getTotalElements(),
                    "totalPages", recommendations.getTotalPages(),
                    "first", recommendations.isFirst(),
                    "last", recommendations.isLast()
            ));

            logger.info("Retrieved {} recommendations for farm: {}", recommendations.getContent().size(), farmId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Farm not found: {}", farmId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Farm not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to farm recommendations: {}", farmId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to farm");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve recommendations for farm: {}", farmId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve farm recommendations");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get recommendations for a specific room.
     *
     * @param roomId the room ID
     * @param page page number
     * @param size page size
     * @param authHeader authorization header
     * @return paginated room recommendations
     */
    @GetMapping("/rooms/{roomId}")
    @Operation(summary = "Get room recommendations", description = "Get AI recommendations for a specific room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Room recommendations retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getRoomRecommendations(
            @PathVariable @Parameter(description = "Room ID") UUID roomId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get room recommendations request: {}", roomId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to the room
            if (!roomService.hasUserAccessToRoom(currentUserId, roomId)) {
                throw new UnauthorizedException("Access denied to room");
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<RecommendationDto> recommendations = recommendationService.getRoomRecommendations(roomId, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", recommendations.getContent());
            response.put("pagination", Map.of(
                    "page", recommendations.getNumber(),
                    "size", recommendations.getSize(),
                    "totalElements", recommendations.getTotalElements(),
                    "totalPages", recommendations.getTotalPages(),
                    "first", recommendations.isFirst(),
                    "last", recommendations.isLast()
            ));

            logger.info("Retrieved {} recommendations for room: {}", recommendations.getContent().size(), roomId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Room not found: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Room not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to room recommendations: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to room");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve recommendations for room: {}", roomId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve room recommendations");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get a specific recommendation by ID.
     *
     * @param recommendationId the recommendation ID
     * @param authHeader authorization header
     * @return recommendation details
     */
    @GetMapping("/{recommendationId}")
    @Operation(summary = "Get recommendation details", description = "Get details of a specific recommendation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recommendation retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Recommendation not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getRecommendation(
            @PathVariable UUID recommendationId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get recommendation details request: {}", recommendationId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            RecommendationDto recommendation = recommendationService.getRecommendationById(recommendationId);

            // Check if user has access to the recommendation (through farm access)
            if (recommendation.getFarmId() != null && 
                !farmService.hasUserAccessToFarm(currentUserId, recommendation.getFarmId())) {
                throw new UnauthorizedException("Access denied to recommendation");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", recommendation);

            logger.info("Recommendation details retrieved successfully: {}", recommendationId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Recommendation not found: {}", recommendationId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Recommendation not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to recommendation: {}", recommendationId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to recommendation");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve recommendation: {}", recommendationId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve recommendation");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Generate new recommendations for a room.
     *
     * @param roomId the room ID
     * @param authHeader authorization header
     * @return generation confirmation
     */
    @PostMapping("/rooms/{roomId}/generate")
    @Operation(summary = "Generate room recommendations", description = "Trigger AI recommendation generation for a room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recommendation generation triggered successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> generateRoomRecommendations(
            @PathVariable @Parameter(description = "Room ID") UUID roomId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Generate room recommendations request: {}", roomId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to the room
            if (!roomService.hasUserAccessToRoom(currentUserId, roomId)) {
                throw new UnauthorizedException("Access denied to room");
            }

            RecommendationDto newRecommendation = recommendationService.generateRoomRecommendations(roomId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Recommendation generation triggered successfully");
            response.put("data", newRecommendation);

            logger.info("Recommendation generation triggered for room: {}", roomId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Room not found: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Room not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to generate recommendations for room: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to room");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to generate recommendations for room: {}", roomId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "generation_failed");
            errorResponse.put("message", "Failed to generate recommendations");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Generate new recommendations for a farm.
     *
     * @param farmId the farm ID
     * @param authHeader authorization header
     * @return generation confirmation
     */
    @PostMapping("/farms/{farmId}/generate")
    @Operation(summary = "Generate farm recommendations", description = "Trigger AI recommendation generation for a farm")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recommendation generation triggered successfully"),
            @ApiResponse(responseCode = "404", description = "Farm not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> generateFarmRecommendations(
            @PathVariable @Parameter(description = "Farm ID") UUID farmId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Generate farm recommendations request: {}", farmId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to the farm
            if (!farmService.hasUserAccessToFarm(currentUserId, farmId)) {
                throw new UnauthorizedException("Access denied to farm");
            }

            RecommendationDto newRecommendation = recommendationService.generateFarmRecommendations(farmId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Recommendation generation triggered successfully");
            response.put("data", newRecommendation);

            logger.info("Recommendation generation triggered for farm: {}", farmId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Farm not found: {}", farmId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Farm not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to generate recommendations for farm: {}", farmId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to farm");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to generate recommendations for farm: {}", farmId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "generation_failed");
            errorResponse.put("message", "Failed to generate recommendations");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Accept a recommendation and execute its suggested actions.
     *
     * @param recommendationId the recommendation ID
     * @param authHeader authorization header
     * @return acceptance confirmation
     */
    @PostMapping("/{recommendationId}/accept")
    @Operation(summary = "Accept recommendation", description = "Accept and execute a recommendation's suggested actions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recommendation accepted successfully"),
            @ApiResponse(responseCode = "404", description = "Recommendation not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> acceptRecommendation(
            @PathVariable UUID recommendationId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Accept recommendation request: {}", recommendationId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            RecommendationDto acceptedRecommendation = recommendationService.acceptRecommendation(
                    recommendationId, currentUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Recommendation accepted and actions executed");
            response.put("data", acceptedRecommendation);

            logger.info("Recommendation accepted successfully: {}", recommendationId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Recommendation not found: {}", recommendationId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Recommendation not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to accept recommendation: {}", recommendationId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to recommendation");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to accept recommendation: {}", recommendationId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "acceptance_failed");
            errorResponse.put("message", "Failed to accept recommendation");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Reject a recommendation.
     *
     * @param recommendationId the recommendation ID
     * @param authHeader authorization header
     * @return rejection confirmation
     */
    @PostMapping("/{recommendationId}/reject")
    @Operation(summary = "Reject recommendation", description = "Reject a recommendation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recommendation rejected successfully"),
            @ApiResponse(responseCode = "404", description = "Recommendation not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> rejectRecommendation(
            @PathVariable UUID recommendationId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Reject recommendation request: {}", recommendationId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            RecommendationDto rejectedRecommendation = recommendationService.rejectRecommendation(
                    recommendationId, currentUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Recommendation rejected successfully");
            response.put("data", rejectedRecommendation);

            logger.info("Recommendation rejected successfully: {}", recommendationId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Recommendation not found: {}", recommendationId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Recommendation not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to reject recommendation: {}", recommendationId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to recommendation");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to reject recommendation: {}", recommendationId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "rejection_failed");
            errorResponse.put("message", "Failed to reject recommendation");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get recommendation statistics for a farm.
     *
     * @param farmId the farm ID
     * @param authHeader authorization header
     * @return recommendation statistics
     */
    @GetMapping("/farms/{farmId}/statistics")
    @Operation(summary = "Get recommendation statistics", description = "Get recommendation statistics for a farm")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Farm not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getFarmRecommendationStatistics(
            @PathVariable UUID farmId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get farm recommendation statistics request: {}", farmId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to the farm
            if (!farmService.hasUserAccessToFarm(currentUserId, farmId)) {
                throw new UnauthorizedException("Access denied to farm");
            }

            Map<String, Object> statistics = recommendationService.getFarmRecommendationStatistics(farmId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", statistics);

            logger.info("Recommendation statistics retrieved successfully for farm: {}", farmId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Farm not found: {}", farmId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Farm not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to farm recommendation statistics: {}", farmId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to farm");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve recommendation statistics for farm: {}", farmId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve recommendation statistics");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}