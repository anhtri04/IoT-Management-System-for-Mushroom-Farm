package com.smartfarm.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration for JWT authentication and authorization.
 * Configures security policies, CORS, and integrates JWT authentication filter.
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                         JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    /**
     * Configure HTTP security with JWT authentication.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring Spring Security with JWT authentication");

        http
            // Disable CSRF for stateless API
            .csrf().disable()
            
            // Enable CORS
            .cors().configurationSource(corsConfigurationSource())
            
            .and()
            
            // Set session management to stateless
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            
            .and()
            
            // Set unauthorized requests exception handler
            .exceptionHandling()
            .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            
            .and()
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - no authentication required
                .requestMatchers(
                    "/auth/**",
                    "/actuator/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/",
                    "/health",
                    "/favicon.ico"
                ).permitAll()
                
                // Internal service endpoints - require internal token
                .requestMatchers("/internal/**")
                .hasRole("INTERNAL_SERVICE")
                
                // Admin endpoints - require admin role
                .requestMatchers(
                    "/api/admin/**",
                    "/api/users/*/role"
                ).hasRole("ADMIN")
                
                // Manager endpoints - require manager or admin role
                .requestMatchers(
                    "/api/farms/*/users",
                    "/api/rooms/*/assign",
                    "/api/devices/register"
                ).hasAnyRole("MANAGER", "ADMIN")
                
                // All other API endpoints require authentication
                .requestMatchers("/api/**")
                .authenticated()
                
                // WebSocket endpoints require authentication
                .requestMatchers("/ws/**")
                .authenticated()
                
                // Any other request requires authentication
                .anyRequest().authenticated()
            );

        // Add JWT authentication filter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        logger.info("Spring Security configuration completed");
        return http.build();
    }

    /**
     * Configure CORS to allow cross-origin requests from frontend applications.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        logger.info("Configuring CORS settings");
        
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow specific origins (configure based on your frontend URLs)
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:3000",    // React development server
            "http://localhost:3001",    // Alternative React port
            "http://localhost:8080",    // Alternative frontend port
            "https://*.smartfarm.com",  // Production domain
            "https://*.amazonaws.com"    // AWS hosted frontends
        ));
        
        // Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Expose headers that frontend might need
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Total-Count",
            "X-Page-Number",
            "X-Page-Size"
        ));
        
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        logger.info("CORS configuration completed");
        return source;
    }
}