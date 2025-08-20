package com.smartfarm.repository;

import com.smartfarm.entity.AutomationRule;
import com.smartfarm.entity.Device;
import com.smartfarm.entity.Room;
import com.smartfarm.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for AutomationRule entity operations.
 * Provides data access methods for automation rule management and evaluation.
 */
@Repository
public interface AutomationRuleRepository extends JpaRepository<AutomationRule, UUID> {

    /**
     * Find automation rules by room.
     *
     * @param room the room
     * @param pageable pagination information
     * @return page of automation rules for the room
     */
    Page<AutomationRule> findByRoom(Room room, Pageable pageable);

    /**
     * Find automation rules by room ID.
     *
     * @param roomId the room ID
     * @param pageable pagination information
     * @return page of automation rules for the room
     */
    Page<AutomationRule> findByRoomRoomId(UUID roomId, Pageable pageable);

    /**
     * Find enabled automation rules by room.
     *
     * @param room the room
     * @return list of enabled automation rules for the room
     */
    List<AutomationRule> findByRoomAndEnabledTrue(Room room);

    /**
     * Find enabled automation rules by room ID.
     *
     * @param roomId the room ID
     * @return list of enabled automation rules for the room
     */
    List<AutomationRule> findByRoomRoomIdAndEnabledTrue(UUID roomId);

    /**
     * Find automation rules by parameter type.
     *
     * @param parameter the parameter type (temperature, humidity, etc.)
     * @param pageable pagination information
     * @return page of automation rules for the parameter
     */
    Page<AutomationRule> findByParameter(AutomationRule.Parameter parameter, Pageable pageable);

    /**
     * Find enabled automation rules by parameter type.
     *
     * @param parameter the parameter type
     * @return list of enabled automation rules for the parameter
     */
    List<AutomationRule> findByParameterAndEnabledTrue(AutomationRule.Parameter parameter);

    /**
     * Find automation rules by action device.
     *
     * @param actionDevice the action device
     * @param pageable pagination information
     * @return page of automation rules using the device
     */
    Page<AutomationRule> findByActionDevice(Device actionDevice, Pageable pageable);

    /**
     * Find automation rules by action device ID.
     *
     * @param deviceId the action device ID
     * @param pageable pagination information
     * @return page of automation rules using the device
     */
    Page<AutomationRule> findByActionDeviceDeviceId(UUID deviceId, Pageable pageable);

    /**
     * Find automation rules by created by user.
     *
     * @param createdBy the user who created the rule
     * @param pageable pagination information
     * @return page of automation rules created by the user
     */
    Page<AutomationRule> findByCreatedBy(User createdBy, Pageable pageable);

    /**
     * Find automation rules by created by user ID.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of automation rules created by the user
     */
    Page<AutomationRule> findByCreatedByUserId(UUID userId, Pageable pageable);

    /**
     * Find automation rules by enabled status.
     *
     * @param enabled the enabled status
     * @param pageable pagination information
     * @return page of automation rules with the specified status
     */
    Page<AutomationRule> findByEnabled(Boolean enabled, Pageable pageable);

    /**
     * Find automation rules by priority.
     *
     * @param priority the priority level
     * @param pageable pagination information
     * @return page of automation rules with the specified priority
     */
    Page<AutomationRule> findByPriority(AutomationRule.Priority priority, Pageable pageable);

    /**
     * Find automation rules by room and parameter.
     *
     * @param roomId the room ID
     * @param parameter the parameter type
     * @return list of automation rules for the room and parameter
     */
    List<AutomationRule> findByRoomRoomIdAndParameter(UUID roomId, AutomationRule.Parameter parameter);

    /**
     * Find enabled automation rules by room and parameter.
     *
     * @param roomId the room ID
     * @param parameter the parameter type
     * @return list of enabled automation rules for the room and parameter
     */
    List<AutomationRule> findByRoomRoomIdAndParameterAndEnabledTrue(UUID roomId, AutomationRule.Parameter parameter);

    /**
     * Find automation rules that should be evaluated for a sensor value.
     *
     * @param roomId the room ID
     * @param parameter the parameter type
     * @param value the sensor value
     * @return list of automation rules that match the criteria
     */
    @Query("SELECT ar FROM AutomationRule ar " +
           "WHERE ar.room.roomId = :roomId " +
           "AND ar.parameter = :parameter " +
           "AND ar.enabled = true " +
           "AND (" +
           "  (ar.comparator = 'GT' AND :value > ar.threshold) OR " +
           "  (ar.comparator = 'LT' AND :value < ar.threshold) OR " +
           "  (ar.comparator = 'GTE' AND :value >= ar.threshold) OR " +
           "  (ar.comparator = 'LTE' AND :value <= ar.threshold) OR " +
           "  (ar.comparator = 'EQ' AND :value = ar.threshold) OR " +
           "  (ar.comparator = 'NEQ' AND :value != ar.threshold)" +
           ") " +
           "AND (ar.cooldownUntil IS NULL OR ar.cooldownUntil < CURRENT_TIMESTAMP) " +
           "ORDER BY ar.priority DESC, ar.createdAt ASC")
    List<AutomationRule> findTriggeredRules(@Param("roomId") UUID roomId,
                                           @Param("parameter") AutomationRule.Parameter parameter,
                                           @Param("value") Double value);

