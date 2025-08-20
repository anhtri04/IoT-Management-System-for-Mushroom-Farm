package com.smartfarm.repository;

import com.smartfarm.entity.Command;
import com.smartfarm.entity.Device;
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
 * Repository interface for Command entity operations.
 * Provides data access methods for device command management and tracking.
 */
@Repository
public interface CommandRepository extends JpaRepository<Command, UUID> {

    /**
     * Find commands by device.
     *
     * @param device the device
     * @param pageable pagination information
     * @return page of commands for the device
     */
    Page<Command> findByDevice(Device device, Pageable pageable);

    /**
     * Find commands by device ID.
     *
     * @param deviceId the device ID
     * @param pageable pagination information
     * @return page of commands for the device
     */
    Page<Command> findByDeviceDeviceId(UUID deviceId, Pageable pageable);

    /**
     * Find commands by room ID.
     *
     * @param roomId the room ID
     * @param pageable pagination information
     * @return page of commands for devices in the room
     */
    Page<Command> findByRoomRoomId(UUID roomId, Pageable pageable);

    /**
     * Find commands by farm ID.
     *
     * @param farmId the farm ID
     * @param pageable pagination information
     * @return page of commands for devices in the farm
     */
    Page<Command> findByFarmFarmId(UUID farmId, Pageable pageable);

    /**
     * Find commands by status.
     *
     * @param status the command status
     * @param pageable pagination information
     * @return page of commands with the specified status
     */
    Page<Command> findByStatus(Command.Status status, Pageable pageable);

    /**
     * Find commands by status (list).
     *
     * @param status the command status
     * @return list of commands with the specified status
     */
    List<Command> findByStatus(Command.Status status);

    /**
     * Find commands by multiple statuses.
     *
     * @param statuses the command statuses
     * @return list of commands with any of the specified statuses
     */
    List<Command> findByStatusIn(List<Command.Status> statuses);

    /**
     * Find commands by issued by user.
     *
     * @param issuedBy the user who issued the command
     * @param pageable pagination information
     * @return page of commands issued by the user
     */
    Page<Command> findByIssuedBy(User issuedBy, Pageable pageable);

    /**
     * Find commands by issued by user ID.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of commands issued by the user
     */
    Page<Command> findByIssuedByUserId(UUID userId, Pageable pageable);

    /**
     * Find commands by device and status.
     *
     * @param deviceId the device ID
     * @param status the command status
     * @return list of commands for the device with the specified status
     */
    List<Command> findByDeviceDeviceIdAndStatus(UUID deviceId, Command.Status status);

