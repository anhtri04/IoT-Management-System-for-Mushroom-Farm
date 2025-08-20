package com.smartfarm.repository;

import com.smartfarm.entity.FarmingCycle;
import com.smartfarm.entity.Room;
import com.smartfarm.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for FarmingCycle entity operations.
 * Provides data access methods for farming cycle management and tracking.
 */
@Repository
public interface FarmingCycleRepository extends JpaRepository<FarmingCycle, UUID> {

    /**
     * Find farming cycles by room.
     *
     * @param room the room
     * @param pageable pagination information
     * @return page of farming cycles for the room
     */
    Page<FarmingCycle> findByRoom(Room room, Pageable pageable);

    /**
     * Find farming cycles by room ID.
     *
     * @param roomId the room ID
     * @param pageable pagination information
     * @return page of farming cycles for the room
     */
    Page<FarmingCycle> findByRoomRoomId(UUID roomId, Pageable pageable);

    /**
     * Find farming cycles by status.
     *
     * @param status the cycle status
     * @param pageable pagination information
     * @return page of farming cycles with the specified status
     */
    Page<FarmingCycle> findByStatus(FarmingCycle.Status status, Pageable pageable);

    /**
     * Find farming cycles by status (list).
     *
     * @param status the cycle status
     * @return list of farming cycles with the specified status
     */
    List<FarmingCycle> findByStatus(FarmingCycle.Status status);

    /**
     * Find farming cycles by multiple statuses.
     *
     * @param statuses the cycle statuses
     * @return list of farming cycles with any of the specified statuses
     */
    List<FarmingCycle> findByStatusIn(List<FarmingCycle.Status> statuses);

    /**
     * Find farming cycles by room and status.
     *
     * @param roomId the room ID
     * @param status the cycle status
     * @return list of farming cycles for the room with the specified status
     */
    List<FarmingCycle> findByRoomRoomIdAndStatus(UUID roomId, FarmingCycle.Status status);

    /**
     * Find active farming cycles by room.
     *
     * @param roomId the room ID
     * @return list of active farming cycles for the room
     */
    @Query("SELECT fc FROM FarmingCycle fc " +
           "WHERE fc.room.roomId = :roomId " +
           "AND fc.status IN ('PLANNING', 'INOCULATION', 'INCUBATION', 'FRUITING', 'HARVESTING')")
    List<FarmingCycle> findActiveCyclesByRoom(@Param("roomId") UUID roomId);

    /**
     * Find current active farming cycle for a room.
     *
     * @param roomId the room ID
     * @return optional current active farming cycle
     */
    @Query("SELECT fc FROM FarmingCycle fc " +
           "WHERE fc.room.roomId = :roomId " +
           "AND fc.status IN ('PLANNING', 'INOCULATION', 'INCUBATION', 'FRUITING', 'HARVESTING') " +
           "ORDER BY fc.startDate DESC " +
           "LIMIT 1")
    Optional<FarmingCycle> findCurrentActiveCycleByRoom(@Param("roomId") UUID roomId);

    /**
     * Find farming cycles by mushroom variety.
     *
     * @param mushroomVariety the mushroom variety
     * @param pageable pagination information
     * @return page of farming cycles with the specified variety
     */
    Page<FarmingCycle> findByMushroomVarietyContainingIgnoreCase(String mushroomVariety, Pageable pageable);

    /**
     * Find farming cycles by created by user.
     *
     * @param createdBy the user who created the cycle
     * @param pageable pagination information
     * @return page of farming cycles created by the user
     */
    Page<FarmingCycle> findByCreatedBy(User createdBy, Pageable pageable);

    /**
     * Find farming cycles by created by user ID.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of farming cycles created by the user
     */
    Page<FarmingCycle> findByCreatedByUserId(UUID userId, Pageable pageable);