    /**
     * Find automation rules by name (case-insensitive search).
     *
     * @param name the rule name to search for
     * @param pageable pagination information
     * @return page of automation rules matching the name
     */
    @Query("SELECT ar FROM AutomationRule ar " +
           "WHERE LOWER(ar.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<AutomationRule> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);

    /**
     * Search automation rules by multiple criteria.
     *
     * @param roomId room ID filter (optional)
     * @param parameter parameter filter (optional)
     * @param enabled enabled status filter (optional)
     * @param priority priority filter (optional)
     * @param deviceId action device ID filter (optional)
     * @param userId created by user ID filter (optional)
     * @param pageable pagination information
     * @return page of matching automation rules
     */
    @Query("SELECT ar FROM AutomationRule ar " +
           "WHERE (:roomId IS NULL OR ar.room.roomId = :roomId) " +
           "AND (:parameter IS NULL OR ar.parameter = :parameter) " +
           "AND (:enabled IS NULL OR ar.enabled = :enabled) " +
           "AND (:priority IS NULL OR ar.priority = :priority) " +
           "AND (:deviceId IS NULL OR ar.actionDevice.deviceId = :deviceId) " +
           "AND (:userId IS NULL OR ar.createdBy.userId = :userId)")
    Page<AutomationRule> searchAutomationRules(@Param("roomId") UUID roomId,
                                              @Param("parameter") AutomationRule.Parameter parameter,
                                              @Param("enabled") Boolean enabled,
                                              @Param("priority") AutomationRule.Priority priority,
                                              @Param("deviceId") UUID deviceId,
                                              @Param("userId") UUID userId,
                                              Pageable pageable);

    /**
     * Find automation rules accessible by a user (through room access).
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of automation rules the user has access to
     */
    @Query("SELECT ar FROM AutomationRule ar " +
           "WHERE ar.room.farm.owner.userId = :userId " +
           "OR ar.room.roomId IN (" +
           "  SELECT ur.room.roomId FROM UserRoom ur " +
           "  WHERE ur.user.userId = :userId AND ur.isActive = true" +
           ")")
    Page<AutomationRule> findAutomationRulesAccessibleByUser(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Find automation rules that need cooldown reset.
     *
     * @param currentTime the current timestamp
     * @return list of automation rules with expired cooldowns
     */
    @Query("SELECT ar FROM AutomationRule ar " +
           "WHERE ar.cooldownUntil IS NOT NULL " +
           "AND ar.cooldownUntil < :currentTime")
    List<AutomationRule> findRulesWithExpiredCooldown(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Get automation rule statistics for a room.
     *
     * @param roomId the room ID
     * @return automation rule statistics for the room
     */
    @Query("SELECT " +
           "COUNT(*) as totalRules, " +
           "COUNT(CASE WHEN ar.enabled = true THEN 1 END) as enabledRules, " +
           "COUNT(CASE WHEN ar.enabled = false THEN 1 END) as disabledRules, " +
           "COUNT(CASE WHEN ar.priority = 'HIGH' THEN 1 END) as highPriorityRules, " +
           "COUNT(CASE WHEN ar.priority = 'MEDIUM' THEN 1 END) as mediumPriorityRules, " +
           "COUNT(CASE WHEN ar.priority = 'LOW' THEN 1 END) as lowPriorityRules, " +
           "COUNT(CASE WHEN ar.executionCount > 0 THEN 1 END) as executedRules, " +
           "AVG(ar.executionCount) as avgExecutionCount " +
           "FROM AutomationRule ar " +
           "WHERE ar.room.roomId = :roomId")
    Optional<AutomationRuleStatistics> getAutomationRuleStatistics(@Param("roomId") UUID roomId);

    /**
     * Get automation rule statistics for a farm.
     *
     * @param farmId the farm ID
     * @return automation rule statistics for the farm
     */
    @Query("SELECT " +
           "COUNT(*) as totalRules, " +
           "COUNT(CASE WHEN ar.enabled = true THEN 1 END) as enabledRules, " +
           "COUNT(CASE WHEN ar.enabled = false THEN 1 END) as disabledRules, " +
           "COUNT(CASE WHEN ar.priority = 'HIGH' THEN 1 END) as highPriorityRules, " +
           "COUNT(CASE WHEN ar.priority = 'MEDIUM' THEN 1 END) as mediumPriorityRules, " +
           "COUNT(CASE WHEN ar.priority = 'LOW' THEN 1 END) as lowPriorityRules, " +
           "COUNT(CASE WHEN ar.executionCount > 0 THEN 1 END) as executedRules, " +
           "AVG(ar.executionCount) as avgExecutionCount " +
           "FROM AutomationRule ar " +
           "WHERE ar.room.farm.farmId = :farmId")
    Optional<AutomationRuleStatistics> getAutomationRuleStatisticsForFarm(@Param("farmId") UUID farmId);

    /**
     * Update automation rule enabled status.
     *
     * @param ruleId the rule ID
     * @param enabled the new enabled status
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE AutomationRule ar SET ar.enabled = :enabled " +
           "WHERE ar.ruleId = :ruleId")
    int updateRuleEnabled(@Param("ruleId") UUID ruleId, @Param("enabled") Boolean enabled);

    /**
     * Update automation rule execution count.
     *
     * @param ruleId the rule ID
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE AutomationRule ar SET ar.executionCount = ar.executionCount + 1, " +
           "ar.lastExecutedAt = CURRENT_TIMESTAMP " +
           "WHERE ar.ruleId = :ruleId")
    int incrementExecutionCount(@Param("ruleId") UUID ruleId);

    /**
     * Update automation rule cooldown.
     *
     * @param ruleId the rule ID
     * @param cooldownUntil the cooldown expiration time
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE AutomationRule ar SET ar.cooldownUntil = :cooldownUntil " +
           "WHERE ar.ruleId = :ruleId")
    int updateRuleCooldown(@Param("ruleId") UUID ruleId, @Param("cooldownUntil") LocalDateTime cooldownUntil);

    /**
     * Reset cooldowns for expired rules.
     *
     * @param currentTime the current timestamp
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE AutomationRule ar SET ar.cooldownUntil = NULL " +
           "WHERE ar.cooldownUntil IS NOT NULL " +
           "AND ar.cooldownUntil < :currentTime")
    int resetExpiredCooldowns(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Bulk enable/disable automation rules for a room.
     *
     * @param roomId the room ID
     * @param enabled the new enabled status
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE AutomationRule ar SET ar.enabled = :enabled " +
           "WHERE ar.room.roomId = :roomId")
    int updateRulesEnabledForRoom(@Param("roomId") UUID roomId, @Param("enabled") Boolean enabled);

    /**
     * Count automation rules by room.
     *
     * @param room the room
     * @return count of automation rules for the room
     */
    long countByRoom(Room room);

    /**
     * Count automation rules by room ID.
     *
     * @param roomId the room ID
     * @return count of automation rules for the room
     */
    long countByRoomRoomId(UUID roomId);

    /**
     * Count enabled automation rules by room.
     *
     * @param room the room
     * @return count of enabled automation rules for the room
     */
    long countByRoomAndEnabledTrue(Room room);

    /**
     * Count automation rules by parameter.
     *
     * @param parameter the parameter type
     * @return count of automation rules for the parameter
     */
    long countByParameter(AutomationRule.Parameter parameter);

    /**
     * Count automation rules by enabled status.
     *
     * @param enabled the enabled status
     * @return count of automation rules with the specified status
     */
    long countByEnabled(Boolean enabled);

    /**
     * Count automation rules by priority.
     *
     * @param priority the priority level
     * @return count of automation rules with the specified priority
     */
    long countByPriority(AutomationRule.Priority priority);

    /**
     * Check if a rule name exists in a room.
     *
     * @param roomId the room ID
     * @param name the rule name
     * @return true if the name exists in the room
     */
    boolean existsByRoomRoomIdAndName(UUID roomId, String name);

    /**
     * Check if a rule name exists in a room (excluding a specific rule).
     *
     * @param roomId the room ID
     * @param name the rule name
     * @param excludeRuleId the rule ID to exclude from the check
     * @return true if the name exists in the room (excluding the specified rule)
     */
    boolean existsByRoomRoomIdAndNameAndRuleIdNot(UUID roomId, String name, UUID excludeRuleId);

    /**
     * Find automation rules created after a specific time.
     *
     * @param time the time threshold
     * @return list of automation rules created after the time
     */
    List<AutomationRule> findByCreatedAtAfter(LocalDateTime time);

    /**
     * Find automation rules created before a specific time.
     *
     * @param time the time threshold
     * @return list of automation rules created before the time
     */
    List<AutomationRule> findByCreatedAtBefore(LocalDateTime time);

    /**
     * Find automation rules by creation date range.
     *
     * @param startTime the start time
     * @param endTime the end time
     * @param pageable pagination information
     * @return page of automation rules in the date range
     */
    Page<AutomationRule> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Delete automation rules created before a specific date.
     *
     * @param cutoffDate the cutoff date
     * @return number of deleted records
     */
    long deleteByCreatedAtBefore(LocalDateTime cutoffDate);

    /**
     * Interface for automation rule statistics projection.
     */
    interface AutomationRuleStatistics {
        Long getTotalRules();
        Long getEnabledRules();
        Long getDisabledRules();
        Long getHighPriorityRules();
        Long getMediumPriorityRules();
        Long getLowPriorityRules();
        Long getExecutedRules();
        Double getAvgExecutionCount();
    }
}