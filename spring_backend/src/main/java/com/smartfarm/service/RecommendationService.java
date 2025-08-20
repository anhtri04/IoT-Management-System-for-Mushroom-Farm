package com.smartfarm.service;

import com.smartfarm.entity.Recommendation;
import com.smartfarm.entity.Farm;
import com.smartfarm.entity.Room;
import com.smartfarm.repository.RecommendationRepository;
import com.smartfarm.repository.FarmRepository;
import com.smartfarm.repository.RoomRepository;
import com.smartfarm.repository.SensorDataRepository;
import com.smartfarm.exception.ResourceNotFoundException;
import com.smartfarm.exception.UnauthorizedException;
import com.smartfarm.exception.ValidationException;
import com.smartfarm.dto.RecommendationDto;
import com.smartfarm.dto.RecommendationCreateDto;
import com.smartfarm.dto.SensorDataDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service class for Recommendation entity operations.
 * Handles business logic for AI-powered recommendations and Bedrock integration.
 */
@Service
@Transactional
public class RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);

    private final RecommendationRepository recommendationRepository;
    private final FarmRepository farmRepository;
    private final RoomRepository roomRepository;
    private final SensorDataRepository sensorDataRepository;
    private final FarmService farmService;
    private final RoomService roomService;
    private final SensorDataService sensorDataService;
    private final ObjectMapper objectMapper;

    @Autowired
    public RecommendationService(RecommendationRepository recommendationRepository, FarmRepository farmRepository,
                                RoomRepository roomRepository, SensorDataRepository sensorDataRepository,
                                FarmService farmService, RoomService roomService, 
                                SensorDataService sensorDataService, ObjectMapper objectMapper) {
        this.recommendationRepository = recommendationRepository;
        this.farmRepository = farmRepository;
        this.roomRepository = roomRepository;
        this.sensorDataRepository = sensorDataRepository;
        this.farmService = farmService;
        this.roomService = roomService;
        this.sensorDataService = sensorDataService;
        this.objectMapper = objectMapper;
    }

    /**
     * Create a new recommendation.
     *
     * @param createDto the recommendation creation data
     * @return the created recommendation
     * @throws ResourceNotFoundException if farm or room not found
     * @throws ValidationException if recommendation data is invalid
     */
    public RecommendationDto createRecommendation(RecommendationCreateDto createDto) {
        logger.debug("Creating recommendation for farm: {}, room: {}, model: {}", 
                    createDto.getFarmId(), createDto.getRoomId(), createDto.getModelId());

        validateRecommendationCreate(createDto);

        Recommendation recommendation = new Recommendation();
        recommendation.setPayload(createDto.getPayload());
        recommendation.setConfidence(createDto.getConfidence());
        recommendation.setModelId(createDto.getModelId());
        recommendation.setCreatedAt(LocalDateTime.now());

        // Set farm reference
        if (createDto.getFarmId() != null) {
            Farm farm = farmRepository.findById(createDto.getFarmId())
                    .orElseThrow(() -> new ResourceNotFoundException("Farm not found with ID: " + createDto.getFarmId()));
            recommendation.setFarm(farm);
        }

        // Set room reference
        if (createDto.getRoomId() != null) {
            Room room = roomRepository.findById(createDto.getRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + createDto.getRoomId()));
            recommendation.setRoom(room);
            
            // If room is set but farm is not, inherit farm from room
            if (recommendation.getFarm() == null) {
                recommendation.setFarm(room.getFarm());
            }
        }

        Recommendation savedRecommendation = recommendationRepository.save(recommendation);
        logger.debug("Recommendation created with ID: {}", savedRecommendation.getRecId());

        return convertToDto(savedRecommendation);
    }

    /**
     * Get recommendation by ID.
     *
     * @param recId the recommendation ID
     * @param userId the user ID (for access control)
     * @return the recommendation
     * @throws ResourceNotFoundException if recommendation not found
     * @throws UnauthorizedException if user doesn't have access
     */
    @Transactional(readOnly = true)
    public RecommendationDto getRecommendation(UUID recId, UUID userId) {
        logger.debug("Fetching recommendation: {} by user: {}", recId, userId);

        Recommendation recommendation = recommendationRepository.findById(recId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found with ID: " + recId));

        if (!hasAccessToRecommendation(recommendation, userId)) {
            throw new UnauthorizedException("User does not have access to recommendation: " + recId);
        }

        return convertToDto(recommendation);
    }

    /**
     * Get recommendations for a farm.
     *
     * @param farmId the farm ID
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of farm recommendations
     * @throws UnauthorizedException if user doesn't have access to farm
     */
    @Transactional(readOnly = true)
    public Page<RecommendationDto> getFarmRecommendations(UUID farmId, UUID userId, Pageable pageable) {
        logger.debug("Fetching recommendations for farm: {} by user: {}", farmId, userId);

        if (!farmService.hasAccessToFarm(farmId, userId)) {
            throw new UnauthorizedException("User does not have access to farm: " + farmId);
        }

        return recommendationRepository.findByFarmOrderByCreatedAtDesc(farmId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get recommendations for a room.
     *
     * @param roomId the room ID
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of room recommendations
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public Page<RecommendationDto> getRoomRecommendations(UUID roomId, UUID userId, Pageable pageable) {
        logger.debug("Fetching recommendations for room: {} by user: {}", roomId, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        return recommendationRepository.findByRoomOrderByCreatedAtDesc(roomId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get recommendations by confidence threshold.
     *
     * @param farmId the farm ID (optional)
     * @param roomId the room ID (optional)
     * @param minConfidence minimum confidence threshold
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of high-confidence recommendations
     */
    @Transactional(readOnly = true)
    public Page<RecommendationDto> getHighConfidenceRecommendations(UUID farmId, UUID roomId, 
                                                                   Double minConfidence, UUID userId, Pageable pageable) {
        logger.debug("Fetching high-confidence recommendations (>= {}) for farm: {}, room: {} by user: {}", 
                    minConfidence, farmId, roomId, userId);

        if (farmId != null && !farmService.hasAccessToFarm(farmId, userId)) {
            throw new UnauthorizedException("User does not have access to farm: " + farmId);
        }

        if (roomId != null && !roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        if (roomId != null) {
            return recommendationRepository.findByRoomAndConfidenceGreaterThanEqualOrderByCreatedAtDesc(
                    roomId, minConfidence, pageable).map(this::convertToDto);
        } else if (farmId != null) {
            return recommendationRepository.findByFarmAndConfidenceGreaterThanEqualOrderByCreatedAtDesc(
                    farmId, minConfidence, pageable).map(this::convertToDto);
        } else {
            return recommendationRepository.findUserRecommendationsByConfidence(userId, minConfidence, pageable)
                    .map(this::convertToDto);
        }
    }

    /**
     * Get recommendations by model ID.
     *
     * @param modelId the model ID
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of recommendations from the specified model
     */
    @Transactional(readOnly = true)
    public Page<RecommendationDto> getRecommendationsByModel(String modelId, UUID userId, Pageable pageable) {
        logger.debug("Fetching recommendations from model: {} by user: {}", modelId, userId);

        return recommendationRepository.findUserRecommendationsByModel(userId, modelId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get recommendations by date range.
     *
     * @param farmId the farm ID (optional)
     * @param roomId the room ID (optional)
     * @param startTime the start time
     * @param endTime the end time
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of recommendations in the date range
     */
    @Transactional(readOnly = true)
    public Page<RecommendationDto> getRecommendationsByDateRange(UUID farmId, UUID roomId, 
                                                               LocalDateTime startTime, LocalDateTime endTime, 
                                                               UUID userId, Pageable pageable) {
        logger.debug("Fetching recommendations for farm: {}, room: {} from {} to {} by user: {}", 
                    farmId, roomId, startTime, endTime, userId);

        if (farmId != null && !farmService.hasAccessToFarm(farmId, userId)) {
            throw new UnauthorizedException("User does not have access to farm: " + farmId);
        }

        if (roomId != null && !roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        if (roomId != null) {
            return recommendationRepository.findByRoomAndDateRange(roomId, startTime, endTime, pageable)
                    .map(this::convertToDto);
        } else if (farmId != null) {
            return recommendationRepository.findByFarmAndDateRange(farmId, startTime, endTime, pageable)
                    .map(this::convertToDto);
        } else {
            return recommendationRepository.findUserRecommendationsByDateRange(userId, startTime, endTime, pageable)
                    .map(this::convertToDto);
        }
    }

    /**
     * Generate recommendation for a room based on recent sensor data.
     * This method aggregates recent sensor data and creates a recommendation payload.
     *
     * @param roomId the room ID
     * @param userId the user ID (for access control)
     * @param hoursBack hours of historical data to analyze
     * @return the generated recommendation
     * @throws UnauthorizedException if user doesn't have access to room
     */
    public RecommendationDto generateRoomRecommendation(UUID roomId, UUID userId, int hoursBack) {
        logger.debug("Generating recommendation for room: {} by user: {} using {} hours of data", 
                    roomId, userId, hoursBack);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

        // Get recent sensor data for analysis
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(hoursBack);
        
        List<SensorDataDto> recentData = sensorDataService.getRoomSensorDataByTimeRange(
                roomId, startTime, endTime, userId);

        // Analyze data and generate recommendation
        Map<String, Object> analysisResult = analyzeSensorData(recentData, room);
        
        // Create recommendation payload
        Map<String, Object> payload = createRecommendationPayload(analysisResult, room);
        
        // Calculate confidence based on data quality and analysis
        double confidence = calculateConfidence(recentData, analysisResult);

        // Create recommendation
        RecommendationCreateDto createDto = new RecommendationCreateDto();
        createDto.setFarmId(room.getFarm().getFarmId());
        createDto.setRoomId(roomId);
        createDto.setPayload(payload);
        createDto.setConfidence(confidence);
        createDto.setModelId("internal-analysis-v1");

        return createRecommendation(createDto);
    }

    /**
     * Generate Bedrock-powered recommendation (placeholder implementation).
     * In production, this would integrate with AWS Bedrock API.
     *
     * @param roomId the room ID
     * @param userId the user ID (for access control)
     * @param modelId the Bedrock model ID
     * @param hoursBack hours of historical data to analyze
     * @return the generated recommendation
     */
    public RecommendationDto generateBedrockRecommendation(UUID roomId, UUID userId, 
                                                          String modelId, int hoursBack) {
        logger.debug("Generating Bedrock recommendation for room: {} using model: {} by user: {}", 
                    roomId, modelId, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

        // Get recent sensor data for analysis
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(hoursBack);
        
        List<SensorDataDto> recentData = sensorDataService.getRoomSensorDataByTimeRange(
                roomId, startTime, endTime, userId);

        // TODO: Integrate with AWS Bedrock API
        // For now, create a mock recommendation
        Map<String, Object> payload = createMockBedrockRecommendation(recentData, room, modelId);
        double confidence = 0.85; // Mock confidence

        // Create recommendation
        RecommendationCreateDto createDto = new RecommendationCreateDto();
        createDto.setFarmId(room.getFarm().getFarmId());
        createDto.setRoomId(roomId);
        createDto.setPayload(payload);
        createDto.setConfidence(confidence);
        createDto.setModelId(modelId);

        return createRecommendation(createDto);
    }

    /**
     * Get recommendation statistics.
     *
     * @param farmId the farm ID (optional)
     * @param roomId the room ID (optional)
     * @param startTime the start time
     * @param endTime the end time
     * @param userId the user ID (for access control)
     * @return recommendation statistics
     */
    @Transactional(readOnly = true)
    public RecommendationStatistics getRecommendationStatistics(UUID farmId, UUID roomId, 
                                                              LocalDateTime startTime, LocalDateTime endTime, 
                                                              UUID userId) {
        logger.debug("Fetching recommendation statistics for farm: {}, room: {} from {} to {} by user: {}", 
                    farmId, roomId, startTime, endTime, userId);

        if (farmId != null && !farmService.hasAccessToFarm(farmId, userId)) {
            throw new UnauthorizedException("User does not have access to farm: " + farmId);
        }

        if (roomId != null && !roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        var stats = recommendationRepository.getRecommendationStatistics(farmId, roomId, startTime, endTime)
                .orElse(new RecommendationRepository.RecommendationStatistics() {
                    @Override public Long getTotalRecommendations() { return 0L; }
                    @Override public Double getAverageConfidence() { return 0.0; }
                    @Override public Long getHighConfidenceRecommendations() { return 0L; }
                    @Override public Long getUniqueModels() { return 0L; }
                });

        return new RecommendationStatistics(
            stats.getTotalRecommendations(),
            stats.getAverageConfidence(),
            stats.getHighConfidenceRecommendations(),
            stats.getUniqueModels()
        );
    }

    /**
     * Delete old recommendations.
     *
     * @param olderThan delete recommendations older than this date
     * @param keepHighConfidence whether to keep high-confidence recommendations
     * @param confidenceThreshold confidence threshold for keeping recommendations
     * @return number of recommendations deleted
     */
    public int deleteOldRecommendations(LocalDateTime olderThan, boolean keepHighConfidence, 
                                       double confidenceThreshold) {
        logger.debug("Deleting recommendations older than: {}, keepHighConfidence: {}, threshold: {}", 
                    olderThan, keepHighConfidence, confidenceThreshold);

        int deletedCount;
        if (keepHighConfidence) {
            deletedCount = recommendationRepository.deleteOldLowConfidenceRecommendations(
                    olderThan, confidenceThreshold);
        } else {
            deletedCount = recommendationRepository.deleteOldRecommendations(olderThan);
        }

        logger.info("Deleted {} old recommendations", deletedCount);
        return deletedCount;
    }

    /**
     * Analyze sensor data to identify patterns and issues.
     *
     * @param sensorData the sensor data to analyze
     * @param room the room context
     * @return analysis results
     */
    private Map<String, Object> analyzeSensorData(List<SensorDataDto> sensorData, Room room) {
        Map<String, Object> analysis = new HashMap<>();
        
        if (sensorData.isEmpty()) {
            analysis.put("status", "insufficient_data");
            analysis.put("message", "Not enough sensor data for analysis");
            return analysis;
        }

        // Calculate averages and ranges
        double avgTemp = sensorData.stream()
                .filter(d -> d.getTemperatureC() != null)
                .mapToDouble(SensorDataDto::getTemperatureC)
                .average().orElse(0.0);

        double avgHumidity = sensorData.stream()
                .filter(d -> d.getHumidityPct() != null)
                .mapToDouble(SensorDataDto::getHumidityPct)
                .average().orElse(0.0);

        double avgCo2 = sensorData.stream()
                .filter(d -> d.getCo2Ppm() != null)
                .mapToDouble(SensorDataDto::getCo2Ppm)
                .average().orElse(0.0);

        double avgLight = sensorData.stream()
                .filter(d -> d.getLightLux() != null)
                .mapToDouble(SensorDataDto::getLightLux)
                .average().orElse(0.0);

        // Analyze against optimal ranges for mushroom cultivation
        analysis.put("temperature", analyzeTemperature(avgTemp, room.getMushroomType(), room.getStage()));
        analysis.put("humidity", analyzeHumidity(avgHumidity, room.getMushroomType(), room.getStage()));
        analysis.put("co2", analyzeCo2(avgCo2, room.getStage()));
        analysis.put("light", analyzeLight(avgLight, room.getStage()));
        analysis.put("data_quality", analyzeDataQuality(sensorData));

        return analysis;
    }

    /**
     * Analyze temperature data.
     */
    private Map<String, Object> analyzeTemperature(double avgTemp, String mushroomType, String stage) {
        Map<String, Object> tempAnalysis = new HashMap<>();
        
        // Optimal temperature ranges (simplified)
        double optimalMin = "incubation".equals(stage) ? 24.0 : 18.0;
        double optimalMax = "incubation".equals(stage) ? 28.0 : 22.0;
        
        tempAnalysis.put("current", round(avgTemp, 1));
        tempAnalysis.put("optimal_min", optimalMin);
        tempAnalysis.put("optimal_max", optimalMax);
        
        if (avgTemp < optimalMin) {
            tempAnalysis.put("status", "too_low");
            tempAnalysis.put("recommendation", "Increase heating");
        } else if (avgTemp > optimalMax) {
            tempAnalysis.put("status", "too_high");
            tempAnalysis.put("recommendation", "Increase ventilation or cooling");
        } else {
            tempAnalysis.put("status", "optimal");
            tempAnalysis.put("recommendation", "Maintain current temperature");
        }
        
        return tempAnalysis;
    }

    /**
     * Analyze humidity data.
     */
    private Map<String, Object> analyzeHumidity(double avgHumidity, String mushroomType, String stage) {
        Map<String, Object> humidityAnalysis = new HashMap<>();
        
        // Optimal humidity ranges (simplified)
        double optimalMin = "fruiting".equals(stage) ? 85.0 : 80.0;
        double optimalMax = "fruiting".equals(stage) ? 95.0 : 90.0;
        
        humidityAnalysis.put("current", round(avgHumidity, 1));
        humidityAnalysis.put("optimal_min", optimalMin);
        humidityAnalysis.put("optimal_max", optimalMax);
        
        if (avgHumidity < optimalMin) {
            humidityAnalysis.put("status", "too_low");
            humidityAnalysis.put("recommendation", "Increase humidification");
        } else if (avgHumidity > optimalMax) {
            humidityAnalysis.put("status", "too_high");
            humidityAnalysis.put("recommendation", "Increase ventilation");
        } else {
            humidityAnalysis.put("status", "optimal");
            humidityAnalysis.put("recommendation", "Maintain current humidity");
        }
        
        return humidityAnalysis;
    }

    /**
     * Analyze CO2 data.
     */
    private Map<String, Object> analyzeCo2(double avgCo2, String stage) {
        Map<String, Object> co2Analysis = new HashMap<>();
        
        // Optimal CO2 ranges (simplified)
        double optimalMin = "fruiting".equals(stage) ? 400.0 : 1000.0;
        double optimalMax = "fruiting".equals(stage) ? 800.0 : 2000.0;
        
        co2Analysis.put("current", round(avgCo2, 0));
        co2Analysis.put("optimal_min", optimalMin);
        co2Analysis.put("optimal_max", optimalMax);
        
        if (avgCo2 < optimalMin) {
            co2Analysis.put("status", "too_low");
            co2Analysis.put("recommendation", "Reduce ventilation or add CO2");
        } else if (avgCo2 > optimalMax) {
            co2Analysis.put("status", "too_high");
            co2Analysis.put("recommendation", "Increase ventilation");
        } else {
            co2Analysis.put("status", "optimal");
            co2Analysis.put("recommendation", "Maintain current CO2 levels");
        }
        
        return co2Analysis;
    }

    /**
     * Analyze light data.
     */
    private Map<String, Object> analyzeLight(double avgLight, String stage) {
        Map<String, Object> lightAnalysis = new HashMap<>();
        
        // Optimal light ranges (simplified)
        double optimalMin = "fruiting".equals(stage) ? 200.0 : 0.0;
        double optimalMax = "fruiting".equals(stage) ? 1000.0 : 100.0;
        
        lightAnalysis.put("current", round(avgLight, 0));
        lightAnalysis.put("optimal_min", optimalMin);
        lightAnalysis.put("optimal_max", optimalMax);
        
        if (avgLight < optimalMin) {
            lightAnalysis.put("status", "too_low");
            lightAnalysis.put("recommendation", "Increase lighting");
        } else if (avgLight > optimalMax) {
            lightAnalysis.put("status", "too_high");
            lightAnalysis.put("recommendation", "Reduce lighting");
        } else {
            lightAnalysis.put("status", "optimal");
            lightAnalysis.put("recommendation", "Maintain current lighting");
        }
        
        return lightAnalysis;
    }

    /**
     * Analyze data quality.
     */
    private Map<String, Object> analyzeDataQuality(List<SensorDataDto> sensorData) {
        Map<String, Object> qualityAnalysis = new HashMap<>();
        
        int totalReadings = sensorData.size();
        long tempReadings = sensorData.stream().filter(d -> d.getTemperatureC() != null).count();
        long humidityReadings = sensorData.stream().filter(d -> d.getHumidityPct() != null).count();
        long co2Readings = sensorData.stream().filter(d -> d.getCo2Ppm() != null).count();
        long lightReadings = sensorData.stream().filter(d -> d.getLightLux() != null).count();
        
        double completeness = (tempReadings + humidityReadings + co2Readings + lightReadings) / (4.0 * totalReadings);
        
        qualityAnalysis.put("total_readings", totalReadings);
        qualityAnalysis.put("completeness", round(completeness * 100, 1));
        qualityAnalysis.put("temperature_coverage", round((double) tempReadings / totalReadings * 100, 1));
        qualityAnalysis.put("humidity_coverage", round((double) humidityReadings / totalReadings * 100, 1));
        qualityAnalysis.put("co2_coverage", round((double) co2Readings / totalReadings * 100, 1));
        qualityAnalysis.put("light_coverage", round((double) lightReadings / totalReadings * 100, 1));
        
        return qualityAnalysis;
    }

    /**
     * Create recommendation payload from analysis results.
     */
    private Map<String, Object> createRecommendationPayload(Map<String, Object> analysis, Room room) {
        Map<String, Object> payload = new HashMap<>();
        
        payload.put("room_id", room.getRoomId().toString());
        payload.put("room_name", room.getName());
        payload.put("mushroom_type", room.getMushroomType());
        payload.put("stage", room.getStage());
        payload.put("analysis", analysis);
        payload.put("generated_at", LocalDateTime.now().toString());
        payload.put("analysis_type", "environmental_optimization");
        
        // Extract key recommendations
        List<String> recommendations = extractRecommendations(analysis);
        payload.put("recommendations", recommendations);
        
        // Determine priority level
        String priority = determinePriority(analysis);
        payload.put("priority", priority);
        
        return payload;
    }

    /**
     * Extract actionable recommendations from analysis.
     */
    @SuppressWarnings("unchecked")
    private List<String> extractRecommendations(Map<String, Object> analysis) {
        List<String> recommendations = new java.util.ArrayList<>();
        
        for (String parameter : List.of("temperature", "humidity", "co2", "light")) {
            Map<String, Object> paramAnalysis = (Map<String, Object>) analysis.get(parameter);
            if (paramAnalysis != null && !"optimal".equals(paramAnalysis.get("status"))) {
                String recommendation = (String) paramAnalysis.get("recommendation");
                if (recommendation != null) {
                    recommendations.add(recommendation);
                }
            }
        }
        
        return recommendations;
    }

    /**
     * Determine priority level based on analysis.
     */
    @SuppressWarnings("unchecked")
    private String determinePriority(Map<String, Object> analysis) {
        int criticalIssues = 0;
        int warnings = 0;
        
        for (String parameter : List.of("temperature", "humidity", "co2", "light")) {
            Map<String, Object> paramAnalysis = (Map<String, Object>) analysis.get(parameter);
            if (paramAnalysis != null) {
                String status = (String) paramAnalysis.get("status");
                if ("too_high".equals(status) || "too_low".equals(status)) {
                    if ("temperature".equals(parameter) || "humidity".equals(parameter)) {
                        criticalIssues++;
                    } else {
                        warnings++;
                    }
                }
            }
        }
        
        if (criticalIssues > 0) {
            return "high";
        } else if (warnings > 1) {
            return "medium";
        } else {
            return "low";
        }
    }

    /**
     * Calculate confidence based on data quality and analysis.
     */
    @SuppressWarnings("unchecked")
    private double calculateConfidence(List<SensorDataDto> sensorData, Map<String, Object> analysis) {
        if (sensorData.isEmpty()) {
            return 0.1;
        }
        
        // Base confidence on data quality
        Map<String, Object> dataQuality = (Map<String, Object>) analysis.get("data_quality");
        double completeness = (Double) dataQuality.get("completeness") / 100.0;
        
        // Adjust based on data volume
        double volumeScore = Math.min(1.0, sensorData.size() / 100.0);
        
        // Calculate final confidence
        double confidence = (completeness * 0.7) + (volumeScore * 0.3);
        
        return round(Math.max(0.1, Math.min(1.0, confidence)), 2);
    }

    /**
     * Create mock Bedrock recommendation (placeholder).
     */
    private Map<String, Object> createMockBedrockRecommendation(List<SensorDataDto> sensorData, 
                                                               Room room, String modelId) {
        Map<String, Object> payload = new HashMap<>();
        
        payload.put("model_id", modelId);
        payload.put("room_id", room.getRoomId().toString());
        payload.put("room_name", room.getName());
        payload.put("mushroom_type", room.getMushroomType());
        payload.put("stage", room.getStage());
        payload.put("generated_at", LocalDateTime.now().toString());
        payload.put("analysis_type", "ai_powered_optimization");
        
        // Mock AI recommendations
        List<String> recommendations = List.of(
            "Optimize ventilation schedule based on CO2 patterns",
            "Adjust humidity cycles for improved yield",
            "Fine-tune temperature control for current growth stage"
        );
        payload.put("recommendations", recommendations);
        payload.put("priority", "medium");
        
        // Mock confidence and reasoning
        payload.put("ai_confidence", 0.85);
        payload.put("reasoning", "Analysis based on environmental patterns and mushroom cultivation best practices");
        
        return payload;
    }

    /**
     * Check if user has access to a recommendation.
     */
    private boolean hasAccessToRecommendation(Recommendation recommendation, UUID userId) {
        if (recommendation.getFarm() != null) {
            return farmService.hasAccessToFarm(recommendation.getFarm().getFarmId(), userId);
        }
        
        if (recommendation.getRoom() != null) {
            return roomService.hasAccessToRoom(recommendation.getRoom().getRoomId(), userId);
        }
        
        // System-wide recommendations are accessible to all users
        return true;
    }

    /**
     * Validate recommendation creation data.
     */
    private void validateRecommendationCreate(RecommendationCreateDto createDto) {
        if (createDto.getPayload() == null || createDto.getPayload().isEmpty()) {
            throw new ValidationException("Recommendation payload cannot be empty");
        }

        if (createDto.getConfidence() == null || createDto.getConfidence() < 0.0 || createDto.getConfidence() > 1.0) {
            throw new ValidationException("Confidence must be between 0.0 and 1.0");
        }

        if (!StringUtils.hasText(createDto.getModelId())) {
            throw new ValidationException("Model ID cannot be empty");
        }
    }

    /**
     * Convert Recommendation entity to RecommendationDto.
     */
    private RecommendationDto convertToDto(Recommendation recommendation) {
        RecommendationDto dto = new RecommendationDto();
        dto.setRecId(recommendation.getRecId());
        dto.setFarmId(recommendation.getFarm() != null ? recommendation.getFarm().getFarmId() : null);
        dto.setFarmName(recommendation.getFarm() != null ? recommendation.getFarm().getName() : null);
        dto.setRoomId(recommendation.getRoom() != null ? recommendation.getRoom().getRoomId() : null);
        dto.setRoomName(recommendation.getRoom() != null ? recommendation.getRoom().getName() : null);
        dto.setPayload(recommendation.getPayload());
        dto.setConfidence(recommendation.getConfidence());
        dto.setModelId(recommendation.getModelId());
        dto.setCreatedAt(recommendation.getCreatedAt());
        return dto;
    }

    /**
     * Round double to specified decimal places.
     */
    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * Recommendation statistics data class.
     */
    public static class RecommendationStatistics {
        private final long totalRecommendations;
        private final double averageConfidence;
        private final long highConfidenceRecommendations;
        private final long uniqueModels;

        public RecommendationStatistics(long totalRecommendations, double averageConfidence,
                                      long highConfidenceRecommendations, long uniqueModels) {
            this.totalRecommendations = totalRecommendations;
            this.averageConfidence = averageConfidence;
            this.highConfidenceRecommendations = highConfidenceRecommendations;
            this.uniqueModels = uniqueModels;
        }

        public long getTotalRecommendations() { return totalRecommendations; }
        public double getAverageConfidence() { return averageConfidence; }
        public long getHighConfidenceRecommendations() { return highConfidenceRecommendations; }
        public long getUniqueModels() { return uniqueModels; }
    }
}