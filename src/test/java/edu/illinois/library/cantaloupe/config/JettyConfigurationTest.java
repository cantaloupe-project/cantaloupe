package edu.illinois.library.cantaloupe.config;

import org.eclipse.jetty.http.UriCompliance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for JettyConfiguration.
 * Tests the proper configuration of URI compliance and servlet handlers.
 */
class JettyConfigurationTest {

    private JettyConfiguration jettyConfiguration;

    @BeforeEach
    void setUp() {
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");

        jettyConfiguration = new JettyConfiguration(Configuration.getInstance());
    }

    @Test
    void testJettyCustomizerBeanCreation() {
        // Test that the jettyCustomizer bean can be created
        WebServerFactoryCustomizer<JettyServletWebServerFactory> customizer =
            jettyConfiguration.jettyCustomizer();

        assertNotNull(customizer, "JettyCustomizer should not be null");
    }

    @Test
    void testCustomizerDoesNotThrowException() {
        // Test that the customizer can be applied without throwing exceptions
        JettyServletWebServerFactory factory = new JettyServletWebServerFactory();
        WebServerFactoryCustomizer<JettyServletWebServerFactory> customizer =
            jettyConfiguration.jettyCustomizer();

        // Apply the customizer (this should not throw any exceptions)
        assertDoesNotThrow(() -> customizer.customize(factory));
    }

    @Test
    void testUriComplianceConfiguration() {
        // This test verifies that the URI compliance string is valid
        // The actual UriCompliance.from() method will throw an exception if the string is invalid
        assertDoesNotThrow(() -> {
            UriCompliance compliance = UriCompliance.from("DEFAULT,SUSPICIOUS_PATH_CHARACTERS,AMBIGUOUS_PATH_SEPARATOR");
            assertNotNull(compliance, "UriCompliance should not be null");
        });
    }

    @Test
    void testConfigurationIsSpringComponent() {
        // Verify that the class is properly annotated as a Spring Configuration
        assertTrue(jettyConfiguration.getClass().isAnnotationPresent(org.springframework.context.annotation.Configuration.class),
                  "JettyConfiguration should be annotated with @Configuration");
    }
}
