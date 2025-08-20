package com.smartfarm.service;

import com.smartfarm.entity.User;
import com.smartfarm.entity.UserRole;
import com.smartfarm.repository.UserRepository;
import com.smartfarm.exception.ResourceNotFoundException;
import com.smartfarm.exception.DuplicateResourceException;
import com.smartfarm.dto.UserDto;
import com.smartfarm.dto.UserRegistrationDto;
import com.smartfarm.dto.UserUpdateDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for User entity operations.
 * Handles business logic for user management, authentication, and authorization.
 */
@Service
@Transactional
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Create a new user.
     *
     * @param registrationDto the user registration data
     * @return the created user
     * @throws DuplicateResourceException if user with email or cognito sub already exists
     */
    public UserDto createUser(UserRegistrationDto registrationDto) {
        logger.info("Creating new user with email: {}", registrationDto.getEmail());

        // Check if user with email already exists
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new DuplicateResourceException("User with email " + registrationDto.getEmail() + " already exists");
        }

        // Check if user with cognito sub already exists
        if (userRepository.existsByCognitoSub(registrationDto.getCognitoSub())) {
            throw new DuplicateResourceException("User with Cognito sub " + registrationDto.getCognitoSub() + " already exists");
        }

        User user = new User();
        user.setCognitoSub(registrationDto.getCognitoSub());
        user.setEmail(registrationDto.getEmail());
        user.setFullName(registrationDto.getFullName());
        user.setRole(registrationDto.getRole() != null ? registrationDto.getRole() : UserRole.VIEWER);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        logger.info("User created successfully with ID: {}", savedUser.getUserId());

        return convertToDto(savedUser);
    }

    /**
     * Get user by ID.
     *
     * @param userId the user ID
     * @return the user
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserDto getUserById(UUID userId) {
        logger.debug("Fetching user by ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        return convertToDto(user);
    }

    /**
     * Get user by email.
     *
     * @param email the user email
     * @return the user
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        logger.debug("Fetching user by email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        return convertToDto(user);
    }

    /**
     * Get user by Cognito sub.
     *
     * @param cognitoSub the Cognito sub
     * @return the user
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserDto getUserByCognitoSub(String cognitoSub) {
        logger.debug("Fetching user by Cognito sub: {}", cognitoSub);
        
        User user = userRepository.findByCognitoSub(cognitoSub)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with Cognito sub: " + cognitoSub));
        
        return convertToDto(user);
    }

    /**
     * Get current authenticated user.
     *
     * @return the current user
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        String cognitoSub = authentication.getName(); // Assuming Cognito sub is used as principal
        return getUserByCognitoSub(cognitoSub);
    }

    /**
     * Update user information.
     *
     * @param userId the user ID
     * @param updateDto the update data
     * @return the updated user
     * @throws ResourceNotFoundException if user not found
     * @throws DuplicateResourceException if email already exists for another user
     */
    public UserDto updateUser(UUID userId, UserUpdateDto updateDto) {
        logger.info("Updating user with ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Check if email is being changed and if it already exists for another user
        if (updateDto.getEmail() != null && !updateDto.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndUserIdNot(updateDto.getEmail(), userId)) {
                throw new DuplicateResourceException("Email " + updateDto.getEmail() + " is already in use by another user");
            }
            user.setEmail(updateDto.getEmail());
        }

        if (updateDto.getFullName() != null) {
            user.setFullName(updateDto.getFullName());
        }

        if (updateDto.getRole() != null) {
            user.setRole(updateDto.getRole());
        }

        User updatedUser = userRepository.save(user);
        logger.info("User updated successfully with ID: {}", updatedUser.getUserId());

        return convertToDto(updatedUser);
    }

    /**
     * Delete user by ID.
     *
     * @param userId the user ID
     * @throws ResourceNotFoundException if user not found
     */
    public void deleteUser(UUID userId) {
        logger.info("Deleting user with ID: {}", userId);
        
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }

        userRepository.deleteById(userId);
        logger.info("User deleted successfully with ID: {}", userId);
    }

    /**
     * Get all users with pagination.
     *
     * @param pageable pagination information
     * @return page of users
     */
    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(Pageable pageable) {
        logger.debug("Fetching all users with pagination");
        
        return userRepository.findAll(pageable)
                .map(this::convertToDto);
    }

    /**
     * Search users by name or email.
     *
     * @param searchTerm the search term
     * @param pageable pagination information
     * @return page of matching users
     */
    @Transactional(readOnly = true)
    public Page<UserDto> searchUsers(String searchTerm, Pageable pageable) {
        logger.debug("Searching users with term: {}", searchTerm);
        
        return userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                searchTerm, searchTerm, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get users by role.
     *
     * @param role the user role
     * @param pageable pagination information
     * @return page of users with the specified role
     */
    @Transactional(readOnly = true)
    public Page<UserDto> getUsersByRole(UserRole role, Pageable pageable) {
        logger.debug("Fetching users by role: {}", role);
        
        return userRepository.findByRole(role, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get users created within a date range.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @param pageable pagination information
     * @return page of users created within the date range
     */
    @Transactional(readOnly = true)
    public Page<UserDto> getUsersByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        logger.debug("Fetching users created between {} and {}", startDate, endDate);
        
        return userRepository.findByCreatedAtBetween(startDate, endDate, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get user statistics.
     *
     * @return user statistics
     */
    @Transactional(readOnly = true)
    public UserStatistics getUserStatistics() {
        logger.debug("Fetching user statistics");
        
        long totalUsers = userRepository.count();
        long adminUsers = userRepository.countByRole(UserRole.ADMIN);
        long managerUsers = userRepository.countByRole(UserRole.MANAGER);
        long viewerUsers = userRepository.countByRole(UserRole.VIEWER);
        
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentUsers = userRepository.countByCreatedAtAfter(thirtyDaysAgo);

        return new UserStatistics(totalUsers, adminUsers, managerUsers, viewerUsers, recentUsers);
    }

    /**
     * Check if user exists by email.
     *
     * @param email the email to check
     * @return true if user exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Check if user exists by Cognito sub.
     *
     * @param cognitoSub the Cognito sub to check
     * @return true if user exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean existsByCognitoSub(String cognitoSub) {
        return userRepository.existsByCognitoSub(cognitoSub);
    }

    /**
     * Find or create user from Cognito authentication.
     * This method is typically called during JWT token validation.
     *
     * @param cognitoSub the Cognito sub
     * @param email the user email
     * @param fullName the user full name
     * @return the user (existing or newly created)
     */
    public UserDto findOrCreateUserFromCognito(String cognitoSub, String email, String fullName) {
        logger.debug("Finding or creating user from Cognito: {}", email);
        
        Optional<User> existingUser = userRepository.findByCognitoSub(cognitoSub);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // Update email and name if they have changed in Cognito
            boolean updated = false;
            
            if (email != null && !email.equals(user.getEmail())) {
                user.setEmail(email);
                updated = true;
            }
            
            if (fullName != null && !fullName.equals(user.getFullName())) {
                user.setFullName(fullName);
                updated = true;
            }
            
            if (updated) {
                user = userRepository.save(user);
                logger.info("Updated user information from Cognito for user: {}", user.getUserId());
            }
            
            return convertToDto(user);
        } else {
            // Create new user
            UserRegistrationDto registrationDto = new UserRegistrationDto();
            registrationDto.setCognitoSub(cognitoSub);
            registrationDto.setEmail(email);
            registrationDto.setFullName(fullName);
            registrationDto.setRole(UserRole.VIEWER); // Default role for new users
            
            logger.info("Creating new user from Cognito authentication: {}", email);
            return createUser(registrationDto);
        }
    }

    /**
     * Convert User entity to UserDto.
     *
     * @param user the user entity
     * @return the user DTO
     */
    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setUserId(user.getUserId());
        dto.setCognitoSub(user.getCognitoSub());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setRole(user.getRole());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    /**
     * User statistics data class.
     */
    public static class UserStatistics {
        private final long totalUsers;
        private final long adminUsers;
        private final long managerUsers;
        private final long viewerUsers;
        private final long recentUsers;

        public UserStatistics(long totalUsers, long adminUsers, long managerUsers, long viewerUsers, long recentUsers) {
            this.totalUsers = totalUsers;
            this.adminUsers = adminUsers;
            this.managerUsers = managerUsers;
            this.viewerUsers = viewerUsers;
            this.recentUsers = recentUsers;
        }

        public long getTotalUsers() { return totalUsers; }
        public long getAdminUsers() { return adminUsers; }
        public long getManagerUsers() { return managerUsers; }
        public long getViewerUsers() { return viewerUsers; }
        public long getRecentUsers() { return recentUsers; }
    }
}