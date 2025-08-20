package com.smartfarm.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT utility class for token validation and parsing.
 * Supports both AWS Cognito JWT validation and development/testing tokens.
 */
@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Cache for Cognito public keys
    private final Map<String, PublicKey> publicKeyCache = new ConcurrentHashMap<>();
    
    @Value("${aws.cognito.region:us-east-1}")
    private String cognitoRegion;
    
    @Value("${aws.cognito.userPoolId:}")
    private String userPoolId;
    
    @Value("${app.security.jwt.secret:smartfarm-dev-secret-key-change-in-production}")
    private String jwtSecret;
    
    @Value("${app.security.internal.token:internal-service-token-change-in-production}")
    private String internalServiceToken;
    
    @Value("${app.security.development.enabled:true}")
    private boolean developmentMode;

    /**
     * Validate JWT token and extract claims.
     */
    public Claims validateToken(String token) {
        try {
            // Check if it's an internal service token
            if (isInternalServiceToken(token)) {
                return createInternalServiceClaims();
            }
            
            // In development mode, allow mock tokens
            if (developmentMode && token.startsWith("dev-")) {
                return validateDevelopmentToken(token);
            }
            
            // Validate Cognito JWT token
            return validateCognitoToken(token);
            
        } catch (Exception e) {
            logger.error("Token validation failed: {}", e.getMessage());
            throw new RuntimeException("Invalid token", e);
        }
    }
    
    /**
     * Check if token is an internal service token.
     */
    private boolean isInternalServiceToken(String token) {
        return internalServiceToken.equals(token);
    }
    
    /**
     * Create claims for internal service authentication.
     */
    private Claims createInternalServiceClaims() {
        Claims claims = Jwts.claims();
        claims.setSubject("internal-service");
        claims.put("cognito:groups", new String[]{"INTERNAL_SERVICE"});
        claims.put("email", "internal@smartfarm.system");
        claims.put("name", "Internal Service");
        return claims;
    }
    
    /**
     * Validate development/testing token.
     */
    private Claims validateDevelopmentToken(String token) {
        logger.debug("Validating development token: {}", token.substring(0, Math.min(token.length(), 20)) + "...");
        
        // Parse development token format: dev-{userId}-{role}
        String[] parts = token.split("-");
        if (parts.length < 3) {
            throw new RuntimeException("Invalid development token format");
        }
        
        String userId = parts[1];
        String role = parts[2];
        
        Claims claims = Jwts.claims();
        claims.setSubject(userId);
        claims.put("cognito:groups", new String[]{role.toUpperCase()});
        claims.put("email", userId + "@dev.smartfarm.com");
        claims.put("name", "Dev User " + userId);
        
        logger.debug("Development token validated for user: {} with role: {}", userId, role);
        return claims;
    }
    
    /**
     * Validate AWS Cognito JWT token.
     */
    private Claims validateCognitoToken(String token) {
        try {
            // Parse token header to get key ID
            String[] tokenParts = token.split("\\.");
            if (tokenParts.length != 3) {
                throw new RuntimeException("Invalid JWT token format");
            }
            
            // Decode header
            String headerJson = new String(Base64.getUrlDecoder().decode(tokenParts[0]));
            JsonNode header = objectMapper.readTree(headerJson);
            String keyId = header.get("kid").asText();
            
            // Get public key for verification
            PublicKey publicKey = getPublicKey(keyId);
            
            // Validate and parse token
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            // Validate token issuer
            String expectedIssuer = String.format("https://cognito-idp.%s.amazonaws.com/%s", 
                                                cognitoRegion, userPoolId);
            if (!expectedIssuer.equals(claims.getIssuer())) {
                throw new RuntimeException("Invalid token issuer");
            }
            
            // Validate token use
            if (!"access".equals(claims.get("token_use"))) {
                throw new RuntimeException("Invalid token use. Expected access token");
            }
            
            logger.debug("Cognito token validated for user: {}", claims.getSubject());
            return claims;
            
        } catch (Exception e) {
            logger.error("Cognito token validation failed: {}", e.getMessage());
            throw new RuntimeException("Invalid Cognito token", e);
        }
    }
    
    /**
     * Get public key from Cognito JWKS endpoint.
     */
    private PublicKey getPublicKey(String keyId) {
        // Check cache first
        if (publicKeyCache.containsKey(keyId)) {
            return publicKeyCache.get(keyId);
        }
        
        try {
            // Fetch JWKS from Cognito
            String jwksUrl = String.format("https://cognito-idp.%s.amazonaws.com/%s/.well-known/jwks.json",
                                         cognitoRegion, userPoolId);
            
            logger.debug("Fetching public key from JWKS: {}", jwksUrl);
            
            // In a real implementation, you would fetch from the URL
            // For now, we'll create a mock key for development
            if (developmentMode) {
                SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
                publicKeyCache.put(keyId, secretKey);
                return secretKey;
            }
            
            // TODO: Implement actual JWKS fetching
            // This would involve:
            // 1. HTTP GET to jwksUrl
            // 2. Parse JSON response
            // 3. Find key with matching kid
            // 4. Convert JWK to PublicKey
            // 5. Cache the key
            
            throw new RuntimeException("JWKS fetching not implemented yet. Use development mode.");
            
        } catch (Exception e) {
            logger.error("Failed to get public key for keyId: {}", keyId, e);
            throw new RuntimeException("Failed to get public key", e);
        }
    }
    
    /**
     * Extract user ID from JWT claims.
     */
    public String getUserIdFromClaims(Claims claims) {
        return claims.getSubject();
    }
    
    /**
     * Extract user email from JWT claims.
     */
    public String getUserEmailFromClaims(Claims claims) {
        return (String) claims.get("email");
    }
    
    /**
     * Extract user roles from JWT claims.
     */
    public String[] getUserRolesFromClaims(Claims claims) {
        Object groups = claims.get("cognito:groups");
        if (groups instanceof String[]) {
            return (String[]) groups;
        } else if (groups instanceof String) {
            return new String[]{(String) groups};
        }
        return new String[]{"VIEWER"}; // Default role
    }
    
    /**
     * Extract user name from JWT claims.
     */
    public String getUserNameFromClaims(Claims claims) {
        String name = (String) claims.get("name");
        if (name == null || name.trim().isEmpty()) {
            name = (String) claims.get("given_name");
            String familyName = (String) claims.get("family_name");
            if (familyName != null) {
                name = (name != null ? name : "") + " " + familyName;
            }
        }
        return name != null ? name.trim() : "Unknown User";
    }
}