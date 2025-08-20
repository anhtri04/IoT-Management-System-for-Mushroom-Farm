package com.smartfarm.repository;

import com.smartfarm.entity.Farm;
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
 * Repository interface for Farm entity operations.
 * Provides data access methods for farm management and ownership.
 */
@Repository
public interface FarmRepository extends JpaRepository<Farm, UUID> {

    /**
     * Find farms owned by a specific user.
     *
     * @param owner the farm owner
     * @return list of farms owned by the user
     */
    List<Farm> findByOwner(User owner);

    /**
     * Find farms owned by a specific user with pagination.
     *
     * @param owner the farm owner
     * @param pageable pagination information
     * @return page of farms owned by the user
     */
    Page<Farm> findByOwner(User owner, Pageable pageable);

    /**
     * Find farms by owner ID.
     *
     * @param ownerId the owner's user ID
     * @return list of farms owned by the user
     */
    List<Farm> findByOwnerUserId(UUID ownerId);

    /**
     * Find farms by owner ID with pagination.
     *
     * @param ownerId the owner's user ID
     * @param pageable pagination information
     * @return page of farms owned by the user
     */
    Page<Farm> findByOwnerUserId(UUID ownerId, Pageable pageable);

    /**
     * Find farms by name (case-insensitive).
     *
     * @param name the farm name
     * @return list of farms with matching name
     */
    List<Farm> findByNameIgnoreCase(String name);

    /**
     * Find farms by name containing search term (case-insensitive).
     *
     * @param searchTerm the search term
     * @return list of farms with names containing the search term
     */
    List<Farm> findByNameContainingIgnoreCase(String searchTerm);

    /**
     * Find farms by name containing search term with pagination.
     *
     * @param searchTerm the search term
     * @param pageable pagination information
     * @return page of farms with names containing the search term
     */
    Page<Farm> findByNameContainingIgnoreCase(String searchTerm, Pageable pageable);

    /**
     * Find farms by location containing search term (case-insensitive).
     *
     * @param location the location search term
     * @return list of farms with locations containing the search term
     */
    List<Farm> findByLocationContainingIgnoreCase(String location);

    /**
     * Find farms created after a specific date.
     *
     * @param date the date threshold
     * @return list of farms created after the date
     */
    List<Farm> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find farms created between two dates.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return list of farms created in the date range
     */
    List<Farm> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find active farms.
     *
     * @return list of active farms
     */
    List<Farm> findByIsActiveTrue();

    /**
     * Find active farms with pagination.
     *
     * @param pageable pagination information
     * @return page of active farms
     */
    Page<Farm> findByIsActiveTrue(Pageable pageable);

    /**
     * Find farms that a user has access to (either as owner or through room assignments).
     *
     * @param userId the user ID
     * @return list of farms the user has access to
     */
    @Query("SELECT DISTINCT f FROM Farm f " +
           "WHERE f.owner.userId = :userId " +
           "OR f.farmId IN (" +
           "  SELECT r.farm.farmId FROM Room r " +
           "  JOIN UserRoom ur ON ur.room = r " +
           "  WHERE ur.user.userId = :userId AND ur.isActive = true" +
           ")")
    List<Farm> findFarmsAccessibleByUser(@Param("userId") UUID userId);

