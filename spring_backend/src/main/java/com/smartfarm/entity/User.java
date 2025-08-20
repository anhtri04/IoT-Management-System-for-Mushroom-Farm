package com.smartfarm.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * User entity representing system users with AWS Cognito integration.
 * Supports role-based access control and room assignments.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_cognito_sub", columnList = "cognito_sub"),
        @Index(name = "idx_users_email", columnList = "email"),
        @Index(name = "idx_users_role", columnList = "role")
})
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID userId;

    @NotBlank
    @Column(name = "cognito_sub", unique = true, nullable = false)
    private String cognitoSub;

    @Email
    @NotBlank
    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Size(max = 200)
    @Column(name = "full_name")
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role = UserRole.VIEWER;

    // Relationships
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Farm> ownedFarms = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<UserRoom> userRooms = new HashSet<>();

    @OneToMany(mappedBy = "issuedBy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Command> issuedCommands = new HashSet<>();

    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<AutomationRule> createdRules = new HashSet<>();

    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<FarmingCycle> createdCycles = new HashSet<>();

    // Constructors
    public User() {
        super();
    }

    public User(String cognitoSub, String email, String fullName, UserRole role) {
        this();
        this.cognitoSub = cognitoSub;
        this.email = email;
        this.fullName = fullName;
        this.role = role != null ? role : UserRole.VIEWER;
    }

    // Getters and Setters
    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getCognitoSub() {
        return cognitoSub;
    }

    public void setCognitoSub(String cognitoSub) {
        this.cognitoSub = cognitoSub;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Set<Farm> getOwnedFarms() {
        return ownedFarms;
    }

    public void setOwnedFarms(Set<Farm> ownedFarms) {
        this.ownedFarms = ownedFarms;
    }

    public Set<UserRoom> getUserRooms() {
        return userRooms;
    }

    public void setUserRooms(Set<UserRoom> userRooms) {
        this.userRooms = userRooms;
    }

    public Set<Command> getIssuedCommands() {
        return issuedCommands;
    }

    public void setIssuedCommands(Set<Command> issuedCommands) {
        this.issuedCommands = issuedCommands;
    }

    public Set<AutomationRule> getCreatedRules() {
        return createdRules;
    }

    public void setCreatedRules(Set<AutomationRule> createdRules) {
        this.createdRules = createdRules;
    }

    public Set<FarmingCycle> getCreatedCycles() {
        return createdCycles;
    }

    public void setCreatedCycles(Set<FarmingCycle> createdCycles) {
        this.createdCycles = createdCycles;
    }

    // Utility methods
    public boolean hasRole(UserRole requiredRole) {
        return this.role.hasPermission(requiredRole);
    }

    public boolean canAccessRoom(UUID roomId) {
        return userRooms.stream()
                .anyMatch(ur -> ur.getRoom().getRoomId().equals(roomId));
    }

    public boolean canManageRoom(UUID roomId) {
        return userRooms.stream()
                .anyMatch(ur -> ur.getRoom().getRoomId().equals(roomId) && 
                        (ur.getRole() == RoomRole.OWNER || ur.getRole() == RoomRole.OPERATOR));
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", cognitoSub='" + cognitoSub + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role=" + role +
                ", createdAt=" + getCreatedAt() +
                '}';
    }

    /**
     * User role enumeration with hierarchical permissions.
     */
    public enum UserRole {
        ADMIN(3),
        MANAGER(2),
        VIEWER(1);

        private final int level;

        UserRole(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }

        public boolean hasPermission(UserRole requiredRole) {
            return this.level >= requiredRole.level;
        }
    }

    /**
     * Room-specific role enumeration.
     */
    public enum RoomRole {
        OWNER,
        OPERATOR,
        VIEWER
    }
}