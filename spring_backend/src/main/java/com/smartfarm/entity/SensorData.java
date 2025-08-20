package com.smartfarm.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SensorData entity representing time-series telemetry data from IoT devices.
 * Optimized for high-frequency data ingestion and time-based queries.
 */
@Entity
@Table(name = "sensor_data", indexes = {
        @Index(name = "idx_sensor_room_time", columnList = "room_id, recorded_at DESC"),
        @Index(name = "idx_sensor_device_time", columnList = "device_id, recorded_at DESC"),
        @Index(name = "idx_sensor_farm_time", columnList = "farm_id, recorded_at DESC"),
        @Index(name = "idx_sensor_recorded_at", columnList = "recorded_at DESC")
})
public class SensorData extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reading_id")
    private UUID readingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    // Environmental sensor readings
    @Column(name = "temperature_c")
    private Float temperatureC;

    @Column(name = "humidity_pct")
    private Float humidityPct;

    @Column(name = "co2_ppm")
    private Float co2Ppm;

    @Column(name = "light_lux")
    private Float lightLux;

    @Column(name = "substrate_moisture")
    private Float substrateMoisture;

    @Column(name = "ph_level")
    private Float phLevel;

    @Column(name = "pressure_hpa")
    private Float pressureHpa;

    @Column(name = "air_quality_index")
    private Float airQualityIndex;

    // Device status readings
    @Column(name = "battery_v")
    private Float batteryV;

    @Column(name = "signal_strength")
    private Float signalStrength;

    @Column(name = "device_temperature_c")
    private Float deviceTemperatureC;

    // Timestamp when the reading was recorded by the device
    @NotNull
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    // Raw JSON payload from device (for extensibility)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;

    // Data quality indicators
    @Column(name = "is_valid")
    private Boolean isValid = true;

    @Column(name = "validation_errors")
    private String validationErrors;

    // Constructors
    public SensorData() {
        super();
    }

    public SensorData(Device device, Room room, Farm farm, LocalDateTime recordedAt) {
        this();
        this.device = device;
        this.room = room;
        this.farm = farm;
        this.recordedAt = recordedAt;
    }

    // Getters and Setters
    public UUID getReadingId() {
        return readingId;
    }

    public void setReadingId(UUID readingId) {
        this.readingId = readingId;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Farm getFarm() {
        return farm;
    }

    public void setFarm(Farm farm) {
        this.farm = farm;
    }

    public Float getTemperatureC() {
        return temperatureC;
    }

    public void setTemperatureC(Float temperatureC) {
        this.temperatureC = temperatureC;
    }

    public Float getHumidityPct() {
        return humidityPct;
    }

    public void setHumidityPct(Float humidityPct) {
        this.humidityPct = humidityPct;
    }

    public Float getCo2Ppm() {
        return co2Ppm;
    }

    public void setCo2Ppm(Float co2Ppm) {
        this.co2Ppm = co2Ppm;
    }

    public Float getLightLux() {
        return lightLux;
    }

    public void setLightLux(Float lightLux) {
        this.lightLux = lightLux;
    }

    public Float getSubstrateMoisture() {
        return substrateMoisture;
    }

    public void setSubstrateMoisture(Float substrateMoisture) {
        this.substrateMoisture = substrateMoisture;
    }

    public Float getPhLevel() {
        return phLevel;
    }

    public void setPhLevel(Float phLevel) {
        this.phLevel = phLevel;
    }

    public Float getPressureHpa() {
        return pressureHpa;
    }

    public void setPressureHpa(Float pressureHpa) {
        this.pressureHpa = pressureHpa;
    }

    public Float getAirQualityIndex() {
        return airQualityIndex;
    }

    public void setAirQualityIndex(Float airQualityIndex) {
        this.airQualityIndex = airQualityIndex;
    }

    public Float getBatteryV() {
        return batteryV;
    }

    public void setBatteryV(Float batteryV) {
        this.batteryV = batteryV;
    }

    public Float getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(Float signalStrength) {
        this.signalStrength = signalStrength;
    }

    public Float getDeviceTemperatureC() {
        return deviceTemperatureC;
    }

    public void setDeviceTemperatureC(Float deviceTemperatureC) {
        this.deviceTemperatureC = deviceTemperatureC;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public Boolean getIsValid() {
        return isValid;
    }

    public void setIsValid(Boolean isValid) {
        this.isValid = isValid;
    }

    public String getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(String validationErrors) {
        this.validationErrors = validationErrors;
    }

    // Utility methods
    public boolean hasTemperature() {
        return temperatureC != null;
    }

    public boolean hasHumidity() {
        return humidityPct != null;
    }

    public boolean hasCo2() {
        return co2Ppm != null;
    }

    public boolean hasLight() {
        return lightLux != null;
    }

    public boolean hasMoisture() {
        return substrateMoisture != null;
    }

    public boolean hasPh() {
        return phLevel != null;
    }

    public boolean hasBattery() {
        return batteryV != null;
    }

    public boolean isLowBattery() {
        return batteryV != null && batteryV < 3.3f; // Typical low battery threshold for ESP32
    }

    public boolean isWeakSignal() {
        return signalStrength != null && signalStrength < -80f; // dBm threshold
    }

    public void markInvalid(String error) {
        this.isValid = false;
        this.validationErrors = error;
    }

    public void markValid() {
        this.isValid = true;
        this.validationErrors = null;
    }

    @Override
    public String toString() {
        return "SensorData{" +
                "readingId=" + readingId +
                ", device=" + (device != null ? device.getName() : "null") +
                ", room=" + (room != null ? room.getName() : "null") +
                ", temperatureC=" + temperatureC +
                ", humidityPct=" + humidityPct +
                ", co2Ppm=" + co2Ppm +
                ", lightLux=" + lightLux +
                ", substrateMoisture=" + substrateMoisture +
                ", batteryV=" + batteryV +
                ", recordedAt=" + recordedAt +
                ", isValid=" + isValid +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}