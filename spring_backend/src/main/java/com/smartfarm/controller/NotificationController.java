package com.smartfarm.controller;

import com.smartfarm.dto.NotificationDto;
import com.smartfarm.service.NotificationService;
import com.smartfarm.service.FarmService;
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
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/**
 * REST controller for notification operations.
 * Handles notification retrieval, acknowledgment, and management.
 */
@RestController
@RequestMapping("/api/notifications")
@Validated
@Tag(name = "Notifications", description = "Notification operations")
@CrossOrigin(origins = "*", maxAge = 3600)
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;
    private final FarmService farmService;

    @Autowired
    public NotificationController(NotificationService notificationService, FarmService farmService) {
        this.notificationService = notificationService;
        this.farmService = farmService;
    }

    /**
     * Get notifications for the current user.
     *
     * @param level filter by notification level
     * @param acknowledged filter by acknowledgment status
     * @param farmId filter by farm ID
     * @param page page number
     * @param size page size
     * @param authHeader authorization header
     * @return paginated notifications
     */
    @GetMapping
    @Operation(summary = "Get user notifications", description = "Get notifications for the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getUserNotifications(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Boolean acknowledged,
            @RequestParam(required = false) UUID farmId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get user notifications request - level: {}, acknowledged: {}, farmId: {}", level, acknowledged, farmId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<NotificationDto> notifications = notificationService.getUserNotifications(
                    currentUserId, level, acknowledged, farmId, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", notifications.getContent());
            response.put("pagination", Map.of(
                    "page", notifications.getNumber(),
                    "size", notifications.getSize(),
                    "totalElements", notifications.getTotalElements(),
                    "totalPages", notifications.getTotalPages(),
                    "first", notifications.isFirst(),
                    "last", notifications.isLast()
            ));

            logger.info("Retrieved {} notifications for user: {}", notifications.getContent().size(), currentUserId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to retrieve notifications for user", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve notifications");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get notifications for a specific farm.
     *
     * @param farmId the farm ID
     * @param level filter by notification level
     * @param acknowledged filter by acknowledgment status
     * @param page page number
     * @param size page size
     * @param authHeader authorization header
     * @return paginated farm notifications
     */
    @GetMapping("/farms/{farmId}")
    @Operation(summary = "Get farm notifications", description = "Get notifications for a specific farm")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Farm notifications retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Farm not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getFarmNotifications(
            @PathVariable @Parameter(description = "Farm ID") UUID farmId,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Boolean acknowledged,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get farm notifications request: {}, level: {}, acknowledged: {}", farmId, level, acknowledged);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to the farm
            if (!farmService.hasUserAccessToFarm(currentUserId, farmId)) {
                throw new UnauthorizedException("Access denied to farm");
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<NotificationDto> notifications = notificationService.getFarmNotifications(
                    farmId, level, acknowledged, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", notifications.getContent());
            response.put("pagination", Map.of(
                    "page", notifications.getNumber(),
                    "size", notifications.getSize(),
                    "totalElements", notifications.getTotalElements(),
                    "totalPages", notifications.getTotalPages(),
                    "first", notifications.isFirst(),
                    "last", notifications.isLast()
            ));

            logger.info("Retrieved {} notifications for farm: {}", notifications.getContent().size(), farmId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Farm not found: {}", farmId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Farm not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to farm notifications: {}", farmId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to farm");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve notifications for farm: {}", farmId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve farm notifications");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get a specific notification by ID.
     *
     * @param notificationId the notification ID
     * @param authHeader authorization header
     * @return notification details
     */
    @GetMapping("/{notificationId}")
    @Operation(summary = "Get notification details", description = "Get details of a specific notification")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notification retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Notification not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getNotification(
            @PathVariable UUID notificationId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get notification details request: {}", notificationId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            NotificationDto notification = notificationService.getNotificationById(notificationId);

            // Check if user has access to the notification (through farm access)
            if (notification.getFarmId() != null && 
                !farmService.hasUserAccessToFarm(currentUserId, notification.getFarmId())) {
                throw new UnauthorizedException("Access denied to notification");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", notification);

            logger.info("Notification details retrieved successfully: {}", notificationId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Notification not found: {}", notificationId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Notification not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to notification: {}", notificationId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to notification");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve notification: {}", notificationId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve notification");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Acknowledge a notification.
     *
     * @param notificationId the notification ID
     * @param authHeader authorization header
     * @return acknowledgment confirmation
     */
    @PutMapping("/{notificationId}/acknowledge")
    @Operation(summary = "Acknowledge notification", description = "Mark a notification as acknowledged")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notification acknowledged successfully"),
            @ApiResponse(responseCode = "404", description = "Notification not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> acknowledgeNotification(
            @PathVariable UUID notificationId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Acknowledge notification request: {}", notificationId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            NotificationDto acknowledgedNotification = notificationService.acknowledgeNotification(notificationId, currentUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification acknowledged successfully");
            response.put("data", acknowledgedNotification);

            logger.info("Notification acknowledged successfully: {}", notificationId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Notification not found: {}", notificationId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Notification not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to acknowledge notification: {}", notificationId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to notification");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to acknowledge notification: {}", notificationId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "acknowledgment_failed");
            errorResponse.put("message", "Failed to acknowledge notification");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Acknowledge multiple notifications.
     *
     * @param notificationIds list of notification IDs
     * @param authHeader authorization header
     * @return bulk acknowledgment confirmation
     */
    @PutMapping("/acknowledge-bulk")
    @Operation(summary = "Acknowledge multiple notifications", description = "Mark multiple notifications as acknowledged")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notifications acknowledged successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid notification IDs"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> acknowledgeNotificationsBulk(
            @RequestBody List<UUID> notificationIds,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Bulk acknowledge notifications request: {} notifications", notificationIds.size());

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            if (notificationIds.isEmpty()) {
                throw new ValidationException("Notification IDs list cannot be empty");
            }

            List<NotificationDto> acknowledgedNotifications = notificationService.acknowledgeNotificationsBulk(
                    notificationIds, currentUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notifications acknowledged successfully");
            response.put("data", acknowledgedNotifications);
            response.put("acknowledgedCount", acknowledgedNotifications.size());

            logger.info("Bulk acknowledged {} notifications", acknowledgedNotifications.size());
            return ResponseEntity.ok(response);

        } catch (ValidationException e) {
            logger.warn("Bulk acknowledge validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "validation_error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to bulk acknowledge notifications", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "acknowledgment_failed");
            errorResponse.put("message", "Failed to acknowledge notifications");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get unread notification count for the current user.
     *
     * @param farmId filter by farm ID
     * @param authHeader authorization header
     * @return unread notification count
     */
    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count", description = "Get the count of unread notifications for the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Unread count retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getUnreadNotificationCount(
            @RequestParam(required = false) UUID farmId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get unread notification count request - farmId: {}", farmId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            long unreadCount = notificationService.getUnreadNotificationCount(currentUserId, farmId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of("unreadCount", unreadCount));

            logger.info("Unread notification count retrieved: {}", unreadCount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to retrieve unread notification count", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve unread count");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Delete a notification.
     *
     * @param notificationId the notification ID
     * @param authHeader authorization header
     * @return deletion confirmation
     */
    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Delete notification", description = "Delete a notification")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notification deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Notification not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> deleteNotification(
            @PathVariable UUID notificationId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Delete notification request: {}", notificationId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            notificationService.deleteNotification(notificationId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification deleted successfully");

            logger.info("Notification deleted successfully: {}", notificationId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Notification not found: {}", notificationId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Notification not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to delete notification: {}", notificationId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to notification");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to delete notification: {}", notificationId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "deletion_failed");
            errorResponse.put("message", "Failed to delete notification");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get notification statistics for a farm.
     *
     * @param farmId the farm ID
     * @param authHeader authorization header
     * @return notification statistics
     */
    @GetMapping("/farms/{farmId}/statistics")
    @Operation(summary = "Get notification statistics", description = "Get notification statistics for a farm")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Farm not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getFarmNotificationStatistics(
            @PathVariable UUID farmId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get farm notification statistics request: {}", farmId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to the farm
            if (!farmService.hasUserAccessToFarm(currentUserId, farmId)) {
                throw new UnauthorizedException("Access denied to farm");
            }

            Map<String, Object> statistics = notificationService.getFarmNotificationStatistics(farmId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", statistics);

            logger.info("Notification statistics retrieved successfully for farm: {}", farmId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Farm not found: {}", farmId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Farm not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to farm notification statistics: {}", farmId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to farm");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve notification statistics for farm: {}", farmId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve notification statistics");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}