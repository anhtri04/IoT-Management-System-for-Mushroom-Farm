package com.smartfarm.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * AutomationRule entity representing automated control rules for IoT devices.
 * Rules trigger device actions based on sensor data thresholds and conditions.
 */
@Entity
@Table(name = "automation_rules", indexes = {
        @Index(name = "idx_automation_room", columnList = "room_id"),
        @Index(name = "idx_automation_enabled", columnList = "enabled"),
        @Index(name = "idx_automation_parameter", columnList = "parameter"),
        @Index(name = "idx_automation_created_by", columnList = "created_by")
})
public class AutomationRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "rule_id")
    private UUID ruleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @NotBlank
    @Size(max = 200)
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "parameter", nullable = false)
    private SensorParameter parameter;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "comparator", nullable = false)
    private Comparator comparator;

    @NotNull
    @Column(name = "threshold", nullable = false)
    private Float threshold;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_device_id")
    private Device actionDevice;

    @NotBlank
    @Column(name = "action_command", nullable = false)
    private String actionCommand;

    @Column(name = "action_params", columnDefinition = "jsonb")
    private String actionParams;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // Advanced rule settings
    @Column(name = "cooldown_minutes")
    private Integer cooldownMinutes = 5; // Minimum time between rule executions

    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    @Column(name = "trigger_count")
    private Long triggerCount = 0L;

    @Column(name = "priority")
    private Integer priority = 5; // 1-10, 1 being highest priority

    @Column(name = "active_hours_start")
    private Integer activeHoursStart; // 0-23, null means always active

    @Column(name = "active_hours_end")
    private Integer activeHoursEnd; // 0-23, null means always active

    @Column(name = "active_days")
    private String activeDays; // JSON array of day names or numbers

    @Column(name = "max_executions_per_day")
    private Integer maxExecutionsPerDay;

    @Column(name = "executions_today")
    private Integer executionsToday = 0;

    @Column(name = "last_execution_date")
    private java.time.LocalDate lastExecutionDate;

    // Relationships
    @OneToMany(mappedBy = "automationRule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<AutomationExecution> executions = new HashSet<>();

    // Constructors
    public AutomationRule() {
        super();
    }

    public AutomationRule(String name, Room room, SensorParameter parameter, 
                         Comparator comparator, Float threshold, 
                         Device actionDevice, String actionCommand, User createdBy) {
        this();
        this.name = name;
        this.room = room;
        this.parameter = parameter;
        this.comparator = comparator;
        this.threshold = threshold;
        this.actionDevice = actionDevice;
        this.actionCommand = actionCommand;
        this.createdBy = createdBy;
    }

    // Getters and Setters
    public UUID getRuleId() {
        return ruleId;
    }

    public void setRuleId(UUID ruleId) {
        this.ruleId = ruleId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SensorParameter getParameter() {
        return parameter;
    }

    public void setParameter(SensorParameter parameter) {
        this.parameter = parameter;
    }

    public Comparator getComparator() {
        return comparator;
    }

    public void setComparator(Comparator comparator) {
        this.comparator = comparator;
    }

    public Float getThreshold() {
        return threshold;
    }

    public void setThreshold(Float threshold) {
        this.threshold = threshold;
    }

    public Device getActionDevice() {
        return actionDevice;
    }

    public void setActionDevice(Device actionDevice) {
        this.actionDevice = actionDevice;
    }

    public String getActionCommand() {
        return actionCommand;
    }

    public void setActionCommand(String actionCommand) {
        this.actionCommand = actionCommand;
    }

    public String getActionParams() {
        return actionParams;
    }

    public void setActionParams(String actionParams) {
        this.actionParams = actionParams;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Integer getCooldownMinutes() {
        return cooldownMinutes;
    }

    public void setCooldownMinutes(Integer cooldownMinutes) {
        this.cooldownMinutes = cooldownMinutes;
    }

    public LocalDateTime getLastTriggeredAt() {
        return lastTriggeredAt;
    }

    public void setLastTriggeredAt(LocalDateTime lastTriggeredAt) {
        this.lastTriggeredAt = lastTriggeredAt;
    }

    public Long getTriggerCount() {
        return triggerCount;
    }

    public void setTriggerCount(Long triggerCount) {
        this.triggerCount = triggerCount;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getActiveHoursStart() {
        return activeHoursStart;
    }

    public void setActiveHoursStart(Integer activeHoursStart) {
        this.activeHoursStart = activeHoursStart;
    }

    public Integer getActiveHoursEnd() {
        return activeHoursEnd;
    }

    public void setActiveHoursEnd(Integer activeHoursEnd) {
        this.activeHoursEnd = activeHoursEnd;
    }

    public String getActiveDays() {
        return activeDays;
    }

    public void setActiveDays(String activeDays) {
        this.activeDays = activeDays;
    }

    public Integer getMaxExecutionsPerDay() {
        return maxExecutionsPerDay;
    }

    public void setMaxExecutionsPerDay(Integer maxExecutionsPerDay) {
        this.maxExecutionsPerDay = maxExecutionsPerDay;
    }

    public Integer getExecutionsToday() {
        return executionsToday;
    }

    public void setExecutionsToday(Integer executionsToday) {
        this.executionsToday = executionsToday;
    }

    public java.time.LocalDate getLastExecutionDate() {
        return lastExecutionDate;
    }

    public void setLastExecutionDate(java.time.LocalDate lastExecutionDate) {
        this.lastExecutionDate = lastExecutionDate;
    }

    public Set<AutomationExecution> getExecutions() {
        return executions;
    }

    public void setExecutions(Set<AutomationExecution> executions) {
        this.executions = executions;
    }

    // Utility methods
    public boolean isEnabled() {
        return enabled != null && enabled;
    }

    public boolean canTrigger() {
        if (!isEnabled()) {
            return false;
        }

        // Check cooldown
        if (lastTriggeredAt != null && cooldownMinutes != null) {
            LocalDateTime cooldownEnd = lastTriggeredAt.plusMinutes(cooldownMinutes);
            if (LocalDateTime.now().isBefore(cooldownEnd)) {
                return false;
            }
        }

        // Check daily execution limit
        if (maxExecutionsPerDay != null) {
            java.time.LocalDate today = java.time.LocalDate.now();
            if (!today.equals(lastExecutionDate)) {
                // Reset daily counter
                executionsToday = 0;
                lastExecutionDate = today;
            }
            if (executionsToday >= maxExecutionsPerDay) {
                return false;
            }
        }

        // Check active hours
        if (activeHoursStart != null && activeHoursEnd != null) {
            int currentHour = LocalDateTime.now().getHour();
            if (activeHoursStart <= activeHoursEnd) {
                // Normal range (e.g., 8-18)
                if (currentHour < activeHoursStart || currentHour > activeHoursEnd) {
                    return false;
                }
            } else {
                // Overnight range (e.g., 22-6)
                if (currentHour > activeHoursEnd && currentHour < activeHoursStart) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean evaluateCondition(Float sensorValue) {
        if (sensorValue == null || threshold == null) {
            return false;
        }

        return switch (comparator) {
            case GREATER_THAN -> sensorValue > threshold;
            case GREATER_THAN_OR_EQUAL -> sensorValue >= threshold;
            case LESS_THAN -> sensorValue < threshold;
            case LESS_THAN_OR_EQUAL -> sensorValue <= threshold;
            case EQUAL -> Float.compare(sensorValue, threshold) == 0;
            case NOT_EQUAL -> Float.compare(sensorValue, threshold) != 0;
        };
    }

    public void recordExecution() {
        this.lastTriggeredAt = LocalDateTime.now();
        this.triggerCount++;
        
        java.time.LocalDate today = java.time.LocalDate.now();
        if (!today.equals(lastExecutionDate)) {
            executionsToday = 1;
            lastExecutionDate = today;
        } else {
            executionsToday++;
        }
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    @Override
    public String toString() {
        return "AutomationRule{" +
                "ruleId=" + ruleId +
                ", name='" + name + '\'' +
                ", room=" + (room != null ? room.getName() : "null") +
                ", parameter=" + parameter +
                ", comparator=" + comparator +
                ", threshold=" + threshold +
                ", actionDevice=" + (actionDevice != null ? actionDevice.getName() : "null") +
                ", actionCommand='" + actionCommand + '\'' +
                ", enabled=" + enabled +
                ", triggerCount=" + triggerCount +
                ", createdAt=" + getCreatedAt() +
                '}';
    }

    /**
     * Enumeration for sensor parameters that can trigger automation rules.
     */
    public enum SensorParameter {
        TEMPERATURE("Temperature", "°C"),
        HUMIDITY("Humidity", "%"),
        CO2("CO2", "ppm"),
        LIGHT("Light", "lux"),
        MOISTURE("Moisture", "%"),
        PH("pH", ""),
        PRESSURE("Pressure", "hPa"),
        AIR_QUALITY("Air Quality", "AQI");

        private final String displayName;
        private final String unit;

        SensorParameter(String displayName, String unit) {
            this.displayName = displayName;
            this.unit = unit;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getUnit() {
            return unit;
        }
    }

    /**
     * Enumeration for comparison operators.
     */
    public enum Comparator {
        GREATER_THAN(">", "Greater than"),
        GREATER_THAN_OR_EQUAL(">=", "Greater than or equal to"),
        LESS_THAN("<", "Less than"),
        LESS_THAN_OR_EQUAL("<=", "Less than or equal to"),
        EQUAL("=", "Equal to"),
        NOT_EQUAL("!=", "Not equal to");

        private final String symbol;
        private final String displayName;

        Comparator(String symbol, String displayName) {
            this.symbol = symbol;
            this.displayName = displayName;
        }

        public String getSymbol() {
            return symbol;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}