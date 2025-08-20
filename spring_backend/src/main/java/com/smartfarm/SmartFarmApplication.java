package com.smartfarm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main Spring Boot application class for IoT Smart Farm Management System.
 * 
 * This application provides:
 * - REST API for farm, room, and device management
 * - Real-time IoT data processing via MQTT
 * - WebSocket support for live updates
 * - AWS Cognito authentication integration
 * - PostgreSQL database with JPA/Hibernate
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
public class SmartFarmApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartFarmApplication.class, args);
    }
}