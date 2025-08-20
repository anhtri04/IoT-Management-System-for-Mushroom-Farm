package com.smartfarm.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Command entity representing control messages sent to IoT devices.
 * Tracks command history, execution status, and acknowledgments.
 */
@Entity
@Table(name = "commands", indexes = {
        @Index(name = "idx_commands_device_time", columnList = "device_id, issued_at DESC"),
        @Index(name = "idx_commands_room_time", columnList = "room_id, issued_at DESC"),
        @Index(name = "idx_commands_farm_time", columnList = "farm_id, issued_at DESC"),
        @Index(name = "idx_commands_status", columnList = "status"),
        @Index(name = "idx_commands_issued_by", columnList = "issued_by")
})
public class Command extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "command_id")
    private UUID commandId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id")
    private Farm farm;

    @NotBlank
    @Column(name = "command", nullable = false)
    private String command;

    @Column(name = "params", columnDefinition = "jsonb")
    private String params;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by")
    private User issuedBy;

    @NotNull
    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CommandStatus status = CommandStatus.PENDING;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "response_payload", columnDefinition = "jsonb")
    private String responsePayload;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds = 30;

    @Column(name = "priority")
    private Integer priority = 5; // 1-10, 1 being highest priority

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // Constructors
    public Command() {
        super();
        this.issuedAt = LocalDateTime.now();
    }

    public Command(Device device, String command, String params, User issuedBy) {
        this();
        this.device = device;
        this.command = command;
        this.params = params;
        this.issuedBy = issuedBy;
        if (device != null && device.getRoom() != null) {
            this.room = device.getRoom();
            this.farm = device.getRoom().getFarm();
        }
    }

    // Getters and Setters
    public UUID getCommandId() {
        return commandId;
    }

    public void setCommandId(UUID commandId) {
        this.commandId = commandId;
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

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public User getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(User issuedBy) {
        this.issuedBy = issuedBy;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public CommandStatus getStatus() {
        return status;
    }

    public void setStatus(CommandStatus status) {
        this.status = status;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(LocalDateTime failedAt) {
        this.failedAt = failedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getResponsePayload() {
        return responsePayload;
    }

    public void setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
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

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    // Utility methods
    public boolean isPending() {
        return status == CommandStatus.PENDING;
    }

    public boolean isSent() {
        return status == CommandStatus.SENT;
    }

    public boolean isAcknowledged() {
        return status == CommandStatus.ACKNOWLEDGED;
    }

    public boolean isCompleted() {
        return status == CommandStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == CommandStatus.FAILED;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean canRetry() {
        return retryCount < maxRetries && (status == CommandStatus.FAILED || status == CommandStatus.TIMEOUT);
    }

    public void markAsSent() {
        this.status = CommandStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    public void markAsAcknowledged(String responsePayload) {
        this.status = CommandStatus.ACKNOWLEDGED;
        this.acknowledgedAt = LocalDateTime.now();
        this.responsePayload = responsePayload;
    }

    public void markAsCompleted(String responsePayload) {
        this.status = CommandStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.responsePayload = responsePayload;
    }

    public void markAsFailed(String errorMessage) {
        this.status = CommandStatus.FAILED;
        this.failedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    public void markAsTimeout() {
        this.status = CommandStatus.TIMEOUT;
        this.failedAt = LocalDateTime.now();
        this.errorMessage = "Command timed out after " + timeoutSeconds + " seconds";
    }

    public void incrementRetryCount() {
        this.retryCount++;
        this.status = CommandStatus.PENDING;
    }

    @Override
    public String toString() {
        return "Command{" +
                "commandId=" + commandId +
                ", device=" + (device != null ? device.getName() : "null") +
                ", command='" + command + '\'' +
                ", status=" + status +
                ", issuedBy=" + (issuedBy != null ? issuedBy.getEmail() : "null") +
                ", issuedAt=" + issuedAt +
                ", retryCount=" + retryCount +
                ", priority=" + priority +
                ", createdAt=" + getCreatedAt() +
                '}';
    }

    /**
     * Enumeration for command execution status.
     */
    public enum CommandStatus {
        PENDING("Pending", "Command is queued for execution"),
        SENT("Sent", "Command has been sent to device"),
        ACKNOWLEDGED("Acknowledged", "Device has acknowledged receipt"),
        COMPLETED("Completed", "Command executed successfully"),
        FAILED("Failed", "Command execution failed"),
        TIMEOUT("Timeout", "Command timed out"),
        CANCELLED("Cancelled", "Command was cancelled"),
        EXPIRED("Expired", "Command expired before execution");

        private final String displayName;
        private final String description;

        CommandStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public boolean isTerminal() {
            return this == COMPLETED || this == FAILED || this == CANCELLED || this == EXPIRED;
        }

        public boolean isSuccessful() {
            return this == COMPLETED;
        }
    }
}