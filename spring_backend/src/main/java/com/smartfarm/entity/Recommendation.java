package com.smartfarm.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Recommendation entity representing AI-generated recommendations from Amazon Bedrock.
 * Stores model outputs, confidence scores, and user interactions with recommendations.
 */
@Entity
@Table(name = "recommendations", indexes = {
        @Index(name = "idx_recommendation_farm", columnList = "farm_id"),
        @Index(name = "idx_recommendation_room", columnList = "room_id"),
        @Index(name = "idx_recommendation_created", columnList = "created_at"),
        @Index(name = "idx_recommendation_status", columnList = "status"),
        @Index(name = "idx_recommendation_confidence", columnList = "confidence"),
        @Index(name = "idx_recommendation_model", columnList = "model_id"),
        @Index(name = "idx_recommendation_category", columnList = "category")
})
public class Recommendation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "rec_id")
    private UUID recId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id")
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id")
    private FarmingCycle farmingCycle;

    @NotNull
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload; // Model output JSON

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @NotBlank
    @Size(max = 200)
    @Column(name = "model_id", nullable = false)
    private String modelId;

    @Column(name = "model_version")
    private String modelVersion;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private RecommendationCategory category;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private RecommendationType type;

    @NotBlank
    @Size(max = 200)
    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private Priority priority = Priority.MEDIUM;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RecommendationStatus status = RecommendationStatus.PENDING;

    @Column(name = "auto_executable")
    private Boolean autoExecutable = false;

    @Column(name = "risk_level")
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Column(name = "estimated_impact")
    private String estimatedImpact;

    @Column(name = "implementation_cost")
    private BigDecimal implementationCost;

    @Column(name = "expected_benefit")
    private String expectedBenefit;

    @Column(name = "time_to_implement_hours")
    private Integer timeToImplementHours;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "conditions_met")
    private Boolean conditionsMet = true;

    @Column(name = "prerequisites", columnDefinition = "jsonb")
    private String prerequisites; // JSON array of prerequisites

    @Column(name = "actions", columnDefinition = "jsonb")
    private String actions; // JSON array of recommended actions

    @Column(name = "parameters", columnDefinition = "jsonb")
    private String parameters; // JSON object with action parameters

    @Column(name = "target_devices", columnDefinition = "jsonb")
    private String targetDevices; // JSON array of device IDs

    @Column(name = "environmental_context", columnDefinition = "jsonb")
    private String environmentalContext; // Environmental data used for recommendation

    @Column(name = "historical_context", columnDefinition = "jsonb")
    private String historicalContext; // Historical data used for recommendation

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_notes")
    private String reviewNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "implemented_by")
    private User implementedBy;

    @Column(name = "implemented_at")
    private LocalDateTime implementedAt;

    @Column(name = "implementation_notes")
    private String implementationNotes;

    @Column(name = "execution_result", columnDefinition = "jsonb")
    private String executionResult; // JSON object with execution results

    @Column(name = "feedback_score")
    private Integer feedbackScore; // 1-5 rating from user

    @Column(name = "feedback_notes")
    private String feedbackNotes;

    @Column(name = "effectiveness_measured")
    private Boolean effectivenessMeasured = false;

    @Column(name = "effectiveness_score")
    private BigDecimal effectivenessScore;

    @Column(name = "effectiveness_notes")
    private String effectivenessNotes;

    @Column(name = "tags")
    private String tags; // Comma-separated tags

    @Column(name = "related_recommendation_id")
    private UUID relatedRecommendationId;

    @Column(name = "superseded_by")
    private UUID supersededBy;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "notification_sent")
    private Boolean notificationSent = false;

    @Column(name = "auto_execution_attempted")
    private Boolean autoExecutionAttempted = false;

    @Column(name = "auto_execution_result")
    private String autoExecutionResult;

    // Constructors
    public Recommendation() {
        super();
    }

    public Recommendation(String payload, BigDecimal confidence, String modelId, 
                         RecommendationCategory category, RecommendationType type, String title) {
        this();
        this.payload = payload;
        this.confidence = confidence;
        this.modelId = modelId;
        this.category = category;
        this.type = type;
        this.title = title;
    }

    public Recommendation(Farm farm, Room room, String payload, BigDecimal confidence, 
                         String modelId, RecommendationCategory category, RecommendationType type, String title) {
        this(payload, confidence, modelId, category, type, title);
        this.farm = farm;
        this.room = room;
    }

    // Getters and Setters
    public UUID getRecId() {
        return recId;
    }

    public void setRecId(UUID recId) {
        this.recId = recId;
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

    public FarmingCycle getFarmingCycle() {
        return farmingCycle;
    }

    public void setFarmingCycle(FarmingCycle farmingCycle) {
        this.farmingCycle = farmingCycle;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public RecommendationCategory getCategory() {
        return category;
    }

    public void setCategory(RecommendationCategory category) {
        this.category = category;
    }

    public RecommendationType getType() {
        return type;
    }

    public void setType(RecommendationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public RecommendationStatus getStatus() {
        return status;
    }

    public void setStatus(RecommendationStatus status) {
        this.status = status;
    }

    public Boolean getAutoExecutable() {
        return autoExecutable;
    }

    public void setAutoExecutable(Boolean autoExecutable) {
        this.autoExecutable = autoExecutable;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getEstimatedImpact() {
        return estimatedImpact;
    }

    public void setEstimatedImpact(String estimatedImpact) {
        this.estimatedImpact = estimatedImpact;
    }

    public BigDecimal getImplementationCost() {
        return implementationCost;
    }

    public void setImplementationCost(BigDecimal implementationCost) {
        this.implementationCost = implementationCost;
    }

    public String getExpectedBenefit() {
        return expectedBenefit;
    }

    public void setExpectedBenefit(String expectedBenefit) {
        this.expectedBenefit = expectedBenefit;
    }

    public Integer getTimeToImplementHours() {
        return timeToImplementHours;
    }

    public void setTimeToImplementHours(Integer timeToImplementHours) {
        this.timeToImplementHours = timeToImplementHours;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }

    public Boolean getConditionsMet() {
        return conditionsMet;
    }

    public void setConditionsMet(Boolean conditionsMet) {
        this.conditionsMet = conditionsMet;
    }

    public String getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(String prerequisites) {
        this.prerequisites = prerequisites;
    }

    public String getActions() {
        return actions;
    }

    public void setActions(String actions) {
        this.actions = actions;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public String getTargetDevices() {
        return targetDevices;
    }

    public void setTargetDevices(String targetDevices) {
        this.targetDevices = targetDevices;
    }

    public String getEnvironmentalContext() {
        return environmentalContext;
    }

    public void setEnvironmentalContext(String environmentalContext) {
        this.environmentalContext = environmentalContext;
    }

    public String getHistoricalContext() {
        return historicalContext;
    }

    public void setHistoricalContext(String historicalContext) {
        this.historicalContext = historicalContext;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public User getImplementedBy() {
        return implementedBy;
    }

    public void setImplementedBy(User implementedBy) {
        this.implementedBy = implementedBy;
    }

    public LocalDateTime getImplementedAt() {
        return implementedAt;
    }

    public void setImplementedAt(LocalDateTime implementedAt) {
        this.implementedAt = implementedAt;
    }

    public String getImplementationNotes() {
        return implementationNotes;
    }

    public void setImplementationNotes(String implementationNotes) {
        this.implementationNotes = implementationNotes;
    }

    public String getExecutionResult() {
        return executionResult;
    }

    public void setExecutionResult(String executionResult) {
        this.executionResult = executionResult;
    }

    public Integer getFeedbackScore() {
        return feedbackScore;
    }

    public void setFeedbackScore(Integer feedbackScore) {
        this.feedbackScore = feedbackScore;
    }

    public String getFeedbackNotes() {
        return feedbackNotes;
    }

    public void setFeedbackNotes(String feedbackNotes) {
        this.feedbackNotes = feedbackNotes;
    }

    public Boolean getEffectivenessMeasured() {
        return effectivenessMeasured;
    }

    public void setEffectivenessMeasured(Boolean effectivenessMeasured) {
        this.effectivenessMeasured = effectivenessMeasured;
    }

    public BigDecimal getEffectivenessScore() {
        return effectivenessScore;
    }

    public void setEffectivenessScore(BigDecimal effectivenessScore) {
        this.effectivenessScore = effectivenessScore;
    }

    public String getEffectivenessNotes() {
        return effectivenessNotes;
    }

    public void setEffectivenessNotes(String effectivenessNotes) {
        this.effectivenessNotes = effectivenessNotes;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public UUID getRelatedRecommendationId() {
        return relatedRecommendationId;
    }

    public void setRelatedRecommendationId(UUID relatedRecommendationId) {
        this.relatedRecommendationId = relatedRecommendationId;
    }

    public UUID getSupersededBy() {
        return supersededBy;
    }

    public void setSupersededBy(UUID supersededBy) {
        this.supersededBy = supersededBy;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getNotificationSent() {
        return notificationSent;
    }

    public void setNotificationSent(Boolean notificationSent) {
        this.notificationSent = notificationSent;
    }

    public Boolean getAutoExecutionAttempted() {
        return autoExecutionAttempted;
    }

    public void setAutoExecutionAttempted(Boolean autoExecutionAttempted) {
        this.autoExecutionAttempted = autoExecutionAttempted;
    }

    public String getAutoExecutionResult() {
        return autoExecutionResult;
    }

    public void setAutoExecutionResult(String autoExecutionResult) {
        this.autoExecutionResult = autoExecutionResult;
    }

    // Utility methods
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return validUntil == null || LocalDateTime.now().isBefore(validUntil);
    }

    public boolean isHighConfidence() {
        return confidence != null && confidence.compareTo(new BigDecimal("0.8")) >= 0;
    }

    public boolean isLowRisk() {
        return riskLevel == RiskLevel.LOW;
    }

    public boolean canAutoExecute() {
        return autoExecutable != null && autoExecutable && isLowRisk() && 
               isHighConfidence() && conditionsMet != null && conditionsMet;
    }

    public boolean isImplemented() {
        return implementedAt != null;
    }

    public boolean isReviewed() {
        return reviewedAt != null;
    }

    public boolean isSuperseded() {
        return supersededBy != null;
    }

    public void approve(User user, String notes) {
        this.reviewedBy = user;
        this.reviewedAt = LocalDateTime.now();
        this.reviewNotes = notes;
        this.status = RecommendationStatus.APPROVED;
    }

    public void reject(User user, String notes) {
        this.reviewedBy = user;
        this.reviewedAt = LocalDateTime.now();
        this.reviewNotes = notes;
        this.status = RecommendationStatus.REJECTED;
    }

    public void implement(User user, String notes) {
        this.implementedBy = user;
        this.implementedAt = LocalDateTime.now();
        this.implementationNotes = notes;
        this.status = RecommendationStatus.IMPLEMENTED;
    }

    public void markExpired() {
        this.status = RecommendationStatus.EXPIRED;
    }

    public void supersede(UUID newRecommendationId) {
        this.supersededBy = newRecommendationId;
        this.status = RecommendationStatus.SUPERSEDED;
    }

    public void provideFeedback(Integer score, String notes) {
        this.feedbackScore = score;
        this.feedbackNotes = notes;
    }

    public void measureEffectiveness(BigDecimal score, String notes) {
        this.effectivenessMeasured = true;
        this.effectivenessScore = score;
        this.effectivenessNotes = notes;
    }

    @Override
    public String toString() {
        return "Recommendation{" +
                "recId=" + recId +
                ", modelId='" + modelId + '\'' +
                ", category=" + category +
                ", type=" + type +
                ", title='" + title + '\'' +
                ", confidence=" + confidence +
                ", priority=" + priority +
                ", status=" + status +
                ", riskLevel=" + riskLevel +
                ", autoExecutable=" + autoExecutable +
                ", farm=" + (farm != null ? farm.getName() : "null") +
                ", room=" + (room != null ? room.getName() : "null") +
                '}';
    }

    /**
     * Enumeration for recommendation categories.
     */
    public enum RecommendationCategory {
        ENVIRONMENTAL("Environmental"),
        AUTOMATION("Automation"),
        MAINTENANCE("Maintenance"),
        OPTIMIZATION("Optimization"),
        ALERT("Alert"),
        HARVEST("Harvest"),
        CONTAMINATION("Contamination"),
        ENERGY("Energy"),
        COST("Cost"),
        QUALITY("Quality");

        private final String displayName;

        RecommendationCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Enumeration for recommendation types.
     */
    public enum RecommendationType {
        ADJUST_TEMPERATURE("Adjust Temperature"),
        ADJUST_HUMIDITY("Adjust Humidity"),
        ADJUST_CO2("Adjust CO2"),
        ADJUST_LIGHTING("Adjust Lighting"),
        INCREASE_VENTILATION("Increase Ventilation"),
        DECREASE_VENTILATION("Decrease Ventilation"),
        WATER_SUBSTRATE("Water Substrate"),
        HARVEST_NOW("Harvest Now"),
        INSPECT_CONTAMINATION("Inspect Contamination"),
        CLEAN_EQUIPMENT("Clean Equipment"),
        REPLACE_FILTER("Replace Filter"),
        CALIBRATE_SENSOR("Calibrate Sensor"),
        UPDATE_FIRMWARE("Update Firmware"),
        SCHEDULE_MAINTENANCE("Schedule Maintenance"),
        OPTIMIZE_SCHEDULE("Optimize Schedule"),
        REDUCE_ENERGY("Reduce Energy"),
        GENERAL_ACTION("General Action");

        private final String displayName;

        RecommendationType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Enumeration for recommendation priority levels.
     */
    public enum Priority {
        LOW("Low", 1),
        MEDIUM("Medium", 2),
        HIGH("High", 3),
        URGENT("Urgent", 4);

        private final String displayName;
        private final int value;

        Priority(String displayName, int value) {
            this.displayName = displayName;
            this.value = value;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * Enumeration for recommendation status.
     */
    public enum RecommendationStatus {
        PENDING("Pending"),
        UNDER_REVIEW("Under Review"),
        APPROVED("Approved"),
        REJECTED("Rejected"),
        IMPLEMENTED("Implemented"),
        FAILED("Failed"),
        EXPIRED("Expired"),
        SUPERSEDED("Superseded");

        private final String displayName;

        RecommendationStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isActive() {
            return this == PENDING || this == UNDER_REVIEW || this == APPROVED;
        }

        public boolean isFinal() {
            return this == IMPLEMENTED || this == REJECTED || this == EXPIRED || this == SUPERSEDED;
        }
    }

    /**
     * Enumeration for risk levels.
     */
    public enum RiskLevel {
        LOW("Low", "Minimal risk, safe for auto-execution"),
        MEDIUM("Medium", "Moderate risk, requires review"),
        HIGH("High", "High risk, requires approval"),
        CRITICAL("Critical", "Critical risk, manual execution only");

        private final String displayName;
        private final String description;

        RiskLevel(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public boolean allowsAutoExecution() {
            return this == LOW;
        }
    }
}