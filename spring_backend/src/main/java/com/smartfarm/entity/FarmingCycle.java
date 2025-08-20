package com.smartfarm.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * FarmingCycle entity representing mushroom cultivation batches and their lifecycle.
 * Tracks cultivation stages, harvest dates, and production metrics for each room.
 */
@Entity
@Table(name = "farming_cycles", indexes = {
        @Index(name = "idx_farming_cycle_room", columnList = "room_id"),
        @Index(name = "idx_farming_cycle_status", columnList = "status"),
        @Index(name = "idx_farming_cycle_start_date", columnList = "start_date"),
        @Index(name = "idx_farming_cycle_harvest_date", columnList = "expected_harvest_date"),
        @Index(name = "idx_farming_cycle_variety", columnList = "mushroom_variety")
})
public class FarmingCycle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "cycle_id")
    private UUID cycleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "expected_harvest_date")
    private LocalDate expectedHarvestDate;

    @Column(name = "actual_harvest_date")
    private LocalDate actualHarvestDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CycleStatus status = CycleStatus.PLANNING;

    @Size(max = 200)
    @Column(name = "mushroom_variety")
    private String mushroomVariety;

    @Column(name = "substrate_type")
    private String substrateType;

    @Column(name = "inoculation_date")
    private LocalDate inoculationDate;

    @Column(name = "incubation_start_date")
    private LocalDate incubationStartDate;

    @Column(name = "fruiting_start_date")
    private LocalDate fruitingStartDate;

    @Column(name = "first_harvest_date")
    private LocalDate firstHarvestDate;

    @Column(name = "final_harvest_date")
    private LocalDate finalHarvestDate;

    // Production metrics
    @Column(name = "expected_yield_kg")
    private Float expectedYieldKg;

    @Column(name = "actual_yield_kg")
    private Float actualYieldKg;

    @Column(name = "substrate_weight_kg")
    private Float substrateWeightKg;

    @Column(name = "biological_efficiency")
    private Float biologicalEfficiency; // (yield / substrate weight) * 100

    @Column(name = "harvest_count")
    private Integer harvestCount = 0;

    // Environmental targets
    @Column(name = "target_temperature_c")
    private Float targetTemperatureC;

    @Column(name = "target_humidity_pct")
    private Float targetHumidityPct;

    @Column(name = "target_co2_ppm")
    private Float targetCo2Ppm;

    @Column(name = "target_light_hours")
    private Float targetLightHours;

    // Quality metrics
    @Column(name = "quality_grade")
    private String qualityGrade; // A, B, C grade

    @Column(name = "contamination_rate")
    private Float contaminationRate; // Percentage

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "managed_by")
    private User managedBy;

    // Relationships
    @OneToMany(mappedBy = "farmingCycle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<HarvestRecord> harvestRecords = new HashSet<>();

    @OneToMany(mappedBy = "farmingCycle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<CycleEvent> cycleEvents = new HashSet<>();

    // Constructors
    public FarmingCycle() {
        super();
    }

    public FarmingCycle(Room room, LocalDate startDate, String mushroomVariety, User createdBy) {
        this();
        this.room = room;
        this.startDate = startDate;
        this.mushroomVariety = mushroomVariety;
        this.createdBy = createdBy;
        this.managedBy = createdBy;
    }

    // Getters and Setters
    public UUID getCycleId() {
        return cycleId;
    }

    public void setCycleId(UUID cycleId) {
        this.cycleId = cycleId;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getExpectedHarvestDate() {
        return expectedHarvestDate;
    }

    public void setExpectedHarvestDate(LocalDate expectedHarvestDate) {
        this.expectedHarvestDate = expectedHarvestDate;
    }

    public LocalDate getActualHarvestDate() {
        return actualHarvestDate;
    }

    public void setActualHarvestDate(LocalDate actualHarvestDate) {
        this.actualHarvestDate = actualHarvestDate;
    }

    public CycleStatus getStatus() {
        return status;
    }

    public void setStatus(CycleStatus status) {
        this.status = status;
    }

    public String getMushroomVariety() {
        return mushroomVariety;
    }

    public void setMushroomVariety(String mushroomVariety) {
        this.mushroomVariety = mushroomVariety;
    }

    public String getSubstrateType() {
        return substrateType;
    }

    public void setSubstrateType(String substrateType) {
        this.substrateType = substrateType;
    }

    public LocalDate getInoculationDate() {
        return inoculationDate;
    }

    public void setInoculationDate(LocalDate inoculationDate) {
        this.inoculationDate = inoculationDate;
    }

    public LocalDate getIncubationStartDate() {
        return incubationStartDate;
    }

    public void setIncubationStartDate(LocalDate incubationStartDate) {
        this.incubationStartDate = incubationStartDate;
    }

    public LocalDate getFruitingStartDate() {
        return fruitingStartDate;
    }

    public void setFruitingStartDate(LocalDate fruitingStartDate) {
        this.fruitingStartDate = fruitingStartDate;
    }

    public LocalDate getFirstHarvestDate() {
        return firstHarvestDate;
    }

    public void setFirstHarvestDate(LocalDate firstHarvestDate) {
        this.firstHarvestDate = firstHarvestDate;
    }

    public LocalDate getFinalHarvestDate() {
        return finalHarvestDate;
    }

    public void setFinalHarvestDate(LocalDate finalHarvestDate) {
        this.finalHarvestDate = finalHarvestDate;
    }

    public Float getExpectedYieldKg() {
        return expectedYieldKg;
    }

    public void setExpectedYieldKg(Float expectedYieldKg) {
        this.expectedYieldKg = expectedYieldKg;
    }

    public Float getActualYieldKg() {
        return actualYieldKg;
    }

    public void setActualYieldKg(Float actualYieldKg) {
        this.actualYieldKg = actualYieldKg;
        calculateBiologicalEfficiency();
    }

    public Float getSubstrateWeightKg() {
        return substrateWeightKg;
    }

    public void setSubstrateWeightKg(Float substrateWeightKg) {
        this.substrateWeightKg = substrateWeightKg;
        calculateBiologicalEfficiency();
    }

    public Float getBiologicalEfficiency() {
        return biologicalEfficiency;
    }

    public void setBiologicalEfficiency(Float biologicalEfficiency) {
        this.biologicalEfficiency = biologicalEfficiency;
    }

    public Integer getHarvestCount() {
        return harvestCount;
    }

    public void setHarvestCount(Integer harvestCount) {
        this.harvestCount = harvestCount;
    }

    public Float getTargetTemperatureC() {
        return targetTemperatureC;
    }

    public void setTargetTemperatureC(Float targetTemperatureC) {
        this.targetTemperatureC = targetTemperatureC;
    }

    public Float getTargetHumidityPct() {
        return targetHumidityPct;
    }

    public void setTargetHumidityPct(Float targetHumidityPct) {
        this.targetHumidityPct = targetHumidityPct;
    }

    public Float getTargetCo2Ppm() {
        return targetCo2Ppm;
    }

    public void setTargetCo2Ppm(Float targetCo2Ppm) {
        this.targetCo2Ppm = targetCo2Ppm;
    }

    public Float getTargetLightHours() {
        return targetLightHours;
    }

    public void setTargetLightHours(Float targetLightHours) {
        this.targetLightHours = targetLightHours;
    }

    public String getQualityGrade() {
        return qualityGrade;
    }

    public void setQualityGrade(String qualityGrade) {
        this.qualityGrade = qualityGrade;
    }

    public Float getContaminationRate() {
        return contaminationRate;
    }

    public void setContaminationRate(Float contaminationRate) {
        this.contaminationRate = contaminationRate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public User getManagedBy() {
        return managedBy;
    }

    public void setManagedBy(User managedBy) {
        this.managedBy = managedBy;
    }

    public Set<HarvestRecord> getHarvestRecords() {
        return harvestRecords;
    }

    public void setHarvestRecords(Set<HarvestRecord> harvestRecords) {
        this.harvestRecords = harvestRecords;
    }

    public Set<CycleEvent> getCycleEvents() {
        return cycleEvents;
    }

    public void setCycleEvents(Set<CycleEvent> cycleEvents) {
        this.cycleEvents = cycleEvents;
    }

    // Utility methods
    public long getDaysInCycle() {
        LocalDate endDate = actualHarvestDate != null ? actualHarvestDate : LocalDate.now();
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    public long getDaysToHarvest() {
        if (expectedHarvestDate == null) {
            return -1;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), expectedHarvestDate);
    }

    public boolean isOverdue() {
        return expectedHarvestDate != null && 
               LocalDate.now().isAfter(expectedHarvestDate) && 
               !isCompleted();
    }

    public boolean isActive() {
        return status == CycleStatus.INOCULATION || 
               status == CycleStatus.INCUBATION || 
               status == CycleStatus.FRUITING || 
               status == CycleStatus.HARVESTING;
    }

    public boolean isCompleted() {
        return status == CycleStatus.COMPLETED;
    }

    public boolean isAborted() {
        return status == CycleStatus.ABORTED;
    }

    public void calculateBiologicalEfficiency() {
        if (actualYieldKg != null && substrateWeightKg != null && substrateWeightKg > 0) {
            this.biologicalEfficiency = (actualYieldKg / substrateWeightKg) * 100;
        }
    }

    public void startInoculation() {
        this.status = CycleStatus.INOCULATION;
        this.inoculationDate = LocalDate.now();
    }

    public void startIncubation() {
        this.status = CycleStatus.INCUBATION;
        this.incubationStartDate = LocalDate.now();
    }

    public void startFruiting() {
        this.status = CycleStatus.FRUITING;
        this.fruitingStartDate = LocalDate.now();
    }

    public void startHarvesting() {
        this.status = CycleStatus.HARVESTING;
        if (firstHarvestDate == null) {
            this.firstHarvestDate = LocalDate.now();
        }
    }

    public void complete() {
        this.status = CycleStatus.COMPLETED;
        this.actualHarvestDate = LocalDate.now();
        this.finalHarvestDate = LocalDate.now();
    }

    public void abort(String reason) {
        this.status = CycleStatus.ABORTED;
        this.notes = (notes != null ? notes + "\n" : "") + "Aborted: " + reason;
    }

    public void addHarvestRecord(HarvestRecord harvestRecord) {
        harvestRecords.add(harvestRecord);
        harvestRecord.setFarmingCycle(this);
        harvestCount++;
        
        // Update total yield
        if (actualYieldKg == null) {
            actualYieldKg = 0f;
        }
        actualYieldKg += harvestRecord.getWeightKg();
        calculateBiologicalEfficiency();
    }

    @Override
    public String toString() {
        return "FarmingCycle{" +
                "cycleId=" + cycleId +
                ", room=" + (room != null ? room.getName() : "null") +
                ", startDate=" + startDate +
                ", status=" + status +
                ", mushroomVariety='" + mushroomVariety + '\'' +
                ", expectedYieldKg=" + expectedYieldKg +
                ", actualYieldKg=" + actualYieldKg +
                ", biologicalEfficiency=" + biologicalEfficiency +
                ", harvestCount=" + harvestCount +
                ", createdAt=" + getCreatedAt() +
                '}';
    }

    /**
     * Enumeration for farming cycle status.
     */
    public enum CycleStatus {
        PLANNING("Planning", "Cycle is being planned and prepared"),
        INOCULATION("Inoculation", "Substrate is being inoculated with mushroom spawn"),
        INCUBATION("Incubation", "Mycelium is colonizing the substrate"),
        FRUITING("Fruiting", "Mushrooms are developing and growing"),
        HARVESTING("Harvesting", "Mushrooms are being harvested"),
        COMPLETED("Completed", "Cycle has been completed successfully"),
        ABORTED("Aborted", "Cycle was terminated due to contamination or other issues");

        private final String displayName;
        private final String description;

        CycleStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public boolean isActive() {
            return this == INOCULATION || this == INCUBATION || 
                   this == FRUITING || this == HARVESTING;
        }

        public boolean isTerminal() {
            return this == COMPLETED || this == ABORTED;
        }
    }
}