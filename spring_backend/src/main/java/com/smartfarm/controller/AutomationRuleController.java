package com.smartfarm.controller;

import com.smartfarm.dto.AutomationRuleDto;
import com.smartfarm.dto.AutomationRuleCreateDto;
import com.smartfarm.dto.AutomationRuleUpdateDto;
import com.smartfarm.service.AutomationRuleService;
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
 * REST controller for automation rule operations.
 * Handles automation rule creation, management, and execution.
 */
@RestController
@RequestMapping("/api/automation")
@Validated
@Tag(name = "Automation Rules", description = "Automation rule operations")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AutomationRuleController {

    private static final Logger logger = LoggerFactory.getLogger(AutomationRuleController.class);

    private final AutomationRuleService automationRuleService;
    private final RoomService roomService;

    @Autowired
    public AutomationRuleController(AutomationRuleService automationRuleService, RoomService roomService) {
        this.automationRuleService = automationRuleService;
        this.roomService = roomService;
    }

    /**
     * Create a new automation rule.
     *
     * @param ruleDto automation rule data
     * @param authHeader authorization header
     * @return created automation rule
     */
    @PostMapping("/rules")
    @Operation(summary = "Create automation rule", description = "Create a new automation rule for a room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Automation rule created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid rule data"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> createAutomationRule(
            @Valid @RequestBody AutomationRuleCreateDto ruleDto,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Create automation rule request for room: {}", ruleDto.getRoomId());

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Set the creator
            ruleDto.setCreatedBy(currentUserId);

            AutomationRuleDto createdRule = automationRuleService.createAutomationRule(ruleDto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Automation rule created successfully");
            response.put("data", createdRule);

            logger.info("Automation rule created successfully: {}", createdRule.getRuleId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (ValidationException e) {
            logger.warn("Automation rule validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "validation_error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to create automation rule for room: {}", ruleDto.getRoomId());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to room");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to create automation rule for room: {}", ruleDto.getRoomId(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "creation_failed");
            errorResponse.put("message", "Failed to create automation rule");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get automation rules for a room.
     *
     * @param roomId the room ID
     * @param enabled filter by enabled status
     * @param parameter filter by parameter type
     * @param page page number
     * @param size page size
     * @param authHeader authorization header
     * @return paginated automation rules
     */
    @GetMapping("/rooms/{roomId}/rules")
    @Operation(summary = "Get room automation rules", description = "Get automation rules for a specific room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Automation rules retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getRoomAutomationRules(
            @PathVariable @Parameter(description = "Room ID") UUID roomId,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String parameter,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get room automation rules request: {}, enabled: {}, parameter: {}", roomId, enabled, parameter);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to the room
            if (!roomService.hasUserAccessToRoom(currentUserId, roomId)) {
                throw new UnauthorizedException("Access denied to room");
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<AutomationRuleDto> rules = automationRuleService.getAutomationRulesByRoom(roomId, enabled, parameter, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", rules.getContent());
            response.put("pagination", Map.of(
                    "page", rules.getNumber(),
                    "size", rules.getSize(),
                    "totalElements", rules.getTotalElements(),
                    "totalPages", rules.getTotalPages(),
                    "first", rules.isFirst(),
                    "last", rules.isLast()
            ));

            logger.info("Retrieved {} automation rules for room: {}", rules.getContent().size(), roomId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Room not found: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Room not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to room automation rules: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to room");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve automation rules for room: {}", roomId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve automation rules");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get a specific automation rule by ID.
     *
     * @param ruleId the rule ID
     * @param authHeader authorization header
     * @return automation rule details
     */
    @GetMapping("/rules/{ruleId}")
    @Operation(summary = "Get automation rule details", description = "Get details of a specific automation rule")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Automation rule retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Rule not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getAutomationRule(
            @PathVariable UUID ruleId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get automation rule details request: {}", ruleId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            AutomationRuleDto rule = automationRuleService.getAutomationRuleById(ruleId);

            // Check if user has access to the rule (through room access)
            if (!roomService.hasUserAccessToRoom(currentUserId, rule.getRoomId())) {
                throw new UnauthorizedException("Access denied to automation rule");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", rule);

            logger.info("Automation rule details retrieved successfully: {}", ruleId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Automation rule not found: {}", ruleId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Automation rule not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to automation rule: {}", ruleId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to automation rule");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve automation rule: {}", ruleId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve automation rule");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Update an automation rule.
     *
     * @param ruleId the rule ID
     * @param updateDto rule update data
     * @param authHeader authorization header
     * @return updated automation rule
     */
    @PutMapping("/rules/{ruleId}")
    @Operation(summary = "Update automation rule", description = "Update an existing automation rule")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Automation rule updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid rule data"),
            @ApiResponse(responseCode = "404", description = "Rule not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> updateAutomationRule(
            @PathVariable UUID ruleId,
            @Valid @RequestBody AutomationRuleUpdateDto updateDto,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Update automation rule request: {}", ruleId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            AutomationRuleDto updatedRule = automationRuleService.updateAutomationRule(ruleId, updateDto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Automation rule updated successfully");
            response.put("data", updatedRule);

            logger.info("Automation rule updated successfully: {}", ruleId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Automation rule not found: {}", ruleId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Automation rule not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to update automation rule: {}", ruleId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to automation rule");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (ValidationException e) {
            logger.warn("Automation rule update validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "validation_error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to update automation rule: {}", ruleId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "update_failed");
            errorResponse.put("message", "Failed to update automation rule");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Delete an automation rule.
     *
     * @param ruleId the rule ID
     * @param authHeader authorization header
     * @return deletion confirmation
     */
    @DeleteMapping("/rules/{ruleId}")
    @Operation(summary = "Delete automation rule", description = "Delete an automation rule")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Automation rule deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Rule not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> deleteAutomationRule(
            @PathVariable UUID ruleId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Delete automation rule request: {}", ruleId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            automationRuleService.deleteAutomationRule(ruleId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Automation rule deleted successfully");

            logger.info("Automation rule deleted successfully: {}", ruleId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Automation rule not found: {}", ruleId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Automation rule not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to delete automation rule: {}", ruleId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to automation rule");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to delete automation rule: {}", ruleId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "deletion_failed");
            errorResponse.put("message", "Failed to delete automation rule");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Toggle automation rule enabled status.
     *
     * @param ruleId the rule ID
     * @param authHeader authorization header
     * @return updated automation rule
     */
    @PutMapping("/rules/{ruleId}/toggle")
    @Operation(summary = "Toggle automation rule", description = "Enable or disable an automation rule")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Automation rule toggled successfully"),
            @ApiResponse(responseCode = "404", description = "Rule not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> toggleAutomationRule(
            @PathVariable UUID ruleId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Toggle automation rule request: {}", ruleId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            AutomationRuleDto toggledRule = automationRuleService.toggleAutomationRule(ruleId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Automation rule toggled successfully");
            response.put("data", toggledRule);

            logger.info("Automation rule toggled successfully: {} -> enabled: {}", ruleId, toggledRule.getEnabled());
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Automation rule not found: {}", ruleId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Automation rule not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to toggle automation rule: {}", ruleId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to automation rule");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to toggle automation rule: {}", ruleId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "toggle_failed");
            errorResponse.put("message", "Failed to toggle automation rule");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Trigger automation rules evaluation for a room (manual trigger).
     *
     * @param roomId the room ID
     * @param authHeader authorization header
     * @return evaluation results
     */
    @PostMapping("/rooms/{roomId}/evaluate")
    @Operation(summary = "Evaluate automation rules", description = "Manually trigger automation rules evaluation for a room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Automation rules evaluated successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> evaluateAutomationRules(
            @PathVariable UUID roomId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Evaluate automation rules request for room: {}", roomId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to the room
            if (!roomService.hasUserAccessToRoom(currentUserId, roomId)) {
                throw new UnauthorizedException("Access denied to room");
            }

            List<AutomationRuleDto> triggeredRules = automationRuleService.evaluateRulesForRoom(roomId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Automation rules evaluated successfully");
            response.put("triggeredRules", triggeredRules);
            response.put("triggeredCount", triggeredRules.size());

            logger.info("Automation rules evaluated for room: {}, {} rules triggered", roomId, triggeredRules.size());
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Room not found: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Room not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to evaluate automation rules for room: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to room");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to evaluate automation rules for room: {}", roomId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "evaluation_failed");
            errorResponse.put("message", "Failed to evaluate automation rules");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get automation rule statistics for a room.
     *
     * @param roomId the room ID
     * @param authHeader authorization header
     * @return automation rule statistics
     */
    @GetMapping("/rooms/{roomId}/statistics")
    @Operation(summary = "Get automation rule statistics", description = "Get automation rule execution statistics for a room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getRoomAutomationStatistics(
            @PathVariable UUID roomId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        logger.info("Get room automation statistics request: {}", roomId);

        try {
            // TODO: Extract user ID from JWT token
            UUID currentUserId = UUID.fromString("mock-user-id"); // Mock for now

            // Check if user has access to the room
            if (!roomService.hasUserAccessToRoom(currentUserId, roomId)) {
                throw new UnauthorizedException("Access denied to room");
            }

            Map<String, Object> statistics = automationRuleService.getRoomAutomationStatistics(roomId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", statistics);

            logger.info("Automation statistics retrieved successfully for room: {}", roomId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.warn("Room not found: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "not_found");
            errorResponse.put("message", "Room not found");
            return ResponseEntity.notFound().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to room automation statistics: {}", roomId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "Access denied to room");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve automation statistics for room: {}", roomId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "retrieval_failed");
            errorResponse.put("message", "Failed to retrieve automation statistics");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}