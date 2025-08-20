package com.smartfarm.repository;

import com.smartfarm.entity.Device;
import com.smartfarm.entity.Room;
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
 * Repository interface for Device entity operations.
 * Provides data access methods for device management, status tracking, and IoT operations.
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {

    /**
     * Find devices by room.
     *
     * @param room the room
     * @return list of devices in the room
     */
    List<Device> findByRoom(Room room);

    /**
     * Find devices by room with pagination.
     *
     * @param room the room
     * @param pageable pagination information
     * @return page of devices in the room
     */
    Page<Device> findByRoom(Room room, Pageable pageable);

    /**
     * Find devices by room ID.
     *
     * @param roomId the room ID
     * @return list of devices in the room
     */
    List<Device> findByRoomRoomId(UUID roomId);

    /**
     * Find devices by room ID with pagination.
     *
     * @param roomId the room ID
     * @param pageable pagination information
     * @return page of devices in the room
     */
    Page<Device> findByRoomRoomId(UUID roomId, Pageable pageable);

    /**
     * Find device by MQTT topic.
     *
     * @param mqttTopic the MQTT topic
     * @return optional device with the MQTT topic
     */
    Optional<Device> findByMqttTopic(String mqttTopic);

    /**
     * Find devices by device type.
     *
     * @param deviceType the device type
     * @return list of devices of the specified type
     */
    List<Device> findByDeviceType(Device.DeviceType deviceType);

    /**
     * Find devices by category.
     *
     * @param category the device category
     * @return list of devices in the specified category
     */
    List<Device> findByCategory(Device.Category category);

    /**
     * Find devices by status.
     *
     * @param status the device status
     * @return list of devices with the specified status
     */
    List<Device> findByStatus(Device.Status status);

    /**
     * Find devices by status with pagination.
     *
     * @param status the device status
     * @param pageable pagination information
     * @return page of devices with the specified status
     */
    Page<Device> findByStatus(Device.Status status, Pageable pageable);

    /**
     * Find online devices.
     *
     * @return list of online devices
     */
    List<Device> findByStatusIn(List<Device.Status> statuses);

    /**
     * Find devices by name (case-insensitive).
     *
     * @param name the device name
     * @return list of devices with matching name
     */
    List<Device> findByNameIgnoreCase(String name);

    /**
     * Find devices by name containing search term (case-insensitive).
     *
     * @param searchTerm the search term
     * @return list of devices with names containing the search term
     */
    List<Device> findByNameContainingIgnoreCase(String searchTerm);

    /**
     * Find devices in a room by type.
     *
     * @param roomId the room ID
     * @param deviceType the device type
     * @return list of devices in the room of the specified type
     */
    List<Device> findByRoomRoomIdAndDeviceType(UUID roomId, Device.DeviceType deviceType);

    /**
     * Find devices in a room by category.
     *
     * @param roomId the room ID
     * @param category the device category
     * @return list of devices in the room of the specified category
     */
    List<Device> findByRoomRoomIdAndCategory(UUID roomId, Device.Category category);

    /**
     * Find devices in a room by status.
     *
     * @param roomId the room ID
     * @param status the device status
     * @return list of devices in the room with the specified status
     */
    List<Device> findByRoomRoomIdAndStatus(UUID roomId, Device.Status status);

    /**
     * Find devices that haven't been seen recently (potentially offline).
     *
     * @param threshold the time threshold
     * @return list of devices not seen since the threshold
     */
    @Query("SELECT d FROM Device d " +
           "WHERE d.lastSeen IS NULL OR d.lastSeen < :threshold")
    List<Device> findDevicesNotSeenSince(@Param("threshold") LocalDateTime threshold);

    /**
     * Find devices with recent activity.
     *
     * @param threshold the time threshold
     * @return list of devices seen after the threshold
     */
    @Query("SELECT d FROM Device d " +
           "WHERE d.lastSeen > :threshold")
    List<Device> findDevicesWithRecentActivity(@Param("threshold") LocalDateTime threshold);

    /**
     * Find devices by firmware version.
     *
     * @param firmwareVersion the firmware version
     * @return list of devices with the specified firmware version
     */
    List<Device> findByFirmwareVersion(String firmwareVersion);

    /**
     * Find devices with outdated firmware.
     *
     * @param currentVersion the current firmware version
     * @return list of devices with outdated firmware
     */
    @Query("SELECT d FROM Device d " +
           "WHERE d.firmwareVersion IS NULL OR d.firmwareVersion != :currentVersion")
    List<Device> findDevicesWithOutdatedFirmware(@Param("currentVersion") String currentVersion);

    /**
     * Search devices by multiple criteria.
     *
     * @param searchTerm search term for name or description
     * @param roomId room ID filter (optional)
     * @param deviceType device type filter (optional)
     * @param category category filter (optional)
     * @param status status filter (optional)
     * @param pageable pagination information
     * @return page of matching devices
     */
    @Query("SELECT d FROM Device d " +
           "WHERE (:searchTerm IS NULL OR " +
           "       LOWER(d.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "       LOWER(d.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND (:roomId IS NULL OR d.room.roomId = :roomId) " +
           "AND (:deviceType IS NULL OR d.deviceType = :deviceType) " +
           "AND (:category IS NULL OR d.category = :category) " +
           "AND (:status IS NULL OR d.status = :status)")
    Page<Device> searchDevices(@Param("searchTerm") String searchTerm,
                              @Param("roomId") UUID roomId,
                              @Param("deviceType") Device.DeviceType deviceType,
                              @Param("category") Device.Category category,
                              @Param("status") Device.Status status,
                              Pageable pageable);

    /**
     * Find devices accessible by a user (through room access).
     *
     * @param userId the user ID
     * @return list of devices the user has access to
     */
    @Query("SELECT DISTINCT d FROM Device d " +
           "WHERE d.room.farm.owner.userId = :userId " +
           "OR d.room.roomId IN (" +
           "  SELECT ur.room.roomId FROM UserRoom ur " +
           "  WHERE ur.user.userId = :userId AND ur.isActive = true" +
           ")")
    List<Device> findDevicesAccessibleByUser(@Param("userId") UUID userId);

    /**
     * Find devices accessible by a user with pagination.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of devices the user has access to
     */
    @Query("SELECT DISTINCT d FROM Device d " +
           "WHERE d.room.farm.owner.userId = :userId " +
           "OR d.room.roomId IN (" +
           "  SELECT ur.room.roomId FROM UserRoom ur " +
           "  WHERE ur.user.userId = :userId AND ur.isActive = true" +
           ")")
    Page<Device> findDevicesAccessibleByUser(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Count devices by room.
     *
     * @param room the room
     * @return count of devices in the room
     */
    long countByRoom(Room room);

    /**
     * Count devices by room ID.
     *
     * @param roomId the room ID
     * @return count of devices in the room
     */
    long countByRoomRoomId(UUID roomId);

    /**
     * Count devices by status.
     *
     * @param status the device status
     * @return count of devices with the specified status
     */
    long countByStatus(Device.Status status);

    /**
     * Count devices by type.
     *
     * @param deviceType the device type
     * @return count of devices of the specified type
     */
    long countByDeviceType(Device.DeviceType deviceType);

    /**
     * Count devices by category.
     *
     * @param category the device category
     * @return count of devices in the specified category
     */
    long countByCategory(Device.Category category);

    /**
     * Get device statistics for a room.
     *
     * @param roomId the room ID
     * @return device statistics for the room
     */
    @Query("SELECT " +
           "COUNT(d.deviceId) as totalDevices, " +
           "COUNT(CASE WHEN d.status = 'ONLINE' THEN 1 END) as onlineDevices, " +
           "COUNT(CASE WHEN d.status = 'OFFLINE' THEN 1 END) as offlineDevices, " +
           "COUNT(CASE WHEN d.status = 'ERROR' THEN 1 END) as errorDevices, " +
           "COUNT(CASE WHEN d.deviceType = 'SENSOR' THEN 1 END) as sensorDevices, " +
           "COUNT(CASE WHEN d.deviceType = 'ACTUATOR' THEN 1 END) as actuatorDevices " +
           "FROM Device d " +
           "WHERE d.room.roomId = :roomId")
    Optional<DeviceStatistics> getDeviceStatistics(@Param("roomId") UUID roomId);

    /**
     * Get device statistics for a farm.
     *
     * @param farmId the farm ID
     * @return device statistics for the farm
     */
    @Query("SELECT " +
           "COUNT(d.deviceId) as totalDevices, " +
           "COUNT(CASE WHEN d.status = 'ONLINE' THEN 1 END) as onlineDevices, " +
           "COUNT(CASE WHEN d.status = 'OFFLINE' THEN 1 END) as offlineDevices, " +
           "COUNT(CASE WHEN d.status = 'ERROR' THEN 1 END) as errorDevices, " +
           "COUNT(CASE WHEN d.deviceType = 'SENSOR' THEN 1 END) as sensorDevices, " +
           "COUNT(CASE WHEN d.deviceType = 'ACTUATOR' THEN 1 END) as actuatorDevices " +
           "FROM Device d " +
           "WHERE d.room.farm.farmId = :farmId")
    Optional<DeviceStatistics> getDeviceStatisticsForFarm(@Param("farmId") UUID farmId);

    /**
     * Update device status and last seen timestamp.
     *
     * @param deviceId the device ID
     * @param status the new status
     * @param lastSeen the last seen timestamp
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE Device d SET d.status = :status, d.lastSeen = :lastSeen " +
           "WHERE d.deviceId = :deviceId")
    int updateDeviceStatus(@Param("deviceId") UUID deviceId,
                          @Param("status") Device.Status status,
                          @Param("lastSeen") LocalDateTime lastSeen);

    /**
     * Update device firmware version.
     *
     * @param deviceId the device ID
     * @param firmwareVersion the new firmware version
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE Device d SET d.firmwareVersion = :firmwareVersion " +
           "WHERE d.deviceId = :deviceId")
    int updateFirmwareVersion(@Param("deviceId") UUID deviceId,
                             @Param("firmwareVersion") String firmwareVersion);

    /**
     * Update last seen timestamp for multiple devices.
     *
     * @param deviceIds the device IDs
     * @param lastSeen the last seen timestamp
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE Device d SET d.lastSeen = :lastSeen " +
           "WHERE d.deviceId IN :deviceIds")
    int updateLastSeenBulk(@Param("deviceIds") List<UUID> deviceIds,
                          @Param("lastSeen") LocalDateTime lastSeen);

    /**
     * Mark devices as offline if not seen recently.
     *
     * @param threshold the time threshold
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE Device d SET d.status = 'OFFLINE' " +
           "WHERE (d.lastSeen IS NULL OR d.lastSeen < :threshold) " +
           "AND d.status != 'OFFLINE'")
    int markDevicesOffline(@Param("threshold") LocalDateTime threshold);

    /**
     * Check if MQTT topic exists.
     *
     * @param mqttTopic the MQTT topic
     * @return true if MQTT topic exists
     */
    boolean existsByMqttTopic(String mqttTopic);

    /**
     * Check if MQTT topic exists, excluding a specific device.
     *
     * @param mqttTopic the MQTT topic
     * @param excludeDeviceId the device ID to exclude
     * @return true if MQTT topic exists (excluding the specified device)
     */
    boolean existsByMqttTopicAndDeviceIdNot(String mqttTopic, UUID excludeDeviceId);

    /**
     * Check if device name exists in a room (case-insensitive).
     *
     * @param name the device name
     * @param roomId the room ID
     * @return true if device name exists in the room
     */
    boolean existsByNameIgnoreCaseAndRoomRoomId(String name, UUID roomId);

    /**
     * Check if device name exists in a room, excluding a specific device.
     *
     * @param name the device name
     * @param roomId the room ID
     * @param excludeDeviceId the device ID to exclude
     * @return true if device name exists in the room (excluding the specified device)
     */
    boolean existsByNameIgnoreCaseAndRoomRoomIdAndDeviceIdNot(String name, UUID roomId, UUID excludeDeviceId);

    /**
     * Find devices created after a specific date.
     *
     * @param date the date threshold
     * @return list of devices created after the date
     */
    List<Device> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find devices created between two dates.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return list of devices created in the date range
     */
    List<Device> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Interface for device statistics projection.
     */
    interface DeviceStatistics {
        Long getTotalDevices();
        Long getOnlineDevices();
        Long getOfflineDevices();
        Long getErrorDevices();
        Long getSensorDevices();
        Long getActuatorDevices();
    }
}