package edu.illinois.library.cantaloupe.config;

import org.eclipse.jetty.ee10.servlet.ServletHandler;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.embedded.jetty.JettyWebServer;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot configuration for customizing the embedded Jetty server.
 * Configures URI compliance and servlet handler settings for proper handling
 * of ambiguous URIs and path characters.
 *
 * This configuration addresses two main requirements:
 * 
 * * Configures Jetty to accept suspicious path characters and ambiguous path separators by setting:
 *     `UriCompliance.from("DEFAULT,SUSPICIOUS_PATH_CHARACTERS,AMBIGUOUS_PATH_SEPARATOR")'
 * 
 * * Enables ServletHandler to decode ambiguous URIs by calling `handler.setDecodeAmbiguousURIs(true)'
 *
 *
 * Timing Considerations:
 * URI compliance is configured during server startup via ServerCustomizers, while
 * ServletHandler configuration is performed after application startup via ApplicationReadyEvent
 * to ensure the handlers are fully initialized.
 */
@Configuration
public class JettyConfiguration implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JettyConfiguration.class);

    /**
     * Customizes the Jetty web server factory to configure URI compliance
     * and servlet handler settings.
     *
     * @return WebServerFactoryCustomizer for JettyServletWebServerFactory
     */
    @Bean
    public WebServerFactoryCustomizer<JettyServletWebServerFactory> jettyCustomizer() {
        return factory -> {
            // Customize the Jetty server configuration
            factory.addServerCustomizers(server -> {
                LOGGER.debug("Configuring Jetty server during startup");
                configureUriCompliance(server);
                // Note: ServletHandlers may not be available at this point,
                // so we configure them in the ApplicationReadyEvent listener
            });
        };
    }

    /**
     * Handle application ready event to configure servlet handlers
     * after the application context is fully initialized.
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (event.getApplicationContext() instanceof ServletWebServerApplicationContext) {
            ServletWebServerApplicationContext context =
                (ServletWebServerApplicationContext) event.getApplicationContext();

            if (context.getWebServer() instanceof JettyWebServer) {
                JettyWebServer jettyWebServer = (JettyWebServer) context.getWebServer();
                try {
                    Server server = jettyWebServer.getServer();
                    configureServletHandlers(server);
                    LOGGER.info("Successfully configured Jetty servlet handlers");
                } catch (Exception e) {
                    LOGGER.warn("Failed to configure Jetty servlet handlers: {}", e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Configure URI compliance settings to allow suspicious path characters
     * and ambiguous path separators.
     *
     * @param server the Jetty server instance
     */
    private void configureUriCompliance(Server server) {
        if (server == null) {
            LOGGER.warn("Server is null, cannot configure URI compliance");
            return;
        }

        var connectors = server.getConnectors();
        if (connectors == null || connectors.length == 0) {
            LOGGER.warn("No connectors found on server, URI compliance not configured");
            return;
        }

        int configuredCount = 0;
        for (var connector : connectors) {
            if (connector == null) continue;

            for (var factory : connector.getConnectionFactories()) {
                if (factory instanceof org.eclipse.jetty.server.HttpConnectionFactory) {
                    try {
                        HttpConfiguration config = ((org.eclipse.jetty.server.HttpConnectionFactory) factory)
                                .getHttpConfiguration();

                        // Set URI compliance to allow suspicious path characters and ambiguous path separators
                        config.setUriCompliance(
                                UriCompliance.from("DEFAULT,SUSPICIOUS_PATH_CHARACTERS,AMBIGUOUS_PATH_SEPARATOR"));
                        configuredCount++;
                        LOGGER.debug("Configured URI compliance on HttpConnectionFactory: {}",
                                   config.getUriCompliance());
                    } catch (Exception e) {
                        LOGGER.warn("Failed to configure URI compliance on HttpConnectionFactory: {}",
                                  e.getMessage(), e);
                    }
                }
            }
        }

        LOGGER.info("Successfully configured URI compliance on {} HttpConnectionFactory instances", configuredCount);
    }

    /**
     * Configure servlet handlers to decode ambiguous URIs.
     *
     * @param server the Jetty server instance
     */
    private void configureServletHandlers(Server server) {
        // Configure all ServletHandler beans to decode ambiguous URIs
        var handlers = server.getContainedBeans(ServletHandler.class);
        LOGGER.debug("Found {} ServletHandler instances", handlers.size());

        handlers.forEach(handler -> {
            handler.setDecodeAmbiguousURIs(true);
            LOGGER.debug("Configured ServletHandler to decode ambiguous URIs: {}",
                        handler.getClass().getSimpleName());
        });

        if (handlers.isEmpty()) {
            LOGGER.warn("No ServletHandler instances found - ambiguous URI decoding not configured");
        }
    }
}
