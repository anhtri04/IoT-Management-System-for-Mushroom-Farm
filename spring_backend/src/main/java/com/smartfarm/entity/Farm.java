package com.smartfarm.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Farm entity representing the top-level grouping in the IoT Smart Farm system.
 * Each farm can contain multiple rooms and is owned by a user.
 */
@Entity
@Table(name = "farms", indexes = {
        @Index(name = "idx_farms_owner", columnList = "owner_id"),
        @Index(name = "idx_farms_name", columnList = "name")
})
public class Farm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "farm_id")
    private UUID farmId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @NotBlank
    @Size(max = 200)
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "location")
    private String location;

    @Column(name = "description")
    private String description;

    // Relationships
    @OneToMany(mappedBy = "farm", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Room> rooms = new HashSet<>();

    @OneToMany(mappedBy = "farm", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<SensorData> sensorData = new HashSet<>();

    @OneToMany(mappedBy = "farm", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Command> commands = new HashSet<>();

    @OneToMany(mappedBy = "farm", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Notification> notifications = new HashSet<>();

    @OneToMany(mappedBy = "farm", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Recommendation> recommendations = new HashSet<>();

    // Constructors
    public Farm() {
        super();
    }

    public Farm(String name, String location, String description, User owner) {
        this();
        this.name = name;
        this.location = location;
        this.description = description;
        this.owner = owner;
    }

    // Getters and Setters
    public UUID getFarmId() {
        return farmId;
    }

    public void setFarmId(UUID farmId) {
        this.farmId = farmId;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Room> getRooms() {
        return rooms;
    }

    public void setRooms(Set<Room> rooms) {
        this.rooms = rooms;
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
    public void addRoom(Room room) {
        rooms.add(room);
        room.setFarm(this);
    }

    public void removeRoom(Room room) {
        rooms.remove(room);
        room.setFarm(null);
    }

    public long getTotalDevices() {
        return rooms.stream()
                .mapToLong(room -> room.getDevices().size())
                .sum();
    }

    public long getOnlineDevices() {
        return rooms.stream()
                .flatMap(room -> room.getDevices().stream())
                .filter(device -> device.getStatus() == Device.DeviceStatus.ONLINE)
                .count();
    }

    @Override
    public String toString() {
        return "Farm{" +
                "farmId=" + farmId +
                ", name='" + name + '\'' +
                ", location='" + location + '\'' +
                ", description='" + description + '\'' +
                ", owner=" + (owner != null ? owner.getEmail() : "null") +
                ", roomCount=" + rooms.size() +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}