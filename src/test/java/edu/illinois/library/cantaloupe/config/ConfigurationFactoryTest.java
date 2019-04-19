package edu.illinois.library.cantaloupe.config;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigurationFactoryTest extends BaseTest {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        ConfigurationFactory.clearInstance();
    }

    @Test
    void testGetInstanceInTesting() {
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");

        ConfigurationProvider provider =
                (ConfigurationProvider) Configuration.getInstance();
        assertTrue(provider.getWrappedConfigurations().get(0) instanceof MapConfiguration);
    }

    @Test
    void testGetInstanceInProduction() {
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "bogus");

        ConfigurationProvider provider =
                (ConfigurationProvider) Configuration.getInstance();
        assertTrue(provider.getWrappedConfigurations().get(0) instanceof EnvironmentConfiguration);
        assertTrue(provider.getWrappedConfigurations().get(1) instanceof HeritablePropertiesConfiguration);
    }

}
