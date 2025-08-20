package com.smartfarm.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Utility class for common security operations.
 * Provides helper methods to access current user information from Spring Security context.
 */
public class SecurityUtils {

    private static final Logger logger = LoggerFactory.getLogger(SecurityUtils.class);

    private SecurityUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Get the current authenticated user from Spring Security context.
     */
    public static Optional<UserPrincipal> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }
            
            Object principal = authentication.getPrincipal();
            
            if (principal instanceof UserPrincipal) {
                return Optional.of((UserPrincipal) principal);
            }
            
            logger.debug("Principal is not UserPrincipal: {}", principal.getClass().getSimpleName());
            return Optional.empty();
            
        } catch (Exception e) {
            logger.error("Error getting current user from security context", e);
            return Optional.empty();
        }
    }

    /**
     * Get the current user ID.
     */
    public static Optional<String> getCurrentUserId() {
        return getCurrentUser().map(UserPrincipal::getUserId);
    }

    /**
     * Get the current user email.
     */
    public static Optional<String> getCurrentUserEmail() {
        return getCurrentUser().map(UserPrincipal::getEmail);
    }

    /**
     * Get the current user name.
     */
    public static Optional<String> getCurrentUserName() {
        return getCurrentUser().map(UserPrincipal::getName);
    }

    /**
     * Check if current user has a specific role.
     */
    public static boolean hasRole(String role) {
        return getCurrentUser()
            .map(user -> user.hasRole(role))
            .orElse(false);
    }

    /**
     * Check if current user is an admin.
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Check if current user is a manager or admin.
     */
    public static boolean isManagerOrAdmin() {
        return getCurrentUser()
            .map(UserPrincipal::isManagerOrAdmin)
            .orElse(false);
    }

    /**
     * Check if current user is an internal service.
     */
    public static boolean isInternalService() {
        return getCurrentUser()
            .map(UserPrincipal::isInternalService)
            .orElse(false);
    }

    /**
     * Check if there is an authenticated user in the current context.
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && 
               authentication.isAuthenticated() && 
               !"anonymousUser".equals(authentication.getPrincipal());
    }

    /**
     * Get the current user ID or throw exception if not authenticated.
     */
    public static String requireCurrentUserId() {
        return getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("No authenticated user found"));
    }

    /**
     * Get the current user or throw exception if not authenticated.
     */
    public static UserPrincipal requireCurrentUser() {
        return getCurrentUser()
            .orElseThrow(() -> new RuntimeException("No authenticated user found"));
    }

    /**
     * Check if the current user can access a specific user's data.
     * Users can access their own data, admins can access any data.
     */
    public static boolean canAccessUserData(String targetUserId) {
        if (isAdmin()) {
            return true;
        }
        
        return getCurrentUserId()
            .map(currentUserId -> currentUserId.equals(targetUserId))
            .orElse(false);
    }

    /**
     * Require that the current user can access specific user data.
     */
    public static void requireUserDataAccess(String targetUserId) {
        if (!canAccessUserData(targetUserId)) {
            throw new RuntimeException("Access denied: Cannot access user data for user: " + targetUserId);
        }
    }

    /**
     * Clear the security context (useful for testing).
     */
    public static void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Get authentication details for logging purposes.
     */
    public static String getAuthenticationInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null) {
            return "No authentication";
        }
        
        if (!authentication.isAuthenticated()) {
            return "Not authenticated";
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserPrincipal) {
            UserPrincipal user = (UserPrincipal) principal;
            return String.format("User: %s (%s) - Roles: %s", 
                               user.getName(), 
                               user.getEmail(), 
                               user.getRoles());
        }
        
        if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            return String.format("UserDetails: %s - Authorities: %s", 
                               userDetails.getUsername(), 
                               userDetails.getAuthorities());
        }
        
        return "Principal: " + principal.toString();
    }
}