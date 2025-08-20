package com.smartfarm.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Custom UserDetails implementation for JWT-authenticated users.
 * Represents the authenticated user in the Spring Security context.
 */
public class UserPrincipal implements UserDetails {

    private final String userId;
    private final String email;
    private final String name;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;

    public UserPrincipal(String userId, String email, String name, String[] roles) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.enabled = true;
        
        // Convert roles to authorities with ROLE_ prefix
        this.authorities = Arrays.stream(roles)
            .map(role -> {
                // Ensure ROLE_ prefix for Spring Security
                if (!role.startsWith("ROLE_")) {
                    return "ROLE_" + role.toUpperCase();
                }
                return role.toUpperCase();
            })
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
    }

    public UserPrincipal(String userId, String email, String name, Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.authorities = authorities;
        this.enabled = true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        // JWT authentication doesn't use passwords
        return null;
    }

    @Override
    public String getUsername() {
        // Use email as username
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // Custom getters
    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    /**
     * Check if user has a specific role.
     */
    public boolean hasRole(String role) {
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return authorities.stream()
            .anyMatch(auth -> auth.getAuthority().equals(roleWithPrefix.toUpperCase()));
    }

    /**
     * Get user roles without ROLE_ prefix.
     */
    public List<String> getRoles() {
        return authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .map(auth -> auth.startsWith("ROLE_") ? auth.substring(5) : auth)
            .collect(Collectors.toList());
    }

    /**
     * Check if user is an admin.
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Check if user is a manager or admin.
     */
    public boolean isManagerOrAdmin() {
        return hasRole("ADMIN") || hasRole("MANAGER");
    }

    /**
     * Check if user is an internal service.
     */
    public boolean isInternalService() {
        return hasRole("INTERNAL_SERVICE");
    }

    @Override
    public String toString() {
        return "UserPrincipal{" +
                "userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", authorities=" + authorities +
                ", enabled=" + enabled +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserPrincipal that = (UserPrincipal) o;
        return userId != null ? userId.equals(that.userId) : that.userId == null;
    }

    @Override
    public int hashCode() {
        return userId != null ? userId.hashCode() : 0;
    }
}