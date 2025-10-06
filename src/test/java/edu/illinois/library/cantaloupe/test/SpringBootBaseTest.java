package edu.illinois.library.cantaloupe.test;

import edu.illinois.library.cantaloupe.CantalouperApplication;
import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Base class for Spring Boot-enabled tests.
 *
 * This class provides Spring Boot test context with proper configuration
 * for testing Cantaloupe components that require Spring dependency injection.
 */
@SpringBootTest(classes = CantalouperApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.main.banner-mode=off",
    "logging.level.root=WARN",
    "logging.level.edu.illinois.library.cantaloupe=INFO"
})
public abstract class SpringBootBaseTest {

    static {
        // Suppress a Dock icon and annoying Space transition in full-screen
        // mode in macOS.
        System.setProperty("java.awt.headless", "true");
        // Suppress an exception thrown by the JAI framework.
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");
        // Enable test mode
        System.setProperty("cantaloupe.test", "true");
    }

    @BeforeEach
    public void setUp() throws Exception {
        ConfigurationFactory.clearInstance();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");

        // Purge the in-memory info cache. Do this AFTER the configuration has
        // been reset so that the derivative and source caches (which may not
        // have been set up properly) are not available.
        new CacheFacade().purge();
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Clean up any test-specific configuration
    }
}
