package com.smartfarm.service;

import com.smartfarm.entity.Device;
import com.smartfarm.entity.Room;
import com.smartfarm.entity.DeviceStatus;
import com.smartfarm.repository.DeviceRepository;
import com.smartfarm.repository.RoomRepository;
import com.smartfarm.exception.ResourceNotFoundException;
import com.smartfarm.exception.UnauthorizedException;
import com.smartfarm.exception.DuplicateResourceException;
import com.smartfarm.dto.DeviceDto;
import com.smartfarm.dto.DeviceCreateDto;
import com.smartfarm.dto.DeviceUpdateDto;
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
 * Service class for Device entity operations.
 * Handles business logic for device management, registration, and monitoring.
 */
@Service
@Transactional
public class DeviceService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);

    private final DeviceRepository deviceRepository;
    private final RoomRepository roomRepository;
    private final RoomService roomService;

    @Autowired
    public DeviceService(DeviceRepository deviceRepository, RoomRepository roomRepository,
                        RoomService roomService) {
        this.deviceRepository = deviceRepository;
        this.roomRepository = roomRepository;
        this.roomService = roomService;
    }

    /**
     * Register a new device.
     *
     * @param createDto the device creation data
     * @param roomId the room ID
     * @param userId the user ID registering the device
     * @return the registered device
     * @throws ResourceNotFoundException if room not found
     * @throws UnauthorizedException if user doesn't have access to room
     * @throws DuplicateResourceException if device name or MQTT topic already exists
     */
    public DeviceDto registerDevice(DeviceCreateDto createDto, UUID roomId, UUID userId) {
        logger.info("Registering new device: {} in room: {} by user: {}", createDto.getName(), roomId, userId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

        // Check if user has access to the room
        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }

        // Check if device name already exists in the room
        if (deviceRepository.existsByRoomAndNameIgnoreCase(room, createDto.getName())) {
            throw new DuplicateResourceException("Device with name '" + createDto.getName() + "' already exists in this room");
        }

        // Check if MQTT topic already exists
        if (deviceRepository.existsByMqttTopic(createDto.getMqttTopic())) {
            throw new DuplicateResourceException("Device with MQTT topic '" + createDto.getMqttTopic() + "' already exists");
        }

        Device device = new Device();
        device.setName(createDto.getName());
        device.setDeviceType(createDto.getDeviceType());
        device.setCategory(createDto.getCategory());
        device.setMqttTopic(createDto.getMqttTopic());
        device.setStatus(DeviceStatus.OFFLINE);
        device.setFirmwareVersion(createDto.getFirmwareVersion());
        device.setRoom(room);
        device.setCreatedAt(LocalDateTime.now());

        Device savedDevice = deviceRepository.save(device);
        logger.info("Device registered successfully with ID: {}", savedDevice.getDeviceId());

        return convertToDto(savedDevice);
    }

    /**
     * Get device by ID.
     *
     * @param deviceId the device ID
     * @return the device
     * @throws ResourceNotFoundException if device not found
     */
    @Transactional(readOnly = true)
    public DeviceDto getDeviceById(UUID deviceId) {
        logger.debug("Fetching device by ID: {}", deviceId);
        
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));
        
        return convertToDto(device);
    }

    /**
     * Get device by ID with access control.
     *
     * @param deviceId the device ID
     * @param userId the user ID requesting access
     * @return the device
     * @throws ResourceNotFoundException if device not found
     * @throws UnauthorizedException if user doesn't have access
     */
    @Transactional(readOnly = true)
    public DeviceDto getDeviceByIdWithAccess(UUID deviceId, UUID userId) {
        logger.debug("Fetching device by ID: {} for user: {}", deviceId, userId);
        
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));
        
        if (!roomService.hasAccessToRoom(device.getRoom().getRoomId(), userId)) {
            throw new UnauthorizedException("User does not have access to device: " + deviceId);
        }
        
        return convertToDto(device);
    }

    /**
     * Update device information.
     *
     * @param deviceId the device ID
     * @param updateDto the update data
     * @param userId the user ID making the update
     * @return the updated device
     * @throws ResourceNotFoundException if device not found
     * @throws UnauthorizedException if user doesn't have access
     * @throws DuplicateResourceException if new name or MQTT topic already exists
     */
    public DeviceDto updateDevice(UUID deviceId, DeviceUpdateDto updateDto, UUID userId) {
        logger.info("Updating device with ID: {} by user: {}", deviceId, userId);
        
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));

        // Check if user has access to the device's room
        if (!roomService.hasAccessToRoom(device.getRoom().getRoomId(), userId)) {
            throw new UnauthorizedException("User does not have access to device: " + deviceId);
        }

        // Check if name is being changed and if it already exists
        if (updateDto.getName() != null && !updateDto.getName().equals(device.getName())) {
            if (deviceRepository.existsByRoomAndNameIgnoreCaseAndDeviceIdNot(device.getRoom(), updateDto.getName(), deviceId)) {
                throw new DuplicateResourceException("Device with name '" + updateDto.getName() + "' already exists in this room");
            }
            device.setName(updateDto.getName());
        }

        // Check if MQTT topic is being changed and if it already exists
        if (updateDto.getMqttTopic() != null && !updateDto.getMqttTopic().equals(device.getMqttTopic())) {
            if (deviceRepository.existsByMqttTopicAndDeviceIdNot(updateDto.getMqttTopic(), deviceId)) {
                throw new DuplicateResourceException("Device with MQTT topic '" + updateDto.getMqttTopic() + "' already exists");
            }
            device.setMqttTopic(updateDto.getMqttTopic());
        }

        if (updateDto.getDeviceType() != null) {
            device.setDeviceType(updateDto.getDeviceType());
        }

        if (updateDto.getCategory() != null) {
            device.setCategory(updateDto.getCategory());
        }

        if (updateDto.getFirmwareVersion() != null) {
            device.setFirmwareVersion(updateDto.getFirmwareVersion());
        }

        Device updatedDevice = deviceRepository.save(device);
        logger.info("Device updated successfully with ID: {}", updatedDevice.getDeviceId());

        return convertToDto(updatedDevice);
    }

    /**
     * Delete device by ID.
     *
     * @param deviceId the device ID
     * @param userId the user ID making the deletion
     * @throws ResourceNotFoundException if device not found
     * @throws UnauthorizedException if user doesn't have access
     */
    public void deleteDevice(UUID deviceId, UUID userId) {
        logger.info("Deleting device with ID: {} by user: {}", deviceId, userId);
        
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));

        // Check if user has access to the device's room
        if (!roomService.hasAccessToRoom(device.getRoom().getRoomId(), userId)) {
            throw new UnauthorizedException("User does not have access to device: " + deviceId);
        }

        deviceRepository.deleteById(deviceId);
        logger.info("Device deleted successfully with ID: {}", deviceId);
    }

    /**
     * Get devices in a room.
     *
     * @param roomId the room ID
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of devices in the room
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public Page<DeviceDto> getDevicesByRoom(UUID roomId, UUID userId, Pageable pageable) {
        logger.debug("Fetching devices in room: {} for user: {}", roomId, userId);
        
        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }
        
        return deviceRepository.findByRoomRoomId(roomId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get devices by type.
     *
     * @param deviceType the device type
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of devices of the specified type
     */
    @Transactional(readOnly = true)
    public Page<DeviceDto> getDevicesByType(String deviceType, UUID userId, Pageable pageable) {
        logger.debug("Fetching devices by type: {} for user: {}", deviceType, userId);
        
        return deviceRepository.findDevicesByTypeAccessibleByUser(deviceType, userId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get devices by category.
     *
     * @param category the device category
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of devices of the specified category
     */
    @Transactional(readOnly = true)
    public Page<DeviceDto> getDevicesByCategory(String category, UUID userId, Pageable pageable) {
        logger.debug("Fetching devices by category: {} for user: {}", category, userId);
        
        return deviceRepository.findDevicesByCategoryAccessibleByUser(category, userId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get devices by status.
     *
     * @param status the device status
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of devices with the specified status
     */
    @Transactional(readOnly = true)
    public Page<DeviceDto> getDevicesByStatus(DeviceStatus status, UUID userId, Pageable pageable) {
        logger.debug("Fetching devices by status: {} for user: {}", status, userId);
        
        return deviceRepository.findDevicesByStatusAccessibleByUser(status, userId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Search devices by name.
     *
     * @param searchTerm the search term
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of matching devices
     */
    @Transactional(readOnly = true)
    public Page<DeviceDto> searchDevices(String searchTerm, UUID userId, Pageable pageable) {
        logger.debug("Searching devices with term: {} for user: {}", searchTerm, userId);
        
        return deviceRepository.searchDevicesAccessibleByUser(searchTerm, userId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get online devices.
     *
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of online devices
     */
    @Transactional(readOnly = true)
    public Page<DeviceDto> getOnlineDevices(UUID userId, Pageable pageable) {
        logger.debug("Fetching online devices for user: {}", userId);
        
        return deviceRepository.findOnlineDevicesAccessibleByUser(userId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get offline devices.
     *
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of offline devices
     */
    @Transactional(readOnly = true)
    public Page<DeviceDto> getOfflineDevices(UUID userId, Pageable pageable) {
        logger.debug("Fetching offline devices for user: {}", userId);
        
        return deviceRepository.findOfflineDevicesAccessibleByUser(userId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get devices with low battery.
     *
     * @param batteryThreshold the battery threshold (percentage)
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of devices with low battery
     */
    @Transactional(readOnly = true)
    public Page<DeviceDto> getDevicesWithLowBattery(double batteryThreshold, UUID userId, Pageable pageable) {
        logger.debug("Fetching devices with low battery (< {}%) for user: {}", batteryThreshold, userId);
        
        return deviceRepository.findDevicesWithLowBatteryAccessibleByUser(batteryThreshold, userId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Update device status.
     *
     * @param deviceId the device ID
     * @param status the new status
     * @param lastSeen the last seen timestamp
     */
    public void updateDeviceStatus(UUID deviceId, DeviceStatus status, LocalDateTime lastSeen) {
        logger.debug("Updating device status: {} to {} at {}", deviceId, status, lastSeen);
        
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));
        
        device.setStatus(status);
        device.setLastSeen(lastSeen);
        deviceRepository.save(device);
        
        logger.debug("Device status updated successfully");
    }

    /**
     * Update device firmware version.
     *
     * @param deviceId the device ID
     * @param firmwareVersion the new firmware version
     * @param userId the user ID making the update
     * @throws ResourceNotFoundException if device not found
     * @throws UnauthorizedException if user doesn't have access
     */
    public void updateDeviceFirmware(UUID deviceId, String firmwareVersion, UUID userId) {
        logger.info("Updating device firmware: {} to version: {} by user: {}", deviceId, firmwareVersion, userId);
        
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));

        // Check if user has access to the device's room
        if (!roomService.hasAccessToRoom(device.getRoom().getRoomId(), userId)) {
            throw new UnauthorizedException("User does not have access to device: " + deviceId);
        }

        device.setFirmwareVersion(firmwareVersion);
        deviceRepository.save(device);
        
        logger.info("Device firmware updated successfully");
    }

    /**
     * Get device statistics for a room.
     *
     * @param roomId the room ID
     * @param userId the user ID (for access control)
     * @return device statistics
     * @throws UnauthorizedException if user doesn't have access to room
     */
    @Transactional(readOnly = true)
    public DeviceStatistics getDeviceStatistics(UUID roomId, UUID userId) {
        logger.debug("Fetching device statistics for room: {} by user: {}", roomId, userId);
        
        if (!roomService.hasAccessToRoom(roomId, userId)) {
            throw new UnauthorizedException("User does not have access to room: " + roomId);
        }
        
        var stats = deviceRepository.getDeviceStatisticsByRoom(roomId)
                .orElse(new DeviceRepository.DeviceStatistics() {
                    @Override public Long getTotalDevices() { return 0L; }
                    @Override public Long getOnlineDevices() { return 0L; }
                    @Override public Long getOfflineDevices() { return 0L; }
                    @Override public Long getSensorDevices() { return 0L; }
                    @Override public Long getActuatorDevices() { return 0L; }
                    @Override public Long getHybridDevices() { return 0L; }
                });
        
        return new DeviceStatistics(
            stats.getTotalDevices(),
            stats.getOnlineDevices(),
            stats.getOfflineDevices(),
            stats.getSensorDevices(),
            stats.getActuatorDevices(),
            stats.getHybridDevices()
        );
    }

    /**
     * Get devices that haven't been seen for a specified duration.
     *
     * @param hours the number of hours
     * @param userId the user ID (for access control)
     * @param pageable pagination information
     * @return page of stale devices
     */
    @Transactional(readOnly = true)
    public Page<DeviceDto> getStaleDevices(int hours, UUID userId, Pageable pageable) {
        logger.debug("Fetching devices not seen for {} hours for user: {}", hours, userId);
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
        return deviceRepository.findStaleDevicesAccessibleByUser(cutoffTime, userId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Bulk update device status.
     *
     * @param deviceIds the list of device IDs
     * @param status the new status
     * @param userId the user ID making the update
     * @return number of devices updated
     * @throws UnauthorizedException if user doesn't have access to any device
     */
    public int bulkUpdateDeviceStatus(List<UUID> deviceIds, DeviceStatus status, UUID userId) {
        logger.info("Bulk updating {} devices to status: {} by user: {}", deviceIds.size(), status, userId);
        
        // Verify user has access to all devices
        for (UUID deviceId : deviceIds) {
            Device device = deviceRepository.findById(deviceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + deviceId));
            
            if (!roomService.hasAccessToRoom(device.getRoom().getRoomId(), userId)) {
                throw new UnauthorizedException("User does not have access to device: " + deviceId);
            }
        }
        
        int updatedCount = deviceRepository.bulkUpdateDeviceStatus(deviceIds, status, LocalDateTime.now());
        logger.info("Bulk updated {} devices successfully", updatedCount);
        
        return updatedCount;
    }

    /**
     * Convert Device entity to DeviceDto.
     *
     * @param device the device entity
     * @return the device DTO
     */
    private DeviceDto convertToDto(Device device) {
        DeviceDto dto = new DeviceDto();
        dto.setDeviceId(device.getDeviceId());
        dto.setName(device.getName());
        dto.setDeviceType(device.getDeviceType());
        dto.setCategory(device.getCategory());
        dto.setMqttTopic(device.getMqttTopic());
        dto.setStatus(device.getStatus());
        dto.setLastSeen(device.getLastSeen());
        dto.setFirmwareVersion(device.getFirmwareVersion());
        dto.setRoomId(device.getRoom().getRoomId());
        dto.setRoomName(device.getRoom().getName());
        dto.setFarmId(device.getRoom().getFarm().getFarmId());
        dto.setFarmName(device.getRoom().getFarm().getName());
        dto.setCreatedAt(device.getCreatedAt());
        return dto;
    }

    /**
     * Device statistics data class.
     */
    public static class DeviceStatistics {
        private final long totalDevices;
        private final long onlineDevices;
        private final long offlineDevices;
        private final long sensorDevices;
        private final long actuatorDevices;
        private final long hybridDevices;

        public DeviceStatistics(long totalDevices, long onlineDevices, long offlineDevices,
                               long sensorDevices, long actuatorDevices, long hybridDevices) {
            this.totalDevices = totalDevices;
            this.onlineDevices = onlineDevices;
            this.offlineDevices = offlineDevices;
            this.sensorDevices = sensorDevices;
            this.actuatorDevices = actuatorDevices;
            this.hybridDevices = hybridDevices;
        }

        public long getTotalDevices() { return totalDevices; }
        public long getOnlineDevices() { return onlineDevices; }
        public long getOfflineDevices() { return offlineDevices; }
        public long getSensorDevices() { return sensorDevices; }
        public long getActuatorDevices() { return actuatorDevices; }
        public long getHybridDevices() { return hybridDevices; }
    }
}