package edu.illinois.library.cantaloupe.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

/**
 * Spring Boot web configuration for Cantaloupe.
 * Handles static file serving and other web-specific configurations.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * This is necessary for putting page numbers in the identifier. Otherwise Spring Boot
     * tries to parse this as a Matrix Variable.
     */
    @Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
            UrlPathHelper urlPathHelper = new UrlPathHelper();
            urlPathHelper.setRemoveSemicolonContent(false);
            configurer.setUrlPathHelper(urlPathHelper);
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
