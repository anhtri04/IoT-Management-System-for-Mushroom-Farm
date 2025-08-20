package com.smartfarm.repository;

import com.smartfarm.entity.Device;
import com.smartfarm.entity.Farm;
import com.smartfarm.entity.Room;
import com.smartfarm.entity.SensorData;
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
 * Repository interface for SensorData entity operations.
 * Provides data access methods for time-series sensor data queries and analytics.
 */
@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, UUID> {

    /**
     * Find sensor data by device.
     *
     * @param device the device
     * @param pageable pagination information
     * @return page of sensor data from the device
     */
    Page<SensorData> findByDevice(Device device, Pageable pageable);

    /**
     * Find sensor data by device ID.
     *
     * @param deviceId the device ID
     * @param pageable pagination information
     * @return page of sensor data from the device
     */
    Page<SensorData> findByDeviceDeviceId(UUID deviceId, Pageable pageable);

    /**
     * Find sensor data by room.
     *
     * @param room the room
     * @param pageable pagination information
     * @return page of sensor data from the room
     */
    Page<SensorData> findByRoom(Room room, Pageable pageable);

    /**
     * Find sensor data by room ID.
     *
     * @param roomId the room ID
     * @param pageable pagination information
     * @return page of sensor data from the room
     */
    Page<SensorData> findByRoomRoomId(UUID roomId, Pageable pageable);

    /**
     * Find sensor data by farm.
     *
     * @param farm the farm
     * @param pageable pagination information
     * @return page of sensor data from the farm
     */
    Page<SensorData> findByFarm(Farm farm, Pageable pageable);

    /**
     * Find sensor data by farm ID.
     *
     * @param farmId the farm ID
     * @param pageable pagination information
     * @return page of sensor data from the farm
     */
    Page<SensorData> findByFarmFarmId(UUID farmId, Pageable pageable);

    /**
     * Find sensor data by device and time range.
     *
     * @param deviceId the device ID
     * @param startTime the start time
     * @param endTime the end time
     * @param pageable pagination information
     * @return page of sensor data in the time range
     */
    Page<SensorData> findByDeviceDeviceIdAndRecordedAtBetween(UUID deviceId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Find sensor data by room and time range.
     *
     * @param roomId the room ID
     * @param startTime the start time
     * @param endTime the end time
     * @param pageable pagination information
     * @return page of sensor data in the time range
     */
    Page<SensorData> findByRoomRoomIdAndRecordedAtBetween(UUID roomId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Find sensor data by farm and time range.
     *
     * @param farmId the farm ID
     * @param startTime the start time
     * @param endTime the end time
     * @param pageable pagination information
     * @return page of sensor data in the time range
     */
    Page<SensorData> findByFarmFarmIdAndRecordedAtBetween(UUID farmId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Find latest sensor data by device.
     *
     * @param deviceId the device ID
     * @return optional latest sensor data from the device
     */
    @Query("SELECT sd FROM SensorData sd " +
           "WHERE sd.device.deviceId = :deviceId " +
           "ORDER BY sd.recordedAt DESC " +
           "LIMIT 1")
    Optional<SensorData> findLatestByDevice(@Param("deviceId") UUID deviceId);

    /**
     * Find latest sensor data by room.
     *
     * @param roomId the room ID
     * @return list of latest sensor data from each device in the room
     */
    @Query("SELECT sd FROM SensorData sd " +
           "WHERE sd.room.roomId = :roomId " +
           "AND sd.recordedAt = (" +
           "  SELECT MAX(sd2.recordedAt) FROM SensorData sd2 " +
           "  WHERE sd2.device = sd.device" +
           ")")
    List<SensorData> findLatestByRoom(@Param("roomId") UUID roomId);

    /**
     * Find latest sensor data by farm.
     *
     * @param farmId the farm ID
     * @return list of latest sensor data from each device in the farm
     */
    @Query("SELECT sd FROM SensorData sd " +
           "WHERE sd.farm.farmId = :farmId " +
           "AND sd.recordedAt = (" +
           "  SELECT MAX(sd2.recordedAt) FROM SensorData sd2 " +
           "  WHERE sd2.device = sd.device" +
           ")")
    List<SensorData> findLatestByFarm(@Param("farmId") UUID farmId);

    /**
     * Get hourly aggregated sensor data for a device.
     *
     * @param deviceId the device ID
     * @param startTime the start time
     * @param endTime the end time
     * @return list of hourly aggregated data
     */
    @Query("SELECT " +
           "DATE_TRUNC('hour', sd.recordedAt) as hour, " +
           "AVG(sd.temperatureC) as avgTemperature, " +
           "MIN(sd.temperatureC) as minTemperature, " +
           "MAX(sd.temperatureC) as maxTemperature, " +
           "AVG(sd.humidityPct) as avgHumidity, " +
           "MIN(sd.humidityPct) as minHumidity, " +
           "MAX(sd.humidityPct) as maxHumidity, " +
           "AVG(sd.co2Ppm) as avgCo2, " +
           "MIN(sd.co2Ppm) as minCo2, " +
           "MAX(sd.co2Ppm) as maxCo2, " +
           "AVG(sd.lightLux) as avgLight, " +
           "MIN(sd.lightLux) as minLight, " +
           "MAX(sd.lightLux) as maxLight, " +
           "COUNT(*) as dataPoints " +
           "FROM SensorData sd " +
           "WHERE sd.device.deviceId = :deviceId " +
           "AND sd.recordedAt BETWEEN :startTime AND :endTime " +
           "GROUP BY DATE_TRUNC('hour', sd.recordedAt) " +
           "ORDER BY hour")
    List<HourlyAggregatedData> getHourlyAggregatedData(@Param("deviceId") UUID deviceId,
                                                       @Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime);

    /**
     * Get daily aggregated sensor data for a room.
     *
     * @param roomId the room ID
     * @param startTime the start time
     * @param endTime the end time
     * @return list of daily aggregated data
     */
    @Query("SELECT " +
           "DATE_TRUNC('day', sd.recordedAt) as day, " +
           "AVG(sd.temperatureC) as avgTemperature, " +
           "MIN(sd.temperatureC) as minTemperature, " +
           "MAX(sd.temperatureC) as maxTemperature, " +
           "AVG(sd.humidityPct) as avgHumidity, " +
           "MIN(sd.humidityPct) as minHumidity, " +
           "MAX(sd.humidityPct) as maxHumidity, " +
           "AVG(sd.co2Ppm) as avgCo2, " +
           "MIN(sd.co2Ppm) as minCo2, " +
           "MAX(sd.co2Ppm) as maxCo2, " +
           "AVG(sd.lightLux) as avgLight, " +
           "MIN(sd.lightLux) as minLight, " +
           "MAX(sd.lightLux) as maxLight, " +
           "COUNT(*) as dataPoints " +
           "FROM SensorData sd " +
           "WHERE sd.room.roomId = :roomId " +
           "AND sd.recordedAt BETWEEN :startTime AND :endTime " +
           "GROUP BY DATE_TRUNC('day', sd.recordedAt) " +
           "ORDER BY day")
    List<DailyAggregatedData> getDailyAggregatedData(@Param("roomId") UUID roomId,
                                                     @Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);

    /**
     * Get sensor data statistics for a device in a time range.
     *
     * @param deviceId the device ID
     * @param startTime the start time
     * @param endTime the end time
     * @return sensor data statistics
     */
    @Query("SELECT " +
           "COUNT(*) as totalReadings, " +
           "AVG(sd.temperatureC) as avgTemperature, " +
           "MIN(sd.temperatureC) as minTemperature, " +
           "MAX(sd.temperatureC) as maxTemperature, " +
           "AVG(sd.humidityPct) as avgHumidity, " +
           "MIN(sd.humidityPct) as minHumidity, " +
           "MAX(sd.humidityPct) as maxHumidity, " +
           "AVG(sd.co2Ppm) as avgCo2, " +
           "MIN(sd.co2Ppm) as minCo2, " +
           "MAX(sd.co2Ppm) as maxCo2, " +
           "AVG(sd.lightLux) as avgLight, " +
           "MIN(sd.lightLux) as minLight, " +
           "MAX(sd.lightLux) as maxLight " +
           "FROM SensorData sd " +
           "WHERE sd.device.deviceId = :deviceId " +
           "AND sd.recordedAt BETWEEN :startTime AND :endTime")
    Optional<SensorDataStatistics> getDeviceStatistics(@Param("deviceId") UUID deviceId,
                                                       @Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime);

    /**
     * Get sensor data statistics for a room in a time range.
     *
     * @param roomId the room ID
     * @param startTime the start time
     * @param endTime the end time
     * @return sensor data statistics
     */
    @Query("SELECT " +
           "COUNT(*) as totalReadings, " +
           "AVG(sd.temperatureC) as avgTemperature, " +
           "MIN(sd.temperatureC) as minTemperature, " +
           "MAX(sd.temperatureC) as maxTemperature, " +
           "AVG(sd.humidityPct) as avgHumidity, " +
           "MIN(sd.humidityPct) as minHumidity, " +
           "MAX(sd.humidityPct) as maxHumidity, " +
           "AVG(sd.co2Ppm) as avgCo2, " +
           "MIN(sd.co2Ppm) as minCo2, " +
           "MAX(sd.co2Ppm) as maxCo2, " +
           "AVG(sd.lightLux) as avgLight, " +
           "MIN(sd.lightLux) as minLight, " +
           "MAX(sd.lightLux) as maxLight " +
           "FROM SensorData sd " +
           "WHERE sd.room.roomId = :roomId " +
           "AND sd.recordedAt BETWEEN :startTime AND :endTime")
    Optional<SensorDataStatistics> getRoomStatistics(@Param("roomId") UUID roomId,
                                                     @Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);

    /**
     * Find sensor data with temperature above threshold.
     *
     * @param roomId the room ID
     * @param threshold the temperature threshold
     * @param startTime the start time
     * @param endTime the end time
     * @return list of sensor data with high temperature
     */
    @Query("SELECT sd FROM SensorData sd " +
           "WHERE sd.room.roomId = :roomId " +
           "AND sd.temperatureC > :threshold " +
           "AND sd.recordedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY sd.recordedAt DESC")
    List<SensorData> findHighTemperatureReadings(@Param("roomId") UUID roomId,
                                                 @Param("threshold") Double threshold,
                                                 @Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);

    /**
     * Find sensor data with humidity above threshold.
     *
     * @param roomId the room ID
     * @param threshold the humidity threshold
     * @param startTime the start time
     * @param endTime the end time
     * @return list of sensor data with high humidity
     */
    @Query("SELECT sd FROM SensorData sd " +
           "WHERE sd.room.roomId = :roomId " +
           "AND sd.humidityPct > :threshold " +
           "AND sd.recordedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY sd.recordedAt DESC")
    List<SensorData> findHighHumidityReadings(@Param("roomId") UUID roomId,
                                              @Param("threshold") Double threshold,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);

    /**
     * Find sensor data with CO2 above threshold.
     *
     * @param roomId the room ID
     * @param threshold the CO2 threshold
     * @param startTime the start time
     * @param endTime the end time
     * @return list of sensor data with high CO2
     */
    @Query("SELECT sd FROM SensorData sd " +
           "WHERE sd.room.roomId = :roomId " +
           "AND sd.co2Ppm > :threshold " +
           "AND sd.recordedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY sd.recordedAt DESC")
    List<SensorData> findHighCo2Readings(@Param("roomId") UUID roomId,
                                         @Param("threshold") Double threshold,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * Find sensor data accessible by a user (through room access).
     *
     * @param userId the user ID
     * @param startTime the start time
     * @param endTime the end time
     * @param pageable pagination information
     * @return page of sensor data the user has access to
     */
    @Query("SELECT sd FROM SensorData sd " +
           "WHERE (sd.room.farm.owner.userId = :userId " +
           "       OR sd.room.roomId IN (" +
           "         SELECT ur.room.roomId FROM UserRoom ur " +
           "         WHERE ur.user.userId = :userId AND ur.isActive = true" +
           "       )) " +
           "AND sd.recordedAt BETWEEN :startTime AND :endTime")
    Page<SensorData> findSensorDataAccessibleByUser(@Param("userId") UUID userId,
                                                    @Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime,
                                                    Pageable pageable);

    /**
     * Count sensor data by device.
     *
     * @param device the device
     * @return count of sensor data from the device
     */
    long countByDevice(Device device);

    /**
     * Count sensor data by device ID.
     *
     * @param deviceId the device ID
     * @return count of sensor data from the device
     */
    long countByDeviceDeviceId(UUID deviceId);

    /**
     * Count sensor data by room.
     *
     * @param room the room
     * @return count of sensor data from the room
     */
    long countByRoom(Room room);

    /**
     * Count sensor data by room ID.
     *
     * @param roomId the room ID
     * @return count of sensor data from the room
     */
    long countByRoomRoomId(UUID roomId);

    /**
     * Count sensor data in time range.
     *
     * @param startTime the start time
     * @param endTime the end time
     * @return count of sensor data in the time range
     */
    long countByRecordedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Delete old sensor data before a specific date.
     *
     * @param cutoffDate the cutoff date
     * @return number of deleted records
     */
    long deleteByRecordedAtBefore(LocalDateTime cutoffDate);

    /**
     * Find sensor data recorded after a specific time.
     *
     * @param time the time threshold
     * @return list of sensor data recorded after the time
     */
    List<SensorData> findByRecordedAtAfter(LocalDateTime time);

    /**
     * Find sensor data recorded before a specific time.
     *
     * @param time the time threshold
     * @return list of sensor data recorded before the time
     */
    List<SensorData> findByRecordedAtBefore(LocalDateTime time);

    /**
     * Interface for hourly aggregated data projection.
     */
    interface HourlyAggregatedData {
        LocalDateTime getHour();
        Double getAvgTemperature();
        Double getMinTemperature();
        Double getMaxTemperature();
        Double getAvgHumidity();
        Double getMinHumidity();
        Double getMaxHumidity();
        Double getAvgCo2();
        Double getMinCo2();
        Double getMaxCo2();
        Double getAvgLight();
        Double getMinLight();
        Double getMaxLight();
        Long getDataPoints();
    }

    /**
     * Interface for daily aggregated data projection.
     */
    interface DailyAggregatedData {
        LocalDateTime getDay();
        Double getAvgTemperature();
        Double getMinTemperature();
        Double getMaxTemperature();
        Double getAvgHumidity();
        Double getMinHumidity();
        Double getMaxHumidity();
        Double getAvgCo2();
        Double getMinCo2();
        Double getMaxCo2();
        Double getAvgLight();
        Double getMinLight();
        Double getMaxLight();
        Long getDataPoints();
    }

    /**
     * Interface for sensor data statistics projection.
     */
    interface SensorDataStatistics {
        Long getTotalReadings();
        Double getAvgTemperature();
        Double getMinTemperature();
        Double getMaxTemperature();
        Double getAvgHumidity();
        Double getMinHumidity();
        Double getMaxHumidity();
        Double getAvgCo2();
        Double getMinCo2();
        Double getMaxCo2();
        Double getAvgLight();
        Double getMinLight();
        Double getMaxLight();
    }
}