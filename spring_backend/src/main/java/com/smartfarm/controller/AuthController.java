package com.smartfarm.controller;

import com.smartfarm.dto.UserDto;
import com.smartfarm.dto.UserCreateDto;
import com.smartfarm.dto.LoginRequestDto;
import com.smartfarm.dto.LoginResponseDto;
import com.smartfarm.service.UserService;
import com.smartfarm.exception.ValidationException;
import com.smartfarm.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.util.Map;
import java.util.HashMap;

/**
 * REST controller for authentication operations.
 * Handles user registration, login, and authentication-related endpoints.
 */
@RestController
@RequestMapping("/auth")
@Validated
@Tag(name = "Authentication", description = "User authentication and registration operations")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Register a new user.
     * Creates a user in both Cognito and the local database.
     *
     * @param createDto the user registration data
     * @return the created user information
     */
    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Create a new user account in Cognito and local database")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "User already exists")
    })
    public ResponseEntity<Map<String, Object>> registerUser(@Valid @RequestBody UserCreateDto createDto) {
        logger.info("User registration request for email: {}", createDto.getEmail());

        try {
            UserDto createdUser = userService.createUser(createDto);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User registered successfully");
            response.put("user", createdUser);
            
            logger.info("User registered successfully: {}", createdUser.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (ValidationException e) {
            logger.warn("User registration validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "validation_error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("User registration failed for email: {}", createDto.getEmail(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "registration_failed");
            errorResponse.put("message", "Failed to register user");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * User login endpoint.
     * Note: In production, this would integrate with AWS Cognito for authentication.
     * This is a placeholder implementation.
     *
     * @param loginRequest the login credentials
     * @return login response with tokens
     */
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return access tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        logger.info("Login request for email: {}", loginRequest.getEmail());

        try {
            // TODO: Integrate with AWS Cognito for actual authentication
            // For now, validate user exists in local database
            UserDto user = userService.getUserByEmail(loginRequest.getEmail());
            
            if (user == null) {
                throw new UnauthorizedException("Invalid credentials");
            }

            // Mock JWT tokens (in production, these would come from Cognito)
            LoginResponseDto loginResponse = new LoginResponseDto();
            loginResponse.setAccessToken("mock-access-token-" + System.currentTimeMillis());
            loginResponse.setRefreshToken("mock-refresh-token-" + System.currentTimeMillis());
            loginResponse.setTokenType("Bearer");
            loginResponse.setExpiresIn(3600); // 1 hour
            loginResponse.setUser(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("data", loginResponse);
            
            logger.info("User logged in successfully: {}", user.getUserId());
            return ResponseEntity.ok(response);
            
        } catch (UnauthorizedException e) {
            logger.warn("Login failed for email: {} - {}", loginRequest.getEmail(), e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "unauthorized");
            errorResponse.put("message", "Invalid credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Login error for email: {}", loginRequest.getEmail(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "login_failed");
            errorResponse.put("message", "Login failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Refresh access token.
     * Note: In production, this would validate the refresh token with Cognito.
     *
     * @param refreshToken the refresh token
     * @return new access token
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Get a new access token using refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid refresh token")
    })
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refresh_token");
        logger.info("Token refresh request");

        try {
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                throw new ValidationException("Refresh token is required");
            }

            // TODO: Validate refresh token with Cognito
            // For now, generate mock tokens
            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("access_token", "mock-access-token-" + System.currentTimeMillis());
            tokenData.put("token_type", "Bearer");
            tokenData.put("expires_in", 3600);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Token refreshed successfully");
            response.put("data", tokenData);
            
            logger.info("Token refreshed successfully");
            return ResponseEntity.ok(response);
            
        } catch (ValidationException e) {
            logger.warn("Token refresh validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "validation_error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Token refresh failed", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "refresh_failed");
            errorResponse.put("message", "Failed to refresh token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    /**
     * User logout endpoint.
     * Note: In production, this would invalidate tokens in Cognito.
     *
     * @return logout confirmation
     */
    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Logout user and invalidate tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful")
    })
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        logger.info("Logout request");

        try {
            // TODO: Invalidate tokens in Cognito
            // Extract token from Authorization header if needed
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Logout successful");
            
            logger.info("User logged out successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Logout failed", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "logout_failed");
            errorResponse.put("message", "Logout failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Verify email endpoint.
     * Note: In production, this would verify email with Cognito.
     *
     * @param verificationCode the verification code
     * @param email the email to verify
     * @return verification result
     */
    @PostMapping("/verify-email")
    @Operation(summary = "Verify email", description = "Verify user email with confirmation code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid verification code")
    })
    public ResponseEntity<Map<String, Object>> verifyEmail(
            @RequestParam @NotBlank String verificationCode,
            @RequestParam @Email String email) {
        logger.info("Email verification request for: {}", email);

        try {
            // TODO: Verify email with Cognito
            // For now, mock verification
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email verified successfully");
            
            logger.info("Email verified successfully for: {}", email);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Email verification failed for: {}", email, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "verification_failed");
            errorResponse.put("message", "Email verification failed");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Resend verification code endpoint.
     *
     * @param email the email to resend verification code to
     * @return resend result
     */
    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification code", description = "Resend email verification code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification code sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid email")
    })
    public ResponseEntity<Map<String, Object>> resendVerificationCode(@RequestParam @Email String email) {
        logger.info("Resend verification code request for: {}", email);

        try {
            // TODO: Resend verification code via Cognito
            // For now, mock resend
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Verification code sent successfully");
            
            logger.info("Verification code resent successfully for: {}", email);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to resend verification code for: {}", email, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "resend_failed");
            errorResponse.put("message", "Failed to resend verification code");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Forgot password endpoint.
     *
     * @param email the email to send password reset to
     * @return password reset result
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Send password reset code to email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset code sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid email")
    })
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestParam @Email String email) {
        logger.info("Forgot password request for: {}", email);

        try {
            // TODO: Send password reset code via Cognito
            // For now, mock password reset
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password reset code sent successfully");
            
            logger.info("Password reset code sent successfully for: {}", email);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to send password reset code for: {}", email, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "reset_failed");
            errorResponse.put("message", "Failed to send password reset code");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Reset password endpoint.
     *
     * @param request the password reset request
     * @return password reset result
     */
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset password with confirmation code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid reset code or password")
    })
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String resetCode = request.get("reset_code");
        String newPassword = request.get("new_password");
        
        logger.info("Password reset request for: {}", email);

        try {
            if (email == null || resetCode == null || newPassword == null) {
                throw new ValidationException("Email, reset code, and new password are required");
            }

            // TODO: Reset password via Cognito
            // For now, mock password reset
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password reset successfully");
            
            logger.info("Password reset successfully for: {}", email);
            return ResponseEntity.ok(response);
            
        } catch (ValidationException e) {
            logger.warn("Password reset validation failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "validation_error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Password reset failed for: {}", email, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "reset_failed");
            errorResponse.put("message", "Password reset failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get current user profile.
     * Note: In production, this would extract user info from JWT token.
     *
     * @param authHeader the authorization header
     * @return current user profile
     */
    @GetMapping("/profile")
    @Operation(summary = "Get current user profile", description = "Get current authenticated user profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, Object>> getCurrentUserProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        logger.info("Get current user profile request");

        try {
            // TODO: Extract user ID from JWT token
            // For now, return mock user profile
            
            Map<String, Object> userProfile = new HashMap<>();
            userProfile.put("user_id", "mock-user-id");
            userProfile.put("email", "user@example.com");
            userProfile.put("full_name", "Mock User");
            userProfile.put("role", "admin");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", userProfile);
            
            logger.info("User profile retrieved successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get user profile", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "profile_failed");
            errorResponse.put("message", "Failed to get user profile");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }
}