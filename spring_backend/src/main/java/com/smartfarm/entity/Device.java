package com.smartfarm.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Device entity representing physical IoT controllers and sensors in rooms.
 * Each device belongs to a room and can collect sensor data or execute commands.
 */
@Entity
@Table(name = "devices", indexes = {
        @Index(name = "idx_devices_room", columnList = "room_id"),
        @Index(name = "idx_devices_type", columnList = "device_type"),
        @Index(name = "idx_devices_category", columnList = "category"),
        @Index(name = "idx_devices_status", columnList = "status"),
        @Index(name = "idx_devices_mqtt_topic", columnList = "mqtt_topic", unique = true)
})
public class Device extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "device_id")
    private UUID deviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @NotBlank
    @Size(max = 200)
    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type")
    private DeviceType deviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private DeviceCategory category;

    @NotBlank
    @Size(max = 512)
    @Column(name = "mqtt_topic", nullable = false, unique = true)
    private String mqttTopic;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DeviceStatus status = DeviceStatus.OFFLINE;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Size(max = 50)
    @Column(name = "firmware_version")
    private String firmwareVersion;

    @Column(name = "description")
    private String description;

    @Column(name = "manufacturer")
    private String manufacturer;

    @Column(name = "model")
    private String model;

    @Column(name = "serial_number")
    private String serialNumber;

    // Configuration JSON for device-specific settings
    @Column(name = "configuration", columnDefinition = "jsonb")
    private String configuration;

    // Relationships
    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<SensorData> sensorData = new HashSet<>();

    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Command> commands = new HashSet<>();

    @OneToMany(mappedBy = "actionDevice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<AutomationRule> automationRules = new HashSet<>();

    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Notification> notifications = new HashSet<>();

    // Constructors
    public Device() {
        super();
    }

    public Device(String name, DeviceType deviceType, DeviceCategory category, String mqttTopic, Room room) {
        this();
        this.name = name;
        this.deviceType = deviceType;
        this.category = category;
        this.mqttTopic = mqttTopic;
        this.room = room;
    }

    // Getters and Setters
    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public DeviceCategory getCategory() {
        return category;
    }

    public void setCategory(DeviceCategory category) {
        this.category = category;
    }

    public String getMqttTopic() {
        return mqttTopic;
    }

    public void setMqttTopic(String mqttTopic) {
        this.mqttTopic = mqttTopic;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceStatus status) {
        this.status = status;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public Set<SensorData> getSensorData() {
        return sensorData;
    }

    public void setSensorData(Set<SensorData> sensorData) {
        this.sensorData = sensorData;
    }

    public Set<Command> getCommands() {
        return commands;
    }

    public void setCommands(Set<Command> commands) {
        this.commands = commands;
    }

    public Set<AutomationRule> getAutomationRules() {
        return automationRules;
    }

    public void setAutomationRules(Set<AutomationRule> automationRules) {
        this.automationRules = automationRules;
    }

    public Set<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(Set<Notification> notifications) {
        this.notifications = notifications;
    }

    // Utility methods
    public boolean isOnline() {
        return status == DeviceStatus.ONLINE;
    }

    public boolean isOffline() {
        return status == DeviceStatus.OFFLINE;
    }

    public void updateLastSeen() {
        this.lastSeen = LocalDateTime.now();
        if (this.status == DeviceStatus.OFFLINE) {
            this.status = DeviceStatus.ONLINE;
        }
    }

    public String getTopicPrefix() {
        if (room != null && room.getFarm() != null) {
            return String.format("farm/%s/room/%s/device/%s", 
                    room.getFarm().getFarmId(), 
                    room.getRoomId(), 
                    deviceId);
        }
        return null;
    }

    public String getTelemetryTopic() {
        String prefix = getTopicPrefix();
        return prefix != null ? prefix + "/telemetry" : null;
    }

    public String getCommandTopic() {
        String prefix = getTopicPrefix();
        return prefix != null ? prefix + "/command" : null;
    }

    public String getStatusTopic() {
        String prefix = getTopicPrefix();
        return prefix != null ? prefix + "/status" : null;
    }

    @Override
    public String toString() {
        return "Device{" +
                "deviceId=" + deviceId +
                ", name='" + name + '\'' +
                ", deviceType=" + deviceType +
                ", category=" + category +
                ", status=" + status +
                ", lastSeen=" + lastSeen +
                ", firmwareVersion='" + firmwareVersion + '\'' +
                ", room=" + (room != null ? room.getName() : "null") +
                ", createdAt=" + getCreatedAt() +
                '}';
    }

    /**
     * Enumeration for device types.
     */
    public enum DeviceType {
        SENSOR("Sensor"),
        ACTUATOR("Actuator"),
        HYBRID("Hybrid"),
        CONTROLLER("Controller"),
        GATEWAY("Gateway");

        private final String displayName;

        DeviceType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Enumeration for device categories.
     */
    public enum DeviceCategory {
        TEMPERATURE("Temperature"),
        HUMIDITY("Humidity"),
        CO2("CO2"),
        LIGHT("Light"),
        MOISTURE("Moisture"),
        PH("pH"),
        FAN("Fan"),
        HUMIDIFIER("Humidifier"),
        HEATER("Heater"),
        COOLER("Cooler"),
        PUMP("Pump"),
        VALVE("Valve"),
        CAMERA("Camera"),
        MULTI_SENSOR("Multi-Sensor"),
        OTHER("Other");

        private final String displayName;

        DeviceCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Enumeration for device status.
     */
    public enum DeviceStatus {
        ONLINE("Online"),
        OFFLINE("Offline"),
        ERROR("Error"),
        MAINTENANCE("Maintenance"),
        UNKNOWN("Unknown");

        private final String displayName;

        DeviceStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}