package edu.illinois.library.cantaloupe.test;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * Base class for all tests.
 */
public abstract class BaseTest {

    static {
        // Suppress a Dock icon and annoying Space transition in full-screen
        // mode in macOS.
        System.setProperty("java.awt.headless", "true");
        // Suppress an exception thrown by the JAI framework.
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ConfigurationFactory.clearInstance();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
    }

    @AfterClass
    public static void afterClass() throws Exception {}

    @Before
    public void setUp() throws Exception {
        ConfigurationFactory.clearInstance();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        System.setProperty(Application.TEST_VM_ARGUMENT, "true");

        // Purge the in-memory info cache. Do this AFTER the configuration has
        // been reset so that the derivative and source caches (which may not
        // have been set up properly) are not available.
        new CacheFacade().purge();
    }

    @After
    public void tearDown() throws Exception {}

}
