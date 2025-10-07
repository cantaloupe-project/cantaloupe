package edu.illinois.library.cantaloupe.config.spring;

import edu.illinois.library.cantaloupe.config.Key;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;

/**
 * Spring Boot configuration for the embedded web server.
 * This replaces the custom Jetty server configuration from ApplicationServer.
 */
@Configuration
public class WebServerConfiguration {

    /**
     * Customizes the embedded web server based on Cantaloupe configuration.
     */
    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> webServerCustomizer() {
        return factory -> {
            try {
                edu.illinois.library.cantaloupe.config.Configuration config =
                    edu.illinois.library.cantaloupe.config.Configuration.getInstance();

                // Configure HTTP port and host
                if (config.getBoolean(Key.HTTP_ENABLED, true)) {
                    factory.setPort(config.getInt(Key.HTTP_PORT, 8182));
                    factory.setAddress(java.net.InetAddress.getByName(
                        config.getString(Key.HTTP_HOST, "0.0.0.0")));
                }

                // Configure HTTPS if enabled
                if (config.getBoolean(Key.HTTPS_ENABLED, false)) {
                    configureSsl(factory, config);
                }

                // Configure Jetty-specific settings if using Jetty
                if (factory instanceof JettyServletWebServerFactory) {
                    configureJetty((JettyServletWebServerFactory) factory, config);
                }

            } catch (Exception e) {
                // Log error but don't fail startup - use defaults
                System.err.println("Failed to configure web server from Cantaloupe config: " + e.getMessage());
            }
        };
    }

    private void configureSsl(ConfigurableServletWebServerFactory factory,
                            edu.illinois.library.cantaloupe.config.Configuration config) {
        try {
            org.springframework.boot.web.server.Ssl ssl = new org.springframework.boot.web.server.Ssl();
            ssl.setEnabled(true);

            String keyStorePath = config.getString(Key.HTTPS_KEY_STORE_PATH);
            if (keyStorePath != null) {
                ssl.setKeyStore(keyStorePath);
            }

            String keyStorePassword = config.getString(Key.HTTPS_KEY_STORE_PASSWORD);
            if (keyStorePassword != null) {
                ssl.setKeyStorePassword(keyStorePassword);
            }

            String keyPassword = config.getString(Key.HTTPS_KEY_PASSWORD);
            if (keyPassword != null) {
                ssl.setKeyPassword(keyPassword);
            }

            String keyStoreType = config.getString(Key.HTTPS_KEY_STORE_TYPE);
            if (keyStoreType != null) {
                ssl.setKeyStoreType(keyStoreType);
            } else {
                ssl.setKeyStoreType("JKS");
            }

            factory.setSsl(ssl);

            // If HTTPS is enabled and HTTP is not, use HTTPS port as main port
            if (!config.getBoolean(Key.HTTP_ENABLED, true)) {
                factory.setPort(config.getInt(Key.HTTPS_PORT, 8183));
            }

        } catch (Exception e) {
            System.err.println("Failed to configure SSL: " + e.getMessage());
        }
    }

    private void configureJetty(JettyServletWebServerFactory factory,
                               edu.illinois.library.cantaloupe.config.Configuration config) {
        factory.addServerCustomizers(server -> {
            // Configure thread pool settings
            int maxThreads = config.getInt(Key.HTTP_MAX_THREADS, 150);
            int minThreads = config.getInt(Key.HTTP_MIN_THREADS, 8);
            int acceptCount = config.getInt(Key.HTTP_ACCEPT_QUEUE_LIMIT, 0);

            // Configure thread pool
            if (server.getThreadPool() instanceof org.eclipse.jetty.util.thread.QueuedThreadPool) {
                org.eclipse.jetty.util.thread.QueuedThreadPool threadPool =
                    (org.eclipse.jetty.util.thread.QueuedThreadPool) server.getThreadPool();
                threadPool.setMaxThreads(maxThreads);
                threadPool.setMinThreads(minThreads);
            }
        });

        factory.addServerCustomizers(server -> {
            // Configure connectors
            for (org.eclipse.jetty.server.Connector connector : server.getConnectors()) {
                if (connector instanceof org.eclipse.jetty.server.ServerConnector) {
                    org.eclipse.jetty.server.ServerConnector serverConnector =
                        (org.eclipse.jetty.server.ServerConnector) connector;

                    // Set connection timeout (30 seconds like the original Jetty config)
                    serverConnector.setIdleTimeout(30000);

                    // Set accept queue size if specified
                    int acceptCount = config.getInt(Key.HTTP_ACCEPT_QUEUE_LIMIT, 0);
                    if (acceptCount > 0) {
                        serverConnector.setAcceptQueueSize(acceptCount);
                    }
                }
            }
        });
    }
}
