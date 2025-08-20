package com.smartfarm.service;

import com.smartfarm.entity.FarmingCycle;
import com.smartfarm.entity.Room;
import com.smartfarm.repository.FarmingCycleRepository;
import com.smartfarm.repository.RoomRepository;
import com.smartfarm.exception.ResourceNotFoundException;
import com.smartfarm.exception.UnauthorizedException;
import com.smartfarm.exception.ValidationException;
import com.smartfarm.dto.FarmingCycleDto;
import com.smartfarm.dto.FarmingCycleCreateDto;
import com.smartfarm.enums.CycleStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * Service class for FarmingCycle entity operations.
 * Handles business logic for farming cycles, batch management, and harvest tracking.
 */
@Service
@Transactional
public class FarmingCycleService {

    private static final Logger logger = LoggerFactory.getLogger(FarmingCycleService.class);

    private final FarmingCycleRepository farmingCycleRepository;
    private final RoomRepository roomRepository;
    private final RoomService roomService;

    @Autowired
    public FarmingCycleService(FarmingCycleRepository farmingCycleRepository, RoomRepository roomRepository,
                              RoomService roomService) {
        this.farmingCycleRepository = farmingCycleRepository;
        this.roomRepository = roomRepository;
        this.roomService = roomService;
    }

    /**
     * Create a new farming cycle.
     *
     * @param roomId the room ID
     * @param createDto the farming cycle creation data
     * @param userId the user ID creating the cycle
     * @return the created farming cycle
     * @throws ResourceNotFoundException if room not found
     * @throws UnauthorizedException if user doesn't have access to room
     * @throws ValidationException if cycle data is invalid
     */
    public FarmingCycleDto createFarmingCycle(UUID roomId, FarmingCycleCreateDto createDto, UUID userId) {
        logger.debug("Creating farming cycle for room: {} by user: {}", roomId, userId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        validateFarmingCycle(createDto);

        // Check if there's already an active cycle in the room
        Optional<FarmingCycle> activeCycle = farmingCycleRepository.findActiveByRoom(roomId);
        if (activeCycle.isPresent()) {
            throw new ValidationException("Room already has an active farming cycle. Complete or abort the current cycle first.");
        }

        FarmingCycle cycle = new FarmingCycle();
        cycle.setRoom(room);
        cycle.setStartDate(createDto.getStartDate());
        cycle.setExpectedHarvestDate(createDto.getExpectedHarvestDate());
        cycle.setStatus(CycleStatus.GROWING);
        cycle.setMushroomVariety(createDto.getMushroomVariety());
        cycle.setNotes(createDto.getNotes());
        cycle.setCreatedAt(LocalDateTime.now());

        FarmingCycle savedCycle = farmingCycleRepository.save(cycle);
        logger.debug("Farming cycle created with ID: {}", savedCycle.getCycleId());

        return convertToDto(savedCycle);
    }

    /**
     * Get farming cycle by ID.
     *
     * @param cycleId the cycle ID
     * @param userId the user ID (for access control)
     * @return the farming cycle
     * @throws ResourceNotFoundException if cycle not found
     * @throws UnauthorizedException if user doesn't have access
     */
    @Transactional(readOnly = true)
    public FarmingCycleDto getFarmingCycle(UUID cycleId, UUID userId) {
        logger.debug("Fetching farming cycle: {} by user: {}", cycleId, userId);

        FarmingCycle cycle = farmingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Farming cycle not found with ID: " + cycleId));

        if (!roomService.hasAccessToRoom(cycle.getRoom().getRoomId(), userId)) {
            throw new UnauthorizedException("User does not have access to farming cycle: " + cycleId);
        }

        return convertToDto(cycle);
    }

    /**
     * Update a farming cycle.
     *
     * @param cycleId the cycle ID
     * @param createDto the updated cycle data
     * @param userId the user ID updating the cycle
     * @return the updated farming cycle
     * @throws ResourceNotFoundException if cycle not found
     * @throws UnauthorizedException if user doesn't have access
     * @throws ValidationException if cycle data is invalid
     */
    public FarmingCycleDto updateFarmingCycle(UUID cycleId, FarmingCycleCreateDto createDto, UUID userId) {
        logger.debug("Updating farming cycle: {} by user: {}", cycleId, userId);

        FarmingCycle cycle = farmingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Farming cycle not found with ID: " + cycleId));

        if (!roomService.hasAccessToRoom(cycle.getRoom().getRoomId(), userId)) {
            throw new UnauthorizedException("User does not have access to farming cycle: " + cycleId);
        }

        validateFarmingCycle(createDto);

        cycle.setStartDate(createDto.getStartDate());
        cycle.setExpectedHarvestDate(createDto.getExpectedHarvestDate());
        cycle.setMushroomVariety(createDto.getMushroomVariety());
        cycle.setNotes(createDto.getNotes());

        FarmingCycle savedCycle = farmingCycleRepository.save(cycle);
        logger.debug("Farming cycle updated: {}", cycleId);

        return convertToDto(savedCycle);
    }

    /**
     * Delete a farming cycle.
     *
     * @param cycleId the cycle ID
     * @param userId the user ID deleting the cycle
     * @throws ResourceNotFoundException if cycle not found
     * @throws UnauthorizedException if user doesn't have access
     * @throws ValidationException if cycle cannot be deleted
     */
    public void deleteFarmingCycle(UUID cycleId, UUID userId) {
        logger.debug("Deleting farming cycle: {} by user: {}", cycleId, userId);

        FarmingCycle cycle = farmingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Farming cycle not found with ID: " + cycleId));

        if (!roomService.hasAccessToRoom(cycle.getRoom().getRoomId(), userId)) {
            throw new UnauthorizedException("User does not have access to farming cycle: " + cycleId);
        }

        if (cycle.getStatus() == CycleStatus.HARVESTED) {
            throw new ValidationException("Cannot delete a harvested farming cycle");
        }

        farmingCycleRepository.delete(cycle);
        logger.debug("Farming cycle deleted: {}", cycleId);
    }

    /**
     * Get farming cycles for a room.
     *
     * @param roomId the room ID
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of farming cycles
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public Page<FarmingCycleDto> getRoomFarmingCycles(UUID roomId, UUID userId, Pageable pageable) {
        logger.debug("Fetching farming cycles for room: {} by user: {}", roomId, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        return farmingCycleRepository.findByRoomOrderByStartDateDesc(roomId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get farming cycles by status.
     *
     * @param roomId the room ID
     * @param status the cycle status
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of farming cycles with the specified status
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public Page<FarmingCycleDto> getCyclesByStatus(UUID roomId, CycleStatus status, UUID userId, Pageable pageable) {
        logger.debug("Fetching farming cycles with status {} for room: {} by user: {}", status, roomId, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        return farmingCycleRepository.findByRoomAndStatusOrderByStartDateDesc(roomId, status, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get active farming cycle for a room.
     *
     * @param roomId the room ID
     * @param userId the user ID (for access control)
     * @return the active farming cycle or null if none
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public FarmingCycleDto getActiveCycle(UUID roomId, UUID userId) {
        logger.debug("Fetching active farming cycle for room: {} by user: {}", roomId, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        Optional<FarmingCycle> activeCycle = farmingCycleRepository.findActiveByRoom(roomId);
        return activeCycle.map(this::convertToDto).orElse(null);
    }

    /**
     * Get farming cycles by date range.
     *
     * @param roomId the room ID
     * @param startDate the start date
     * @param endDate the end date
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of farming cycles in the date range
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public Page<FarmingCycleDto> getCyclesByDateRange(UUID roomId, LocalDate startDate, LocalDate endDate, 
                                                     UUID userId, Pageable pageable) {
        logger.debug("Fetching farming cycles for room: {} from {} to {} by user: {}", roomId, startDate, endDate, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        return farmingCycleRepository.findByRoomAndDateRange(roomId, startDate, endDate, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get farming cycles by mushroom variety.
     *
     * @param roomId the room ID
     * @param variety the mushroom variety
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of farming cycles with the specified variety
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public Page<FarmingCycleDto> getCyclesByVariety(UUID roomId, String variety, UUID userId, Pageable pageable) {
        logger.debug("Fetching farming cycles with variety {} for room: {} by user: {}", variety, roomId, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        return farmingCycleRepository.findByRoomAndVariety(roomId, variety, pageable)
                .map(this::convertToDto);
    }

    /**
     * Complete a farming cycle (mark as harvested).
     *
     * @param cycleId the cycle ID
     * @param harvestDate the harvest date
     * @param harvestNotes optional harvest notes
     * @param userId the user ID completing the cycle
     * @return the completed farming cycle
     * @throws ResourceNotFoundException if cycle not found
     * @throws UnauthorizedException if user doesn't have access
     * @throws ValidationException if cycle cannot be completed
     */
    public FarmingCycleDto completeCycle(UUID cycleId, LocalDate harvestDate, String harvestNotes, UUID userId) {
        logger.debug("Completing farming cycle: {} by user: {}", cycleId, userId);

        FarmingCycle cycle = farmingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Farming cycle not found with ID: " + cycleId));

        if (!roomService.hasAccessToRoom(cycle.getRoom().getRoomId(), userId)) {
            throw new UnauthorizedException("User does not have access to farming cycle: " + cycleId);
        }

        if (cycle.getStatus() != CycleStatus.GROWING) {
            throw new ValidationException("Only growing cycles can be completed");
        }

        if (harvestDate.isBefore(cycle.getStartDate())) {
            throw new ValidationException("Harvest date cannot be before start date");
        }

        cycle.setStatus(CycleStatus.HARVESTED);
        cycle.setActualHarvestDate(harvestDate);
        if (StringUtils.hasText(harvestNotes)) {
            String updatedNotes = cycle.getNotes() != null ? 
                cycle.getNotes() + "\n\nHarvest Notes: " + harvestNotes : 
                "Harvest Notes: " + harvestNotes;
            cycle.setNotes(updatedNotes);
        }

        FarmingCycle savedCycle = farmingCycleRepository.save(cycle);
        logger.info("Farming cycle completed: {}", cycleId);

        return convertToDto(savedCycle);
    }

    /**
     * Abort a farming cycle.
     *
     * @param cycleId the cycle ID
     * @param abortReason the reason for aborting
     * @param userId the user ID aborting the cycle
     * @return the aborted farming cycle
     * @throws ResourceNotFoundException if cycle not found
     * @throws UnauthorizedException if user doesn't have access
     * @throws ValidationException if cycle cannot be aborted
     */
    public FarmingCycleDto abortCycle(UUID cycleId, String abortReason, UUID userId) {
        logger.debug("Aborting farming cycle: {} by user: {}", cycleId, userId);

        FarmingCycle cycle = farmingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Farming cycle not found with ID: " + cycleId));

        if (!roomService.hasAccessToRoom(cycle.getRoom().getRoomId(), userId)) {
            throw new UnauthorizedException("User does not have access to farming cycle: " + cycleId);
        }

        if (cycle.getStatus() != CycleStatus.GROWING) {
            throw new ValidationException("Only growing cycles can be aborted");
        }

        cycle.setStatus(CycleStatus.ABORTED);
        if (StringUtils.hasText(abortReason)) {
            String updatedNotes = cycle.getNotes() != null ? 
                cycle.getNotes() + "\n\nAborted: " + abortReason : 
                "Aborted: " + abortReason;
            cycle.setNotes(updatedNotes);
        }

        FarmingCycle savedCycle = farmingCycleRepository.save(cycle);
        logger.info("Farming cycle aborted: {}", cycleId);

        return convertToDto(savedCycle);
    }

    /**
     * Get farming cycle statistics for a room.
     *
     * @param roomId the room ID
     * @param startDate the start date
     * @param endDate the end date
     * @param userId the user ID (for access control)
     * @return farming cycle statistics
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public FarmingCycleStatistics getCycleStatistics(UUID roomId, LocalDate startDate, LocalDate endDate, UUID userId) {
        logger.debug("Fetching farming cycle statistics for room: {} from {} to {} by user: {}", roomId, startDate, endDate, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        var stats = farmingCycleRepository.getFarmingCycleStatistics(roomId, startDate, endDate)
                .orElse(new FarmingCycleRepository.FarmingCycleStatistics() {
                    @Override public Long getTotalCycles() { return 0L; }
                    @Override public Long getCompletedCycles() { return 0L; }
                    @Override public Long getAbortedCycles() { return 0L; }
                    @Override public Long getActiveCycles() { return 0L; }
                    @Override public Double getAverageCycleDays() { return 0.0; }
                    @Override public Double getSuccessRate() { return 0.0; }
                });

        return new FarmingCycleStatistics(
            stats.getTotalCycles(),
            stats.getCompletedCycles(),
            stats.getAbortedCycles(),
            stats.getActiveCycles(),
            stats.getAverageCycleDays(),
            stats.getSuccessRate()
        );
    }

    /**
     * Get cycles due for harvest (within specified days).
     *
     * @param roomId the room ID
     * @param daysAhead number of days to look ahead
     * @param userId the user ID (for access control)
     * @return list of cycles due for harvest
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public List<FarmingCycleDto> getCyclesDueForHarvest(UUID roomId, int daysAhead, UUID userId) {
        logger.debug("Fetching cycles due for harvest in {} days for room: {} by user: {}", daysAhead, roomId, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        LocalDate cutoffDate = LocalDate.now().plusDays(daysAhead);
        List<FarmingCycle> dueForHarvest = farmingCycleRepository.findCyclesDueForHarvest(roomId, cutoffDate);
        
        return dueForHarvest.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     * Get overdue cycles (past expected harvest date).
     *
     * @param roomId the room ID
     * @param userId the user ID (for access control)
     * @return list of overdue cycles
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public List<FarmingCycleDto> getOverdueCycles(UUID roomId, UUID userId) {
        logger.debug("Fetching overdue cycles for room: {} by user: {}", roomId, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        List<FarmingCycle> overdueCycles = farmingCycleRepository.findOverdueCycles(roomId, LocalDate.now());
        
        return overdueCycles.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     * Validate farming cycle data.
     *
     * @param createDto the farming cycle creation data
     * @throws ValidationException if cycle data is invalid
     */
    private void validateFarmingCycle(FarmingCycleCreateDto createDto) {
        if (createDto.getStartDate() == null) {
            throw new ValidationException("Start date cannot be null");
        }

        if (createDto.getExpectedHarvestDate() == null) {
            throw new ValidationException("Expected harvest date cannot be null");
        }

        if (createDto.getExpectedHarvestDate().isBefore(createDto.getStartDate())) {
            throw new ValidationException("Expected harvest date cannot be before start date");
        }

        if (!StringUtils.hasText(createDto.getMushroomVariety())) {
            throw new ValidationException("Mushroom variety cannot be empty");
        }

        // Validate start date is not too far in the past
        LocalDate earliestAllowed = LocalDate.now().minusYears(1);
        if (createDto.getStartDate().isBefore(earliestAllowed)) {
            throw new ValidationException("Start date cannot be more than 1 year in the past");
        }

        // Validate expected harvest date is not too far in the future
        LocalDate latestAllowed = LocalDate.now().plusYears(1);
        if (createDto.getExpectedHarvestDate().isAfter(latestAllowed)) {
            throw new ValidationException("Expected harvest date cannot be more than 1 year in the future");
        }
    }

    /**
     * Convert FarmingCycle entity to FarmingCycleDto.
     *
     * @param cycle the farming cycle entity
     * @return the farming cycle DTO
     */
    private FarmingCycleDto convertToDto(FarmingCycle cycle) {
        FarmingCycleDto dto = new FarmingCycleDto();
        dto.setCycleId(cycle.getCycleId());
        dto.setRoomId(cycle.getRoom().getRoomId());
        dto.setRoomName(cycle.getRoom().getName());
        dto.setStartDate(cycle.getStartDate());
        dto.setExpectedHarvestDate(cycle.getExpectedHarvestDate());
        dto.setActualHarvestDate(cycle.getActualHarvestDate());
        dto.setStatus(cycle.getStatus());
        dto.setMushroomVariety(cycle.getMushroomVariety());
        dto.setNotes(cycle.getNotes());
        dto.setCreatedAt(cycle.getCreatedAt());
        
        // Calculate cycle duration if completed
        if (cycle.getActualHarvestDate() != null) {
            dto.setActualDurationDays(
                (int) java.time.temporal.ChronoUnit.DAYS.between(cycle.getStartDate(), cycle.getActualHarvestDate())
            );
        }
        
        // Calculate expected duration
        dto.setExpectedDurationDays(
            (int) java.time.temporal.ChronoUnit.DAYS.between(cycle.getStartDate(), cycle.getExpectedHarvestDate())
        );
        
        return dto;
    }

    /**
     * Farming cycle statistics data class.
     */
    public static class FarmingCycleStatistics {
        private final long totalCycles;
        private final long completedCycles;
        private final long abortedCycles;
        private final long activeCycles;
        private final double averageCycleDays;
        private final double successRate;

        public FarmingCycleStatistics(long totalCycles, long completedCycles, long abortedCycles,
                                     long activeCycles, double averageCycleDays, double successRate) {
            this.totalCycles = totalCycles;
            this.completedCycles = completedCycles;
            this.abortedCycles = abortedCycles;
            this.activeCycles = activeCycles;
            this.averageCycleDays = averageCycleDays;
            this.successRate = successRate;
        }

        public long getTotalCycles() { return totalCycles; }
        public long getCompletedCycles() { return completedCycles; }
        public long getAbortedCycles() { return abortedCycles; }
        public long getActiveCycles() { return activeCycles; }
        public double getAverageCycleDays() { return averageCycleDays; }
        public double getSuccessRate() { return successRate; }
    }
}