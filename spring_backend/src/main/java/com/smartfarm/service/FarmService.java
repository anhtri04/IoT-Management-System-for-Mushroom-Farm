package com.smartfarm.service;

import com.smartfarm.entity.Farm;
import com.smartfarm.entity.User;
import com.smartfarm.repository.FarmRepository;
import com.smartfarm.repository.UserRepository;
import com.smartfarm.exception.ResourceNotFoundException;
import com.smartfarm.exception.UnauthorizedException;
import com.smartfarm.dto.FarmDto;
import com.smartfarm.dto.FarmCreateDto;
import com.smartfarm.dto.FarmUpdateDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for Farm entity operations.
 * Handles business logic for farm management, ownership, and access control.
 */
@Service
@Transactional
public class FarmService {

    private static final Logger logger = LoggerFactory.getLogger(FarmService.class);

    private final FarmRepository farmRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Autowired
    public FarmService(FarmRepository farmRepository, UserRepository userRepository, UserService userService) {
        this.farmRepository = farmRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    /**
     * Create a new farm.
     *
     * @param createDto the farm creation data
     * @param ownerId the owner user ID
     * @return the created farm
     * @throws ResourceNotFoundException if owner not found
     */
    public FarmDto createFarm(FarmCreateDto createDto, UUID ownerId) {
        logger.info("Creating new farm: {} for owner: {}", createDto.getName(), ownerId);

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found with ID: " + ownerId));

        Farm farm = new Farm();
        farm.setName(createDto.getName());
        farm.setLocation(createDto.getLocation());
        farm.setOwner(owner);
        farm.setCreatedAt(LocalDateTime.now());

        Farm savedFarm = farmRepository.save(farm);
        logger.info("Farm created successfully with ID: {}", savedFarm.getFarmId());

        return convertToDto(savedFarm);
    }

    /**
     * Create a new farm for the current authenticated user.
     *
     * @param createDto the farm creation data
     * @return the created farm
     */
    public FarmDto createFarmForCurrentUser(FarmCreateDto createDto) {
        var currentUser = userService.getCurrentUser();
        return createFarm(createDto, currentUser.getUserId());
    }

    /**
     * Get farm by ID.
     *
     * @param farmId the farm ID
     * @return the farm
     * @throws ResourceNotFoundException if farm not found
     */
    @Transactional(readOnly = true)
    public FarmDto getFarmById(UUID farmId) {
        logger.debug("Fetching farm by ID: {}", farmId);
        
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with ID: " + farmId));
        
        return convertToDto(farm);
    }

    /**
     * Get farm by ID with access control.
     *
     * @param farmId the farm ID
     * @param userId the user ID requesting access
     * @return the farm
     * @throws ResourceNotFoundException if farm not found
     * @throws UnauthorizedException if user doesn't have access
     */
    @Transactional(readOnly = true)
    public FarmDto getFarmByIdWithAccess(UUID farmId, UUID userId) {
        logger.debug("Fetching farm by ID: {} for user: {}", farmId, userId);
        
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with ID: " + farmId));
        
        if (!hasAccessToFarm(farm, userId)) {
            throw new UnauthorizedException("User does not have access to farm: " + farmId);
        }
        
        return convertToDto(farm);
    }

    /**
     * Update farm information.
     *
     * @param farmId the farm ID
     * @param updateDto the update data
     * @param userId the user ID making the update
     * @return the updated farm
     * @throws ResourceNotFoundException if farm not found
     * @throws UnauthorizedException if user is not the owner
     */
    public FarmDto updateFarm(UUID farmId, FarmUpdateDto updateDto, UUID userId) {
        logger.info("Updating farm with ID: {} by user: {}", farmId, userId);
        
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with ID: " + farmId));

        // Only owner can update farm
        if (!farm.getOwner().getUserId().equals(userId)) {
            throw new UnauthorizedException("Only farm owner can update farm details");
        }

        if (updateDto.getName() != null) {
            farm.setName(updateDto.getName());
        }

        if (updateDto.getLocation() != null) {
            farm.setLocation(updateDto.getLocation());
        }

        Farm updatedFarm = farmRepository.save(farm);
        logger.info("Farm updated successfully with ID: {}", updatedFarm.getFarmId());

        return convertToDto(updatedFarm);
    }

    /**
     * Delete farm by ID.
     *
     * @param farmId the farm ID
     * @param userId the user ID making the deletion
     * @throws ResourceNotFoundException if farm not found
     * @throws UnauthorizedException if user is not the owner
     */
    public void deleteFarm(UUID farmId, UUID userId) {
        logger.info("Deleting farm with ID: {} by user: {}", farmId, userId);
        
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with ID: " + farmId));

        // Only owner can delete farm
        if (!farm.getOwner().getUserId().equals(userId)) {
            throw new UnauthorizedException("Only farm owner can delete farm");
        }

        farmRepository.deleteById(farmId);
        logger.info("Farm deleted successfully with ID: {}", farmId);
    }

    /**
     * Get farms owned by a user.
     *
     * @param ownerId the owner user ID
     * @param pageable pagination information
     * @return page of farms owned by the user
     */
    @Transactional(readOnly = true)
    public Page<FarmDto> getFarmsByOwner(UUID ownerId, Pageable pageable) {
        logger.debug("Fetching farms owned by user: {}", ownerId);
        
        return farmRepository.findByOwnerUserId(ownerId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get farms accessible by a user (owned or has room access).
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of farms accessible by the user
     */
    @Transactional(readOnly = true)
    public Page<FarmDto> getFarmsAccessibleByUser(UUID userId, Pageable pageable) {
        logger.debug("Fetching farms accessible by user: {}", userId);
        
        return farmRepository.findFarmsAccessibleByUser(userId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Search farms by name or location.
     *
     * @param searchTerm the search term
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of matching farms
     */
    @Transactional(readOnly = true)
    public Page<FarmDto> searchFarms(String searchTerm, UUID userId, Pageable pageable) {
        logger.debug("Searching farms with term: {} for user: {}", searchTerm, userId);
        
        return farmRepository.searchFarmsAccessibleByUser(searchTerm, userId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get farms created within a date range.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of farms created within the date range
     */
    @Transactional(readOnly = true)
    public Page<FarmDto> getFarmsByDateRange(LocalDateTime startDate, LocalDateTime endDate, UUID userId, Pageable pageable) {
        logger.debug("Fetching farms created between {} and {} for user: {}", startDate, endDate, userId);
        
        return farmRepository.findFarmsCreatedBetweenAccessibleByUser(startDate, endDate, userId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get farm statistics for a user.
     *
     * @param userId the user ID
     * @return farm statistics
     */
    @Transactional(readOnly = true)
    public FarmStatistics getFarmStatisticsForUser(UUID userId) {
        logger.debug("Fetching farm statistics for user: {}", userId);
        
        long ownedFarms = farmRepository.countByOwnerUserId(userId);
        long accessibleFarms = farmRepository.countFarmsAccessibleByUser(userId);
        
        var farmStats = farmRepository.getFarmStatisticsForUser(userId);
        
        return new FarmStatistics(
            ownedFarms,
            accessibleFarms,
            farmStats.map(FarmRepository.FarmStatistics::getTotalRooms).orElse(0L),
            farmStats.map(FarmRepository.FarmStatistics::getTotalDevices).orElse(0L),
            farmStats.map(FarmRepository.FarmStatistics::getOnlineDevices).orElse(0L),
            farmStats.map(FarmRepository.FarmStatistics::getActiveAlerts).orElse(0L)
        );
    }

    /**
     * Get farm statistics by farm ID.
     *
     * @param farmId the farm ID
     * @param userId the user ID (for access control)
     * @return farm statistics
     * @throws ResourceNotFoundException if farm not found
     * @throws UnauthorizedException if user doesn't have access
     */
    @Transactional(readOnly = true)
    public FarmDetailStatistics getFarmStatistics(UUID farmId, UUID userId) {
        logger.debug("Fetching statistics for farm: {} by user: {}", farmId, userId);
        
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with ID: " + farmId));
        
        if (!hasAccessToFarm(farm, userId)) {
            throw new UnauthorizedException("User does not have access to farm: " + farmId);
        }
        
        var stats = farmRepository.getFarmStatistics(farmId)
                .orElse(new FarmRepository.FarmStatistics() {
                    @Override public Long getTotalRooms() { return 0L; }
                    @Override public Long getTotalDevices() { return 0L; }
                    @Override public Long getOnlineDevices() { return 0L; }
                    @Override public Long getActiveAlerts() { return 0L; }
                });
        
        return new FarmDetailStatistics(
            stats.getTotalRooms(),
            stats.getTotalDevices(),
            stats.getOnlineDevices(),
            stats.getActiveAlerts()
        );
    }

    /**
     * Get nearby farms within a radius.
     *
     * @param latitude the latitude
     * @param longitude the longitude
     * @param radiusKm the radius in kilometers
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of nearby farms
     */
    @Transactional(readOnly = true)
    public Page<FarmDto> getNearbyFarms(Double latitude, Double longitude, Double radiusKm, UUID userId, Pageable pageable) {
        logger.debug("Fetching farms near {}, {} within {} km for user: {}", latitude, longitude, radiusKm, userId);
        
        return farmRepository.findNearbyFarmsAccessibleByUser(latitude, longitude, radiusKm, userId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Check if user has access to a farm.
     *
     * @param farm the farm
     * @param userId the user ID
     * @return true if user has access, false otherwise
     */
    private boolean hasAccessToFarm(Farm farm, UUID userId) {
        // User has access if they are the owner or have access to any room in the farm
        if (farm.getOwner().getUserId().equals(userId)) {
            return true;
        }
        
        // Check if user has access to any room in the farm
        return farmRepository.userHasAccessToFarm(userId, farm.getFarmId());
    }

    /**
     * Check if user has access to a farm by farm ID.
     *
     * @param farmId the farm ID
     * @param userId the user ID
     * @return true if user has access, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasAccessToFarm(UUID farmId, UUID userId) {
        return farmRepository.userHasAccessToFarm(userId, farmId);
    }

    /**
     * Transfer farm ownership.
     *
     * @param farmId the farm ID
     * @param newOwnerId the new owner user ID
     * @param currentUserId the current user ID (must be current owner)
     * @return the updated farm
     * @throws ResourceNotFoundException if farm or new owner not found
     * @throws UnauthorizedException if current user is not the owner
     */
    public FarmDto transferOwnership(UUID farmId, UUID newOwnerId, UUID currentUserId) {
        logger.info("Transferring ownership of farm: {} from user: {} to user: {}", farmId, currentUserId, newOwnerId);
        
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with ID: " + farmId));
        
        User newOwner = userRepository.findById(newOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("New owner not found with ID: " + newOwnerId));
        
        // Only current owner can transfer ownership
        if (!farm.getOwner().getUserId().equals(currentUserId)) {
            throw new UnauthorizedException("Only current farm owner can transfer ownership");
        }
        
        farm.setOwner(newOwner);
        Farm updatedFarm = farmRepository.save(farm);
        
        logger.info("Farm ownership transferred successfully for farm: {}", farmId);
        return convertToDto(updatedFarm);
    }

    /**
     * Convert Farm entity to FarmDto.
     *
     * @param farm the farm entity
     * @return the farm DTO
     */
    private FarmDto convertToDto(Farm farm) {
        FarmDto dto = new FarmDto();
        dto.setFarmId(farm.getFarmId());
        dto.setName(farm.getName());
        dto.setLocation(farm.getLocation());
        dto.setOwnerId(farm.getOwner().getUserId());
        dto.setOwnerName(farm.getOwner().getFullName());
        dto.setOwnerEmail(farm.getOwner().getEmail());
        dto.setCreatedAt(farm.getCreatedAt());
        return dto;
    }

    /**
     * Farm statistics data class.
     */
    public static class FarmStatistics {
        private final long ownedFarms;
        private final long accessibleFarms;
        private final long totalRooms;
        private final long totalDevices;
        private final long onlineDevices;
        private final long activeAlerts;

        public FarmStatistics(long ownedFarms, long accessibleFarms, long totalRooms, 
                             long totalDevices, long onlineDevices, long activeAlerts) {
            this.ownedFarms = ownedFarms;
            this.accessibleFarms = accessibleFarms;
            this.totalRooms = totalRooms;
            this.totalDevices = totalDevices;
            this.onlineDevices = onlineDevices;
            this.activeAlerts = activeAlerts;
        }

        public long getOwnedFarms() { return ownedFarms; }
        public long getAccessibleFarms() { return accessibleFarms; }
        public long getTotalRooms() { return totalRooms; }
        public long getTotalDevices() { return totalDevices; }
        public long getOnlineDevices() { return onlineDevices; }
        public long getActiveAlerts() { return activeAlerts; }
    }

    /**
     * Farm detail statistics data class.
     */
    public static class FarmDetailStatistics {
        private final long totalRooms;
        private final long totalDevices;
        private final long onlineDevices;
        private final long activeAlerts;

        public FarmDetailStatistics(long totalRooms, long totalDevices, long onlineDevices, long activeAlerts) {
            this.totalRooms = totalRooms;
            this.totalDevices = totalDevices;
            this.onlineDevices = onlineDevices;
            this.activeAlerts = activeAlerts;
        }

        public long getTotalRooms() { return totalRooms; }
        public long getTotalDevices() { return totalDevices; }
        public long getOnlineDevices() { return onlineDevices; }
        public long getActiveAlerts() { return activeAlerts; }
    }
}