package com.smartfarm.security;

import com.smartfarm.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/**
 * JWT Authentication Filter for AWS Cognito tokens.
 * Validates JWT tokens and sets up Spring Security authentication context.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    @Value("${aws.cognito.region:us-east-1}")
    private String cognitoRegion;

    @Value("${aws.cognito.userPoolId:}")
    private String userPoolId;

    @Value("${app.internal.token:internal-service-token}")
    private String internalServiceToken;

    @Value("${app.jwt.secret:mySecretKey}")
    private String jwtSecret;

    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Autowired
    public JwtAuthenticationFilter(UserService userService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Skip authentication for public endpoints
            String requestPath = request.getRequestURI();
            if (isPublicEndpoint(requestPath)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Check for internal service token first
            String internalToken = request.getHeader(INTERNAL_TOKEN_HEADER);
            if (internalToken != null && internalToken.equals(internalServiceToken)) {
                setInternalServiceAuthentication();
                filterChain.doFilter(request, response);
                return;
            }

            // Extract JWT token from Authorization header
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                logger.debug("No valid Authorization header found for path: {}", requestPath);
                sendUnauthorizedResponse(response, "Missing or invalid Authorization header");
                return;
            }

            String token = authHeader.substring(BEARER_PREFIX.length());
            
            // Validate and process JWT token
            if (validateAndSetAuthentication(token)) {
                filterChain.doFilter(request, response);
            } else {
                sendUnauthorizedResponse(response, "Invalid or expired JWT token");
            }

        } catch (Exception e) {
            logger.error("Authentication error: ", e);
            sendUnauthorizedResponse(response, "Authentication failed");
        }
    }

    /**
     * Check if the endpoint is public and doesn't require authentication.
     */
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/auth/") ||
               path.startsWith("/actuator/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs") ||
               path.equals("/") ||
               path.equals("/health");
    }

    /**
     * Validate JWT token and set authentication context.
     */
    private boolean validateAndSetAuthentication(String token) {
        try {
            // For development/testing, use a simple validation
            // In production, this should validate against AWS Cognito JWKs
            if (isDevelopmentMode()) {
                return validateDevelopmentToken(token);
            } else {
                return validateCognitoToken(token);
            }
        } catch (Exception e) {
            logger.error("Token validation failed: ", e);
            return false;
        }
    }

    /**
     * Validate token in development mode (simplified validation).
     */
    private boolean validateDevelopmentToken(String token) {
        try {
            // Simple validation for development
            Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String userId = claims.getSubject();
            String email = claims.get("email", String.class);
            String role = claims.get("role", String.class);

            if (userId != null && email != null) {
                setUserAuthentication(userId, email, role != null ? role : "viewer");
                return true;
            }

            return false;
        } catch (Exception e) {
            logger.debug("Development token validation failed: ", e);
            return false;
        }
    }

    /**
     * Validate AWS Cognito JWT token.
     * In production, this should fetch and validate against Cognito JWKs.
     */
    private boolean validateCognitoToken(String token) {
        try {
            // TODO: Implement proper Cognito JWT validation
            // This should:
            // 1. Fetch JWKs from Cognito
            // 2. Validate token signature
            // 3. Validate token claims (iss, aud, exp, etc.)
            
            // For now, return a mock validation
            logger.warn("Cognito JWT validation not fully implemented - using mock validation");
            
            // Mock validation - decode token without verification
            String[] tokenParts = token.split("\\.");
            if (tokenParts.length != 3) {
                return false;
            }

            String payload = new String(Base64.getUrlDecoder().decode(tokenParts[1]));
            Map<String, Object> claims = objectMapper.readValue(payload, Map.class);

            String cognitoSub = (String) claims.get("sub");
            String email = (String) claims.get("email");
            
            if (cognitoSub != null && email != null) {
                // Try to find existing user or create new one
                String role = "viewer"; // Default role
                setUserAuthentication(cognitoSub, email, role);
                return true;
            }

            return false;
        } catch (Exception e) {
            logger.error("Cognito token validation failed: ", e);
            return false;
        }
    }

    /**
     * Set authentication context for a regular user.
     */
    private void setUserAuthentication(String userId, String email, String role) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        UserPrincipal userPrincipal = new UserPrincipal(userId, email, role);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal, null, authorities);
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        logger.debug("Set user authentication: {} ({})", email, role);
    }

    /**
     * Set authentication context for internal service calls.
     */
    private void setInternalServiceAuthentication() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE"));

        UserPrincipal servicePrincipal = new UserPrincipal("internal-service", "internal@service.local", "service");
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                servicePrincipal, null, authorities);
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        logger.debug("Set internal service authentication");
    }

    /**
     * Send unauthorized response.
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", "unauthorized");
        errorResponse.put("message", message);
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * Check if running in development mode.
     */
    private boolean isDevelopmentMode() {
        // Check if Cognito configuration is missing (development mode)
        return userPoolId == null || userPoolId.isEmpty();
    }

    /**
     * User principal class for authentication context.
     */
    public static class UserPrincipal {
        private final String userId;
        private final String email;
        private final String role;

        public UserPrincipal(String userId, String email, String role) {
            this.userId = userId;
            this.email = email;
            this.role = role;
        }

        public String getUserId() {
            return userId;
        }

        public UUID getUserIdAsUUID() {
            try {
                return UUID.fromString(userId);
            } catch (IllegalArgumentException e) {
                // For non-UUID user IDs (like Cognito sub), generate a deterministic UUID
                return UUID.nameUUIDFromBytes(userId.getBytes());
            }
        }

        public String getEmail() {
            return email;
        }

        public String getRole() {
            return role;
        }

        @Override
        public String toString() {
            return "UserPrincipal{" +
                    "userId='" + userId + '\'' +
                    ", email='" + email + '\'' +
                    ", role='" + role + '\'' +
                    '}';
        }
    }
}