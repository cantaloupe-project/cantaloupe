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
    public void testGetInstanceInTesting() {
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");

        ConfigurationProvider provider =
                (ConfigurationProvider) Configuration.getInstance();
        assertTrue(provider.getWrappedConfigurations().get(0) instanceof MemoryConfiguration);
    }

    @Test
    public void testGetInstanceInProduction() {
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "bogus");

        ConfigurationProvider provider =
                (ConfigurationProvider) Configuration.getInstance();
        assertTrue(provider.getWrappedConfigurations().get(0) instanceof EnvironmentConfiguration);
        assertTrue(provider.getWrappedConfigurations().get(1) instanceof HeritablePropertiesConfiguration);
    }

}
