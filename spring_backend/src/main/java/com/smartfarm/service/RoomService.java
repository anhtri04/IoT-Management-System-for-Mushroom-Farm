package com.smartfarm.service;

import com.smartfarm.entity.Room;
import com.smartfarm.entity.Farm;
import com.smartfarm.entity.User;
import com.smartfarm.entity.UserRoom;
import com.smartfarm.entity.UserRoomRole;
import com.smartfarm.repository.RoomRepository;
import com.smartfarm.repository.FarmRepository;
import com.smartfarm.repository.UserRepository;
import com.smartfarm.repository.UserRoomRepository;
import com.smartfarm.exception.ResourceNotFoundException;
import com.smartfarm.exception.UnauthorizedException;
import com.smartfarm.exception.DuplicateResourceException;
import com.smartfarm.dto.RoomDto;
import com.smartfarm.dto.RoomCreateDto;
import com.smartfarm.dto.RoomUpdateDto;
import com.smartfarm.dto.UserRoomAssignmentDto;
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
 * Service class for Room entity operations.
 * Handles business logic for room management, user assignments, and access control.
 */
@Service
@Transactional
public class RoomService {

    private static final Logger logger = LoggerFactory.getLogger(RoomService.class);

    private final RoomRepository roomRepository;
    private final FarmRepository farmRepository;
    private final UserRepository userRepository;
    private final UserRoomRepository userRoomRepository;
    private final FarmService farmService;

    @Autowired
    public RoomService(RoomRepository roomRepository, FarmRepository farmRepository, 
                      UserRepository userRepository, UserRoomRepository userRoomRepository,
                      FarmService farmService) {
        this.roomRepository = roomRepository;
        this.farmRepository = farmRepository;
        this.userRepository = userRepository;
        this.userRoomRepository = userRoomRepository;
        this.farmService = farmService;
    }

    /**
     * Create a new room.
     *
     * @param createDto the room creation data
     * @param farmId the farm ID
     * @param userId the user ID creating the room
     * @return the created room
     * @throws ResourceNotFoundException if farm not found
     * @throws UnauthorizedException if user doesn't have access to farm
     * @throws DuplicateResourceException if room name already exists in farm
     */
    public RoomDto createRoom(RoomCreateDto createDto, UUID farmId, UUID userId) {
        logger.info("Creating new room: {} in farm: {} by user: {}", createDto.getName(), farmId, userId);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with ID: " + farmId));

        // Check if user has access to the farm
        if (!farmService.hasAccessToFarm(farmId, userId)) {
            throw new UnauthorizedException("User does not have access to farm: " + farmId);
        }

        // Check if room name already exists in the farm
        if (roomRepository.existsByFarmAndNameIgnoreCase(farm, createDto.getName())) {
            throw new DuplicateResourceException("Room with name '" + createDto.getName() + "' already exists in this farm");
        }

        Room room = new Room();
        room.setName(createDto.getName());
        room.setDescription(createDto.getDescription());
        room.setMushroomType(createDto.getMushroomType());
        room.setStage(createDto.getStage());
        room.setFarm(farm);
        room.setCreatedAt(LocalDateTime.now());

        Room savedRoom = roomRepository.save(room);
        logger.info("Room created successfully with ID: {}", savedRoom.getRoomId());

        return convertToDto(savedRoom);
    }

