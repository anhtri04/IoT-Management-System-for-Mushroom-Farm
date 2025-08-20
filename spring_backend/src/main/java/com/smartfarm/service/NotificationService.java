package com.smartfarm.service;

import com.smartfarm.entity.Notification;
import com.smartfarm.entity.Farm;
import com.smartfarm.entity.Room;
import com.smartfarm.entity.Device;
import com.smartfarm.repository.NotificationRepository;
import com.smartfarm.repository.FarmRepository;
import com.smartfarm.repository.RoomRepository;
import com.smartfarm.repository.DeviceRepository;
import com.smartfarm.exception.ResourceNotFoundException;
import com.smartfarm.exception.UnauthorizedException;
import com.smartfarm.exception.ValidationException;
import com.smartfarm.dto.NotificationDto;
import com.smartfarm.dto.NotificationCreateDto;
import com.smartfarm.enums.NotificationLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * Service class for Notification entity operations.
 * Handles business logic for notifications, alerts, and acknowledgments.
 */
@Service
@Transactional
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final FarmRepository farmRepository;
    private final RoomRepository roomRepository;
    private final DeviceRepository deviceRepository;
    private final FarmService farmService;
    private final RoomService roomService;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository, FarmRepository farmRepository,
                              RoomRepository roomRepository, DeviceRepository deviceRepository,
                              FarmService farmService, RoomService roomService) {
        this.notificationRepository = notificationRepository;
        this.farmRepository = farmRepository;
        this.roomRepository = roomRepository;
        this.deviceRepository = deviceRepository;
        this.farmService = farmService;
        this.roomService = roomService;
    }

    /**
     * Create a new notification.
     *
     * @param createDto the notification creation data
     * @return the created notification
     * @throws ResourceNotFoundException if farm, room, or device not found
     * @throws ValidationException if notification data is invalid
     */
    public NotificationDto createNotification(NotificationCreateDto createDto) {
        logger.debug("Creating notification with level: {} for farm: {}, room: {}, device: {}", 
                    createDto.getLevel(), createDto.getFarmId(), createDto.getRoomId(), createDto.getDeviceId());

        validateNotificationCreate(createDto);

        Notification notification = new Notification();
        notification.setLevel(createDto.getLevel());
        notification.setMessage(createDto.getMessage());
        notification.setCreatedAt(LocalDateTime.now());

        // Set farm reference
        if (createDto.getFarmId() != null) {
            Farm farm = farmRepository.findById(createDto.getFarmId())
                    .orElseThrow(() -> new ResourceNotFoundException("Farm not found with ID: " + createDto.getFarmId()));
            notification.setFarm(farm);
        }

        // Set room reference
        if (createDto.getRoomId() != null) {
            Room room = roomRepository.findById(createDto.getRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + createDto.getRoomId()));
            notification.setRoom(room);
            
            // If room is set but farm is not, inherit farm from room
            if (notification.getFarm() == null) {
                notification.setFarm(room.getFarm());
            }
        }

        // Set device reference
        if (createDto.getDeviceId() != null) {
            Device device = deviceRepository.findById(createDto.getDeviceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + createDto.getDeviceId()));
            notification.setDevice(device);
            
            // If device is set but room/farm are not, inherit from device
            if (notification.getRoom() == null) {
                notification.setRoom(device.getRoom());
            }
            if (notification.getFarm() == null) {
                notification.setFarm(device.getRoom().getFarm());
            }
        }

        Notification savedNotification = notificationRepository.save(notification);
        logger.debug("Notification created with ID: {}", savedNotification.getNotificationId());

        return convertToDto(savedNotification);
    }

    /**
     * Get notification by ID.
     *
     * @param notificationId the notification ID
     * @param userId the user ID (for access control)
     * @return the notification
     * @throws ResourceNotFoundException if notification not found
     * @throws UnauthorizedException if user doesn't have access
     */
    @Transactional(readOnly = true)
    public NotificationDto getNotification(UUID notificationId, UUID userId) {
        logger.debug("Fetching notification: {} by user: {}", notificationId, userId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        if (!hasAccessToNotification(notification, userId)) {
            throw new UnauthorizedException("User does not have access to notification: " + notificationId);
        }

        return convertToDto(notification);
    }

    /**
     * Get notifications for a user with pagination.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of notifications
     */
    @Transactional(readOnly = true)
    public Page<NotificationDto> getUserNotifications(UUID userId, Pageable pageable) {
        logger.debug("Fetching notifications for user: {}", userId);

        return notificationRepository.findUserNotifications(userId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get unacknowledged notifications for a user.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of unacknowledged notifications
     */
    @Transactional(readOnly = true)
    public Page<NotificationDto> getUnacknowledgedNotifications(UUID userId, Pageable pageable) {
        logger.debug("Fetching unacknowledged notifications for user: {}", userId);

        return notificationRepository.findUnacknowledgedUserNotifications(userId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get notifications by level.
     *
     * @param userId the user ID
     * @param level the notification level
     * @param pageable pagination information
     * @return page of notifications with the specified level
     */
    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotificationsByLevel(UUID userId, NotificationLevel level, Pageable pageable) {
        logger.debug("Fetching notifications with level {} for user: {}", level, userId);

        return notificationRepository.findUserNotificationsByLevel(userId, level, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get notifications for a farm.
     *
     * @param farmId the farm ID
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of farm notifications
     * @throws UnauthorizedException if user doesn't have access to farm
     */
    @Transactional(readOnly = true)
    public Page<NotificationDto> getFarmNotifications(UUID farmId, UUID userId, Pageable pageable) {
        logger.debug("Fetching notifications for farm: {} by user: {}", farmId, userId);

        if (!farmService.hasAccessToFarm(farmId, userId)) {
            throw new UnauthorizedException("User does not have access to farm: " + farmId);
        }

        return notificationRepository.findByFarmOrderByCreatedAtDesc(farmId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get notifications for a room.
     *
     * @param roomId the room ID
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of room notifications
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public Page<NotificationDto> getRoomNotifications(UUID roomId, UUID userId, Pageable pageable) {
        logger.debug("Fetching notifications for room: {} by user: {}", roomId, userId);

        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        return notificationRepository.findByRoomOrderByCreatedAtDesc(roomId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get notifications for a device.
     *
     * @param deviceId the device ID
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of device notifications
     * @throws ResourceNotFoundException if device not found
     * @throws UnauthorizedException if user doesn't have access to device
     */
    @Transactional(readOnly = true)
    public Page<NotificationDto> getDeviceNotifications(UUID deviceId, UUID userId, Pageable pageable) {
        logger.debug("Fetching notifications for device: {} by user: {}", deviceId, userId);

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));

        if (!roomService.hasAccessToRoom(device.getRoom().getRoomId(), userId)) {
            throw new UnauthorizedException("User does not have access to device: " + deviceId);
        }

        return notificationRepository.findByDeviceOrderByCreatedAtDesc(deviceId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get notifications by date range.
     *
     * @param userId the user ID
     * @param startTime the start time
     * @param endTime the end time
     * @param pageable pagination information
     * @return page of notifications in the date range
     */
    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotificationsByDateRange(UUID userId, LocalDateTime startTime, 
                                                           LocalDateTime endTime, Pageable pageable) {
        logger.debug("Fetching notifications for user: {} from {} to {}", userId, startTime, endTime);

        return notificationRepository.findUserNotificationsByDateRange(userId, startTime, endTime, pageable)
                .map(this::convertToDto);
    }

    /**
     * Acknowledge a notification.
     *
     * @param notificationId the notification ID
     * @param userId the user ID acknowledging the notification
     * @return the acknowledged notification
     * @throws ResourceNotFoundException if notification not found
     * @throws UnauthorizedException if user doesn't have access
     * @throws ValidationException if notification is already acknowledged
     */
    public NotificationDto acknowledgeNotification(UUID notificationId, UUID userId) {
        logger.debug("Acknowledging notification: {} by user: {}", notificationId, userId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        if (!hasAccessToNotification(notification, userId)) {
            throw new UnauthorizedException("User does not have access to notification: " + notificationId);
        }

        if (notification.getAcknowledgedBy() != null) {
            throw new ValidationException("Notification is already acknowledged");
        }

        notification.setAcknowledgedBy(userId);
        notification.setAcknowledgedAt(LocalDateTime.now());

        Notification savedNotification = notificationRepository.save(notification);
        logger.debug("Notification acknowledged: {}", notificationId);

        return convertToDto(savedNotification);
    }

    /**
     * Bulk acknowledge notifications.
     *
     * @param notificationIds the list of notification IDs
     * @param userId the user ID acknowledging the notifications
     * @return number of notifications acknowledged
     * @throws UnauthorizedException if user doesn't have access to any notification
     */
    public int bulkAcknowledgeNotifications(List<UUID> notificationIds, UUID userId) {
        logger.debug("Bulk acknowledging {} notifications by user: {}", notificationIds.size(), userId);

        int acknowledgedCount = 0;
        LocalDateTime acknowledgedAt = LocalDateTime.now();

        for (UUID notificationId : notificationIds) {
            Optional<Notification> optionalNotification = notificationRepository.findById(notificationId);
            if (optionalNotification.isPresent()) {
                Notification notification = optionalNotification.get();
                
                if (hasAccessToNotification(notification, userId) && notification.getAcknowledgedBy() == null) {
                    notification.setAcknowledgedBy(userId);
                    notification.setAcknowledgedAt(acknowledgedAt);
                    notificationRepository.save(notification);
                    acknowledgedCount++;
                }
            }
        }

        logger.info("Bulk acknowledged {} notifications by user: {}", acknowledgedCount, userId);
        return acknowledgedCount;
    }

    /**
     * Delete old notifications.
     *
     * @param olderThan delete notifications older than this date
     * @param keepCritical whether to keep critical notifications
     * @return number of notifications deleted
     */
    public int deleteOldNotifications(LocalDateTime olderThan, boolean keepCritical) {
        logger.debug("Deleting notifications older than: {}, keepCritical: {}", olderThan, keepCritical);

        int deletedCount;
        if (keepCritical) {
            deletedCount = notificationRepository.deleteOldNonCriticalNotifications(olderThan);
        } else {
            deletedCount = notificationRepository.deleteOldNotifications(olderThan);
        }

        logger.info("Deleted {} old notifications", deletedCount);
        return deletedCount;
    }

    /**
     * Get notification statistics for a user.
     *
     * @param userId the user ID
     * @param startTime the start time
     * @param endTime the end time
     * @return notification statistics
     */
    @Transactional(readOnly = true)
    public NotificationStatistics getNotificationStatistics(UUID userId, LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Fetching notification statistics for user: {} from {} to {}", userId, startTime, endTime);

        var stats = notificationRepository.getNotificationStatistics(userId, startTime, endTime)
                .orElse(new NotificationRepository.NotificationStatistics() {
                    @Override public Long getTotalNotifications() { return 0L; }
                    @Override public Long getUnacknowledgedNotifications() { return 0L; }
                    @Override public Long getCriticalNotifications() { return 0L; }
                    @Override public Long getWarningNotifications() { return 0L; }
                    @Override public Long getInfoNotifications() { return 0L; }
                    @Override public Double getAcknowledgmentRate() { return 0.0; }
                });

        return new NotificationStatistics(
            stats.getTotalNotifications(),
            stats.getUnacknowledgedNotifications(),
            stats.getCriticalNotifications(),
            stats.getWarningNotifications(),
            stats.getInfoNotifications(),
            stats.getAcknowledgmentRate()
        );
    }

    /**
     * Create system notification for automation events.
     *
     * @param farmId the farm ID
     * @param roomId the room ID (optional)
     * @param deviceId the device ID (optional)
     * @param level the notification level
     * @param message the notification message
     * @return the created notification
     */
    public NotificationDto createSystemNotification(UUID farmId, UUID roomId, UUID deviceId, 
                                                   NotificationLevel level, String message) {
        logger.debug("Creating system notification for farm: {}, room: {}, device: {}", farmId, roomId, deviceId);

        NotificationCreateDto createDto = new NotificationCreateDto();
        createDto.setFarmId(farmId);
        createDto.setRoomId(roomId);
        createDto.setDeviceId(deviceId);
        createDto.setLevel(level);
        createDto.setMessage(message);

        return createNotification(createDto);
    }

    /**
     * Create device alert notification.
     *
     * @param deviceId the device ID
     * @param level the alert level
     * @param message the alert message
     * @return the created notification
     * @throws ResourceNotFoundException if device not found
     */
    public NotificationDto createDeviceAlert(UUID deviceId, NotificationLevel level, String message) {
        logger.debug("Creating device alert for device: {} with level: {}", deviceId, level);

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));

        String alertMessage = String.format("Device '%s' Alert: %s", device.getName(), message);
        
        return createSystemNotification(
            device.getRoom().getFarm().getFarmId(),
            device.getRoom().getRoomId(),
            deviceId,
            level,
            alertMessage
        );
    }

    /**
     * Check if user has access to a notification.
     *
     * @param notification the notification
     * @param userId the user ID
     * @return true if user has access
     */
    private boolean hasAccessToNotification(Notification notification, UUID userId) {
        // If notification is farm-level, check farm access
        if (notification.getFarm() != null) {
            return farmService.hasAccessToFarm(notification.getFarm().getFarmId(), userId);
        }
        
        // If notification is room-level, check room access
        if (notification.getRoom() != null) {
            return roomService.hasAccessToRoom(notification.getRoom().getRoomId(), userId);
        }
        
        // If notification is device-level, check room access through device
        if (notification.getDevice() != null) {
            return roomService.hasAccessToRoom(notification.getDevice().getRoom().getRoomId(), userId);
        }
        
        // System-wide notifications are accessible to all users
        return true;
    }

    /**
     * Validate notification creation data.
     *
     * @param createDto the notification creation data
     * @throws ValidationException if notification data is invalid
     */
    private void validateNotificationCreate(NotificationCreateDto createDto) {
        if (createDto.getLevel() == null) {
            throw new ValidationException("Notification level cannot be null");
        }

        if (!StringUtils.hasText(createDto.getMessage())) {
            throw new ValidationException("Notification message cannot be empty");
        }

        if (createDto.getMessage().length() > 1000) {
            throw new ValidationException("Notification message cannot exceed 1000 characters");
        }

        // At least one of farm, room, or device must be specified for non-system notifications
        if (createDto.getFarmId() == null && createDto.getRoomId() == null && createDto.getDeviceId() == null) {
            throw new ValidationException("At least one of farmId, roomId, or deviceId must be specified");
        }
    }

    /**
     * Convert Notification entity to NotificationDto.
     *
     * @param notification the notification entity
     * @return the notification DTO
     */
    private NotificationDto convertToDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setNotificationId(notification.getNotificationId());
        dto.setFarmId(notification.getFarm() != null ? notification.getFarm().getFarmId() : null);
        dto.setFarmName(notification.getFarm() != null ? notification.getFarm().getName() : null);
        dto.setRoomId(notification.getRoom() != null ? notification.getRoom().getRoomId() : null);
        dto.setRoomName(notification.getRoom() != null ? notification.getRoom().getName() : null);
        dto.setDeviceId(notification.getDevice() != null ? notification.getDevice().getDeviceId() : null);
        dto.setDeviceName(notification.getDevice() != null ? notification.getDevice().getName() : null);
        dto.setLevel(notification.getLevel());
        dto.setMessage(notification.getMessage());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setAcknowledgedBy(notification.getAcknowledgedBy());
        dto.setAcknowledgedAt(notification.getAcknowledgedAt());
        dto.setAcknowledged(notification.getAcknowledgedBy() != null);
        return dto;
    }

    /**
     * Notification statistics data class.
     */
    public static class NotificationStatistics {
        private final long totalNotifications;
        private final long unacknowledgedNotifications;
        private final long criticalNotifications;
        private final long warningNotifications;
        private final long infoNotifications;
        private final double acknowledgmentRate;

        public NotificationStatistics(long totalNotifications, long unacknowledgedNotifications,
                                    long criticalNotifications, long warningNotifications,
                                    long infoNotifications, double acknowledgmentRate) {
            this.totalNotifications = totalNotifications;
            this.unacknowledgedNotifications = unacknowledgedNotifications;
            this.criticalNotifications = criticalNotifications;
            this.warningNotifications = warningNotifications;
            this.infoNotifications = infoNotifications;
            this.acknowledgmentRate = acknowledgmentRate;
        }

        public long getTotalNotifications() { return totalNotifications; }
        public long getUnacknowledgedNotifications() { return unacknowledgedNotifications; }
        public long getCriticalNotifications() { return criticalNotifications; }
        public long getWarningNotifications() { return warningNotifications; }
        public long getInfoNotifications() { return infoNotifications; }
        public double getAcknowledgmentRate() { return acknowledgmentRate; }
    }
}