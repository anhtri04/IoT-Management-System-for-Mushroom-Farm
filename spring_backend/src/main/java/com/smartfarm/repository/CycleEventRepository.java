package com.smartfarm.repository;

import com.smartfarm.entity.CycleEvent;
import com.smartfarm.entity.FarmingCycle;
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
 * Repository interface for CycleEvent entity operations.
 * Provides data access methods for cycle event management and tracking.
 */
@Repository
public interface CycleEventRepository extends JpaRepository<CycleEvent, UUID> {

    /**
     * Find cycle events by farming cycle.
     *
     * @param farmingCycle the farming cycle
     * @param pageable pagination information
     * @return page of cycle events for the farming cycle
     */
    Page<CycleEvent> findByFarmingCycle(FarmingCycle farmingCycle, Pageable pageable);

    /**
     * Find cycle events by farming cycle ID.
     *
     * @param cycleId the farming cycle ID
     * @param pageable pagination information
     * @return page of cycle events for the farming cycle
     */
    Page<CycleEvent> findByFarmingCycleCycleId(UUID cycleId, Pageable pageable);

    /**
     * Find cycle events by event type.
     *
     * @param eventType the event type
     * @param pageable pagination information
     * @return page of cycle events with the specified type
     */
    Page<CycleEvent> findByEventType(CycleEvent.EventType eventType, Pageable pageable);

    /**
     * Find cycle events by severity level.
     *
     * @param severity the severity level
     * @param pageable pagination information
     * @return page of cycle events with the specified severity
     */
    Page<CycleEvent> findBySeverity(CycleEvent.Severity severity, Pageable pageable);

    /**
     * Find cycle events by recorded by user.
     *
     * @param recordedBy the user who recorded the event
     * @param pageable pagination information
     * @return page of cycle events recorded by the user
     */
    Page<CycleEvent> findByRecordedBy(User recordedBy, Pageable pageable);

    /**
     * Find cycle events by recorded by user ID.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of cycle events recorded by the user
     */
    Page<CycleEvent> findByRecordedByUserId(UUID userId, Pageable pageable);

    /**
     * Find cycle events by milestone status.
     *
     * @param isMilestone the milestone status
     * @param pageable pagination information
     * @return page of cycle events with the specified milestone status
     */
    Page<CycleEvent> findByIsMilestone(Boolean isMilestone, Pageable pageable);

    /**
     * Find milestone events by farming cycle.
     *
     * @param cycleId the farming cycle ID
     * @return list of milestone events for the farming cycle
     */
    List<CycleEvent> findByFarmingCycleCycleIdAndIsMilestoneTrue(UUID cycleId);

    /**
     * Find cycle events by farming cycle and event type.
     *
     * @param cycleId the farming cycle ID
     * @param eventType the event type
     * @return list of cycle events for the farming cycle with the specified type
     */
    List<CycleEvent> findByFarmingCycleCycleIdAndEventType(UUID cycleId, CycleEvent.EventType eventType);

    /**
     * Find cycle events by farming cycle and severity.
     *
     * @param cycleId the farming cycle ID
     * @param severity the severity level
     * @return list of cycle events for the farming cycle with the specified severity
     */
    List<CycleEvent> findByFarmingCycleCycleIdAndSeverity(UUID cycleId, CycleEvent.Severity severity);