    /**
     * Find farming cycles by start date range.
     *
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @param pageable pagination information
     * @return page of farming cycles in the date range
     */
    Page<FarmingCycle> findByStartDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Find farming cycles by expected harvest date range.
     *
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @param pageable pagination information
     * @return page of farming cycles with expected harvest in the date range
     */
    Page<FarmingCycle> findByExpectedHarvestDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Find farming cycles by actual harvest date range.
     *
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @param pageable pagination information
     * @return page of farming cycles with actual harvest in the date range
     */
    Page<FarmingCycle> findByActualHarvestDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Find farming cycles due for harvest soon.
     *
     * @param currentDate the current date
     * @param daysAhead number of days to look ahead
     * @return list of farming cycles due for harvest
     */
    @Query("SELECT fc FROM FarmingCycle fc " +
           "WHERE fc.expectedHarvestDate BETWEEN :currentDate AND :futureDate " +
           "AND fc.status IN ('INCUBATION', 'FRUITING') " +
           "ORDER BY fc.expectedHarvestDate ASC")
    List<FarmingCycle> findCyclesDueForHarvest(@Param("currentDate") LocalDate currentDate,
                                              @Param("futureDate") LocalDate futureDate);

    /**
     * Find overdue farming cycles.
     *
     * @param currentDate the current date
     * @return list of overdue farming cycles
     */
    @Query("SELECT fc FROM FarmingCycle fc " +
           "WHERE fc.expectedHarvestDate < :currentDate " +
           "AND fc.status IN ('INCUBATION', 'FRUITING') " +
           "ORDER BY fc.expectedHarvestDate ASC")
    List<FarmingCycle> findOverdueCycles(@Param("currentDate") LocalDate currentDate);

    /**
     * Search farming cycles by multiple criteria.
     *
     * @param roomId room ID filter (optional)
     * @param status status filter (optional)
     * @param mushroomVariety mushroom variety filter (optional)
     * @param startDateFrom start date range from (optional)
     * @param startDateTo start date range to (optional)
     * @param userId created by user ID filter (optional)
     * @param pageable pagination information
     * @return page of matching farming cycles
     */
    @Query("SELECT fc FROM FarmingCycle fc " +
           "WHERE (:roomId IS NULL OR fc.room.roomId = :roomId) " +
           "AND (:status IS NULL OR fc.status = :status) " +
           "AND (:mushroomVariety IS NULL OR LOWER(fc.mushroomVariety) LIKE LOWER(CONCAT('%', :mushroomVariety, '%'))) " +
           "AND (:startDateFrom IS NULL OR fc.startDate >= :startDateFrom) " +
           "AND (:startDateTo IS NULL OR fc.startDate <= :startDateTo) " +
           "AND (:userId IS NULL OR fc.createdBy.userId = :userId)")
    Page<FarmingCycle> searchFarmingCycles(@Param("roomId") UUID roomId,
                                          @Param("status") FarmingCycle.Status status,
                                          @Param("mushroomVariety") String mushroomVariety,
                                          @Param("startDateFrom") LocalDate startDateFrom,
                                          @Param("startDateTo") LocalDate startDateTo,
                                          @Param("userId") UUID userId,
                                          Pageable pageable);