    /**
     * Get room by ID.
     *
     * @param roomId the room ID
     * @return the room
     * @throws ResourceNotFoundException if room not found
     */
    @Transactional(readOnly = true)
    public RoomDto getRoomById(UUID roomId) {
        logger.debug("Fetching room by ID: {}", roomId);
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));
        
        return convertToDto(room);
    }

    /**
     * Get room by ID with access control.
     *
     * @param roomId the room ID
     * @param userId the user ID requesting access
     * @return the room
     * @throws ResourceNotFoundException if room not found
     * @throws UnauthorizedException if user doesn't have access
     */
    @Transactional(readOnly = true)
    public RoomDto getRoomByIdWithAccess(UUID roomId, UUID userId) {
        logger.debug("Fetching room by ID: {} for user: {}", roomId, userId);
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));
        
        if (!hasAccessToRoom(room, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }
        
        return convertToDto(room);
    }

    /**
     * Update room information.
     *
     * @param roomId the room ID
     * @param updateDto the update data
     * @param userId the user ID making the update
     * @return the updated room
     * @throws ResourceNotFoundException if room not found
     * @throws UnauthorizedException if user doesn't have access
     * @throws DuplicateResourceException if new name already exists
     */
    public RoomDto updateRoom(UUID roomId, RoomUpdateDto updateDto, UUID userId) {
        logger.info("Updating room with ID: {} by user: {}", roomId, userId);
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

        // Check if user has access to the room
        if (!hasAccessToRoom(room, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        // Check if name is being changed and if it already exists
        if (updateDto.getName() != null && !updateDto.getName().equals(room.getName())) {
            if (roomRepository.existsByFarmAndNameIgnoreCaseAndRoomIdNot(room.getFarm(), updateDto.getName(), roomId)) {
                throw new DuplicateResourceException("Room with name '" + updateDto.getName() + "' already exists in this farm");
            }
            room.setName(updateDto.getName());
        }

        if (updateDto.getDescription() != null) {
            room.setDescription(updateDto.getDescription());
        }

        if (updateDto.getMushroomType() != null) {
            room.setMushroomType(updateDto.getMushroomType());
        }

        if (updateDto.getStage() != null) {
            room.setStage(updateDto.getStage());
        }

        Room updatedRoom = roomRepository.save(room);
        logger.info("Room updated successfully with ID: {}", updatedRoom.getRoomId());

        return convertToDto(updatedRoom);
    }

    /**
     * Delete room by ID.
     *
     * @param roomId the room ID
     * @param userId the user ID making the deletion
     * @throws ResourceNotFoundException if room not found
     * @throws UnauthorizedException if user is not farm owner
     */
    public void deleteRoom(UUID roomId, UUID userId) {
        logger.info("Deleting room with ID: {} by user: {}", roomId, userId);
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

        // Only farm owner can delete rooms
        if (!room.getFarm().getOwner().getUserId().equals(userId)) {
            throw new UnauthorizedException("Only farm owner can delete rooms");
        }

        roomRepository.deleteById(roomId);
        logger.info("Room deleted successfully with ID: {}", roomId);
    }

    /**
     * Get rooms in a farm.
     *
     * @param farmId the farm ID
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of rooms in the farm
     * @throws UnauthorizedException if user doesn't have access to farm
     */
    @Transactional(readOnly = true)
    public Page<RoomDto> getRoomsByFarm(UUID farmId, UUID userId, Pageable pageable) {
        logger.debug("Fetching rooms in farm: {} for user: {}", farmId, userId);
        
        if (!farmService.hasAccessToFarm(farmId, userId)) {
            throw new UnauthorizedException("User does not have access to farm: " + farmId);
        }
        
        return roomRepository.findByFarmFarmId(farmId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get rooms accessible by a user.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of rooms accessible by the user
     */
    @Transactional(readOnly = true)
    public Page<RoomDto> getRoomsAccessibleByUser(UUID userId, Pageable pageable) {
        logger.debug("Fetching rooms accessible by user: {}", userId);
        
        return roomRepository.findRoomsAccessibleByUser(userId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Search rooms by name or mushroom type.
     *
     * @param searchTerm the search term
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of matching rooms
     */
    @Transactional(readOnly = true)
    public Page<RoomDto> searchRooms(String searchTerm, UUID userId, Pageable pageable) {
        logger.debug("Searching rooms with term: {} for user: {}", searchTerm, userId);
        
        return roomRepository.searchRoomsAccessibleByUser(searchTerm, userId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get rooms by cultivation stage.
     *
     * @param stage the cultivation stage
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of rooms in the specified stage
     */
    @Transactional(readOnly = true)
    public Page<RoomDto> getRoomsByStage(String stage, UUID userId, Pageable pageable) {
        logger.debug("Fetching rooms by stage: {} for user: {}", stage, userId);
        
        return roomRepository.findRoomsByStageAccessibleByUser(stage, userId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get active rooms (with active farming cycles).
     *
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of active rooms
     */
    @Transactional(readOnly = true)
    public Page<RoomDto> getActiveRooms(UUID userId, Pageable pageable) {
        logger.debug("Fetching active rooms for user: {}", userId);
        
        return roomRepository.findActiveRoomsAccessibleByUser(userId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Assign user to room.
     *
     * @param roomId the room ID
     * @param assignmentDto the user assignment data
     * @param assignerId the user ID making the assignment (must be farm owner)
     * @throws ResourceNotFoundException if room or user not found
     * @throws UnauthorizedException if assigner is not farm owner
     * @throws DuplicateResourceException if user already assigned to room
     */
    public void assignUserToRoom(UUID roomId, UserRoomAssignmentDto assignmentDto, UUID assignerId) {
        logger.info("Assigning user: {} to room: {} with role: {} by user: {}", 
                   assignmentDto.getUserId(), roomId, assignmentDto.getRole(), assignerId);
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));
        
        User user = userRepository.findById(assignmentDto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + assignmentDto.getUserId()));
        
        // Only farm owner can assign users to rooms
        if (!room.getFarm().getOwner().getUserId().equals(assignerId)) {
            throw new UnauthorizedException("Only farm owner can assign users to rooms");
        }
        
        // Check if user is already assigned to this room
        if (userRoomRepository.existsByUserAndRoom(user, room)) {
            throw new DuplicateResourceException("User is already assigned to this room");
        }
        
        UserRoom userRoom = new UserRoom();
        userRoom.setUser(user);
        userRoom.setRoom(room);
        userRoom.setRole(assignmentDto.getRole());
        userRoom.setAssignedAt(LocalDateTime.now());
        userRoom.setActive(true);
        
        userRoomRepository.save(userRoom);
        logger.info("User assigned to room successfully");
    }

    /**
     * Remove user from room.
     *
     * @param roomId the room ID
     * @param userId the user ID to remove
     * @param removerId the user ID making the removal (must be farm owner)
     * @throws ResourceNotFoundException if room or assignment not found
     * @throws UnauthorizedException if remover is not farm owner
     */
    public void removeUserFromRoom(UUID roomId, UUID userId, UUID removerId) {
        logger.info("Removing user: {} from room: {} by user: {}", userId, roomId, removerId);
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));
        
        // Only farm owner can remove users from rooms
        if (!room.getFarm().getOwner().getUserId().equals(removerId)) {
            throw new UnauthorizedException("Only farm owner can remove users from rooms");
        }
        
        UserRoom userRoom = userRoomRepository.findByUserUserIdAndRoomRoomId(userId, roomId)
                .orElseThrow(() -> new ResourceNotFoundException("User assignment not found for room"));
        
        userRoomRepository.delete(userRoom);
        logger.info("User removed from room successfully");
    }

    /**
     * Update user role in room.
     *
     * @param roomId the room ID
     * @param userId the user ID
     * @param newRole the new role
     * @param updaterId the user ID making the update (must be farm owner)
     * @throws ResourceNotFoundException if room or assignment not found
     * @throws UnauthorizedException if updater is not farm owner
     */
    public void updateUserRoleInRoom(UUID roomId, UUID userId, UserRoomRole newRole, UUID updaterId) {
        logger.info("Updating user: {} role in room: {} to: {} by user: {}", userId, roomId, newRole, updaterId);
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));
        
        // Only farm owner can update user roles
        if (!room.getFarm().getOwner().getUserId().equals(updaterId)) {
            throw new UnauthorizedException("Only farm owner can update user roles in rooms");
        }
        
        UserRoom userRoom = userRoomRepository.findByUserUserIdAndRoomRoomId(userId, roomId)
                .orElseThrow(() -> new ResourceNotFoundException("User assignment not found for room"));
        
        userRoom.setRole(newRole);
        userRoomRepository.save(userRoom);
        
        logger.info("User role updated successfully");
    }

    /**
     * Get room statistics.
     *
     * @param roomId the room ID
     * @param userId the user ID (for access control)
     * @return room statistics
     * @throws ResourceNotFoundException if room not found
     * @throws UnauthorizedException if user doesn't have access
     */
    @Transactional(readOnly = true)
    public RoomStatistics getRoomStatistics(UUID roomId, UUID userId) {
        logger.debug("Fetching statistics for room: {} by user: {}", roomId, userId);
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));
        
        if (!hasAccessToRoom(room, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }
        
        var stats = roomRepository.getRoomStatistics(roomId)
                .orElse(new RoomRepository.RoomStatistics() {
                    @Override public Long getTotalDevices() { return 0L; }
                    @Override public Long getOnlineDevices() { return 0L; }
                    @Override public Long getActiveAutomationRules() { return 0L; }
                    @Override public Long getActiveFarmingCycles() { return 0L; }
                    @Override public Long getActiveAlerts() { return 0L; }
                });
        
        return new RoomStatistics(
            stats.getTotalDevices(),
            stats.getOnlineDevices(),
            stats.getActiveAutomationRules(),
            stats.getActiveFarmingCycles(),
            stats.getActiveAlerts()
        );
    }

    /**
     * Check if user has access to a room.
     *
     * @param room the room
     * @param userId the user ID
     * @return true if user has access, false otherwise
     */
    private boolean hasAccessToRoom(Room room, UUID userId) {
        // User has access if they are the farm owner or assigned to the room
        if (room.getFarm().getOwner().getUserId().equals(userId)) {
            return true;
        }
        
        return userRoomRepository.existsByUserUserIdAndRoomRoomIdAndIsActiveTrue(userId, room.getRoomId());
    }

    /**
     * Check if user has access to a room by room ID.
     *
     * @param roomId the room ID
     * @param userId the user ID
     * @return true if user has access, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasAccessToRoom(UUID roomId, UUID userId) {
        return roomRepository.userHasAccessToRoom(userId, roomId);
    }

    /**
     * Convert Room entity to RoomDto.
     *
     * @param room the room entity
     * @return the room DTO
     */
    private RoomDto convertToDto(Room room) {
        RoomDto dto = new RoomDto();
        dto.setRoomId(room.getRoomId());
        dto.setName(room.getName());
        dto.setDescription(room.getDescription());
        dto.setMushroomType(room.getMushroomType());
        dto.setStage(room.getStage());
        dto.setFarmId(room.getFarm().getFarmId());
        dto.setFarmName(room.getFarm().getName());
        dto.setCreatedAt(room.getCreatedAt());
        return dto;
    }

    /**
     * Room statistics data class.
     */
    public static class RoomStatistics {
        private final long totalDevices;
        private final long onlineDevices;
        private final long activeAutomationRules;
        private final long activeFarmingCycles;
        private final long activeAlerts;

        public RoomStatistics(long totalDevices, long onlineDevices, long activeAutomationRules, 
                             long activeFarmingCycles, long activeAlerts) {
            this.totalDevices = totalDevices;
            this.onlineDevices = onlineDevices;
            this.activeAutomationRules = activeAutomationRules;
            this.activeFarmingCycles = activeFarmingCycles;
            this.activeAlerts = activeAlerts;
        }

        public long getTotalDevices() { return totalDevices; }
        public long getOnlineDevices() { return onlineDevices; }
        public long getActiveAutomationRules() { return activeAutomationRules; }
        public long getActiveFarmingCycles() { return activeFarmingCycles; }
        public long getActiveAlerts() { return activeAlerts; }
    }
}