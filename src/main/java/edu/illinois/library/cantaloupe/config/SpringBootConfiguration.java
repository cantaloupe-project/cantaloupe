package edu.illinois.library.cantaloupe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Boot configuration class for Cantaloupe image server.
 * This class bridges the existing Cantaloupe configuration system with Spring Boot.
 */
@Configuration
public class SpringBootConfiguration {

    /**
     * Configure CORS settings based on Cantaloupe configuration.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                edu.illinois.library.cantaloupe.config.Configuration config =
                    edu.illinois.library.cantaloupe.config.Configuration.getInstance();

                // CORS configuration - using basic setup since specific CORS keys may not exist
                // You can extend this based on your Cantaloupe configuration needs
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                        .allowedHeaders("*")
                        .allowCredentials(false)
                        .maxAge(3600);
            }
        };
    }

    /**
     * Configuration properties for server settings.
     */
    @ConfigurationProperties(prefix = "server")
    public static class ServerProperties {
        private int port = 8182; // Cantaloupe default
        private String address = "0.0.0.0";

        public int getPort() {
            // Override with Cantaloupe config if available
            try {
                edu.illinois.library.cantaloupe.config.Configuration config =
                    edu.illinois.library.cantaloupe.config.Configuration.getInstance();
                return config.getInt(Key.HTTP_PORT, port);
            } catch (Exception e) {
                return port;
            }
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getAddress() {
            // Override with Cantaloupe config if available
            try {
                edu.illinois.library.cantaloupe.config.Configuration config =
                    edu.illinois.library.cantaloupe.config.Configuration.getInstance();
                return config.getString(Key.HTTP_HOST, address);
            } catch (Exception e) {
                return address;
            }
        }

        public void setAddress(String address) {
            this.address = address;
        }
    }

    @Bean
    @ConfigurationProperties(prefix = "server")
    public ServerProperties serverProperties() {
        return new ServerProperties();
    }
}