    /**
     * Find farming cycles accessible by a user (through room access).
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of farming cycles the user has access to
     */
    @Query("SELECT fc FROM FarmingCycle fc " +
           "WHERE fc.room.farm.owner.userId = :userId " +
           "OR fc.room.roomId IN (" +
           "  SELECT ur.room.roomId FROM UserRoom ur " +
           "  WHERE ur.user.userId = :userId AND ur.isActive = true" +
           ")")
    Page<FarmingCycle> findFarmingCyclesAccessibleByUser(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Get farming cycle statistics for a room.
     *
     * @param roomId the room ID
     * @param startDate the start date for statistics
     * @param endDate the end date for statistics
     * @return farming cycle statistics for the room
     */
    @Query("SELECT " +
           "COUNT(*) as totalCycles, " +
           "COUNT(CASE WHEN fc.status = 'COMPLETED' THEN 1 END) as completedCycles, " +
           "COUNT(CASE WHEN fc.status = 'ABORTED' THEN 1 END) as abortedCycles, " +
           "COUNT(CASE WHEN fc.status IN ('PLANNING', 'INOCULATION', 'INCUBATION', 'FRUITING', 'HARVESTING') THEN 1 END) as activeCycles, " +
           "AVG(CASE WHEN fc.actualHarvestDate IS NOT NULL AND fc.startDate IS NOT NULL " +
           "    THEN DATEDIFF(fc.actualHarvestDate, fc.startDate) END) as avgCycleDurationDays, " +
           "AVG(fc.yieldKg) as avgYieldKg, " +
           "SUM(fc.yieldKg) as totalYieldKg " +
           "FROM FarmingCycle fc " +
           "WHERE fc.room.roomId = :roomId " +
           "AND fc.startDate BETWEEN :startDate AND :endDate")
    Optional<FarmingCycleStatistics> getFarmingCycleStatistics(@Param("roomId") UUID roomId,
                                                              @Param("startDate") LocalDate startDate,
                                                              @Param("endDate") LocalDate endDate);

    /**
     * Get farming cycle statistics for a farm.
     *
     * @param farmId the farm ID
     * @param startDate the start date for statistics
     * @param endDate the end date for statistics
     * @return farming cycle statistics for the farm
     */
    @Query("SELECT " +
           "COUNT(*) as totalCycles, " +
           "COUNT(CASE WHEN fc.status = 'COMPLETED' THEN 1 END) as completedCycles, " +
           "COUNT(CASE WHEN fc.status = 'ABORTED' THEN 1 END) as abortedCycles, " +
           "COUNT(CASE WHEN fc.status IN ('PLANNING', 'INOCULATION', 'INCUBATION', 'FRUITING', 'HARVESTING') THEN 1 END) as activeCycles, " +
           "AVG(CASE WHEN fc.actualHarvestDate IS NOT NULL AND fc.startDate IS NOT NULL " +
           "    THEN DATEDIFF(fc.actualHarvestDate, fc.startDate) END) as avgCycleDurationDays, " +
           "AVG(fc.yieldKg) as avgYieldKg, " +
           "SUM(fc.yieldKg) as totalYieldKg " +
           "FROM FarmingCycle fc " +
           "WHERE fc.room.farm.farmId = :farmId " +
           "AND fc.startDate BETWEEN :startDate AND :endDate")
    Optional<FarmingCycleStatistics> getFarmingCycleStatisticsForFarm(@Param("farmId") UUID farmId,
                                                                     @Param("startDate") LocalDate startDate,
                                                                     @Param("endDate") LocalDate endDate);

    /**
     * Update farming cycle status.
     *
     * @param cycleId the cycle ID
     * @param status the new status
     * @param timestamp the timestamp for the status change
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE FarmingCycle fc SET fc.status = :status, " +
           "fc.inoculationDate = CASE WHEN :status = 'INOCULATION' THEN CAST(:timestamp AS DATE) ELSE fc.inoculationDate END, " +
           "fc.incubationStartDate = CASE WHEN :status = 'INCUBATION' THEN CAST(:timestamp AS DATE) ELSE fc.incubationStartDate END, " +
           "fc.fruitingStartDate = CASE WHEN :status = 'FRUITING' THEN CAST(:timestamp AS DATE) ELSE fc.fruitingStartDate END, " +
           "fc.harvestStartDate = CASE WHEN :status = 'HARVESTING' THEN CAST(:timestamp AS DATE) ELSE fc.harvestStartDate END, " +
           "fc.actualHarvestDate = CASE WHEN :status = 'COMPLETED' THEN CAST(:timestamp AS DATE) ELSE fc.actualHarvestDate END, " +
           "fc.abortedDate = CASE WHEN :status = 'ABORTED' THEN CAST(:timestamp AS DATE) ELSE fc.abortedDate END " +
           "WHERE fc.cycleId = :cycleId")
    int updateCycleStatus(@Param("cycleId") UUID cycleId,
                         @Param("status") FarmingCycle.Status status,
                         @Param("timestamp") LocalDateTime timestamp);

    /**
     * Update farming cycle yield.
     *
     * @param cycleId the cycle ID
     * @param yieldKg the yield in kilograms
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE FarmingCycle fc SET fc.yieldKg = :yieldKg " +
           "WHERE fc.cycleId = :cycleId")
    int updateCycleYield(@Param("cycleId") UUID cycleId, @Param("yieldKg") Double yieldKg);

    /**
     * Update farming cycle expected harvest date.
     *
     * @param cycleId the cycle ID
     * @param expectedHarvestDate the new expected harvest date
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE FarmingCycle fc SET fc.expectedHarvestDate = :expectedHarvestDate " +
           "WHERE fc.cycleId = :cycleId")
    int updateExpectedHarvestDate(@Param("cycleId") UUID cycleId,
                                 @Param("expectedHarvestDate") LocalDate expectedHarvestDate);

    /**
     * Update farming cycle notes.
     *
     * @param cycleId the cycle ID
     * @param notes the new notes
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE FarmingCycle fc SET fc.notes = :notes " +
           "WHERE fc.cycleId = :cycleId")
    int updateCycleNotes(@Param("cycleId") UUID cycleId, @Param("notes") String notes);

    /**
     * Count farming cycles by room.
     *
     * @param room the room
     * @return count of farming cycles for the room
     */
    long countByRoom(Room room);

    /**
     * Count farming cycles by room ID.
     *
     * @param roomId the room ID
     * @return count of farming cycles for the room
     */
    long countByRoomRoomId(UUID roomId);

    /**
     * Count farming cycles by status.
     *
     * @param status the cycle status
     * @return count of farming cycles with the specified status
     */
    long countByStatus(FarmingCycle.Status status);

    /**
     * Count farming cycles by room and status.
     *
     * @param roomId the room ID
     * @param status the cycle status
     * @return count of farming cycles for the room with the specified status
     */
    long countByRoomRoomIdAndStatus(UUID roomId, FarmingCycle.Status status);

    /**
     * Count farming cycles in date range.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return count of farming cycles in the date range
     */
    long countByStartDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Check if a cycle batch number exists in a room.
     *
     * @param roomId the room ID
     * @param batchNumber the batch number
     * @return true if the batch number exists in the room
     */
    boolean existsByRoomRoomIdAndBatchNumber(UUID roomId, String batchNumber);

    /**
     * Check if a cycle batch number exists in a room (excluding a specific cycle).
     *
     * @param roomId the room ID
     * @param batchNumber the batch number
     * @param excludeCycleId the cycle ID to exclude from the check
     * @return true if the batch number exists in the room (excluding the specified cycle)
     */
    boolean existsByRoomRoomIdAndBatchNumberAndCycleIdNot(UUID roomId, String batchNumber, UUID excludeCycleId);

    /**
     * Find farming cycles created after a specific time.
     *
     * @param time the time threshold
     * @return list of farming cycles created after the time
     */
    List<FarmingCycle> findByCreatedAtAfter(LocalDateTime time);

    /**
     * Find farming cycles created before a specific time.
     *
     * @param time the time threshold
     * @return list of farming cycles created before the time
     */
    List<FarmingCycle> findByCreatedAtBefore(LocalDateTime time);

    /**
     * Find farming cycles by creation date range.
     *
     * @param startTime the start time
     * @param endTime the end time
     * @param pageable pagination information
     * @return page of farming cycles in the date range
     */
    Page<FarmingCycle> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Delete farming cycles created before a specific date.
     *
     * @param cutoffDate the cutoff date
     * @return number of deleted records
     */
    long deleteByCreatedAtBefore(LocalDateTime cutoffDate);

    /**
     * Interface for farming cycle statistics projection.
     */
    interface FarmingCycleStatistics {
        Long getTotalCycles();
        Long getCompletedCycles();
        Long getAbortedCycles();
        Long getActiveCycles();
        Double getAvgCycleDurationDays();
        Double getAvgYieldKg();
        Double getTotalYieldKg();
    }
}