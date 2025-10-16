package edu.illinois.library.cantaloupe.config;

import edu.illinois.library.cantaloupe.processor.codec.IIOProviderContextListener;
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
     * Register the main Cantaloupe HandlerServlet to handle all requests.
     * This preserves the existing routing and processing logic.
     */
    @Bean
    public ServletRegistrationBean<HandlerServlet> cantaloupeServlet() {
        ServletRegistrationBean<HandlerServlet> registration =
            new ServletRegistrationBean<>(new HandlerServlet(), "/*");

        registration.setName("CantaloupeHandler");
        registration.setLoadOnStartup(1);
        registration.setAsyncSupported(true);

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
