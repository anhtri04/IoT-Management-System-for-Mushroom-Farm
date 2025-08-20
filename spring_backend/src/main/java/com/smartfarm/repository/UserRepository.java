package com.smartfarm.repository;

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
 * Repository interface for User entity operations.
 * Provides data access methods for user management and authentication.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by Cognito subject ID.
     * Used for authentication and user lookup after JWT validation.
     *
     * @param cognitoSub the Cognito subject ID
     * @return Optional containing the user if found
     */
    Optional<User> findByCognitoSub(String cognitoSub);

    /**
     * Find user by email address.
     * Used for user lookup and duplicate email validation.
     *
     * @param email the email address
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by email address (case-insensitive).
     *
     * @param email the email address
     * @return Optional containing the user if found
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Check if user exists by Cognito subject ID.
     *
     * @param cognitoSub the Cognito subject ID
     * @return true if user exists
     */
    boolean existsByCognitoSub(String cognitoSub);

    /**
     * Check if user exists by email address.
     *
     * @param email the email address
     * @return true if user exists
     */
    boolean existsByEmail(String email);

    /**
     * Check if user exists by email address (case-insensitive).
     *
     * @param email the email address
     * @return true if user exists
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Find users by role.
     *
     * @param role the user role
     * @return list of users with the specified role
     */
    List<User> findByRole(User.Role role);

    /**
     * Find users by role with pagination.
     *
     * @param role the user role
     * @param pageable pagination information
     * @return page of users with the specified role
     */
    Page<User> findByRole(User.Role role, Pageable pageable);

    /**
     * Find active users.
     *
     * @return list of active users
     */
    List<User> findByIsActiveTrue();

    /**
     * Find active users with pagination.
     *
     * @param pageable pagination information
     * @return page of active users
     */
    Page<User> findByIsActiveTrue(Pageable pageable);

    /**
     * Find users by full name containing the search term (case-insensitive).
     *
     * @param searchTerm the search term
     * @return list of matching users
     */
    List<User> findByFullNameContainingIgnoreCase(String searchTerm);

    /**
     * Find users by full name containing the search term with pagination.
     *
     * @param searchTerm the search term
     * @param pageable pagination information
     * @return page of matching users
     */
    Page<User> findByFullNameContainingIgnoreCase(String searchTerm, Pageable pageable);

    /**
     * Find users created after a specific date.
     *
     * @param date the date threshold
     * @return list of users created after the date
     */
    List<User> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find users created between two dates.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return list of users created in the date range
     */
    List<User> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find users who have access to a specific farm.
     *
     * @param farmId the farm ID
     * @return list of users with access to the farm
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN Farm f ON f.owner = u " +
           "WHERE f.farmId = :farmId")
    List<User> findUsersWithAccessToFarm(@Param("farmId") UUID farmId);

    /**
     * Find users who have access to a specific room.
     *
     * @param roomId the room ID
     * @return list of users with access to the room
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN UserRoom ur ON ur.user = u " +
           "WHERE ur.room.roomId = :roomId AND ur.isActive = true")
    List<User> findUsersWithAccessToRoom(@Param("roomId") UUID roomId);

    /**
     * Find users by room access role.
     *
     * @param roomId the room ID
     * @param role the room role
     * @return list of users with the specified role in the room
     */
    @Query("SELECT u FROM User u " +
           "JOIN UserRoom ur ON ur.user = u " +
           "WHERE ur.room.roomId = :roomId AND ur.role = :role AND ur.isActive = true")
    List<User> findUsersByRoomRole(@Param("roomId") UUID roomId, @Param("role") User.RoomRole role);

    /**
     * Search users by multiple criteria.
     *
     * @param searchTerm search term for name or email
     * @param role user role filter (optional)
     * @param isActive active status filter (optional)
     * @param pageable pagination information
     * @return page of matching users
     */
    @Query("SELECT u FROM User u " +
           "WHERE (:searchTerm IS NULL OR " +
           "       LOWER(u.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "       LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND (:role IS NULL OR u.role = :role) " +
           "AND (:isActive IS NULL OR u.isActive = :isActive)")
    Page<User> searchUsers(@Param("searchTerm") String searchTerm,
                          @Param("role") User.Role role,
                          @Param("isActive") Boolean isActive,
                          Pageable pageable);

    /**
     * Count users by role.
     *
     * @param role the user role
     * @return count of users with the specified role
     */
    long countByRole(User.Role role);

    /**
     * Count active users.
     *
     * @return count of active users
     */
    long countByIsActiveTrue();

    /**
     * Count users created after a specific date.
     *
     * @param date the date threshold
     * @return count of users created after the date
     */
    long countByCreatedAtAfter(LocalDateTime date);

    /**
     * Find users with expired sessions (last login before threshold).
     *
     * @param threshold the date threshold
     * @return list of users with expired sessions
     */
    @Query("SELECT u FROM User u " +
           "WHERE u.lastLoginAt IS NOT NULL AND u.lastLoginAt < :threshold")
    List<User> findUsersWithExpiredSessions(@Param("threshold") LocalDateTime threshold);

    /**
     * Find users who never logged in.
     *
     * @return list of users who never logged in
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt IS NULL")
    List<User> findUsersWhoNeverLoggedIn();

    /**
     * Update user's last login timestamp.
     *
     * @param userId the user ID
     * @param lastLoginAt the last login timestamp
     */
    @Query("UPDATE User u SET u.lastLoginAt = :lastLoginAt WHERE u.userId = :userId")
    void updateLastLoginAt(@Param("userId") UUID userId, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    /**
     * Update user's active status.
     *
     * @param userId the user ID
     * @param isActive the active status
     */
    @Query("UPDATE User u SET u.isActive = :isActive WHERE u.userId = :userId")
    void updateActiveStatus(@Param("userId") UUID userId, @Param("isActive") boolean isActive);

    /**
     * Find users who own farms.
     *
     * @return list of farm owners
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN Farm f ON f.owner = u")
    List<User> findFarmOwners();

    /**
     * Find users who own farms with pagination.
     *
     * @param pageable pagination information
     * @return page of farm owners
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN Farm f ON f.owner = u")
    Page<User> findFarmOwners(Pageable pageable);

    /**
     * Find users by timezone.
     *
     * @param timezone the timezone
     * @return list of users in the specified timezone
     */
    List<User> findByTimezone(String timezone);

    /**
     * Find users by language preference.
     *
     * @param language the language code
     * @return list of users with the specified language preference
     */
    List<User> findByLanguage(String language);

    /**
     * Find users who should receive notifications for a specific farm.
     *
     * @param farmId the farm ID
     * @return list of users who should receive notifications
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "WHERE u.userId IN (" +
           "  SELECT f.owner.userId FROM Farm f WHERE f.farmId = :farmId" +
           "  UNION" +
           "  SELECT ur.user.userId FROM UserRoom ur " +
           "  JOIN Room r ON ur.room = r " +
           "  WHERE r.farm.farmId = :farmId AND ur.isActive = true" +
           ") AND u.isActive = true")
    List<User> findNotificationRecipientsForFarm(@Param("farmId") UUID farmId);

    /**
     * Find users who should receive notifications for a specific room.
     *
     * @param roomId the room ID
     * @return list of users who should receive notifications
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "WHERE u.userId IN (" +
           "  SELECT r.farm.owner.userId FROM Room r WHERE r.roomId = :roomId" +
           "  UNION" +
           "  SELECT ur.user.userId FROM UserRoom ur " +
           "  WHERE ur.room.roomId = :roomId AND ur.isActive = true" +
           ") AND u.isActive = true")
    List<User> findNotificationRecipientsForRoom(@Param("roomId") UUID roomId);
}