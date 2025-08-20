package com.smartfarm.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * UserRoom entity representing the many-to-many relationship between users and rooms
 * with role-based access control for room-specific permissions.
 */
@Entity
@Table(name = "user_rooms", indexes = {
        @Index(name = "idx_user_rooms_user", columnList = "user_id"),
        @Index(name = "idx_user_rooms_room", columnList = "room_id"),
        @Index(name = "idx_user_rooms_role", columnList = "role")
})
@IdClass(UserRoomId.class)
public class UserRoom {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private RoomRole role = RoomRole.VIEWER;

    @NotNull
    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "notes")
    private String notes;

    // Constructors
    public UserRoom() {
        this.assignedAt = LocalDateTime.now();
    }

    public UserRoom(User user, Room room, RoomRole role) {
        this();
        this.user = user;
        this.room = room;
        this.role = role;
    }

    public UserRoom(User user, Room room, RoomRole role, UUID assignedBy) {
        this(user, room, role);
        this.assignedBy = assignedBy;
    }

    // Getters and Setters
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public RoomRole getRole() {
        return role;
    }

    public void setRole(RoomRole role) {
        this.role = role;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public UUID getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(UUID assignedBy) {
        this.assignedBy = assignedBy;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Utility methods
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValidAccess() {
        return isActive != null && isActive && !isExpired();
    }

    public boolean canRead() {
        return isValidAccess() && (role == RoomRole.OWNER || role == RoomRole.OPERATOR || role == RoomRole.VIEWER);
    }

    public boolean canWrite() {
        return isValidAccess() && (role == RoomRole.OWNER || role == RoomRole.OPERATOR);
    }

    public boolean canManage() {
        return isValidAccess() && role == RoomRole.OWNER;
    }

    public boolean canControlDevices() {
        return isValidAccess() && (role == RoomRole.OWNER || role == RoomRole.OPERATOR);
    }

    public boolean canCreateAutomationRules() {
        return isValidAccess() && (role == RoomRole.OWNER || role == RoomRole.OPERATOR);
    }

    public boolean canAssignUsers() {
        return isValidAccess() && role == RoomRole.OWNER;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserRoom userRoom = (UserRoom) o;
        return Objects.equals(user != null ? user.getUserId() : null, 
                             userRoom.user != null ? userRoom.user.getUserId() : null) &&
               Objects.equals(room != null ? room.getRoomId() : null, 
                             userRoom.room != null ? userRoom.room.getRoomId() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            user != null ? user.getUserId() : null, 
            room != null ? room.getRoomId() : null
        );
    }

    @Override
    public String toString() {
        return "UserRoom{" +
                "user=" + (user != null ? user.getEmail() : "null") +
                ", room=" + (room != null ? room.getName() : "null") +
                ", role=" + role +
                ", assignedAt=" + assignedAt +
                ", isActive=" + isActive +
                ", expiresAt=" + expiresAt +
                '}';
    }

    /**
     * Enumeration for room-specific user roles.
     */
    public enum RoomRole {
        OWNER("Owner", "Full control over the room including user management"),
        OPERATOR("Operator", "Can control devices and create automation rules"),
        VIEWER("Viewer", "Read-only access to room data and telemetry");

        private final String displayName;
        private final String description;

        RoomRole(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public boolean hasHigherPrivilegesThan(RoomRole other) {
            return this.ordinal() < other.ordinal();
        }

        public boolean hasEqualOrHigherPrivilegesThan(RoomRole other) {
            return this.ordinal() <= other.ordinal();
        }
    }
}