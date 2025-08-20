package com.smartfarm.repository;

import com.smartfarm.entity.Notification;
import com.smartfarm.entity.Farm;
import com.smartfarm.entity.Room;
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
 * Repository interface for Notification entity operations.
 * Provides data access methods for notification and alert management.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Find notifications by farm.
     *
     * @param farm the farm
     * @param pageable pagination information
     * @return page of notifications for the farm
     */
    Page<Notification> findByFarm(Farm farm, Pageable pageable);

    /**
     * Find notifications by farm ID.
     *
     * @param farmId the farm ID
     * @param pageable pagination information
     * @return page of notifications for the farm
     */
    Page<Notification> findByFarmFarmId(UUID farmId, Pageable pageable);

    /**
     * Find notifications by room.
     *
     * @param room the room
     * @param pageable pagination information
     * @return page of notifications for the room
     */
    Page<Notification> findByRoom(Room room, Pageable pageable);

    /**
     * Find notifications by room ID.
     *
     * @param roomId the room ID
     * @param pageable pagination information
     * @return page of notifications for the room
     */
    Page<Notification> findByRoomRoomId(UUID roomId, Pageable pageable);

    /**
     * Find notifications by device.
     *
     * @param device the device
     * @param pageable pagination information
     * @return page of notifications for the device
     */
    Page<Notification> findByDevice(Device device, Pageable pageable);

    /**
     * Find notifications by device ID.
     *
     * @param deviceId the device ID
     * @param pageable pagination information
     * @return page of notifications for the device
     */
    Page<Notification> findByDeviceDeviceId(UUID deviceId, Pageable pageable);

    /**
     * Find notifications by level.
     *
     * @param level the notification level
     * @param pageable pagination information
     * @return page of notifications with the specified level
     */
    Page<Notification> findByLevel(Notification.Level level, Pageable pageable);

    /**
     * Find notifications by acknowledged status.
     *
     * @param acknowledged the acknowledged status
     * @param pageable pagination information
     * @return page of notifications with the specified acknowledged status
     */
    Page<Notification> findByAcknowledged(Boolean acknowledged, Pageable pageable);

    /**
     * Find unacknowledged notifications.
     *
     * @param pageable pagination information
     * @return page of unacknowledged notifications
     */
    Page<Notification> findByAcknowledgedFalse(Pageable pageable);

    /**
     * Find acknowledged notifications.
     *
     * @param pageable pagination information
     * @return page of acknowledged notifications
     */
    Page<Notification> findByAcknowledgedTrue(Pageable pageable);

    /**
     * Find notifications by acknowledged by user.
     *
     * @param acknowledgedBy the user who acknowledged the notification
     * @param pageable pagination information
     * @return page of notifications acknowledged by the user
     */
    Page<Notification> findByAcknowledgedBy(User acknowledgedBy, Pageable pageable);

    /**
     * Find notifications by acknowledged by user ID.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of notifications acknowledged by the user
     */
    Page<Notification> findByAcknowledgedByUserId(UUID userId, Pageable pageable);

    /**
     * Find notifications by time range.
     *
     * @param startTime the start time
     * @param endTime the end time
     * @param pageable pagination information
     * @return page of notifications in the time range
     */
    Page<Notification> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Find notifications by acknowledged time range.
     *
     * @param startTime the start time
     * @param endTime the end time
     * @param pageable pagination information
     * @return page of notifications acknowledged in the time range
     */
    Page<Notification> findByAcknowledgedAtBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Find unacknowledged notifications by farm.
     *
     * @param farmId the farm ID
     * @return list of unacknowledged notifications for the farm
     */
    List<Notification> findByFarmFarmIdAndAcknowledgedFalse(UUID farmId);

    /**
     * Find unacknowledged notifications by room.
     *
     * @param roomId the room ID
     * @return list of unacknowledged notifications for the room
     */
    List<Notification> findByRoomRoomIdAndAcknowledgedFalse(UUID roomId);

    /**
     * Find unacknowledged notifications by device.
     *
     * @param deviceId the device ID
     * @return list of unacknowledged notifications for the device
     */
    List<Notification> findByDeviceDeviceIdAndAcknowledgedFalse(UUID deviceId);

    /**
     * Find unacknowledged notifications by level.
     *
     * @param level the notification level
     * @return list of unacknowledged notifications with the specified level
     */
    List<Notification> findByLevelAndAcknowledgedFalse(Notification.Level level);

    /**
     * Find critical unacknowledged notifications.
     *
     * @return list of critical unacknowledged notifications
     */
    List<Notification> findByLevelAndAcknowledgedFalse(Notification.Level level);

    /**
     * Find recent notifications for a farm.
     *
     * @param farmId the farm ID
     * @param limit the maximum number of notifications to return
     * @return list of recent notifications for the farm
     */
    @Query("SELECT n FROM Notification n " +
           "WHERE n.farm.farmId = :farmId " +
           "ORDER BY n.createdAt DESC " +
           "LIMIT :limit")
    List<Notification> findRecentNotificationsForFarm(@Param("farmId") UUID farmId, @Param("limit") int limit);

    /**
     * Find recent notifications for a room.
     *
     * @param roomId the room ID
     * @param limit the maximum number of notifications to return
     * @return list of recent notifications for the room
     */
    @Query("SELECT n FROM Notification n " +
           "WHERE n.room.roomId = :roomId " +
           "ORDER BY n.createdAt DESC " +
           "LIMIT :limit")
    List<Notification> findRecentNotificationsForRoom(@Param("roomId") UUID roomId, @Param("limit") int limit);

    /**
     * Find recent notifications for a device.
     *
     * @param deviceId the device ID
     * @param limit the maximum number of notifications to return
     * @return list of recent notifications for the device
     */
    @Query("SELECT n FROM Notification n " +
           "WHERE n.device.deviceId = :deviceId " +
           "ORDER BY n.createdAt DESC " +
           "LIMIT :limit")
    List<Notification> findRecentNotificationsForDevice(@Param("deviceId") UUID deviceId, @Param("limit") int limit);

    /**
     * Search notifications by multiple criteria.
     *
     * @param farmId farm ID filter (optional)
     * @param roomId room ID filter (optional)
     * @param deviceId device ID filter (optional)
     * @param level notification level filter (optional)
     * @param acknowledged acknowledged status filter (optional)
     * @param messageKeyword message keyword filter (optional)
     * @param startTime start time filter (optional)
     * @param endTime end time filter (optional)
     * @param pageable pagination information
     * @return page of matching notifications
     */
    @Query("SELECT n FROM Notification n " +
           "WHERE (:farmId IS NULL OR n.farm.farmId = :farmId) " +
           "AND (:roomId IS NULL OR n.room.roomId = :roomId) " +
           "AND (:deviceId IS NULL OR n.device.deviceId = :deviceId) " +
           "AND (:level IS NULL OR n.level = :level) " +
           "AND (:acknowledged IS NULL OR n.acknowledged = :acknowledged) " +
           "AND (:messageKeyword IS NULL OR LOWER(n.message) LIKE LOWER(CONCAT('%', :messageKeyword, '%'))) " +
           "AND (:startTime IS NULL OR n.createdAt >= :startTime) " +
           "AND (:endTime IS NULL OR n.createdAt <= :endTime)")
    Page<Notification> searchNotifications(@Param("farmId") UUID farmId,
                                          @Param("roomId") UUID roomId,
                                          @Param("deviceId") UUID deviceId,
                                          @Param("level") Notification.Level level,
                                          @Param("acknowledged") Boolean acknowledged,
                                          @Param("messageKeyword") String messageKeyword,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime,
                                          Pageable pageable);

    /**
     * Find notifications accessible by a user.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of notifications the user has access to
     */
    @Query("SELECT n FROM Notification n " +
           "WHERE n.farm.owner.userId = :userId " +
           "OR n.room.roomId IN (" +
           "  SELECT ur.room.roomId FROM UserRoom ur " +
           "  WHERE ur.user.userId = :userId AND ur.isActive = true" +
           ")")
    Page<Notification> findNotificationsAccessibleByUser(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Find unacknowledged notifications accessible by a user.
     *
     * @param userId the user ID
     * @return list of unacknowledged notifications the user has access to
     */
    @Query("SELECT n FROM Notification n " +
           "WHERE n.acknowledged = false " +
           "AND (n.farm.owner.userId = :userId " +
           "OR n.room.roomId IN (" +
           "  SELECT ur.room.roomId FROM UserRoom ur " +
           "  WHERE ur.user.userId = :userId AND ur.isActive = true" +
           ")) " +
           "ORDER BY n.level DESC, n.createdAt DESC")
    List<Notification> findUnacknowledgedNotificationsForUser(@Param("userId") UUID userId);

    /**
     * Get notification statistics for a farm.
     *
     * @param farmId the farm ID
     * @param startTime the start time
     * @param endTime the end time
     * @return notification statistics for the farm
     */
    @Query("SELECT " +
           "COUNT(*) as totalNotifications, " +
           "COUNT(CASE WHEN n.level = 'INFO' THEN 1 END) as infoNotifications, " +
           "COUNT(CASE WHEN n.level = 'WARNING' THEN 1 END) as warningNotifications, " +
           "COUNT(CASE WHEN n.level = 'CRITICAL' THEN 1 END) as criticalNotifications, " +
           "COUNT(CASE WHEN n.acknowledged = true THEN 1 END) as acknowledgedNotifications, " +
           "COUNT(CASE WHEN n.acknowledged = false THEN 1 END) as unacknowledgedNotifications " +
           "FROM Notification n " +
           "WHERE n.farm.farmId = :farmId " +
           "AND n.createdAt BETWEEN :startTime AND :endTime")
    Optional<NotificationStatistics> getNotificationStatisticsForFarm(@Param("farmId") UUID farmId,
                                                                     @Param("startTime") LocalDateTime startTime,
                                                                     @Param("endTime") LocalDateTime endTime);

    /**
     * Get notification statistics for a room.
     *
     * @param roomId the room ID
     * @param startTime the start time
     * @param endTime the end time
     * @return notification statistics for the room
     */
    @Query("SELECT " +
           "COUNT(*) as totalNotifications, " +
           "COUNT(CASE WHEN n.level = 'INFO' THEN 1 END) as infoNotifications, " +
           "COUNT(CASE WHEN n.level = 'WARNING' THEN 1 END) as warningNotifications, " +
           "COUNT(CASE WHEN n.level = 'CRITICAL' THEN 1 END) as criticalNotifications, " +
           "COUNT(CASE WHEN n.acknowledged = true THEN 1 END) as acknowledgedNotifications, " +
           "COUNT(CASE WHEN n.acknowledged = false THEN 1 END) as unacknowledgedNotifications " +
           "FROM Notification n " +
           "WHERE n.room.roomId = :roomId " +
           "AND n.createdAt BETWEEN :startTime AND :endTime")
    Optional<NotificationStatistics> getNotificationStatisticsForRoom(@Param("roomId") UUID roomId,
                                                                     @Param("startTime") LocalDateTime startTime,
                                                                     @Param("endTime") LocalDateTime endTime);

    /**
     * Get notification statistics for a device.
     *
     * @param deviceId the device ID
     * @param startTime the start time
     * @param endTime the end time
     * @return notification statistics for the device
     */
    @Query("SELECT " +
           "COUNT(*) as totalNotifications, " +
           "COUNT(CASE WHEN n.level = 'INFO' THEN 1 END) as infoNotifications, " +
           "COUNT(CASE WHEN n.level = 'WARNING' THEN 1 END) as warningNotifications, " +
           "COUNT(CASE WHEN n.level = 'CRITICAL' THEN 1 END) as criticalNotifications, " +
           "COUNT(CASE WHEN n.acknowledged = true THEN 1 END) as acknowledgedNotifications, " +
           "COUNT(CASE WHEN n.acknowledged = false THEN 1 END) as unacknowledgedNotifications " +
           "FROM Notification n " +
           "WHERE n.device.deviceId = :deviceId " +
           "AND n.createdAt BETWEEN :startTime AND :endTime")
    Optional<NotificationStatistics> getNotificationStatisticsForDevice(@Param("deviceId") UUID deviceId,
                                                                       @Param("startTime") LocalDateTime startTime,
                                                                       @Param("endTime") LocalDateTime endTime);

    /**
     * Acknowledge a notification.
     *
     * @param notificationId the notification ID
     * @param acknowledgedBy the user acknowledging the notification
     * @param acknowledgedAt the acknowledgment timestamp
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE Notification n SET n.acknowledged = true, " +
           "n.acknowledgedBy = :acknowledgedBy, " +
           "n.acknowledgedAt = :acknowledgedAt " +
           "WHERE n.notificationId = :notificationId")
    int acknowledgeNotification(@Param("notificationId") UUID notificationId,
                               @Param("acknowledgedBy") User acknowledgedBy,
                               @Param("acknowledgedAt") LocalDateTime acknowledgedAt);

    /**
     * Bulk acknowledge notifications by IDs.
     *
     * @param notificationIds the notification IDs
     * @param acknowledgedBy the user acknowledging the notifications
     * @param acknowledgedAt the acknowledgment timestamp
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE Notification n SET n.acknowledged = true, " +
           "n.acknowledgedBy = :acknowledgedBy, " +
           "n.acknowledgedAt = :acknowledgedAt " +
           "WHERE n.notificationId IN :notificationIds")
    int bulkAcknowledgeNotifications(@Param("notificationIds") List<UUID> notificationIds,
                                    @Param("acknowledgedBy") User acknowledgedBy,
                                    @Param("acknowledgedAt") LocalDateTime acknowledgedAt);

    /**
     * Acknowledge all notifications for a farm.
     *
     * @param farmId the farm ID
     * @param acknowledgedBy the user acknowledging the notifications
     * @param acknowledgedAt the acknowledgment timestamp
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE Notification n SET n.acknowledged = true, " +
           "n.acknowledgedBy = :acknowledgedBy, " +
           "n.acknowledgedAt = :acknowledgedAt " +
           "WHERE n.farm.farmId = :farmId AND n.acknowledged = false")
    int acknowledgeAllNotificationsForFarm(@Param("farmId") UUID farmId,
                                          @Param("acknowledgedBy") User acknowledgedBy,
                                          @Param("acknowledgedAt") LocalDateTime acknowledgedAt);

    /**
     * Acknowledge all notifications for a room.
     *
     * @param roomId the room ID
     * @param acknowledgedBy the user acknowledging the notifications
     * @param acknowledgedAt the acknowledgment timestamp
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE Notification n SET n.acknowledged = true, " +
           "n.acknowledgedBy = :acknowledgedBy, " +
           "n.acknowledgedAt = :acknowledgedAt " +
           "WHERE n.room.roomId = :roomId AND n.acknowledged = false")
    int acknowledgeAllNotificationsForRoom(@Param("roomId") UUID roomId,
                                          @Param("acknowledgedBy") User acknowledgedBy,
                                          @Param("acknowledgedAt") LocalDateTime acknowledgedAt);

    /**
     * Count notifications by farm.
     *
     * @param farm the farm
     * @return count of notifications for the farm
     */
    long countByFarm(Farm farm);

    /**
     * Count notifications by farm ID.
     *
     * @param farmId the farm ID
     * @return count of notifications for the farm
     */
    long countByFarmFarmId(UUID farmId);

    /**
     * Count notifications by room.
     *
     * @param room the room
     * @return count of notifications for the room
     */
    long countByRoom(Room room);

    /**
     * Count notifications by room ID.
     *
     * @param roomId the room ID
     * @return count of notifications for the room
     */
    long countByRoomRoomId(UUID roomId);

    /**
     * Count notifications by device.
     *
     * @param device the device
     * @return count of notifications for the device
     */
    long countByDevice(Device device);

    /**
     * Count notifications by device ID.
     *
     * @param deviceId the device ID
     * @return count of notifications for the device
     */
    long countByDeviceDeviceId(UUID deviceId);

    /**
     * Count notifications by level.
     *
     * @param level the notification level
     * @return count of notifications with the specified level
     */
    long countByLevel(Notification.Level level);

    /**
     * Count unacknowledged notifications.
     *
     * @return count of unacknowledged notifications
     */
    long countByAcknowledgedFalse();

    /**
     * Count acknowledged notifications.
     *
     * @return count of acknowledged notifications
     */
    long countByAcknowledgedTrue();

    /**
     * Count unacknowledged notifications by farm.
     *
     * @param farmId the farm ID
     * @return count of unacknowledged notifications for the farm
     */
    long countByFarmFarmIdAndAcknowledgedFalse(UUID farmId);

    /**
     * Count unacknowledged notifications by room.
     *
     * @param roomId the room ID
     * @return count of unacknowledged notifications for the room
     */
    long countByRoomRoomIdAndAcknowledgedFalse(UUID roomId);

    /**
     * Count unacknowledged notifications by device.
     *
     * @param deviceId the device ID
     * @return count of unacknowledged notifications for the device
     */
    long countByDeviceDeviceIdAndAcknowledgedFalse(UUID deviceId);

    /**
     * Count notifications in time range.
     *
     * @param startTime the start time
     * @param endTime the end time
     * @return count of notifications in the time range
     */
    long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find notifications created after a specific time.
     *
     * @param time the time threshold
     * @return list of notifications created after the time
     */
    List<Notification> findByCreatedAtAfter(LocalDateTime time);

    /**
     * Find notifications created before a specific time.
     *
     * @param time the time threshold
     * @return list of notifications created before the time
     */
    List<Notification> findByCreatedAtBefore(LocalDateTime time);

    /**
     * Delete notifications created before a specific date.
     *
     * @param cutoffDate the cutoff date
     * @return number of deleted records
     */
    long deleteByCreatedAtBefore(LocalDateTime cutoffDate);

    /**
     * Delete acknowledged notifications created before a specific date.
     *
     * @param cutoffDate the cutoff date
     * @return number of deleted records
     */
    long deleteByAcknowledgedTrueAndCreatedAtBefore(LocalDateTime cutoffDate);

    /**
     * Interface for notification statistics projection.
     */
    interface NotificationStatistics {
        Long getTotalNotifications();
        Long getInfoNotifications();
        Long getWarningNotifications();
        Long getCriticalNotifications();
        Long getAcknowledgedNotifications();
        Long getUnacknowledgedNotifications();
    }
}