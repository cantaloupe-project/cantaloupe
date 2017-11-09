package edu.illinois.library.cantaloupe.config;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigurationFactoryTest extends BaseTest {

    @Before
    public void setUp() {
        ConfigurationFactory.clearInstance();
    }

    @Test
    public void testGetInstanceReturnsMemoryConfiguration() throws Exception {
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        assertTrue(Configuration.getInstance() instanceof MemoryConfiguration);
    }

    @Test
    public void testGetInstanceReturnsPropertiesConfiguration() throws Exception {
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "bogus");

        Configuration config = Configuration.getInstance();
        assertTrue(config instanceof HeritablePropertiesConfiguration);
    }

}
