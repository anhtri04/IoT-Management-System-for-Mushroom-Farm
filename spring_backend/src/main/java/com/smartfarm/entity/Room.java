package com.smartfarm.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Room entity representing physical grow areas (blocks/houses) within a farm.
 * Each room can contain multiple devices and has specific mushroom cultivation settings.
 */
@Entity
@Table(name = "rooms", indexes = {
        @Index(name = "idx_rooms_farm", columnList = "farm_id"),
        @Index(name = "idx_rooms_stage", columnList = "stage"),
        @Index(name = "idx_rooms_mushroom_type", columnList = "mushroom_type")
})
public class Room extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "room_id")
    private UUID roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @NotBlank
    @Size(max = 200)
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Size(max = 100)
    @Column(name = "mushroom_type")
    private String mushroomType;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage")
    private CultivationStage stage;

    // Relationships
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Device> devices = new HashSet<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<UserRoom> userRooms = new HashSet<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<SensorData> sensorData = new HashSet<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Command> commands = new HashSet<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<AutomationRule> automationRules = new HashSet<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<FarmingCycle> farmingCycles = new HashSet<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Notification> notifications = new HashSet<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Recommendation> recommendations = new HashSet<>();

    // Constructors
    public Room() {
        super();
    }

    public Room(String name, String description, String mushroomType, CultivationStage stage, Farm farm) {
        this();
        this.name = name;
        this.description = description;
        this.mushroomType = mushroomType;
        this.stage = stage;
        this.farm = farm;
    }

    // Getters and Setters
    public UUID getRoomId() {
        return roomId;
    }

    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
    }

    public Farm getFarm() {
        return farm;
    }

    public void setFarm(Farm farm) {
        this.farm = farm;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMushroomType() {
        return mushroomType;
    }

    public void setMushroomType(String mushroomType) {
        this.mushroomType = mushroomType;
    }

    public CultivationStage getStage() {
        return stage;
    }

    public void setStage(CultivationStage stage) {
        this.stage = stage;
    }

    public Set<Device> getDevices() {
        return devices;
    }

    public void setDevices(Set<Device> devices) {
        this.devices = devices;
    }

    public Set<UserRoom> getUserRooms() {
        return userRooms;
    }

    public void setUserRooms(Set<UserRoom> userRooms) {
        this.userRooms = userRooms;
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

    public Set<FarmingCycle> getFarmingCycles() {
        return farmingCycles;
    }

    public void setFarmingCycles(Set<FarmingCycle> farmingCycles) {
        this.farmingCycles = farmingCycles;
    }

    public Set<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(Set<Notification> notifications) {
        this.notifications = notifications;
    }

    public Set<Recommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(Set<Recommendation> recommendations) {
        this.recommendations = recommendations;
    }

    // Utility methods
    public void addDevice(Device device) {
        devices.add(device);
        device.setRoom(this);
    }

    public void removeDevice(Device device) {
        devices.remove(device);
        device.setRoom(null);
    }

    public void addAutomationRule(AutomationRule rule) {
        automationRules.add(rule);
        rule.setRoom(this);
    }

    public void removeAutomationRule(AutomationRule rule) {
        automationRules.remove(rule);
        rule.setRoom(null);
    }

    public long getOnlineDeviceCount() {
        return devices.stream()
                .filter(device -> device.getStatus() == Device.DeviceStatus.ONLINE)
                .count();
    }

    public long getActiveAutomationRuleCount() {
        return automationRules.stream()
                .filter(AutomationRule::isEnabled)
                .count();
    }

    public boolean hasUserAccess(UUID userId) {
        return userRooms.stream()
                .anyMatch(ur -> ur.getUser().getUserId().equals(userId));
    }

    @Override
    public String toString() {
        return "Room{" +
                "roomId=" + roomId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", mushroomType='" + mushroomType + '\'' +
                ", stage=" + stage +
                ", farm=" + (farm != null ? farm.getName() : "null") +
                ", deviceCount=" + devices.size() +
                ", createdAt=" + getCreatedAt() +
                '}';
    }

    /**
     * Enumeration for mushroom cultivation stages.
     */
    public enum CultivationStage {
        PREPARATION("Preparation"),
        INOCULATION("Inoculation"),
        INCUBATION("Incubation"),
        FRUITING("Fruiting"),
        HARVESTING("Harvesting"),
        MAINTENANCE("Maintenance"),
        CLEANING("Cleaning");

        private final String displayName;

        CultivationStage(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}