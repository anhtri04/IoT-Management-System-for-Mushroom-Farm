package com.smartfarm.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification entity representing alerts and notifications in the IoT system.
 * Handles system alerts, device notifications, and user messages.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_farm", columnList = "farm_id"),
        @Index(name = "idx_notification_room", columnList = "room_id"),
        @Index(name = "idx_notification_device", columnList = "device_id"),
        @Index(name = "idx_notification_level", columnList = "level"),
        @Index(name = "idx_notification_created", columnList = "created_at"),
        @Index(name = "idx_notification_acknowledged", columnList = "acknowledged_at"),
        @Index(name = "idx_notification_status", columnList = "status")
})
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id")
    private UUID notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id")
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private NotificationLevel level;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @NotBlank
    @Size(max = 200)
    @Column(name = "title", nullable = false)
    private String title;

    @NotBlank
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationStatus status = NotificationStatus.UNREAD;

    @Column(name = "source")
    private String source; // e.g., "device", "automation", "system", "user"

    @Column(name = "source_id")
    private String sourceId; // ID of the source entity

    @Column(name = "category")
    private String category; // e.g., "temperature", "humidity", "contamination"

    @Column(name = "tags")
    private String tags; // Comma-separated tags

    @Column(name = "data", columnDefinition = "jsonb")
    private String data; // Additional JSON data

    @Column(name = "action_required")
    private Boolean actionRequired = false;

    @Column(name = "action_url")
    private String actionUrl; // URL for action button

    @Column(name = "action_text")
    private String actionText; // Text for action button

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acknowledged_by")
    private User acknowledgedBy;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "acknowledgment_note")
    private String acknowledgmentNote;

    @Column(name = "auto_acknowledge")
    private Boolean autoAcknowledge = false;

    @Column(name = "auto_acknowledge_after_minutes")
    private Integer autoAcknowledgeAfterMinutes;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    @Column(name = "last_sent_at")
    private LocalDateTime lastSentAt;

    @Column(name = "delivery_channels")
    private String deliveryChannels; // e.g., "email,sms,push,websocket"

    @Column(name = "delivery_status", columnDefinition = "jsonb")
    private String deliveryStatus; // JSON object tracking delivery per channel

    @Column(name = "priority_score")
    private Integer priorityScore; // Calculated priority for sorting

    @Column(name = "related_notification_id")
    private UUID relatedNotificationId; // For grouping related notifications

    @Column(name = "escalation_level")
    private Integer escalationLevel = 0;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_note")
    private String resolutionNote;

    // Constructors
    public Notification() {
        super();
    }

    public Notification(NotificationLevel level, NotificationType type, String title, String message) {
        this();
        this.level = level;
        this.type = type;
        this.title = title;
        this.message = message;
        this.priorityScore = calculatePriorityScore();
    }

    public Notification(Farm farm, Room room, Device device, NotificationLevel level, 
                      NotificationType type, String title, String message) {
        this(level, type, title, message);
        this.farm = farm;
        this.room = room;
        this.device = device;
    }

    // Getters and Setters
    public UUID getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }

    public Farm getFarm() {
        return farm;
    }

    public void setFarm(Farm farm) {
        this.farm = farm;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public NotificationLevel getLevel() {
        return level;
    }

    public void setLevel(NotificationLevel level) {
        this.level = level;
        this.priorityScore = calculatePriorityScore();
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Boolean getActionRequired() {
        return actionRequired;
    }

    public void setActionRequired(Boolean actionRequired) {
        this.actionRequired = actionRequired;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    public String getActionText() {
        return actionText;
    }

    public void setActionText(String actionText) {
        this.actionText = actionText;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public User getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public void setAcknowledgedBy(User acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public String getAcknowledgmentNote() {
        return acknowledgmentNote;
    }

    public void setAcknowledgmentNote(String acknowledgmentNote) {
        this.acknowledgmentNote = acknowledgmentNote;
    }

    public Boolean getAutoAcknowledge() {
        return autoAcknowledge;
    }

    public void setAutoAcknowledge(Boolean autoAcknowledge) {
        this.autoAcknowledge = autoAcknowledge;
    }

    public Integer getAutoAcknowledgeAfterMinutes() {
        return autoAcknowledgeAfterMinutes;
    }

    public void setAutoAcknowledgeAfterMinutes(Integer autoAcknowledgeAfterMinutes) {
        this.autoAcknowledgeAfterMinutes = autoAcknowledgeAfterMinutes;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public LocalDateTime getLastSentAt() {
        return lastSentAt;
    }

    public void setLastSentAt(LocalDateTime lastSentAt) {
        this.lastSentAt = lastSentAt;
    }

    public String getDeliveryChannels() {
        return deliveryChannels;
    }

    public void setDeliveryChannels(String deliveryChannels) {
        this.deliveryChannels = deliveryChannels;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public Integer getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(Integer priorityScore) {
        this.priorityScore = priorityScore;
    }

    public UUID getRelatedNotificationId() {
        return relatedNotificationId;
    }

    public void setRelatedNotificationId(UUID relatedNotificationId) {
        this.relatedNotificationId = relatedNotificationId;
    }

    public Integer getEscalationLevel() {
        return escalationLevel;
    }

    public void setEscalationLevel(Integer escalationLevel) {
        this.escalationLevel = escalationLevel;
    }

    public LocalDateTime getEscalatedAt() {
        return escalatedAt;
    }

    public void setEscalatedAt(LocalDateTime escalatedAt) {
        this.escalatedAt = escalatedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    // Utility methods
    public boolean isAcknowledged() {
        return acknowledgedAt != null;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isResolved() {
        return resolvedAt != null;
    }

    public boolean requiresAction() {
        return actionRequired != null && actionRequired;
    }

    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    public boolean shouldAutoAcknowledge() {
        if (!autoAcknowledge || autoAcknowledgeAfterMinutes == null) {
            return false;
        }
        return getCreatedAt().plusMinutes(autoAcknowledgeAfterMinutes).isBefore(LocalDateTime.now());
    }

    public void acknowledge(User user, String note) {
        this.acknowledgedBy = user;
        this.acknowledgedAt = LocalDateTime.now();
        this.acknowledgmentNote = note;
        this.status = NotificationStatus.ACKNOWLEDGED;
    }

    public void resolve(String note) {
        this.resolvedAt = LocalDateTime.now();
        this.resolutionNote = note;
        this.status = NotificationStatus.RESOLVED;
    }

    public void escalate() {
        this.escalationLevel++;
        this.escalatedAt = LocalDateTime.now();
        this.status = NotificationStatus.ESCALATED;
    }

    public void markAsRead() {
        if (this.status == NotificationStatus.UNREAD) {
            this.status = NotificationStatus.READ;
        }
    }

    public void incrementRetryCount() {
        this.retryCount++;
        this.lastSentAt = LocalDateTime.now();
    }

    public void setAction(String actionText, String actionUrl) {
        this.actionRequired = true;
        this.actionText = actionText;
        this.actionUrl = actionUrl;
    }

    private Integer calculatePriorityScore() {
        if (level == null) return 0;
        
        int score = level.getPriorityValue();
        
        // Adjust score based on type
        if (type != null) {
            if (type.isCritical()) {
                score += 50;
            } else if (type.isWarning()) {
                score += 20;
            }
        }
        
        // Adjust score based on action required
        if (requiresAction()) {
            score += 30;
        }
        
        return score;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "notificationId=" + notificationId +
                ", level=" + level +
                ", type=" + type +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", farm=" + (farm != null ? farm.getName() : "null") +
                ", room=" + (room != null ? room.getName() : "null") +
                ", device=" + (device != null ? device.getName() : "null") +
                ", acknowledgedAt=" + acknowledgedAt +
                ", resolvedAt=" + resolvedAt +
                '}';
    }

    /**
     * Enumeration for notification levels.
     */
    public enum NotificationLevel {
        INFO("Info", 10),
        WARNING("Warning", 50),
        CRITICAL("Critical", 100);

        private final String displayName;
        private final int priorityValue;

        NotificationLevel(String displayName, int priorityValue) {
            this.displayName = displayName;
            this.priorityValue = priorityValue;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getPriorityValue() {
            return priorityValue;
        }

        public boolean isCritical() {
            return this == CRITICAL;
        }

        public boolean isWarning() {
            return this == WARNING;
        }
    }

    /**
     * Enumeration for notification types.
     */
    public enum NotificationType {
        DEVICE_OFFLINE("Device Offline", true, false),
        DEVICE_ONLINE("Device Online", false, false),
        SENSOR_ALERT("Sensor Alert", true, true),
        AUTOMATION_TRIGGERED("Automation Triggered", false, false),
        AUTOMATION_FAILED("Automation Failed", true, true),
        COMMAND_FAILED("Command Failed", true, true),
        CONTAMINATION_DETECTED("Contamination Detected", true, true),
        HARVEST_READY("Harvest Ready", false, true),
        MAINTENANCE_DUE("Maintenance Due", false, true),
        SYSTEM_ERROR("System Error", true, true),
        USER_ACTION("User Action", false, false),
        THRESHOLD_EXCEEDED("Threshold Exceeded", true, true),
        BATTERY_LOW("Battery Low", true, true),
        FIRMWARE_UPDATE("Firmware Update", false, false),
        CYCLE_MILESTONE("Cycle Milestone", false, false),
        GENERAL("General", false, false);

        private final String displayName;
        private final boolean isWarning;
        private final boolean isCritical;

        NotificationType(String displayName, boolean isWarning, boolean isCritical) {
            this.displayName = displayName;
            this.isWarning = isWarning;
            this.isCritical = isCritical;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isWarning() {
            return isWarning;
        }

        public boolean isCritical() {
            return isCritical;
        }
    }

    /**
     * Enumeration for notification status.
     */
    public enum NotificationStatus {
        UNREAD("Unread"),
        READ("Read"),
        ACKNOWLEDGED("Acknowledged"),
        ESCALATED("Escalated"),
        RESOLVED("Resolved"),
        EXPIRED("Expired"),
        DISMISSED("Dismissed");

        private final String displayName;

        NotificationStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isActive() {
            return this == UNREAD || this == READ || this == ESCALATED;
        }

        public boolean isFinal() {
            return this == RESOLVED || this == EXPIRED || this == DISMISSED;
        }
    }
}