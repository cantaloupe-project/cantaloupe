package edu.illinois.library.cantaloupe.config;

import edu.illinois.library.cantaloupe.processor.codec.IIOProviderContextListener;
import edu.illinois.library.cantaloupe.resource.FileServlet;
import edu.illinois.library.cantaloupe.resource.HandlerServlet;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

/**
 * Spring Boot configuration for registering Cantaloupe's existing servlets.
 * This allows the existing HandlerServlet to work within Spring Boot's embedded server.
 */
@Configuration
public class ServletConfiguration {

    /**
     * Register the main Cantaloupe HandlerServlet to handle requests.
     * Excludes static resource paths to allow Spring Boot to serve them.
     */
    @Bean
    public ServletRegistrationBean<HandlerServlet> cantaloupeServlet() {
        ServletRegistrationBean<HandlerServlet> registration =
            new ServletRegistrationBean<>(new HandlerServlet());

        // Map to all Cantaloupe endpoints but exclude static resources
        registration.addUrlMappings(
            "/",
            "/iiif/*",
            "/admin",
            "/admin/*",
            "/api/*",
            "/tasks/*",
            "/health",
            "/status",
            "/configuration"
        );

        registration.setName("CantaloupeHandler");
        registration.setLoadOnStartup(1);
        registration.setAsyncSupported(true);

        return registration;
    }

    /**
     * Register the FileServlet to handle static resource requests.
     * This serves files from /webapp/ directory under /static/ URLs.
     */
    @Bean
    public ServletRegistrationBean<FileServlet> fileServlet() {
        ServletRegistrationBean<FileServlet> registration =
            new ServletRegistrationBean<>(new FileServlet(), "/static/*");

        registration.setName("FileServlet");
        registration.setLoadOnStartup(2);

        return registration;
    }

    /**
     * Register the IIOProviderContextListener to ensure proper ImageIO setup.
     * This is critical for image processing functionality.
     */
    @Bean
    public ServletContextInitializer iioProviderContextInitializer() {
        return new ServletContextInitializer() {
            @Override
            public void onStartup(ServletContext servletContext) throws ServletException {
                // Register the IIOProviderContextListener
                IIOProviderContextListener listener = new IIOProviderContextListener();
                servletContext.addListener(listener);

                // Set servlet context attributes that Cantaloupe might need
                servletContext.setInitParameter("cantaloupe.config.source", "spring-boot");
            }
        };
    }
}
