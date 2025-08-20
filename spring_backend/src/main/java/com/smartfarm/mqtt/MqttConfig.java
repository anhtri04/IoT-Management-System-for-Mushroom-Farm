package com.smartfarm.mqtt;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Properties;

/**
 * MQTT configuration for AWS IoT Core connectivity.
 * Handles SSL/TLS configuration and connection parameters.
 */
@Configuration
public class MqttConfig {

    private static final Logger logger = LoggerFactory.getLogger(MqttConfig.class);

    @Value("${aws.iot.endpoint:}")
    private String iotEndpoint;

    @Value("${aws.iot.certificatePath:}")
    private String certificatePath;

    @Value("${aws.iot.privateKeyPath:}")
    private String privateKeyPath;

    @Value("${aws.iot.caCertPath:}")
    private String caCertPath;

    @Value("${aws.iot.keystorePassword:changeit}")
    private String keystorePassword;

    @Value("${mqtt.qos:1}")
    private int defaultQos;

    @Value("${mqtt.keepAlive:60}")
    private int keepAliveInterval;

    @Value("${mqtt.connectionTimeout:30}")
    private int connectionTimeout;

    @Value("${mqtt.maxReconnectDelay:128000}")
    private int maxReconnectDelay;

    @Value("${mqtt.development.enabled:true}")
    private boolean developmentMode;

    /**
     * Create MQTT connection options for AWS IoT Core.
     */
    @Bean
    public MqttConnectOptions mqttConnectOptions() {
        logger.info("Configuring MQTT connection options");
        
        MqttConnectOptions options = new MqttConnectOptions();
        
        // Basic connection settings
        options.setCleanSession(true);
        options.setKeepAliveInterval(keepAliveInterval);
        options.setConnectionTimeout(connectionTimeout);
        options.setAutomaticReconnect(true);
        options.setMaxReconnectDelay(maxReconnectDelay);
        
        // Set MQTT version
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        
        if (!developmentMode) {
            // Configure SSL for production
            configureSSL(options);
        } else {
            logger.warn("MQTT running in development mode - SSL configuration skipped");
        }
        
        logger.info("MQTT connection options configured successfully");
        return options;
    }

    /**
     * Configure SSL/TLS settings for AWS IoT Core.
     */
    private void configureSSL(MqttConnectOptions options) {
        try {
            if (certificatePath == null || certificatePath.trim().isEmpty()) {
                logger.warn("Certificate path not configured - using default SSL settings");
                return;
            }
            
            logger.info("Configuring SSL with certificate: {}", certificatePath);
            
            // Create SSL context with device certificates
            SSLContext sslContext = createSSLContext();
            options.setSocketFactory(sslContext.getSocketFactory());
            
            // Set SSL properties
            Properties sslProps = new Properties();
            sslProps.setProperty("com.ibm.ssl.protocol", "TLSv1.2");
            sslProps.setProperty("com.ibm.ssl.contextProvider", "IBMJSSE2");
            sslProps.setProperty("com.ibm.ssl.keyStore", certificatePath);
            sslProps.setProperty("com.ibm.ssl.keyStorePassword", keystorePassword);
            sslProps.setProperty("com.ibm.ssl.keyStoreType", "JKS");
            sslProps.setProperty("com.ibm.ssl.trustStore", caCertPath);
            sslProps.setProperty("com.ibm.ssl.trustStorePassword", keystorePassword);
            sslProps.setProperty("com.ibm.ssl.trustStoreType", "JKS");
            
            options.setSSLProperties(sslProps);
            
            logger.info("SSL configuration completed successfully");
            
        } catch (Exception e) {
            logger.error("Failed to configure SSL for MQTT connection", e);
            throw new RuntimeException("SSL configuration failed", e);
        }
    }

    /**
     * Create SSL context with device certificates for AWS IoT Core.
     */
    private SSLContext createSSLContext() throws Exception {
        logger.debug("Creating SSL context with device certificates");
        
        // Load keystore (device certificate and private key)
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream keyStoreStream = new FileInputStream(certificatePath)) {
            keyStore.load(keyStoreStream, keystorePassword.toCharArray());
        }
        
        // Load truststore (CA certificate)
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (FileInputStream trustStoreStream = new FileInputStream(caCertPath)) {
            trustStore.load(trustStoreStream, keystorePassword.toCharArray());
        }
        
        // Initialize key manager factory
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keystorePassword.toCharArray());
        
        // Initialize trust manager factory
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        
        // Create SSL context
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(
            keyManagerFactory.getKeyManagers(),
            trustManagerFactory.getTrustManagers(),
            new SecureRandom()
        );
        
        logger.debug("SSL context created successfully");
        return sslContext;
    }

    /**
     * MQTT configuration properties bean.
     */
    @Bean
    public MqttProperties mqttProperties() {
        MqttProperties properties = new MqttProperties();
        properties.setEndpoint(iotEndpoint);
        properties.setCertificatePath(certificatePath);
        properties.setPrivateKeyPath(privateKeyPath);
        properties.setCaCertPath(caCertPath);
        properties.setKeystorePassword(keystorePassword);
        properties.setDefaultQos(defaultQos);
        properties.setKeepAliveInterval(keepAliveInterval);
        properties.setConnectionTimeout(connectionTimeout);
        properties.setMaxReconnectDelay(maxReconnectDelay);
        properties.setDevelopmentMode(developmentMode);
        
        return properties;
    }

    /**
     * MQTT properties holder class.
     */
    public static class MqttProperties {
        private String endpoint;
        private String certificatePath;
        private String privateKeyPath;
        private String caCertPath;
        private String keystorePassword;
        private int defaultQos;
        private int keepAliveInterval;
        private int connectionTimeout;
        private int maxReconnectDelay;
        private boolean developmentMode;

        // Getters and setters
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

        public String getCertificatePath() { return certificatePath; }
        public void setCertificatePath(String certificatePath) { this.certificatePath = certificatePath; }

        public String getPrivateKeyPath() { return privateKeyPath; }
        public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }

        public String getCaCertPath() { return caCertPath; }
        public void setCaCertPath(String caCertPath) { this.caCertPath = caCertPath; }

        public String getKeystorePassword() { return keystorePassword; }
        public void setKeystorePassword(String keystorePassword) { this.keystorePassword = keystorePassword; }

        public int getDefaultQos() { return defaultQos; }
        public void setDefaultQos(int defaultQos) { this.defaultQos = defaultQos; }

        public int getKeepAliveInterval() { return keepAliveInterval; }
        public void setKeepAliveInterval(int keepAliveInterval) { this.keepAliveInterval = keepAliveInterval; }

        public int getConnectionTimeout() { return connectionTimeout; }
        public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }

        public int getMaxReconnectDelay() { return maxReconnectDelay; }
        public void setMaxReconnectDelay(int maxReconnectDelay) { this.maxReconnectDelay = maxReconnectDelay; }

        public boolean isDevelopmentMode() { return developmentMode; }
        public void setDevelopmentMode(boolean developmentMode) { this.developmentMode = developmentMode; }
    }
}