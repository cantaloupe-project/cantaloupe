package edu.illinois.library.cantaloupe.config;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import edu.illinois.library.cantaloupe.delegate.DelegateProxyService;
import jakarta.annotation.PostConstruct;

/**
 * Spring Boot web configuration for Cantaloupe.
 * Handles static file serving and other web-specific configurations.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired(required = false)
    private Optional<DelegateProxyService> delegateProxyService;

    @Bean
    public edu.illinois.library.cantaloupe.config.Configuration configuration() {
        return edu.illinois.library.cantaloupe.config.Configuration.getInstance();
    }

    /**
     * Initialize DelegateProxyService after Spring context is ready.
     * This ensures the service starts watching for script changes if needed.
     */
    @PostConstruct
    public void initializeDelegateProxyService() {
        delegateProxyService.ifPresent(DelegateProxyService::startWatching);
    }

    /**
     * Configure static resource handlers.
     * Replaces the FileServlet functionality for serving static files.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Handle static files similar to the previous FileServlet mapping
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/webapp/")
                .setCachePeriod(31536000); // 1 year cache

        // Handle favicon and other root-level static files
        registry.addResourceHandler("/favicon.ico", "/robots.txt", "/*.html")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(86400); // 1 day cache
    }
}
