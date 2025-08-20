package com.smartfarm.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AutomationExecution entity representing the execution history of automation rules.
 * Tracks when rules were triggered, their success/failure status, and execution details.
 */
@Entity
@Table(name = "automation_executions", indexes = {
        @Index(name = "idx_automation_exec_rule", columnList = "rule_id"),
        @Index(name = "idx_automation_exec_time", columnList = "executed_at"),
        @Index(name = "idx_automation_exec_status", columnList = "status"),
        @Index(name = "idx_automation_exec_device", columnList = "device_id")
})
public class AutomationExecution extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "execution_id")
    private UUID executionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private AutomationRule automationRule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @NotNull
    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExecutionStatus status;

    @Column(name = "trigger_value")
    private Float triggerValue;

    @Column(name = "threshold_value")
    private Float thresholdValue;

    @Column(name = "command_sent")
    private String commandSent;

    @Column(name = "command_params", columnDefinition = "jsonb")
    private String commandParams;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "command_id")
    private Command resultingCommand;

    @Column(name = "execution_duration_ms")
    private Long executionDurationMs;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "notes")
    private String notes;

    // Constructors
    public AutomationExecution() {
        super();
        this.executedAt = LocalDateTime.now();
    }

    public AutomationExecution(AutomationRule automationRule, Device device, 
                              Float triggerValue, Float thresholdValue) {
        this();
        this.automationRule = automationRule;
        this.device = device;
        this.room = device != null ? device.getRoom() : null;
        this.triggerValue = triggerValue;
        this.thresholdValue = thresholdValue;
        this.status = ExecutionStatus.PENDING;
    }

    // Getters and Setters
    public UUID getExecutionId() {
        return executionId;
    }

    public void setExecutionId(UUID executionId) {
        this.executionId = executionId;
    }

    public AutomationRule getAutomationRule() {
        return automationRule;
    }

    public void setAutomationRule(AutomationRule automationRule) {
        this.automationRule = automationRule;
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

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public Float getTriggerValue() {
        return triggerValue;
    }

    public void setTriggerValue(Float triggerValue) {
        this.triggerValue = triggerValue;
    }

    public Float getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(Float thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public String getCommandSent() {
        return commandSent;
    }

    public void setCommandSent(String commandSent) {
        this.commandSent = commandSent;
    }

    public String getCommandParams() {
        return commandParams;
    }

    public void setCommandParams(String commandParams) {
        this.commandParams = commandParams;
    }

    public Command getResultingCommand() {
        return resultingCommand;
    }

    public void setResultingCommand(Command resultingCommand) {
        this.resultingCommand = resultingCommand;
    }

    public Long getExecutionDurationMs() {
        return executionDurationMs;
    }

    public void setExecutionDurationMs(Long executionDurationMs) {
        this.executionDurationMs = executionDurationMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Utility methods
    public void markAsStarted() {
        this.status = ExecutionStatus.EXECUTING;
        this.executedAt = LocalDateTime.now();
    }

    public void markAsSuccessful(Command resultingCommand, long durationMs) {
        this.status = ExecutionStatus.SUCCESS;
        this.resultingCommand = resultingCommand;
        this.executionDurationMs = durationMs;
        this.errorMessage = null;
    }

    public void markAsFailed(String errorMessage, long durationMs) {
        this.status = ExecutionStatus.FAILED;
        this.errorMessage = errorMessage;
        this.executionDurationMs = durationMs;
    }

    public void markAsSkipped(String reason) {
        this.status = ExecutionStatus.SKIPPED;
        this.notes = reason;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public boolean isSuccessful() {
        return status == ExecutionStatus.SUCCESS;
    }

    public boolean isFailed() {
        return status == ExecutionStatus.FAILED;
    }

    public boolean isPending() {
        return status == ExecutionStatus.PENDING;
    }

    public boolean isExecuting() {
        return status == ExecutionStatus.EXECUTING;
    }

    @Override
    public String toString() {
        return "AutomationExecution{" +
                "executionId=" + executionId +
                ", automationRule=" + (automationRule != null ? automationRule.getName() : "null") +
                ", device=" + (device != null ? device.getName() : "null") +
                ", executedAt=" + executedAt +
                ", status=" + status +
                ", triggerValue=" + triggerValue +
                ", thresholdValue=" + thresholdValue +
                ", commandSent='" + commandSent + '\'' +
                ", executionDurationMs=" + executionDurationMs +
                ", retryCount=" + retryCount +
                '}';
    }

    /**
     * Enumeration for automation execution status.
     */
    public enum ExecutionStatus {
        PENDING("Pending", "Execution is queued and waiting to start"),
        EXECUTING("Executing", "Execution is currently in progress"),
        SUCCESS("Success", "Execution completed successfully"),
        FAILED("Failed", "Execution failed due to an error"),
        SKIPPED("Skipped", "Execution was skipped due to conditions not being met"),
        TIMEOUT("Timeout", "Execution timed out"),
        CANCELLED("Cancelled", "Execution was cancelled by user or system");

        private final String displayName;
        private final String description;

        ExecutionStatus(String displayName, String description) {
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
            return this == SUCCESS || this == FAILED || this == SKIPPED || 
                   this == TIMEOUT || this == CANCELLED;
        }

        public boolean isActive() {
            return this == PENDING || this == EXECUTING;
        }
    }
}