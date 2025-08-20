package com.smartfarm.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * HarvestRecord entity representing individual harvest events within farming cycles.
 * Tracks harvest details, quality metrics, and production data for each harvest.
 */
@Entity
@Table(name = "harvest_records", indexes = {
        @Index(name = "idx_harvest_cycle", columnList = "cycle_id"),
        @Index(name = "idx_harvest_date", columnList = "harvest_date"),
        @Index(name = "idx_harvest_room", columnList = "room_id"),
        @Index(name = "idx_harvest_harvested_by", columnList = "harvested_by")
})
public class HarvestRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "harvest_id")
    private UUID harvestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private FarmingCycle farmingCycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @NotNull
    @Column(name = "harvest_date", nullable = false)
    private LocalDateTime harvestDate;

    @NotNull
    @Positive
    @Column(name = "weight_kg", nullable = false)
    private Float weightKg;

    @Column(name = "quality_grade")
    private String qualityGrade; // A, B, C grade

    @Column(name = "mushroom_count")
    private Integer mushroomCount;

    @Column(name = "average_size_cm")
    private Float averageSizeCm;

    @Column(name = "moisture_content_pct")
    private Float moistureContentPct;

    @Column(name = "shelf_life_days")
    private Integer shelfLifeDays;

    @Column(name = "harvest_method")
    private String harvestMethod; // Manual, Semi-automated, Automated

    @Column(name = "storage_location")
    private String storageLocation;

    @Column(name = "packaging_type")
    private String packagingType;

    @Column(name = "batch_number")
    private String batchNumber;

    @Column(name = "market_price_per_kg")
    private Float marketPricePerKg;

    @Column(name = "estimated_revenue")
    private Float estimatedRevenue;

    @Column(name = "defect_rate_pct")
    private Float defectRatePct;

    @Column(name = "contamination_detected")
    private Boolean contaminationDetected = false;

    @Column(name = "contamination_type")
    private String contaminationType;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "harvested_by", nullable = false)
    private User harvestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quality_checked_by")
    private User qualityCheckedBy;

    @Column(name = "quality_check_date")
    private LocalDateTime qualityCheckDate;

    @Column(name = "processing_status")
    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus = ProcessingStatus.FRESH;

    @Column(name = "sold_date")
    private LocalDateTime soldDate;

    @Column(name = "actual_sale_price_per_kg")
    private Float actualSalePricePerKg;

    @Column(name = "customer_feedback")
    private String customerFeedback;

    // Environmental conditions at harvest
    @Column(name = "harvest_temperature_c")
    private Float harvestTemperatureC;

    @Column(name = "harvest_humidity_pct")
    private Float harvestHumidityPct;

    @Column(name = "harvest_co2_ppm")
    private Float harvestCo2Ppm;

    // Constructors
    public HarvestRecord() {
        super();
        this.harvestDate = LocalDateTime.now();
    }

    public HarvestRecord(FarmingCycle farmingCycle, Room room, Float weightKg, User harvestedBy) {
        this();
        this.farmingCycle = farmingCycle;
        this.room = room;
        this.weightKg = weightKg;
        this.harvestedBy = harvestedBy;
    }

    // Getters and Setters
    public UUID getHarvestId() {
        return harvestId;
    }

    public void setHarvestId(UUID harvestId) {
        this.harvestId = harvestId;
    }

    public FarmingCycle getFarmingCycle() {
        return farmingCycle;
    }

    public void setFarmingCycle(FarmingCycle farmingCycle) {
        this.farmingCycle = farmingCycle;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public LocalDateTime getHarvestDate() {
        return harvestDate;
    }

    public void setHarvestDate(LocalDateTime harvestDate) {
        this.harvestDate = harvestDate;
    }

    public Float getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(Float weightKg) {
        this.weightKg = weightKg;
        calculateEstimatedRevenue();
    }

    public String getQualityGrade() {
        return qualityGrade;
    }

    public void setQualityGrade(String qualityGrade) {
        this.qualityGrade = qualityGrade;
    }

    public Integer getMushroomCount() {
        return mushroomCount;
    }

    public void setMushroomCount(Integer mushroomCount) {
        this.mushroomCount = mushroomCount;
    }

    public Float getAverageSizeCm() {
        return averageSizeCm;
    }

    public void setAverageSizeCm(Float averageSizeCm) {
        this.averageSizeCm = averageSizeCm;
    }

    public Float getMoistureContentPct() {
        return moistureContentPct;
    }

    public void setMoistureContentPct(Float moistureContentPct) {
        this.moistureContentPct = moistureContentPct;
    }

    public Integer getShelfLifeDays() {
        return shelfLifeDays;
    }

    public void setShelfLifeDays(Integer shelfLifeDays) {
        this.shelfLifeDays = shelfLifeDays;
    }

    public String getHarvestMethod() {
        return harvestMethod;
    }

    public void setHarvestMethod(String harvestMethod) {
        this.harvestMethod = harvestMethod;
    }

    public String getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }

    public String getPackagingType() {
        return packagingType;
    }

    public void setPackagingType(String packagingType) {
        this.packagingType = packagingType;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public Float getMarketPricePerKg() {
        return marketPricePerKg;
    }

    public void setMarketPricePerKg(Float marketPricePerKg) {
        this.marketPricePerKg = marketPricePerKg;
        calculateEstimatedRevenue();
    }

    public Float getEstimatedRevenue() {
        return estimatedRevenue;
    }

    public void setEstimatedRevenue(Float estimatedRevenue) {
        this.estimatedRevenue = estimatedRevenue;
    }

    public Float getDefectRatePct() {
        return defectRatePct;
    }

    public void setDefectRatePct(Float defectRatePct) {
        this.defectRatePct = defectRatePct;
    }

    public Boolean getContaminationDetected() {
        return contaminationDetected;
    }

    public void setContaminationDetected(Boolean contaminationDetected) {
        this.contaminationDetected = contaminationDetected;
    }

    public String getContaminationType() {
        return contaminationType;
    }

    public void setContaminationType(String contaminationType) {
        this.contaminationType = contaminationType;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public User getHarvestedBy() {
        return harvestedBy;
    }

    public void setHarvestedBy(User harvestedBy) {
        this.harvestedBy = harvestedBy;
    }

    public User getQualityCheckedBy() {
        return qualityCheckedBy;
    }

    public void setQualityCheckedBy(User qualityCheckedBy) {
        this.qualityCheckedBy = qualityCheckedBy;
    }

    public LocalDateTime getQualityCheckDate() {
        return qualityCheckDate;
    }

    public void setQualityCheckDate(LocalDateTime qualityCheckDate) {
        this.qualityCheckDate = qualityCheckDate;
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public LocalDateTime getSoldDate() {
        return soldDate;
    }

    public void setSoldDate(LocalDateTime soldDate) {
        this.soldDate = soldDate;
    }

    public Float getActualSalePricePerKg() {
        return actualSalePricePerKg;
    }

    public void setActualSalePricePerKg(Float actualSalePricePerKg) {
        this.actualSalePricePerKg = actualSalePricePerKg;
    }

    public String getCustomerFeedback() {
        return customerFeedback;
    }

    public void setCustomerFeedback(String customerFeedback) {
        this.customerFeedback = customerFeedback;
    }

    public Float getHarvestTemperatureC() {
        return harvestTemperatureC;
    }

    public void setHarvestTemperatureC(Float harvestTemperatureC) {
        this.harvestTemperatureC = harvestTemperatureC;
    }

    public Float getHarvestHumidityPct() {
        return harvestHumidityPct;
    }

    public void setHarvestHumidityPct(Float harvestHumidityPct) {
        this.harvestHumidityPct = harvestHumidityPct;
    }

    public Float getHarvestCo2Ppm() {
        return harvestCo2Ppm;
    }

    public void setHarvestCo2Ppm(Float harvestCo2Ppm) {
        this.harvestCo2Ppm = harvestCo2Ppm;
    }

    // Utility methods
    public void calculateEstimatedRevenue() {
        if (weightKg != null && marketPricePerKg != null) {
            float grossRevenue = weightKg * marketPricePerKg;
            if (defectRatePct != null) {
                float defectAdjustment = grossRevenue * (defectRatePct / 100);
                this.estimatedRevenue = grossRevenue - defectAdjustment;
            } else {
                this.estimatedRevenue = grossRevenue;
            }
        }
    }

    public Float getActualRevenue() {
        if (actualSalePricePerKg != null && weightKg != null) {
            return actualSalePricePerKg * weightKg;
        }
        return estimatedRevenue;
    }

    public Float getProfitMargin() {
        Float actual = getActualRevenue();
        if (actual != null && estimatedRevenue != null && estimatedRevenue > 0) {
            return ((actual - estimatedRevenue) / estimatedRevenue) * 100;
        }
        return null;
    }

    public boolean isQualityChecked() {
        return qualityCheckedBy != null && qualityCheckDate != null;
    }

    public boolean isSold() {
        return soldDate != null;
    }

    public boolean hasContamination() {
        return contaminationDetected != null && contaminationDetected;
    }

    public void markAsSold(Float salePrice, LocalDateTime saleDate) {
        this.actualSalePricePerKg = salePrice;
        this.soldDate = saleDate;
        this.processingStatus = ProcessingStatus.SOLD;
    }

    public void performQualityCheck(User checker, String grade) {
        this.qualityCheckedBy = checker;
        this.qualityCheckDate = LocalDateTime.now();
        this.qualityGrade = grade;
    }

    public void reportContamination(String type, String notes) {
        this.contaminationDetected = true;
        this.contaminationType = type;
        this.notes = (this.notes != null ? this.notes + "\n" : "") + 
                    "Contamination detected: " + type + ". " + notes;
        this.processingStatus = ProcessingStatus.DISCARDED;
    }

    @Override
    public String toString() {
        return "HarvestRecord{" +
                "harvestId=" + harvestId +
                ", farmingCycle=" + (farmingCycle != null ? farmingCycle.getCycleId() : "null") +
                ", room=" + (room != null ? room.getName() : "null") +
                ", harvestDate=" + harvestDate +
                ", weightKg=" + weightKg +
                ", qualityGrade='" + qualityGrade + '\'' +
                ", mushroomCount=" + mushroomCount +
                ", processingStatus=" + processingStatus +
                ", harvestedBy=" + (harvestedBy != null ? harvestedBy.getFullName() : "null") +
                ", estimatedRevenue=" + estimatedRevenue +
                ", contaminationDetected=" + contaminationDetected +
                '}';
    }

    /**
     * Enumeration for harvest processing status.
     */
    public enum ProcessingStatus {
        FRESH("Fresh", "Freshly harvested, ready for processing"),
        PROCESSED("Processed", "Has been processed (cleaned, packaged, etc.)"),
        STORED("Stored", "In storage awaiting sale or further processing"),
        SOLD("Sold", "Has been sold to customer"),
        DISCARDED("Discarded", "Discarded due to quality issues or contamination"),
        EXPIRED("Expired", "Passed shelf life and no longer sellable");

        private final String displayName;
        private final String description;

        ProcessingStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public boolean isSellable() {
            return this == FRESH || this == PROCESSED || this == STORED;
        }

        public boolean isTerminal() {
            return this == SOLD || this == DISCARDED || this == EXPIRED;
        }
    }
}