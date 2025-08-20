package com.smartfarm.repository;

import com.smartfarm.entity.Recommendation;
import com.smartfarm.entity.Farm;
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
 * Repository interface for Recommendation entity operations.
 * Provides data access methods for AI-generated recommendation management.
 */
@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

    /**
     * Find recommendations by farm.
     *
     * @param farm the farm
     * @param pageable pagination information
     * @return page of recommendations for the farm
     */
    Page<Recommendation> findByFarm(Farm farm, Pageable pageable);

    /**
     * Find recommendations by farm ID.
     *
     * @param farmId the farm ID
     * @param pageable pagination information
     * @return page of recommendations for the farm
     */
    Page<Recommendation> findByFarmFarmId(UUID farmId, Pageable pageable);

    /**
     * Find recommendations by room.
     *
     * @param room the room
     * @param pageable pagination information
     * @return page of recommendations for the room
     */
    Page<Recommendation> findByRoom(Room room, Pageable pageable);

    /**
     * Find recommendations by room ID.
     *
     * @param roomId the room ID
     * @param pageable pagination information
     * @return page of recommendations for the room
     */
    Page<Recommendation> findByRoomRoomId(UUID roomId, Pageable pageable);

    /**
     * Find recommendations by model ID.
     *
     * @param modelId the model ID
     * @param pageable pagination information
     * @return page of recommendations from the specified model
     */
    Page<Recommendation> findByModelId(String modelId, Pageable pageable);

    /**
     * Find recommendations by confidence threshold.
     *
     * @param minConfidence the minimum confidence threshold
     * @param pageable pagination information
     * @return page of recommendations with confidence >= threshold
     */
    Page<Recommendation> findByConfidenceGreaterThanEqual(Double minConfidence, Pageable pageable);

    /**
     * Find recommendations by confidence range.
     *
     * @param minConfidence the minimum confidence
     * @param maxConfidence the maximum confidence
     * @param pageable pagination information
     * @return page of recommendations within confidence range
     */
    Page<Recommendation> findByConfidenceBetween(Double minConfidence, Double maxConfidence, Pageable pageable);

    /**
     * Find recommendations by time range.
     *
     * @param startTime the start time
     * @param endTime the end time
     * @param pageable pagination information
     * @return page of recommendations in the time range
     */
    Page<Recommendation> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Find recent recommendations for a farm.
     *
     * @param farmId the farm ID
     * @param limit the maximum number of recommendations to return
     * @return list of recent recommendations for the farm
     */
    @Query("SELECT r FROM Recommendation r " +
           "WHERE r.farm.farmId = :farmId " +
           "ORDER BY r.createdAt DESC " +
           "LIMIT :limit")
    List<Recommendation> findRecentRecommendationsForFarm(@Param("farmId") UUID farmId, @Param("limit") int limit);

    /**
     * Find recent recommendations for a room.
     *
     * @param roomId the room ID
     * @param limit the maximum number of recommendations to return
     * @return list of recent recommendations for the room
     */
    @Query("SELECT r FROM Recommendation r " +
           "WHERE r.room.roomId = :roomId " +
           "ORDER BY r.createdAt DESC " +
           "LIMIT :limit")
    List<Recommendation> findRecentRecommendationsForRoom(@Param("roomId") UUID roomId, @Param("limit") int limit);

    /**
     * Find high-confidence recommendations for a farm.
     *
     * @param farmId the farm ID
     * @param minConfidence the minimum confidence threshold
     * @return list of high-confidence recommendations for the farm
     */
    @Query("SELECT r FROM Recommendation r " +
           "WHERE r.farm.farmId = :farmId " +
           "AND r.confidence >= :minConfidence " +
           "ORDER BY r.confidence DESC, r.createdAt DESC")
    List<Recommendation> findHighConfidenceRecommendationsForFarm(@Param("farmId") UUID farmId, 
                                                                 @Param("minConfidence") Double minConfidence);

    /**
     * Find high-confidence recommendations for a room.
     *
     * @param roomId the room ID
     * @param minConfidence the minimum confidence threshold
     * @return list of high-confidence recommendations for the room
     */
    @Query("SELECT r FROM Recommendation r " +
           "WHERE r.room.roomId = :roomId " +
           "AND r.confidence >= :minConfidence " +
           "ORDER BY r.confidence DESC, r.createdAt DESC")
    List<Recommendation> findHighConfidenceRecommendationsForRoom(@Param("roomId") UUID roomId, 
                                                                 @Param("minConfidence") Double minConfidence);

    /**
     * Search recommendations by multiple criteria.
     *
     * @param farmId farm ID filter (optional)
     * @param roomId room ID filter (optional)
     * @param modelId model ID filter (optional)
     * @param minConfidence minimum confidence filter (optional)
     * @param maxConfidence maximum confidence filter (optional)
     * @param startTime start time filter (optional)
     * @param endTime end time filter (optional)
     * @param pageable pagination information
     * @return page of matching recommendations
     */
    @Query("SELECT r FROM Recommendation r " +
           "WHERE (:farmId IS NULL OR r.farm.farmId = :farmId) " +
           "AND (:roomId IS NULL OR r.room.roomId = :roomId) " +
           "AND (:modelId IS NULL OR r.modelId = :modelId) " +
           "AND (:minConfidence IS NULL OR r.confidence >= :minConfidence) " +
           "AND (:maxConfidence IS NULL OR r.confidence <= :maxConfidence) " +
           "AND (:startTime IS NULL OR r.createdAt >= :startTime) " +
           "AND (:endTime IS NULL OR r.createdAt <= :endTime)")
    Page<Recommendation> searchRecommendations(@Param("farmId") UUID farmId,
                                              @Param("roomId") UUID roomId,
                                              @Param("modelId") String modelId,
                                              @Param("minConfidence") Double minConfidence,
                                              @Param("maxConfidence") Double maxConfidence,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime,
                                              Pageable pageable);

    /**
     * Find recommendations accessible by a user.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of recommendations the user has access to
     */
    @Query("SELECT r FROM Recommendation r " +
           "WHERE r.farm.owner.userId = :userId " +
           "OR r.room.roomId IN (" +
           "  SELECT ur.room.roomId FROM UserRoom ur " +
           "  WHERE ur.user.userId = :userId AND ur.isActive = true" +
           ")")
    Page<Recommendation> findRecommendationsAccessibleByUser(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Find actionable recommendations for a user (high confidence).
     *
     * @param userId the user ID
     * @param minConfidence the minimum confidence threshold
     * @return list of actionable recommendations for the user
     */
    @Query("SELECT r FROM Recommendation r " +
           "WHERE r.confidence >= :minConfidence " +
           "AND (r.farm.owner.userId = :userId " +
           "OR r.room.roomId IN (" +
           "  SELECT ur.room.roomId FROM UserRoom ur " +
           "  WHERE ur.user.userId = :userId AND ur.isActive = true" +
           ")) " +
           "ORDER BY r.confidence DESC, r.createdAt DESC")
    List<Recommendation> findActionableRecommendationsForUser(@Param("userId") UUID userId, 
                                                             @Param("minConfidence") Double minConfidence);

    /**
     * Get recommendation statistics for a farm.
     *
     * @param farmId the farm ID
     * @param startTime the start time
     * @param endTime the end time
     * @return recommendation statistics for the farm
     */
    @Query("SELECT " +
           "COUNT(*) as totalRecommendations, " +
           "AVG(r.confidence) as averageConfidence, " +
           "MIN(r.confidence) as minConfidence, " +
           "MAX(r.confidence) as maxConfidence, " +
           "COUNT(CASE WHEN r.confidence >= 0.8 THEN 1 END) as highConfidenceRecommendations, " +
           "COUNT(CASE WHEN r.confidence >= 0.6 AND r.confidence < 0.8 THEN 1 END) as mediumConfidenceRecommendations, " +
           "COUNT(CASE WHEN r.confidence < 0.6 THEN 1 END) as lowConfidenceRecommendations, " +
           "COUNT(DISTINCT r.modelId) as uniqueModels " +
           "FROM Recommendation r " +
           "WHERE r.farm.farmId = :farmId " +
           "AND r.createdAt BETWEEN :startTime AND :endTime")
    Optional<RecommendationStatistics> getRecommendationStatisticsForFarm(@Param("farmId") UUID farmId,
                                                                          @Param("startTime") LocalDateTime startTime,
                                                                          @Param("endTime") LocalDateTime endTime);

    /**
     * Get recommendation statistics for a room.
     *
     * @param roomId the room ID
     * @param startTime the start time
     * @param endTime the end time
     * @return recommendation statistics for the room
     */
    @Query("SELECT " +
           "COUNT(*) as totalRecommendations, " +
           "AVG(r.confidence) as averageConfidence, " +
           "MIN(r.confidence) as minConfidence, " +
           "MAX(r.confidence) as maxConfidence, " +
           "COUNT(CASE WHEN r.confidence >= 0.8 THEN 1 END) as highConfidenceRecommendations, " +
           "COUNT(CASE WHEN r.confidence >= 0.6 AND r.confidence < 0.8 THEN 1 END) as mediumConfidenceRecommendations, " +
           "COUNT(CASE WHEN r.confidence < 0.6 THEN 1 END) as lowConfidenceRecommendations, " +
           "COUNT(DISTINCT r.modelId) as uniqueModels " +
           "FROM Recommendation r " +
           "WHERE r.room.roomId = :roomId " +
           "AND r.createdAt BETWEEN :startTime AND :endTime")
    Optional<RecommendationStatistics> getRecommendationStatisticsForRoom(@Param("roomId") UUID roomId,
                                                                          @Param("startTime") LocalDateTime startTime,
                                                                          @Param("endTime") LocalDateTime endTime);

    /**
     * Get model performance statistics.
     *
     * @param modelId the model ID
     * @param startTime the start time
     * @param endTime the end time
     * @return model performance statistics
     */
    @Query("SELECT " +
           "COUNT(*) as totalRecommendations, " +
           "AVG(r.confidence) as averageConfidence, " +
           "MIN(r.confidence) as minConfidence, " +
           "MAX(r.confidence) as maxConfidence, " +
           "STDDEV(r.confidence) as confidenceStdDev " +
           "FROM Recommendation r " +
           "WHERE r.modelId = :modelId " +
           "AND r.createdAt BETWEEN :startTime AND :endTime")
    Optional<ModelPerformanceStatistics> getModelPerformanceStatistics(@Param("modelId") String modelId,
                                                                       @Param("startTime") LocalDateTime startTime,
                                                                       @Param("endTime") LocalDateTime endTime);

    /**
     * Find recommendations by payload content (JSON search).
     *
     * @param jsonPath the JSON path to search
     * @param value the value to search for
     * @param pageable pagination information
     * @return page of recommendations matching the JSON criteria
     */
    @Query(value = "SELECT * FROM recommendations r " +
                   "WHERE r.payload ->> :jsonPath = :value", 
           nativeQuery = true)
    Page<Recommendation> findByPayloadContent(@Param("jsonPath") String jsonPath, 
                                             @Param("value") String value, 
                                             Pageable pageable);

    /**
     * Find recommendations containing specific action type in payload.
     *
     * @param actionType the action type to search for
     * @param pageable pagination information
     * @return page of recommendations containing the action type
     */
    @Query(value = "SELECT * FROM recommendations r " +
                   "WHERE r.payload -> 'actions' @> '[{\"type\": \":actionType\"}]'", 
           nativeQuery = true)
    Page<Recommendation> findByActionType(@Param("actionType") String actionType, Pageable pageable);

    /**
     * Find recommendations with specific priority level in payload.
     *
     * @param priority the priority level
     * @param pageable pagination information
     * @return page of recommendations with the specified priority
     */
    @Query(value = "SELECT * FROM recommendations r " +
                   "WHERE r.payload ->> 'priority' = :priority", 
           nativeQuery = true)
    Page<Recommendation> findByPriority(@Param("priority") String priority, Pageable pageable);

    /**
     * Count recommendations by farm.
     *
     * @param farm the farm
     * @return count of recommendations for the farm
     */
    long countByFarm(Farm farm);

    /**
     * Count recommendations by farm ID.
     *
     * @param farmId the farm ID
     * @return count of recommendations for the farm
     */
    long countByFarmFarmId(UUID farmId);

    /**
     * Count recommendations by room.
     *
     * @param room the room
     * @return count of recommendations for the room
     */
    long countByRoom(Room room);

    /**
     * Count recommendations by room ID.
     *
     * @param roomId the room ID
     * @return count of recommendations for the room
     */
    long countByRoomRoomId(UUID roomId);

    /**
     * Count recommendations by model ID.
     *
     * @param modelId the model ID
     * @return count of recommendations from the specified model
     */
    long countByModelId(String modelId);

    /**
     * Count high-confidence recommendations.
     *
     * @param minConfidence the minimum confidence threshold
     * @return count of recommendations with confidence >= threshold
     */
    long countByConfidenceGreaterThanEqual(Double minConfidence);

    /**
     * Count recommendations in time range.
     *
     * @param startTime the start time
     * @param endTime the end time
     * @return count of recommendations in the time range
     */
    long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find recommendations created after a specific time.
     *
     * @param time the time threshold
     * @return list of recommendations created after the time
     */
    List<Recommendation> findByCreatedAtAfter(LocalDateTime time);

    /**
     * Find recommendations created before a specific time.
     *
     * @param time the time threshold
     * @return list of recommendations created before the time
     */
    List<Recommendation> findByCreatedAtBefore(LocalDateTime time);

    /**
     * Delete recommendations created before a specific date.
     *
     * @param cutoffDate the cutoff date
     * @return number of deleted records
     */
    long deleteByCreatedAtBefore(LocalDateTime cutoffDate);

    /**
     * Delete low-confidence recommendations created before a specific date.
     *
     * @param cutoffDate the cutoff date
     * @param maxConfidence the maximum confidence threshold for deletion
     * @return number of deleted records
     */
    long deleteByCreatedAtBeforeAndConfidenceLessThan(LocalDateTime cutoffDate, Double maxConfidence);

    /**
     * Interface for recommendation statistics projection.
     */
    interface RecommendationStatistics {
        Long getTotalRecommendations();
        Double getAverageConfidence();
        Double getMinConfidence();
        Double getMaxConfidence();
        Long getHighConfidenceRecommendations();
        Long getMediumConfidenceRecommendations();
        Long getLowConfidenceRecommendations();
        Long getUniqueModels();
    }

    /**
     * Interface for model performance statistics projection.
     */
    interface ModelPerformanceStatistics {
        Long getTotalRecommendations();
        Double getAverageConfidence();
        Double getMinConfidence();
        Double getMaxConfidence();
        Double getConfidenceStdDev();
    }
}