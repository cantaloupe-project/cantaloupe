package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.CantalouperApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Spring Boot test runner utility for Cantaloupe tests.
 *
 * This utility class provides common setup and teardown functionality
 * for Spring Boot-enabled tests, along with helper methods for test
 * configuration and validation.
 *
 * Usage examples:
 * - Extend this class for common Spring Boot test setup
 * - Use static methods for test configuration
 * - Run validation checks for Spring Boot integration
 */
@SpringBootTest(classes = CantalouperApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
    "spring.main.banner-mode=off",
    "logging.level.root=WARN",
    "logging.level.edu.illinois.library.cantaloupe=INFO",
    "http.enabled=true",
    "http.port=0"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SpringBootTestRunner {

    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * Global setup for Spring Boot tests.
     * Configures Cantaloupe for testing environment.
     */
    @BeforeAll
    public static void setupSpringBootTesting() {
        if (!initialized.getAndSet(true)) {
            // Suppress AWT operations in headless mode
            System.setProperty("java.awt.headless", "true");

            // Disable JAI media library to prevent exceptions in tests
            System.setProperty("com.sun.media.jai.disableMediaLib", "true");

            // Enable test mode for Cantaloupe
            System.setProperty("cantaloupe.test", "true");

            // Set memory-based configuration
            System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");

            // Configure basic test settings
            try {
                Configuration config = Configuration.getInstance();

                // Enable endpoints for testing
                config.setProperty(Key.IIIF_1_ENDPOINT_ENABLED, true);
                config.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, true);
                config.setProperty(Key.IIIF_3_ENDPOINT_ENABLED, true);
                config.setProperty(Key.API_ENABLED, true);
                config.setProperty(Key.ADMIN_ENABLED, true);
                config.setProperty(Key.HEALTH_ENDPOINT_ENABLED, true);

                // Configure HTTP settings
                config.setProperty(Key.HTTP_ENABLED, true);
                config.setProperty(Key.HTTP_HOST, "localhost");
                config.setProperty(Key.HTTP_PORT, 8182);
                config.setProperty(Key.HTTPS_ENABLED, false);

                // Set logging levels
                config.setProperty(Key.APPLICATION_LOG_LEVEL, "WARN");

            } catch (Exception e) {
                System.err.println("Failed to configure Cantaloupe for testing: " + e.getMessage());
            }
        }
    }

    /**
     * Global cleanup for Spring Boot tests.
     */
    @AfterAll
    public static void cleanupSpringBootTesting() {
        try {
            // Clear configuration instance
            ConfigurationFactory.clearInstance();

            // Clear test-related system properties
            System.clearProperty("cantaloupe.test");
            System.clearProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT);

        } catch (Exception e) {
            System.err.println("Error during test cleanup: " + e.getMessage());
        }
    }

    /**
     * Validates that Spring Boot context is properly configured.
     */
    @Test
    public void validateSpringBootConfiguration() {
        // Check that required system properties are set
        assertTrue("Headless mode should be enabled",
                   Boolean.parseBoolean(System.getProperty("java.awt.headless")));
        assertTrue("Test mode should be enabled",
                   Boolean.parseBoolean(System.getProperty("cantaloupe.test")));

        // Verify configuration is accessible
        Configuration config = Configuration.getInstance();
        assertNotNull("Configuration should be available", config);

        // Check that endpoints are enabled
        assertTrue("IIIF v3 endpoint should be enabled",
                   config.getBoolean(Key.IIIF_3_ENDPOINT_ENABLED, false));
        assertTrue("HTTP should be enabled",
                   config.getBoolean(Key.HTTP_ENABLED, false));
    }

    /**
     * Tests basic Spring Boot application context loading.
     */
    @Test
    public void validateApplicationContext() {
        // This test passes if the Spring Boot context loads successfully
        // The @SpringBootTest annotation ensures the context is created
        assertTrue("Spring Boot context should load successfully", true);
    }

    /**
     * Helper method to create test configuration with common settings.
     *
     * @return Configuration object with test settings
     */
    public static Configuration createTestConfiguration() {
        Configuration config = Configuration.getInstance();

        // Reset configuration first
        ConfigurationFactory.clearInstance();
        config = Configuration.getInstance();

        // Apply test-specific settings
        config.setProperty(Key.HTTP_ENABLED, true);
        config.setProperty(Key.HTTP_HOST, "localhost");
        config.setProperty(Key.HTTP_PORT, 0); // Random port
        config.setProperty(Key.HTTPS_ENABLED, false);

        // Enable all endpoints
        config.setProperty(Key.IIIF_1_ENDPOINT_ENABLED, true);
        config.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, true);
        config.setProperty(Key.IIIF_3_ENDPOINT_ENABLED, true);
        config.setProperty(Key.API_ENABLED, true);
        config.setProperty(Key.ADMIN_ENABLED, true);
        config.setProperty(Key.HEALTH_ENDPOINT_ENABLED, true);

        // Set reasonable defaults for testing
        config.setProperty(Key.APPLICATION_LOG_LEVEL, "WARN");

        return config;
    }

    /**
     * Helper method to reset configuration between tests.
     */
    public static void resetConfiguration() {
        ConfigurationFactory.clearInstance();
        createTestConfiguration();
    }

    /**
     * Validates that a test class is properly configured for Spring Boot testing.
     *
     * @param testClass The test class to validate
     * @return true if properly configured, false otherwise
     */
    public static boolean validateTestClass(Class<?> testClass) {
        // Check for required annotations
        boolean hasSpringBootTest = testClass.isAnnotationPresent(SpringBootTest.class);
        if (!hasSpringBootTest) {
            System.err.println("Test class " + testClass.getSimpleName() +
                             " is missing @SpringBootTest annotation");
            return false;
        }

        // Check SpringBootTest configuration
        SpringBootTest springBootTest = testClass.getAnnotation(SpringBootTest.class);
        Class<?>[] classes = springBootTest.classes();
        boolean hasCorrectMainClass = false;
        for (Class<?> clazz : classes) {
            if (clazz.equals(CantalouperApplication.class)) {
                hasCorrectMainClass = true;
                break;
            }
        }

        if (!hasCorrectMainClass) {
            System.err.println("Test class " + testClass.getSimpleName() +
                             " should specify CantalouperApplication.class in @SpringBootTest");
            return false;
        }

        return true;
    }

    /**
     * Runs validation on common Spring Boot test patterns.
     */
    @Test
    public void validateCommonTestPatterns() {
        // Validate that our main test classes are properly configured
        assertTrue("RequestTest should be properly configured",
                   validateTestClass(RequestTest.class));
        assertTrue("AbstractResourceTest should be properly configured",
                   validateTestClass(AbstractResourceTest.class));
        assertTrue("FileServletTest should be properly configured",
                   validateTestClass(FileServletTest.class));
    }

    /**
     * Simple assertion helper for tests.
     */
    private static void assertTrue(String message, boolean condition) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /**
     * Simple assertion helper for null checks.
     */
    private static void assertNotNull(String message, Object object) {
        if (object == null) {
            throw new AssertionError(message);
        }
    }

    /**
     * Utility method to print test configuration for debugging.
     */
    public static void printTestConfiguration() {
        System.out.println("=== Spring Boot Test Configuration ===");
        System.out.println("Headless mode: " + System.getProperty("java.awt.headless"));
        System.out.println("Test mode: " + System.getProperty("cantaloupe.test"));
        System.out.println("Config VM arg: " + System.getProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT));

        try {
            Configuration config = Configuration.getInstance();
            System.out.println("HTTP enabled: " + config.getBoolean(Key.HTTP_ENABLED, false));
            System.out.println("HTTP host: " + config.getString(Key.HTTP_HOST, "not set"));
            System.out.println("HTTP port: " + config.getInt(Key.HTTP_PORT, -1));
            System.out.println("IIIF v3 enabled: " + config.getBoolean(Key.IIIF_3_ENDPOINT_ENABLED, false));
        } catch (Exception e) {
            System.out.println("Error reading configuration: " + e.getMessage());
        }

        System.out.println("=====================================");
    }
}