    /**
     * Find commands by device and time range.
     *
     * @param deviceId the device ID
     * @param startTime the start time
     * @param endTime the end time
     * @param pageable pagination information
     * @return page of commands in the time range
     */
    Page<Command> findByDeviceDeviceIdAndIssuedAtBetween(UUID deviceId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Find commands by room and time range.
     *
     * @param roomId the room ID
     * @param startTime the start time
     * @param endTime the end time
     * @param pageable pagination information
     * @return page of commands in the time range
     */
    Page<Command> findByRoomRoomIdAndIssuedAtBetween(UUID roomId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Find pending commands for a device.
     *
     * @param deviceId the device ID
     * @return list of pending commands for the device
     */
    @Query("SELECT c FROM Command c " +
           "WHERE c.device.deviceId = :deviceId " +
           "AND c.status IN ('PENDING', 'SENT') " +
           "ORDER BY c.issuedAt ASC")
    List<Command> findPendingCommandsForDevice(@Param("deviceId") UUID deviceId);

    /**
     * Find recent commands for a device.
     *
     * @param deviceId the device ID
     * @param limit the maximum number of commands to return
     * @return list of recent commands for the device
     */
    @Query("SELECT c FROM Command c " +
           "WHERE c.device.deviceId = :deviceId " +
           "ORDER BY c.issuedAt DESC " +
           "LIMIT :limit")
    List<Command> findRecentCommandsForDevice(@Param("deviceId") UUID deviceId, @Param("limit") int limit);

    /**
     * Find recent commands for a room.
     *
     * @param roomId the room ID
     * @param limit the maximum number of commands to return
     * @return list of recent commands for devices in the room
     */
    @Query("SELECT c FROM Command c " +
           "WHERE c.room.roomId = :roomId " +
           "ORDER BY c.issuedAt DESC " +
           "LIMIT :limit")
    List<Command> findRecentCommandsForRoom(@Param("roomId") UUID roomId, @Param("limit") int limit);

    /**
     * Find failed commands that can be retried.
     *
     * @param maxRetries the maximum number of retries
     * @param retryAfter the minimum time to wait before retry
     * @return list of commands that can be retried
     */
    @Query("SELECT c FROM Command c " +
           "WHERE c.status = 'FAILED' " +
           "AND c.retryCount < :maxRetries " +
           "AND (c.lastRetryAt IS NULL OR c.lastRetryAt < :retryAfter) " +
           "ORDER BY c.issuedAt ASC")
    List<Command> findCommandsForRetry(@Param("maxRetries") Integer maxRetries, @Param("retryAfter") LocalDateTime retryAfter);

    /**
     * Find expired commands that should be marked as failed.
     *
     * @param expirationTime the expiration time threshold
     * @return list of expired commands
     */
    @Query("SELECT c FROM Command c " +
           "WHERE c.status IN ('PENDING', 'SENT') " +
           "AND c.issuedAt < :expirationTime")
    List<Command> findExpiredCommands(@Param("expirationTime") LocalDateTime expirationTime);

    /**
     * Search commands by multiple criteria.
     *
     * @param deviceId device ID filter (optional)
     * @param roomId room ID filter (optional)
     * @param status status filter (optional)
     * @param userId user ID filter (optional)
     * @param startTime start time filter (optional)
     * @param endTime end time filter (optional)
     * @param pageable pagination information
     * @return page of matching commands
     */
    @Query("SELECT c FROM Command c " +
           "WHERE (:deviceId IS NULL OR c.device.deviceId = :deviceId) " +
           "AND (:roomId IS NULL OR c.room.roomId = :roomId) " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND (:userId IS NULL OR c.issuedBy.userId = :userId) " +
           "AND (:startTime IS NULL OR c.issuedAt >= :startTime) " +
           "AND (:endTime IS NULL OR c.issuedAt <= :endTime)")
    Page<Command> searchCommands(@Param("deviceId") UUID deviceId,
                                @Param("roomId") UUID roomId,
                                @Param("status") Command.Status status,
                                @Param("userId") UUID userId,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime,
                                Pageable pageable);

    /**
     * Find commands accessible by a user (through room access).
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of commands the user has access to
     */
    @Query("SELECT c FROM Command c " +
           "WHERE c.room.farm.owner.userId = :userId " +
           "OR c.room.roomId IN (" +
           "  SELECT ur.room.roomId FROM UserRoom ur " +
           "  WHERE ur.user.userId = :userId AND ur.isActive = true" +
           ")")
    Page<Command> findCommandsAccessibleByUser(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Get command statistics for a device.
     *
     * @param deviceId the device ID
     * @param startTime the start time
     * @param endTime the end time
     * @return command statistics for the device
     */
    @Query("SELECT " +
           "COUNT(*) as totalCommands, " +
           "COUNT(CASE WHEN c.status = 'PENDING' THEN 1 END) as pendingCommands, " +
           "COUNT(CASE WHEN c.status = 'SENT' THEN 1 END) as sentCommands, " +
           "COUNT(CASE WHEN c.status = 'ACKNOWLEDGED' THEN 1 END) as acknowledgedCommands, " +
           "COUNT(CASE WHEN c.status = 'COMPLETED' THEN 1 END) as completedCommands, " +
           "COUNT(CASE WHEN c.status = 'FAILED' THEN 1 END) as failedCommands, " +
           "AVG(CASE WHEN c.acknowledgedAt IS NOT NULL AND c.sentAt IS NOT NULL " +
           "    THEN EXTRACT(EPOCH FROM (c.acknowledgedAt - c.sentAt)) END) as avgResponseTimeSeconds " +
           "FROM Command c " +
           "WHERE c.device.deviceId = :deviceId " +
           "AND c.issuedAt BETWEEN :startTime AND :endTime")
    Optional<CommandStatistics> getCommandStatistics(@Param("deviceId") UUID deviceId,
                                                     @Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);

    /**
     * Get command statistics for a room.
     *
     * @param roomId the room ID
     * @param startTime the start time
     * @param endTime the end time
     * @return command statistics for the room
     */
    @Query("SELECT " +
           "COUNT(*) as totalCommands, " +
           "COUNT(CASE WHEN c.status = 'PENDING' THEN 1 END) as pendingCommands, " +
           "COUNT(CASE WHEN c.status = 'SENT' THEN 1 END) as sentCommands, " +
           "COUNT(CASE WHEN c.status = 'ACKNOWLEDGED' THEN 1 END) as acknowledgedCommands, " +
           "COUNT(CASE WHEN c.status = 'COMPLETED' THEN 1 END) as completedCommands, " +
           "COUNT(CASE WHEN c.status = 'FAILED' THEN 1 END) as failedCommands, " +
           "AVG(CASE WHEN c.acknowledgedAt IS NOT NULL AND c.sentAt IS NOT NULL " +
           "    THEN EXTRACT(EPOCH FROM (c.acknowledgedAt - c.sentAt)) END) as avgResponseTimeSeconds " +
           "FROM Command c " +
           "WHERE c.room.roomId = :roomId " +
           "AND c.issuedAt BETWEEN :startTime AND :endTime")
    Optional<CommandStatistics> getCommandStatisticsForRoom(@Param("roomId") UUID roomId,
                                                           @Param("startTime") LocalDateTime startTime,
                                                           @Param("endTime") LocalDateTime endTime);

    /**
     * Update command status.
     *
     * @param commandId the command ID
     * @param status the new status
     * @param timestamp the timestamp for the status change
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE Command c SET c.status = :status, " +
           "c.sentAt = CASE WHEN :status = 'SENT' THEN :timestamp ELSE c.sentAt END, " +
           "c.acknowledgedAt = CASE WHEN :status = 'ACKNOWLEDGED' THEN :timestamp ELSE c.acknowledgedAt END, " +
           "c.completedAt = CASE WHEN :status = 'COMPLETED' THEN :timestamp ELSE c.completedAt END, " +
           "c.failedAt = CASE WHEN :status = 'FAILED' THEN :timestamp ELSE c.failedAt END " +
           "WHERE c.commandId = :commandId")
    int updateCommandStatus(@Param("commandId") UUID commandId,
                           @Param("status") Command.Status status,
                           @Param("timestamp") LocalDateTime timestamp);

    /**
     * Update command retry information.
     *
     * @param commandId the command ID
     * @param retryCount the new retry count
     * @param lastRetryAt the last retry timestamp
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE Command c SET c.retryCount = :retryCount, c.lastRetryAt = :lastRetryAt " +
           "WHERE c.commandId = :commandId")
    int updateCommandRetry(@Param("commandId") UUID commandId,
                          @Param("retryCount") Integer retryCount,
                          @Param("lastRetryAt") LocalDateTime lastRetryAt);

    /**
     * Update command error message.
     *
     * @param commandId the command ID
     * @param errorMessage the error message
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE Command c SET c.errorMessage = :errorMessage " +
           "WHERE c.commandId = :commandId")
    int updateCommandError(@Param("commandId") UUID commandId,
                          @Param("errorMessage") String errorMessage);

    /**
     * Mark expired commands as failed.
     *
     * @param expirationTime the expiration time threshold
     * @param failedAt the timestamp to set as failed time
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE Command c SET c.status = 'FAILED', c.failedAt = :failedAt, " +
           "c.errorMessage = 'Command expired' " +
           "WHERE c.status IN ('PENDING', 'SENT') " +
           "AND c.issuedAt < :expirationTime")
    int markExpiredCommandsAsFailed(@Param("expirationTime") LocalDateTime expirationTime,
                                   @Param("failedAt") LocalDateTime failedAt);

    /**
     * Count commands by device.
     *
     * @param device the device
     * @return count of commands for the device
     */
    long countByDevice(Device device);

    /**
     * Count commands by device ID.
     *
     * @param deviceId the device ID
     * @return count of commands for the device
     */
    long countByDeviceDeviceId(UUID deviceId);

    /**
     * Count commands by status.
     *
     * @param status the command status
     * @return count of commands with the specified status
     */
    long countByStatus(Command.Status status);

    /**
     * Count commands by room ID.
     *
     * @param roomId the room ID
     * @return count of commands for devices in the room
     */
    long countByRoomRoomId(UUID roomId);

    /**
     * Count commands in time range.
     *
     * @param startTime the start time
     * @param endTime the end time
     * @return count of commands in the time range
     */
    long countByIssuedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Delete old commands before a specific date.
     *
     * @param cutoffDate the cutoff date
     * @return number of deleted records
     */
    long deleteByIssuedAtBefore(LocalDateTime cutoffDate);

    /**
     * Find commands issued after a specific time.
     *
     * @param time the time threshold
     * @return list of commands issued after the time
     */
    List<Command> findByIssuedAtAfter(LocalDateTime time);

    /**
     * Find commands issued before a specific time.
     *
     * @param time the time threshold
     * @return list of commands issued before the time
     */
    List<Command> findByIssuedAtBefore(LocalDateTime time);

    /**
     * Interface for command statistics projection.
     */
    interface CommandStatistics {
        Long getTotalCommands();
        Long getPendingCommands();
        Long getSentCommands();
        Long getAcknowledgedCommands();
        Long getCompletedCommands();
        Long getFailedCommands();
        Double getAvgResponseTimeSeconds();
    }
}