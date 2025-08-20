package com.smartfarm.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CycleEvent entity representing important events and milestones during farming cycles.
 * Tracks activities, observations, and interventions throughout the cultivation process.
 */
@Entity
@Table(name = "cycle_events", indexes = {
        @Index(name = "idx_cycle_event_cycle", columnList = "cycle_id"),
        @Index(name = "idx_cycle_event_date", columnList = "event_date"),
        @Index(name = "idx_cycle_event_type", columnList = "event_type"),
        @Index(name = "idx_cycle_event_recorded_by", columnList = "recorded_by")
})
public class CycleEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id")
    private UUID eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private FarmingCycle farmingCycle;

    @NotNull
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @NotBlank
    @Size(max = 200)
    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private Severity severity = Severity.INFO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by", nullable = false)
    private User recordedBy;

    // Environmental data at time of event
    @Column(name = "temperature_c")
    private Float temperatureC;

    @Column(name = "humidity_pct")
    private Float humidityPct;

    @Column(name = "co2_ppm")
    private Float co2Ppm;

    @Column(name = "light_lux")
    private Float lightLux;

    // Event-specific data
    @Column(name = "substrate_moisture_pct")
    private Float substrateMoisturePct;

    @Column(name = "ph_level")
    private Float phLevel;

    @Column(name = "contamination_level")
    private String contaminationLevel;

    @Column(name = "growth_stage")
    private String growthStage;

    @Column(name = "intervention_taken")
    private String interventionTaken;

    @Column(name = "materials_used")
    private String materialsUsed;

    @Column(name = "cost_incurred")
    private Float costIncurred;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "follow_up_required")
    private Boolean followUpRequired = false;

    @Column(name = "follow_up_date")
    private LocalDateTime followUpDate;

    @Column(name = "follow_up_notes")
    private String followUpNotes;

    @Column(name = "photos_taken")
    private Integer photosTaken = 0;

    @Column(name = "photo_urls", columnDefinition = "jsonb")
    private String photoUrls; // JSON array of photo URLs

    @Column(name = "attachments", columnDefinition = "jsonb")
    private String attachments; // JSON array of attachment metadata

    @Column(name = "tags")
    private String tags; // Comma-separated tags for categorization

    @Column(name = "is_milestone")
    private Boolean isMilestone = false;

    @Column(name = "affects_timeline")
    private Boolean affectsTimeline = false;

    @Column(name = "timeline_impact_days")
    private Integer timelineImpactDays;

    // Constructors
    public CycleEvent() {
        super();
        this.eventDate = LocalDateTime.now();
    }

    public CycleEvent(FarmingCycle farmingCycle, EventType eventType, String title, User recordedBy) {
        this();
        this.farmingCycle = farmingCycle;
        this.eventType = eventType;
        this.title = title;
        this.recordedBy = recordedBy;
    }

    // Getters and Setters
    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public FarmingCycle getFarmingCycle() {
        return farmingCycle;
    }

    public void setFarmingCycle(FarmingCycle farmingCycle) {
        this.farmingCycle = farmingCycle;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
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

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public User getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(User recordedBy) {
        this.recordedBy = recordedBy;
    }

    public Float getTemperatureC() {
        return temperatureC;
    }

    public void setTemperatureC(Float temperatureC) {
        this.temperatureC = temperatureC;
    }

    public Float getHumidityPct() {
        return humidityPct;
    }

    public void setHumidityPct(Float humidityPct) {
        this.humidityPct = humidityPct;
    }

    public Float getCo2Ppm() {
        return co2Ppm;
    }

    public void setCo2Ppm(Float co2Ppm) {
        this.co2Ppm = co2Ppm;
    }

    public Float getLightLux() {
        return lightLux;
    }

    public void setLightLux(Float lightLux) {
        this.lightLux = lightLux;
    }

    public Float getSubstrateMoisturePct() {
        return substrateMoisturePct;
    }

    public void setSubstrateMoisturePct(Float substrateMoisturePct) {
        this.substrateMoisturePct = substrateMoisturePct;
    }

    public Float getPhLevel() {
        return phLevel;
    }

    public void setPhLevel(Float phLevel) {
        this.phLevel = phLevel;
    }

    public String getContaminationLevel() {
        return contaminationLevel;
    }

    public void setContaminationLevel(String contaminationLevel) {
        this.contaminationLevel = contaminationLevel;
    }

    public String getGrowthStage() {
        return growthStage;
    }

    public void setGrowthStage(String growthStage) {
        this.growthStage = growthStage;
    }

    public String getInterventionTaken() {
        return interventionTaken;
    }

    public void setInterventionTaken(String interventionTaken) {
        this.interventionTaken = interventionTaken;
    }

    public String getMaterialsUsed() {
        return materialsUsed;
    }

    public void setMaterialsUsed(String materialsUsed) {
        this.materialsUsed = materialsUsed;
    }

    public Float getCostIncurred() {
        return costIncurred;
    }

    public void setCostIncurred(Float costIncurred) {
        this.costIncurred = costIncurred;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Boolean getFollowUpRequired() {
        return followUpRequired;
    }

    public void setFollowUpRequired(Boolean followUpRequired) {
        this.followUpRequired = followUpRequired;
    }

    public LocalDateTime getFollowUpDate() {
        return followUpDate;
    }

    public void setFollowUpDate(LocalDateTime followUpDate) {
        this.followUpDate = followUpDate;
    }

    public String getFollowUpNotes() {
        return followUpNotes;
    }

    public void setFollowUpNotes(String followUpNotes) {
        this.followUpNotes = followUpNotes;
    }

    public Integer getPhotosTaken() {
        return photosTaken;
    }

    public void setPhotosTaken(Integer photosTaken) {
        this.photosTaken = photosTaken;
    }

    public String getPhotoUrls() {
        return photoUrls;
    }

    public void setPhotoUrls(String photoUrls) {
        this.photoUrls = photoUrls;
    }

    public String getAttachments() {
        return attachments;
    }

    public void setAttachments(String attachments) {
        this.attachments = attachments;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Boolean getIsMilestone() {
        return isMilestone;
    }

    public void setIsMilestone(Boolean isMilestone) {
        this.isMilestone = isMilestone;
    }

    public Boolean getAffectsTimeline() {
        return affectsTimeline;
    }

    public void setAffectsTimeline(Boolean affectsTimeline) {
        this.affectsTimeline = affectsTimeline;
    }

    public Integer getTimelineImpactDays() {
        return timelineImpactDays;
    }

    public void setTimelineImpactDays(Integer timelineImpactDays) {
        this.timelineImpactDays = timelineImpactDays;
    }

    // Utility methods
    public boolean isMilestone() {
        return isMilestone != null && isMilestone;
    }

    public boolean requiresFollowUp() {
        return followUpRequired != null && followUpRequired;
    }

    public boolean isOverdueForFollowUp() {
        return requiresFollowUp() && followUpDate != null && 
               LocalDateTime.now().isAfter(followUpDate);
    }

    public boolean hasPhotos() {
        return photosTaken != null && photosTaken > 0;
    }

    public boolean hasAttachments() {
        return attachments != null && !attachments.trim().isEmpty();
    }

    public boolean affectsTimeline() {
        return affectsTimeline != null && affectsTimeline;
    }

    public void markAsCompleted() {
        this.followUpRequired = false;
        this.followUpDate = null;
    }

    public void scheduleFollowUp(LocalDateTime followUpDate, String notes) {
        this.followUpRequired = true;
        this.followUpDate = followUpDate;
        this.followUpNotes = notes;
    }

    public void addPhoto(String photoUrl) {
        if (photosTaken == null) {
            photosTaken = 0;
        }
        photosTaken++;
        
        // Add to JSON array (simplified - in real implementation, use proper JSON handling)
        if (photoUrls == null || photoUrls.trim().isEmpty()) {
            photoUrls = "[\"" + photoUrl + "\"]";
        } else {
            // Insert before the closing bracket
            photoUrls = photoUrls.substring(0, photoUrls.length() - 1) + ",\"" + photoUrl + "\"]";
        }
    }

    @Override
    public String toString() {
        return "CycleEvent{" +
                "eventId=" + eventId +
                ", farmingCycle=" + (farmingCycle != null ? farmingCycle.getCycleId() : "null") +
                ", eventDate=" + eventDate +
                ", eventType=" + eventType +
                ", title='" + title + '\'' +
                ", severity=" + severity +
                ", recordedBy=" + (recordedBy != null ? recordedBy.getFullName() : "null") +
                ", isMilestone=" + isMilestone +
                ", followUpRequired=" + followUpRequired +
                ", affectsTimeline=" + affectsTimeline +
                '}';
    }

    /**
     * Enumeration for cycle event types.
     */
    public enum EventType {
        INOCULATION("Inoculation", "Substrate inoculation with mushroom spawn"),
        INCUBATION_START("Incubation Start", "Beginning of incubation period"),
        MYCELIUM_GROWTH("Mycelium Growth", "Mycelium colonization progress"),
        CONTAMINATION("Contamination", "Contamination detected"),
        ENVIRONMENTAL_ADJUSTMENT("Environmental Adjustment", "Temperature, humidity, or other environmental changes"),
        FRUITING_START("Fruiting Start", "Beginning of fruiting period"),
        PINNING("Pinning", "Pin formation observed"),
        HARVEST("Harvest", "Mushroom harvest event"),
        MAINTENANCE("Maintenance", "Equipment or facility maintenance"),
        INSPECTION("Inspection", "Routine or special inspection"),
        TREATMENT("Treatment", "Application of treatments or interventions"),
        MILESTONE("Milestone", "Important milestone reached"),
        ISSUE("Issue", "Problem or issue encountered"),
        OBSERVATION("Observation", "General observation or note"),
        COMPLETION("Completion", "Cycle completion"),
        ABORT("Abort", "Cycle termination");

        private final String displayName;
        private final String description;

        EventType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public boolean isCritical() {
            return this == CONTAMINATION || this == ISSUE || this == ABORT;
        }

        public boolean isMilestone() {
            return this == INOCULATION || this == INCUBATION_START || 
                   this == FRUITING_START || this == HARVEST || 
                   this == COMPLETION || this == MILESTONE;
        }
    }

    /**
     * Enumeration for event severity levels.
     */
    public enum Severity {
        INFO("Info", "Informational event"),
        LOW("Low", "Low priority event"),
        MEDIUM("Medium", "Medium priority event"),
        HIGH("High", "High priority event"),
        CRITICAL("Critical", "Critical event requiring immediate attention");

        private final String displayName;
        private final String description;

        Severity(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public boolean requiresAttention() {
            return this == HIGH || this == CRITICAL;
        }
    }
}