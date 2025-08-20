package com.smartfarm.service;

import com.smartfarm.entity.SensorData;
import com.smartfarm.entity.Device;
import com.smartfarm.entity.Room;
import com.smartfarm.repository.SensorDataRepository;
import com.smartfarm.repository.DeviceRepository;
import com.smartfarm.repository.RoomRepository;
import com.smartfarm.exception.ResourceNotFoundException;
import com.smartfarm.exception.UnauthorizedException;
import com.smartfarm.dto.SensorDataDto;
import com.smartfarm.dto.SensorDataCreateDto;
import com.smartfarm.dto.TelemetryAggregationDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for SensorData entity operations.
 * Handles business logic for telemetry data ingestion, retrieval, and aggregation.
 */
@Service
@Transactional
public class SensorDataService {

    private static final Logger logger = LoggerFactory.getLogger(SensorDataService.class);

    private final SensorDataRepository sensorDataRepository;
    private final DeviceRepository deviceRepository;
    private final RoomRepository roomRepository;
    private final RoomService roomService;

    @Autowired
    public SensorDataService(SensorDataRepository sensorDataRepository, DeviceRepository deviceRepository,
                            RoomRepository roomRepository, RoomService roomService) {
        this.sensorDataRepository = sensorDataRepository;
        this.deviceRepository = deviceRepository;
        this.roomRepository = roomRepository;
        this.roomService = roomService;
    }