    /**
     * Find farms that a user has access to with pagination.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of farms the user has access to
     */
    @Query("SELECT DISTINCT f FROM Farm f " +
           "WHERE f.owner.userId = :userId " +
           "OR f.farmId IN (" +
           "  SELECT r.farm.farmId FROM Room r " +
           "  JOIN UserRoom ur ON ur.room = r " +
           "  WHERE ur.user.userId = :userId AND ur.isActive = true" +
           ")")
    Page<Farm> findFarmsAccessibleByUser(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Find farms where user has a specific role.
     *
     * @param userId the user ID
     * @param role the room role
     * @return list of farms where user has the specified role
     */
    @Query("SELECT DISTINCT f FROM Farm f " +
           "JOIN Room r ON r.farm = f " +
           "JOIN UserRoom ur ON ur.room = r " +
           "WHERE ur.user.userId = :userId AND ur.role = :role AND ur.isActive = true")
    List<Farm> findFarmsByUserRole(@Param("userId") UUID userId, @Param("role") User.RoomRole role);

    /**
     * Search farms by multiple criteria.
     *
     * @param searchTerm search term for name or location
     * @param ownerId owner ID filter (optional)
     * @param isActive active status filter (optional)
     * @param pageable pagination information
     * @return page of matching farms
     */
    @Query("SELECT f FROM Farm f " +
           "WHERE (:searchTerm IS NULL OR " +
           "       LOWER(f.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "       LOWER(f.location) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND (:ownerId IS NULL OR f.owner.userId = :ownerId) " +
           "AND (:isActive IS NULL OR f.isActive = :isActive)")
    Page<Farm> searchFarms(@Param("searchTerm") String searchTerm,
                          @Param("ownerId") UUID ownerId,
                          @Param("isActive") Boolean isActive,
                          Pageable pageable);

    /**
     * Count farms by owner.
     *
     * @param owner the farm owner
     * @return count of farms owned by the user
     */
    long countByOwner(User owner);

    /**
     * Count farms by owner ID.
     *
     * @param ownerId the owner's user ID
     * @return count of farms owned by the user
     */
    long countByOwnerUserId(UUID ownerId);

    /**
     * Count active farms.
     *
     * @return count of active farms
     */
    long countByIsActiveTrue();

    /**
     * Count farms created after a specific date.
     *
     * @param date the date threshold
     * @return count of farms created after the date
     */
    long countByCreatedAtAfter(LocalDateTime date);

    /**
     * Find farms with recent activity (rooms with recent sensor data).
     *
     * @param threshold the time threshold
     * @return list of farms with recent activity
     */
    @Query("SELECT DISTINCT f FROM Farm f " +
           "JOIN Room r ON r.farm = f " +
           "JOIN Device d ON d.room = r " +
           "JOIN SensorData sd ON sd.device = d " +
           "WHERE sd.recordedAt > :threshold")
    List<Farm> findFarmsWithRecentActivity(@Param("threshold") LocalDateTime threshold);

    /**
     * Find farms with devices that are offline.
     *
     * @return list of farms with offline devices
     */
    @Query("SELECT DISTINCT f FROM Farm f " +
           "JOIN Room r ON r.farm = f " +
           "JOIN Device d ON d.room = r " +
           "WHERE d.status = 'OFFLINE'")
    List<Farm> findFarmsWithOfflineDevices();

    /**
     * Find farms with active alerts/notifications.
     *
     * @return list of farms with active alerts
     */
    @Query("SELECT DISTINCT f FROM Farm f " +
           "JOIN Notification n ON n.farm = f " +
           "WHERE n.acknowledgedAt IS NULL")
    List<Farm> findFarmsWithActiveAlerts();

    /**
     * Get farm statistics including room count, device count, etc.
     *
     * @param farmId the farm ID
     * @return farm statistics
     */
    @Query("SELECT f.farmId as farmId, f.name as farmName, " +
           "COUNT(DISTINCT r.roomId) as roomCount, " +
           "COUNT(DISTINCT d.deviceId) as deviceCount, " +
           "COUNT(DISTINCT CASE WHEN d.status = 'ONLINE' THEN d.deviceId END) as onlineDeviceCount " +
           "FROM Farm f " +
           "LEFT JOIN Room r ON r.farm = f " +
           "LEFT JOIN Device d ON d.room = r " +
           "WHERE f.farmId = :farmId " +
           "GROUP BY f.farmId, f.name")
    Optional<FarmStatistics> getFarmStatistics(@Param("farmId") UUID farmId);

    /**
     * Find farms by geographic coordinates within a radius.
     *
     * @param latitude the center latitude
     * @param longitude the center longitude
     * @param radiusKm the radius in kilometers
     * @return list of farms within the specified radius
     */
    @Query("SELECT f FROM Farm f " +
           "WHERE f.latitude IS NOT NULL AND f.longitude IS NOT NULL " +
           "AND (6371 * acos(cos(radians(:latitude)) * cos(radians(f.latitude)) * " +
           "cos(radians(f.longitude) - radians(:longitude)) + " +
           "sin(radians(:latitude)) * sin(radians(f.latitude)))) <= :radiusKm")
    List<Farm> findFarmsWithinRadius(@Param("latitude") Double latitude,
                                    @Param("longitude") Double longitude,
                                    @Param("radiusKm") Double radiusKm);

    /**
     * Find farms by farm type.
     *
     * @param farmType the farm type
     * @return list of farms with the specified type
     */
    List<Farm> findByFarmType(Farm.FarmType farmType);

    /**
     * Find farms by farm type with pagination.
     *
     * @param farmType the farm type
     * @param pageable pagination information
     * @return page of farms with the specified type
     */
    Page<Farm> findByFarmType(Farm.FarmType farmType, Pageable pageable);

    /**
     * Check if a farm name exists for a specific owner (case-insensitive).
     *
     * @param name the farm name
     * @param ownerId the owner ID
     * @return true if farm name exists for the owner
     */
    boolean existsByNameIgnoreCaseAndOwnerUserId(String name, UUID ownerId);

    /**
     * Check if a farm name exists for a specific owner, excluding a specific farm ID.
     *
     * @param name the farm name
     * @param ownerId the owner ID
     * @param excludeFarmId the farm ID to exclude
     * @return true if farm name exists for the owner (excluding the specified farm)
     */
    boolean existsByNameIgnoreCaseAndOwnerUserIdAndFarmIdNot(String name, UUID ownerId, UUID excludeFarmId);

    /**
     * Interface for farm statistics projection.
     */
    interface FarmStatistics {
        UUID getFarmId();
        String getFarmName();
        Long getRoomCount();
        Long getDeviceCount();
        Long getOnlineDeviceCount();
    }
}