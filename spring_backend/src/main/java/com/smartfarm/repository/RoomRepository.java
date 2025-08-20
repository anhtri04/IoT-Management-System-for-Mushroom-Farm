package com.smartfarm.repository;

import com.smartfarm.entity.Farm;
import com.smartfarm.entity.Room;
import com.smartfarm.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Room entity operations.
 * Provides data access methods for room management and user access control.
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    /**
     * Find rooms by farm.
     *
     * @param farm the farm
     * @return list of rooms in the farm
     */
    List<Room> findByFarm(Farm farm);

    /**
     * Find rooms by farm with pagination.
     *
     * @param farm the farm
     * @param pageable pagination information
     * @return page of rooms in the farm
     */
    Page<Room> findByFarm(Farm farm, Pageable pageable);

    /**
     * Find rooms by farm ID.
     *
     * @param farmId the farm ID
     * @return list of rooms in the farm
     */
    List<Room> findByFarmFarmId(UUID farmId);

    /**
     * Find rooms by farm ID with pagination.
     *
     * @param farmId the farm ID
     * @param pageable pagination information
     * @return page of rooms in the farm
     */
    Page<Room> findByFarmFarmId(UUID farmId, Pageable pageable);

    /**
     * Find rooms by name (case-insensitive).
     *
     * @param name the room name
     * @return list of rooms with matching name
     */
    List<Room> findByNameIgnoreCase(String name);

    /**
     * Find rooms by name containing search term (case-insensitive).
     *
     * @param searchTerm the search term
     * @return list of rooms with names containing the search term
     */
    List<Room> findByNameContainingIgnoreCase(String searchTerm);

    /**
     * Find rooms by mushroom type.
     *
     * @param mushroomType the mushroom type
     * @return list of rooms growing the specified mushroom type
     */
    List<Room> findByMushroomType(String mushroomType);

    /**
     * Find rooms by stage.
     *
     * @param stage the cultivation stage
     * @return list of rooms in the specified stage
     */
    List<Room> findByStage(Room.Stage stage);

    /**
     * Find rooms by stage with pagination.
     *
     * @param stage the cultivation stage
     * @param pageable pagination information
     * @return page of rooms in the specified stage
     */
    Page<Room> findByStage(Room.Stage stage, Pageable pageable);

    /**
     * Find active rooms.
     *
     * @return list of active rooms
     */
    List<Room> findByIsActiveTrue();

    /**
     * Find active rooms with pagination.
     *
     * @param pageable pagination information
     * @return page of active rooms
     */
    Page<Room> findByIsActiveTrue(Pageable pageable);

    /**
     * Find rooms that a user has access to.
     *
     * @param userId the user ID
     * @return list of rooms the user has access to
     */
    @Query("SELECT DISTINCT r FROM Room r " +
           "WHERE r.farm.owner.userId = :userId " +
           "OR r.roomId IN (" +
           "  SELECT ur.room.roomId FROM UserRoom ur " +
           "  WHERE ur.user.userId = :userId AND ur.isActive = true" +
           ")")
    List<Room> findRoomsAccessibleByUser(@Param("userId") UUID userId);

    /**
     * Find rooms that a user has access to with pagination.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of rooms the user has access to
     */
    @Query("SELECT DISTINCT r FROM Room r " +
           "WHERE r.farm.owner.userId = :userId " +
           "OR r.roomId IN (" +
           "  SELECT ur.room.roomId FROM UserRoom ur " +
           "  WHERE ur.user.userId = :userId AND ur.isActive = true" +
           ")")
    Page<Room> findRoomsAccessibleByUser(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Find rooms where user has a specific role.
     *
     * @param userId the user ID
     * @param role the room role
     * @return list of rooms where user has the specified role
     */
    @Query("SELECT r FROM Room r " +
           "JOIN UserRoom ur ON ur.room = r " +
           "WHERE ur.user.userId = :userId AND ur.role = :role AND ur.isActive = true")
    List<Room> findRoomsByUserRole(@Param("userId") UUID userId, @Param("role") User.RoomRole role);

    /**
     * Find rooms in a farm that a user has access to.
     *
     * @param farmId the farm ID
     * @param userId the user ID
     * @return list of rooms in the farm that the user has access to
     */
    @Query("SELECT DISTINCT r FROM Room r " +
           "WHERE r.farm.farmId = :farmId " +
           "AND (r.farm.owner.userId = :userId " +
           "     OR r.roomId IN (" +
           "       SELECT ur.room.roomId FROM UserRoom ur " +
           "       WHERE ur.user.userId = :userId AND ur.isActive = true" +
           "     ))")
    List<Room> findRoomsInFarmAccessibleByUser(@Param("farmId") UUID farmId, @Param("userId") UUID userId);

    /**
     * Search rooms by multiple criteria.
     *
     * @param searchTerm search term for name or description
     * @param farmId farm ID filter (optional)
     * @param stage stage filter (optional)
     * @param mushroomType mushroom type filter (optional)
     * @param isActive active status filter (optional)
     * @param pageable pagination information
     * @return page of matching rooms
     */
    @Query("SELECT r FROM Room r " +
           "WHERE (:searchTerm IS NULL OR " +
           "       LOWER(r.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "       LOWER(r.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND (:farmId IS NULL OR r.farm.farmId = :farmId) " +
           "AND (:stage IS NULL OR r.stage = :stage) " +
           "AND (:mushroomType IS NULL OR LOWER(r.mushroomType) = LOWER(:mushroomType)) " +
           "AND (:isActive IS NULL OR r.isActive = :isActive)")
    Page<Room> searchRooms(@Param("searchTerm") String searchTerm,
                          @Param("farmId") UUID farmId,
                          @Param("stage") Room.Stage stage,
                          @Param("mushroomType") String mushroomType,
                          @Param("isActive") Boolean isActive,
                          Pageable pageable);

    /**
     * Count rooms by farm.
     *
     * @param farm the farm
     * @return count of rooms in the farm
     */
    long countByFarm(Farm farm);

    /**
     * Count rooms by farm ID.
     *
     * @param farmId the farm ID
     * @return count of rooms in the farm
     */
    long countByFarmFarmId(UUID farmId);

    /**
     * Count rooms by stage.
     *
     * @param stage the cultivation stage
     * @return count of rooms in the specified stage
     */
    long countByStage(Room.Stage stage);

    /**
     * Count active rooms.
     *
     * @return count of active rooms
     */
    long countByIsActiveTrue();

    /**
     * Find rooms with recent sensor data.
     *
     * @param threshold the time threshold
     * @return list of rooms with recent sensor data
     */
    @Query("SELECT DISTINCT r FROM Room r " +
           "JOIN Device d ON d.room = r " +
           "JOIN SensorData sd ON sd.device = d " +
           "WHERE sd.recordedAt > :threshold")
    List<Room> findRoomsWithRecentActivity(@Param("threshold") LocalDateTime threshold);

    /**
     * Find rooms with offline devices.
     *
     * @return list of rooms with offline devices
     */
    @Query("SELECT DISTINCT r FROM Room r " +
           "JOIN Device d ON d.room = r " +
           "WHERE d.status = 'OFFLINE'")
    List<Room> findRoomsWithOfflineDevices();

    /**
     * Find rooms with active alerts/notifications.
     *
     * @return list of rooms with active alerts
     */
    @Query("SELECT DISTINCT r FROM Room r " +
           "JOIN Notification n ON n.room = r " +
           "WHERE n.acknowledgedAt IS NULL")
    List<Room> findRoomsWithActiveAlerts();

    /**
     * Find rooms with active farming cycles.
     *
     * @return list of rooms with active farming cycles
     */
    @Query("SELECT DISTINCT r FROM Room r " +
           "JOIN FarmingCycle fc ON fc.room = r " +
           "WHERE fc.status IN ('PLANNING', 'INOCULATION', 'INCUBATION', 'FRUITING', 'HARVESTING')")
    List<Room> findRoomsWithActiveCycles();

    /**
     * Find rooms with automation rules.
     *
     * @return list of rooms with automation rules
     */
    @Query("SELECT DISTINCT r FROM Room r " +
           "JOIN AutomationRule ar ON ar.room = r " +
           "WHERE ar.enabled = true")
    List<Room> findRoomsWithAutomationRules();

    /**
     * Get room statistics including device count, sensor data count, etc.
     *
     * @param roomId the room ID
     * @return room statistics
     */
    @Query("SELECT r.roomId as roomId, r.name as roomName, " +
           "COUNT(DISTINCT d.deviceId) as deviceCount, " +
           "COUNT(DISTINCT CASE WHEN d.status = 'ONLINE' THEN d.deviceId END) as onlineDeviceCount, " +
           "COUNT(DISTINCT ar.ruleId) as automationRuleCount, " +
           "COUNT(DISTINCT fc.cycleId) as farmingCycleCount " +
           "FROM Room r " +
           "LEFT JOIN Device d ON d.room = r " +
           "LEFT JOIN AutomationRule ar ON ar.room = r " +
           "LEFT JOIN FarmingCycle fc ON fc.room = r " +
           "WHERE r.roomId = :roomId " +
           "GROUP BY r.roomId, r.name")
    Optional<RoomStatistics> getRoomStatistics(@Param("roomId") UUID roomId);

    /**
     * Find rooms by environmental conditions (temperature range).
     *
     * @param minTemp minimum temperature
     * @param maxTemp maximum temperature
     * @return list of rooms with target temperature in the specified range
     */
    @Query("SELECT r FROM Room r " +
           "WHERE r.targetTemperature BETWEEN :minTemp AND :maxTemp")
    List<Room> findRoomsByTemperatureRange(@Param("minTemp") Double minTemp, @Param("maxTemp") Double maxTemp);

    /**
     * Find rooms by humidity range.
     *
     * @param minHumidity minimum humidity
     * @param maxHumidity maximum humidity
     * @return list of rooms with target humidity in the specified range
     */
    @Query("SELECT r FROM Room r " +
           "WHERE r.targetHumidity BETWEEN :minHumidity AND :maxHumidity")
    List<Room> findRoomsByHumidityRange(@Param("minHumidity") Double minHumidity, @Param("maxHumidity") Double maxHumidity);

    /**
     * Check if a room name exists in a specific farm (case-insensitive).
     *
     * @param name the room name
     * @param farmId the farm ID
     * @return true if room name exists in the farm
     */
    boolean existsByNameIgnoreCaseAndFarmFarmId(String name, UUID farmId);

    /**
     * Check if a room name exists in a specific farm, excluding a specific room ID.
     *
     * @param name the room name
     * @param farmId the farm ID
     * @param excludeRoomId the room ID to exclude
     * @return true if room name exists in the farm (excluding the specified room)
     */
    boolean existsByNameIgnoreCaseAndFarmFarmIdAndRoomIdNot(String name, UUID farmId, UUID excludeRoomId);

    /**
     * Find rooms created after a specific date.
     *
     * @param date the date threshold
     * @return list of rooms created after the date
     */
    List<Room> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find rooms created between two dates.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return list of rooms created in the date range
     */
    List<Room> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Interface for room statistics projection.
     */
    interface RoomStatistics {
        UUID getRoomId();
        String getRoomName();
        Long getDeviceCount();
        Long getOnlineDeviceCount();
        Long getAutomationRuleCount();
        Long getFarmingCycleCount();
    }
}