    /**
     * Ingest sensor data from IoT devices.
     * This method is typically called by MQTT message handlers or internal ingestion endpoints.
     *
     * @param createDto the sensor data to ingest
     * @param deviceId the device ID
     * @return the ingested sensor data
     * @throws ResourceNotFoundException if device not found
     */
    public SensorDataDto ingestSensorData(SensorDataCreateDto createDto, UUID deviceId) {
        logger.debug("Ingesting sensor data from device: {}", deviceId);

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));

        SensorData sensorData = new SensorData();
        sensorData.setDevice(device);
        sensorData.setRoom(device.getRoom());
        sensorData.setFarm(device.getRoom().getFarm());
        sensorData.setTemperatureC(createDto.getTemperatureC());
        sensorData.setHumidityPct(createDto.getHumidityPct());
        sensorData.setCo2Ppm(createDto.getCo2Ppm());
        sensorData.setLightLux(createDto.getLightLux());
        sensorData.setSubstrateMoisture(createDto.getSubstrateMoisture());
        sensorData.setBatteryV(createDto.getBatteryV());
        sensorData.setRecordedAt(createDto.getRecordedAt() != null ? createDto.getRecordedAt() : LocalDateTime.now());

        SensorData savedData = sensorDataRepository.save(sensorData);
        logger.debug("Sensor data ingested successfully with ID: {}", savedData.getReadingId());

        return convertToDto(savedData);
    }

    /**
     * Get latest sensor readings for a device.
     *
     * @param deviceId the device ID
     * @param userId the user ID (for access control)
     * @param limit the maximum number of readings to return
     * @return list of latest sensor readings
     * @throws ResourceNotFoundException if device not found
     * @throws UnauthorizedException if user doesn't have access
     */
    @Transactional(readOnly = true)
    public List<SensorDataDto> getLatestReadings(UUID deviceId, UUID userId, int limit) {
        logger.debug("Fetching latest {} readings for device: {} by user: {}", limit, deviceId, userId);

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));

        if (!roomService.hasAccessToRoom(device.getRoom().getRoomId(), userId)) {
            throw new UnauthorizedException("User does not have access to device: " + deviceId);
        }

        List<SensorData> readings = sensorDataRepository.findLatestByDevice(deviceId, limit);
        return readings.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     * Get sensor data for a room within a time range.
     *
     * @param roomId the room ID
     * @param startTime the start time
     * @param endTime the end time
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of sensor data
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public Page<SensorDataDto> getRoomSensorData(UUID roomId, LocalDateTime startTime, LocalDateTime endTime,
                                                UUID userId, Pageable pageable) {
        logger.debug("Fetching sensor data for room: {} from {} to {} by user: {}", roomId, startTime, endTime, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        return sensorDataRepository.findByRoomAndTimeRange(roomId, startTime, endTime, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get sensor data for a device within a time range.
     *
     * @param deviceId the device ID
     * @param startTime the start time
     * @param endTime the end time
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of sensor data
     * @throws ResourceNotFoundException if device not found
     * @throws UnauthorizedException if user doesn't have access
     */
    @Transactional(readOnly = true)
    public Page<SensorDataDto> getDeviceSensorData(UUID deviceId, LocalDateTime startTime, LocalDateTime endTime,
                                                  UUID userId, Pageable pageable) {
        logger.debug("Fetching sensor data for device: {} from {} to {} by user: {}", deviceId, startTime, endTime, userId);

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));

        if (!roomService.hasAccessToRoom(device.getRoom().getRoomId(), userId)) {
            throw new UnauthorizedException("User does not have access to device: " + deviceId);
        }

        return sensorDataRepository.findByDeviceAndTimeRange(deviceId, startTime, endTime, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get aggregated sensor data for a room.
     *
     * @param roomId the room ID
     * @param startTime the start time
     * @param endTime the end time
     * @param intervalMinutes the aggregation interval in minutes
     * @param userId the user ID (for access control)
     * @return list of aggregated telemetry data
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public List<TelemetryAggregationDto> getAggregatedRoomData(UUID roomId, LocalDateTime startTime, LocalDateTime endTime,
                                                              int intervalMinutes, UUID userId) {
        logger.debug("Fetching aggregated data for room: {} from {} to {} with {}min intervals by user: {}",
                    roomId, startTime, endTime, intervalMinutes, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        List<SensorDataRepository.TelemetryAggregation> aggregations = 
            sensorDataRepository.getAggregatedDataByRoom(roomId, startTime, endTime, intervalMinutes);

        return aggregations.stream().map(agg -> new TelemetryAggregationDto(
            agg.getTimeBucket(),
            agg.getAvgTemperature(),
            agg.getAvgHumidity(),
            agg.getAvgCo2(),
            agg.getAvgLight(),
            agg.getAvgSubstrateMoisture(),
            agg.getMinTemperature(),
            agg.getMaxTemperature(),
            agg.getMinHumidity(),
            agg.getMaxHumidity(),
            agg.getReadingCount()
        )).collect(Collectors.toList());
    }

    /**
     * Get aggregated sensor data for a device.
     *
     * @param deviceId the device ID
     * @param startTime the start time
     * @param endTime the end time
     * @param intervalMinutes the aggregation interval in minutes
     * @param userId the user ID (for access control)
     * @return list of aggregated telemetry data
     * @throws ResourceNotFoundException if device not found
     * @throws UnauthorizedException if user doesn't have access
     */
    @Transactional(readOnly = true)
    public List<TelemetryAggregationDto> getAggregatedDeviceData(UUID deviceId, LocalDateTime startTime, LocalDateTime endTime,
                                                                int intervalMinutes, UUID userId) {
        logger.debug("Fetching aggregated data for device: {} from {} to {} with {}min intervals by user: {}",
                    deviceId, startTime, endTime, intervalMinutes, userId);

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));

        if (!roomService.hasAccessToRoom(device.getRoom().getRoomId(), userId)) {
            throw new UnauthorizedException("User does not have access to device: " + deviceId);
        }

        List<SensorDataRepository.TelemetryAggregation> aggregations = 
            sensorDataRepository.getAggregatedDataByDevice(deviceId, startTime, endTime, intervalMinutes);

        return aggregations.stream().map(agg -> new TelemetryAggregationDto(
            agg.getTimeBucket(),
            agg.getAvgTemperature(),
            agg.getAvgHumidity(),
            agg.getAvgCo2(),
            agg.getAvgLight(),
            agg.getAvgSubstrateMoisture(),
            agg.getMinTemperature(),
            agg.getMaxTemperature(),
            agg.getMinHumidity(),
            agg.getMaxHumidity(),
            agg.getReadingCount()
        )).collect(Collectors.toList());
    }

    /**
     * Get current environmental conditions for a room.
     *
     * @param roomId the room ID
     * @param userId the user ID (for access control)
     * @return current environmental conditions
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public RoomEnvironmentalConditions getCurrentConditions(UUID roomId, UUID userId) {
        logger.debug("Fetching current conditions for room: {} by user: {}", roomId, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        var conditions = sensorDataRepository.getCurrentEnvironmentalConditions(roomId)
                .orElse(new SensorDataRepository.EnvironmentalConditions() {
                    @Override public Double getAvgTemperature() { return null; }
                    @Override public Double getAvgHumidity() { return null; }
                    @Override public Double getAvgCo2() { return null; }
                    @Override public Double getAvgLight() { return null; }
                    @Override public Double getAvgSubstrateMoisture() { return null; }
                    @Override public Double getMinTemperature() { return null; }
                    @Override public Double getMaxTemperature() { return null; }
                    @Override public Double getMinHumidity() { return null; }
                    @Override public Double getMaxHumidity() { return null; }
                    @Override public LocalDateTime getLastReading() { return null; }
                });

        return new RoomEnvironmentalConditions(
            conditions.getAvgTemperature(),
            conditions.getAvgHumidity(),
            conditions.getAvgCo2(),
            conditions.getAvgLight(),
            conditions.getAvgSubstrateMoisture(),
            conditions.getMinTemperature(),
            conditions.getMaxTemperature(),
            conditions.getMinHumidity(),
            conditions.getMaxHumidity(),
            conditions.getLastReading()
        );
    }

    /**
     * Get sensor data statistics for a room.
     *
     * @param roomId the room ID
     * @param startTime the start time
     * @param endTime the end time
     * @param userId the user ID (for access control)
     * @return sensor data statistics
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public SensorDataStatistics getRoomStatistics(UUID roomId, LocalDateTime startTime, LocalDateTime endTime, UUID userId) {
        logger.debug("Fetching sensor statistics for room: {} from {} to {} by user: {}", roomId, startTime, endTime, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        var stats = sensorDataRepository.getSensorDataStatistics(roomId, startTime, endTime)
                .orElse(new SensorDataRepository.SensorDataStatistics() {
                    @Override public Long getTotalReadings() { return 0L; }
                    @Override public Long getActiveDevices() { return 0L; }
                    @Override public Double getDataCompleteness() { return 0.0; }
                    @Override public LocalDateTime getFirstReading() { return null; }
                    @Override public LocalDateTime getLastReading() { return null; }
                });

        return new SensorDataStatistics(
            stats.getTotalReadings(),
            stats.getActiveDevices(),
            stats.getDataCompleteness(),
            stats.getFirstReading(),
            stats.getLastReading()
        );
    }

    /**
     * Delete old sensor data.
     *
     * @param cutoffDate the cutoff date (data older than this will be deleted)
     * @param userId the user ID making the deletion (must be admin)
     * @return number of records deleted
     * @throws UnauthorizedException if user is not admin
     */
    public long deleteOldSensorData(LocalDateTime cutoffDate, UUID userId) {
        logger.info("Deleting sensor data older than {} by user: {}", cutoffDate, userId);

        // Note: In a real implementation, you would check if the user is an admin
        // This is a placeholder for the admin check
        // if (!userService.isAdmin(userId)) {
        //     throw new UnauthorizedException("Only administrators can delete old sensor data");
        // }

        long deletedCount = sensorDataRepository.deleteOldSensorData(cutoffDate);
        logger.info("Deleted {} old sensor data records", deletedCount);

        return deletedCount;
    }

    /**
     * Get devices with recent data activity.
     *
     * @param roomId the room ID
     * @param hours the number of hours to look back
     * @param userId the user ID (for access control)
     * @return list of device IDs with recent activity
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public List<UUID> getActiveDevices(UUID roomId, int hours, UUID userId) {
        logger.debug("Fetching devices with activity in last {} hours for room: {} by user: {}", hours, roomId, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
        return sensorDataRepository.findActiveDevicesInRoom(roomId, cutoffTime);
    }

    /**
     * Get sensor data export for a room.
     * This method is useful for generating reports or data analysis.
     *
     * @param roomId the room ID
     * @param startTime the start time
     * @param endTime the end time
     * @param userId the user ID (for access control)
     * @return list of sensor data for export
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public List<SensorDataDto> exportRoomData(UUID roomId, LocalDateTime startTime, LocalDateTime endTime, UUID userId) {
        logger.debug("Exporting sensor data for room: {} from {} to {} by user: {}", roomId, startTime, endTime, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        List<SensorData> data = sensorDataRepository.findByRoomAndTimeRangeForExport(roomId, startTime, endTime);
        return data.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     * Convert SensorData entity to SensorDataDto.
     *
     * @param sensorData the sensor data entity
     * @return the sensor data DTO
     */
    private SensorDataDto convertToDto(SensorData sensorData) {
        SensorDataDto dto = new SensorDataDto();
        dto.setReadingId(sensorData.getReadingId());
        dto.setDeviceId(sensorData.getDevice().getDeviceId());
        dto.setDeviceName(sensorData.getDevice().getName());
        dto.setRoomId(sensorData.getRoom().getRoomId());
        dto.setRoomName(sensorData.getRoom().getName());
        dto.setFarmId(sensorData.getFarm().getFarmId());
        dto.setFarmName(sensorData.getFarm().getName());
        dto.setTemperatureC(sensorData.getTemperatureC());
        dto.setHumidityPct(sensorData.getHumidityPct());
        dto.setCo2Ppm(sensorData.getCo2Ppm());
        dto.setLightLux(sensorData.getLightLux());
        dto.setSubstrateMoisture(sensorData.getSubstrateMoisture());
        dto.setBatteryV(sensorData.getBatteryV());
        dto.setRecordedAt(sensorData.getRecordedAt());
        return dto;
    }

    /**
     * Room environmental conditions data class.
     */
    public static class RoomEnvironmentalConditions {
        private final Double avgTemperature;
        private final Double avgHumidity;
        private final Double avgCo2;
        private final Double avgLight;
        private final Double avgSubstrateMoisture;
        private final Double minTemperature;
        private final Double maxTemperature;
        private final Double minHumidity;
        private final Double maxHumidity;
        private final LocalDateTime lastReading;

        public RoomEnvironmentalConditions(Double avgTemperature, Double avgHumidity, Double avgCo2,
                                          Double avgLight, Double avgSubstrateMoisture, Double minTemperature,
                                          Double maxTemperature, Double minHumidity, Double maxHumidity,
                                          LocalDateTime lastReading) {
            this.avgTemperature = avgTemperature;
            this.avgHumidity = avgHumidity;
            this.avgCo2 = avgCo2;
            this.avgLight = avgLight;
            this.avgSubstrateMoisture = avgSubstrateMoisture;
            this.minTemperature = minTemperature;
            this.maxTemperature = maxTemperature;
            this.minHumidity = minHumidity;
            this.maxHumidity = maxHumidity;
            this.lastReading = lastReading;
        }

        // Getters
        public Double getAvgTemperature() { return avgTemperature; }
        public Double getAvgHumidity() { return avgHumidity; }
        public Double getAvgCo2() { return avgCo2; }
        public Double getAvgLight() { return avgLight; }
        public Double getAvgSubstrateMoisture() { return avgSubstrateMoisture; }
        public Double getMinTemperature() { return minTemperature; }
        public Double getMaxTemperature() { return maxTemperature; }
        public Double getMinHumidity() { return minHumidity; }
        public Double getMaxHumidity() { return maxHumidity; }
        public LocalDateTime getLastReading() { return lastReading; }
    }

    /**
     * Sensor data statistics data class.
     */
    public static class SensorDataStatistics {
        private final long totalReadings;
        private final long activeDevices;
        private final double dataCompleteness;
        private final LocalDateTime firstReading;
        private final LocalDateTime lastReading;

        public SensorDataStatistics(long totalReadings, long activeDevices, double dataCompleteness,
                                   LocalDateTime firstReading, LocalDateTime lastReading) {
            this.totalReadings = totalReadings;
            this.activeDevices = activeDevices;
            this.dataCompleteness = dataCompleteness;
            this.firstReading = firstReading;
            this.lastReading = lastReading;
        }

        public long getTotalReadings() { return totalReadings; }
        public long getActiveDevices() { return activeDevices; }
        public double getDataCompleteness() { return dataCompleteness; }
        public LocalDateTime getFirstReading() { return firstReading; }
        public LocalDateTime getLastReading() { return lastReading; }
    }
}