    /**
     * Find cycle events by time range.
     *
     * @param startTime the start time
     * @param endTime the end time
     * @param pageable pagination information
     * @return page of cycle events in the time range
     */
    Page<CycleEvent> findByEventTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Find cycle events by farming cycle and time range.
     *
     * @param cycleId the farming cycle ID
     * @param startTime the start time
     * @param endTime the end time
     * @param pageable pagination information
     * @return page of cycle events in the time range
     */
    Page<CycleEvent> findByFarmingCycleCycleIdAndEventTimeBetween(UUID cycleId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Find events requiring follow-up.
     *
     * @param currentTime the current timestamp
     * @return list of events requiring follow-up
     */
    @Query("SELECT ce FROM CycleEvent ce " +
           "WHERE ce.requiresFollowUp = true " +
           "AND ce.followUpCompleted = false " +
           "AND (ce.followUpDueDate IS NULL OR ce.followUpDueDate <= :currentTime) " +
           "ORDER BY ce.followUpDueDate ASC, ce.eventTime ASC")
    List<CycleEvent> findEventsRequiringFollowUp(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Find overdue follow-up events.
     *
     * @param currentTime the current timestamp
     * @return list of overdue follow-up events
     */
    @Query("SELECT ce FROM CycleEvent ce " +
           "WHERE ce.requiresFollowUp = true " +
           "AND ce.followUpCompleted = false " +
           "AND ce.followUpDueDate < :currentTime " +
           "ORDER BY ce.followUpDueDate ASC")
    List<CycleEvent> findOverdueFollowUpEvents(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Find recent events for a farming cycle.
     *
     * @param cycleId the farming cycle ID
     * @param limit the maximum number of events to return
     * @return list of recent events for the farming cycle
     */
    @Query("SELECT ce FROM CycleEvent ce " +
           "WHERE ce.farmingCycle.cycleId = :cycleId " +
           "ORDER BY ce.eventTime DESC " +
           "LIMIT :limit")
    List<CycleEvent> findRecentEventsForCycle(@Param("cycleId") UUID cycleId, @Param("limit") int limit);

    /**
     * Search cycle events by multiple criteria.
     *
     * @param cycleId farming cycle ID filter (optional)
     * @param eventType event type filter (optional)
     * @param severity severity filter (optional)
     * @param isMilestone milestone status filter (optional)
     * @param requiresFollowUp follow-up requirement filter (optional)
     * @param userId recorded by user ID filter (optional)
     * @param startTime start time filter (optional)
     * @param endTime end time filter (optional)
     * @param pageable pagination information
     * @return page of matching cycle events
     */
    @Query("SELECT ce FROM CycleEvent ce " +
           "WHERE (:cycleId IS NULL OR ce.farmingCycle.cycleId = :cycleId) " +
           "AND (:eventType IS NULL OR ce.eventType = :eventType) " +
           "AND (:severity IS NULL OR ce.severity = :severity) " +
           "AND (:isMilestone IS NULL OR ce.isMilestone = :isMilestone) " +
           "AND (:requiresFollowUp IS NULL OR ce.requiresFollowUp = :requiresFollowUp) " +
           "AND (:userId IS NULL OR ce.recordedBy.userId = :userId) " +
           "AND (:startTime IS NULL OR ce.eventTime >= :startTime) " +
           "AND (:endTime IS NULL OR ce.eventTime <= :endTime)")
    Page<CycleEvent> searchCycleEvents(@Param("cycleId") UUID cycleId,
                                      @Param("eventType") CycleEvent.EventType eventType,
                                      @Param("severity") CycleEvent.Severity severity,
                                      @Param("isMilestone") Boolean isMilestone,
                                      @Param("requiresFollowUp") Boolean requiresFollowUp,
                                      @Param("userId") UUID userId,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime,
                                      Pageable pageable);

    /**
     * Find cycle events accessible by a user (through farming cycle access).
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of cycle events the user has access to
     */
    @Query("SELECT ce FROM CycleEvent ce " +
           "WHERE ce.farmingCycle.room.farm.owner.userId = :userId " +
           "OR ce.farmingCycle.room.roomId IN (" +
           "  SELECT ur.room.roomId FROM UserRoom ur " +
           "  WHERE ur.user.userId = :userId AND ur.isActive = true" +
           ")")
    Page<CycleEvent> findCycleEventsAccessibleByUser(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Get cycle event statistics for a farming cycle.
     *
     * @param cycleId the farming cycle ID
     * @return cycle event statistics for the farming cycle
     */
    @Query("SELECT " +
           "COUNT(*) as totalEvents, " +
           "COUNT(CASE WHEN ce.severity = 'LOW' THEN 1 END) as lowSeverityEvents, " +
           "COUNT(CASE WHEN ce.severity = 'MEDIUM' THEN 1 END) as mediumSeverityEvents, " +
           "COUNT(CASE WHEN ce.severity = 'HIGH' THEN 1 END) as highSeverityEvents, " +
           "COUNT(CASE WHEN ce.severity = 'CRITICAL' THEN 1 END) as criticalSeverityEvents, " +
           "COUNT(CASE WHEN ce.isMilestone = true THEN 1 END) as milestoneEvents, " +
           "COUNT(CASE WHEN ce.requiresFollowUp = true THEN 1 END) as followUpEvents, " +
           "COUNT(CASE WHEN ce.requiresFollowUp = true AND ce.followUpCompleted = false THEN 1 END) as pendingFollowUpEvents " +
           "FROM CycleEvent ce " +
           "WHERE ce.farmingCycle.cycleId = :cycleId")
    Optional<CycleEventStatistics> getCycleEventStatistics(@Param("cycleId") UUID cycleId);

    /**
     * Get cycle event statistics for a room.
     *
     * @param roomId the room ID
     * @param startTime the start time
     * @param endTime the end time
     * @return cycle event statistics for the room
     */
    @Query("SELECT " +
           "COUNT(*) as totalEvents, " +
           "COUNT(CASE WHEN ce.severity = 'LOW' THEN 1 END) as lowSeverityEvents, " +
           "COUNT(CASE WHEN ce.severity = 'MEDIUM' THEN 1 END) as mediumSeverityEvents, " +
           "COUNT(CASE WHEN ce.severity = 'HIGH' THEN 1 END) as highSeverityEvents, " +
           "COUNT(CASE WHEN ce.severity = 'CRITICAL' THEN 1 END) as criticalSeverityEvents, " +
           "COUNT(CASE WHEN ce.isMilestone = true THEN 1 END) as milestoneEvents, " +
           "COUNT(CASE WHEN ce.requiresFollowUp = true THEN 1 END) as followUpEvents, " +
           "COUNT(CASE WHEN ce.requiresFollowUp = true AND ce.followUpCompleted = false THEN 1 END) as pendingFollowUpEvents " +
           "FROM CycleEvent ce " +
           "WHERE ce.farmingCycle.room.roomId = :roomId " +
           "AND ce.eventTime BETWEEN :startTime AND :endTime")
    Optional<CycleEventStatistics> getCycleEventStatisticsForRoom(@Param("roomId") UUID roomId,
                                                                 @Param("startTime") LocalDateTime startTime,
                                                                 @Param("endTime") LocalDateTime endTime);

    /**
     * Update follow-up completion status.
     *
     * @param eventId the event ID
     * @param followUpCompleted the follow-up completion status
     * @param followUpCompletedAt the follow-up completion timestamp
     * @param followUpNotes the follow-up notes
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE CycleEvent ce SET ce.followUpCompleted = :followUpCompleted, " +
           "ce.followUpCompletedAt = :followUpCompletedAt, " +
           "ce.followUpNotes = :followUpNotes " +
           "WHERE ce.eventId = :eventId")
    int updateFollowUpStatus(@Param("eventId") UUID eventId,
                            @Param("followUpCompleted") Boolean followUpCompleted,
                            @Param("followUpCompletedAt") LocalDateTime followUpCompletedAt,
                            @Param("followUpNotes") String followUpNotes);

    /**
     * Update follow-up due date.
     *
     * @param eventId the event ID
     * @param followUpDueDate the new follow-up due date
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE CycleEvent ce SET ce.followUpDueDate = :followUpDueDate " +
           "WHERE ce.eventId = :eventId")
    int updateFollowUpDueDate(@Param("eventId") UUID eventId,
                             @Param("followUpDueDate") LocalDateTime followUpDueDate);

    /**
     * Update event notes.
     *
     * @param eventId the event ID
     * @param notes the new notes
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE CycleEvent ce SET ce.notes = :notes " +
           "WHERE ce.eventId = :eventId")
    int updateEventNotes(@Param("eventId") UUID eventId, @Param("notes") String notes);

    /**
     * Count cycle events by farming cycle.
     *
     * @param farmingCycle the farming cycle
     * @return count of cycle events for the farming cycle
     */
    long countByFarmingCycle(FarmingCycle farmingCycle);

    /**
     * Count cycle events by farming cycle ID.
     *
     * @param cycleId the farming cycle ID
     * @return count of cycle events for the farming cycle
     */
    long countByFarmingCycleCycleId(UUID cycleId);

    /**
     * Count cycle events by event type.
     *
     * @param eventType the event type
     * @return count of cycle events with the specified type
     */
    long countByEventType(CycleEvent.EventType eventType);

    /**
     * Count cycle events by severity.
     *
     * @param severity the severity level
     * @return count of cycle events with the specified severity
     */
    long countBySeverity(CycleEvent.Severity severity);

    /**
     * Count milestone events by farming cycle.
     *
     * @param cycleId the farming cycle ID
     * @return count of milestone events for the farming cycle
     */
    long countByFarmingCycleCycleIdAndIsMilestoneTrue(UUID cycleId);

    /**
     * Count events requiring follow-up.
     *
     * @return count of events requiring follow-up
     */
    long countByRequiresFollowUpTrueAndFollowUpCompletedFalse();

    /**
     * Count cycle events in time range.
     *
     * @param startTime the start time
     * @param endTime the end time
     * @return count of cycle events in the time range
     */
    long countByEventTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find cycle events created after a specific time.
     *
     * @param time the time threshold
     * @return list of cycle events created after the time
     */
    List<CycleEvent> findByCreatedAtAfter(LocalDateTime time);

    /**
     * Find cycle events created before a specific time.
     *
     * @param time the time threshold
     * @return list of cycle events created before the time
     */
    List<CycleEvent> findByCreatedAtBefore(LocalDateTime time);

    /**
     * Delete cycle events created before a specific date.
     *
     * @param cutoffDate the cutoff date
     * @return number of deleted records
     */
    long deleteByCreatedAtBefore(LocalDateTime cutoffDate);

    /**
     * Interface for cycle event statistics projection.
     */
    interface CycleEventStatistics {
        Long getTotalEvents();
        Long getLowSeverityEvents();
        Long getMediumSeverityEvents();
        Long getHighSeverityEvents();
        Long getCriticalSeverityEvents();
        Long getMilestoneEvents();
        Long getFollowUpEvents();
        Long getPendingFollowUpEvents();
    }
}