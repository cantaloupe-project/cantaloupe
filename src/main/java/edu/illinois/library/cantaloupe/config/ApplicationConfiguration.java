package edu.illinois.library.cantaloupe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration class that provides the Cantaloupe Configuration
 * as a Spring-managed bean, enabling dependency injection instead of
 * using the singleton pattern with Configuration.getInstance().
 *
 * This is separated from WebConfig to avoid circular dependencies.
 */
@Configuration
public class ApplicationConfiguration {

    /**
     * Provides the global Configuration instance as a Spring bean.
     * This allows other Spring components to inject the Configuration
     * instead of calling Configuration.getInstance().
     *
     * @return The global Configuration instance
     */
    @Bean
    @Primary
    @Lazy
    public edu.illinois.library.cantaloupe.config.Configuration configuration() {
        // Use the existing factory to get the singleton instance
        // This maintains compatibility with existing non-Spring code
        try {
            return ConfigurationFactory.getInstance();
        } catch (Exception e) {
            // If configuration fails to initialize, log and rethrow
            throw new RuntimeException("Failed to initialize Configuration: " + e.getMessage(), e);
        }
    }
}
