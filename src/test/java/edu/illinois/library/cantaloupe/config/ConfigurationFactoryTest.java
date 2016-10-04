package edu.illinois.library.cantaloupe.config;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class ConfigurationFactoryTest {

    @Before
    public void setUp() {
        ConfigurationFactory.clearInstance();
    }

    @Test
    public void testGetInstanceReturnsEnvironmentConfigurationWithNoVmOption() {
        System.clearProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT);
        assertTrue(ConfigurationFactory.getInstance() instanceof EnvironmentConfiguration);
    }

    @Test
    public void testGetInstanceReturnsJsonConfiguration() throws Exception {
        File directory = new File(".");
        String cwd = directory.getCanonicalPath();
        Path testPath = Paths.get(cwd, "src", "test", "java", "edu",
                "illinois", "library", "cantaloupe", "test");
        String opt = testPath + File.separator + "cantaloupe.json";
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, opt);

        assertTrue(ConfigurationFactory.getInstance() instanceof JsonConfiguration);
    }

    @Test
    public void testGetInstanceReturnsMemoryConfiguration() throws Exception {
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        assertTrue(ConfigurationFactory.getInstance() instanceof MemoryConfiguration);
    }

    @Test
    public void testGetInstanceReturnsPropertiesConfiguration() throws Exception {
        File directory = new File(".");
        String cwd = directory.getCanonicalPath();
        Path testPath = Paths.get(cwd, "src", "test", "java", "edu",
                "illinois", "library", "cantaloupe", "test");
        String opt = testPath + File.separator + "cantaloupe.properties";
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, opt);

        assertTrue(ConfigurationFactory.getInstance() instanceof PropertiesConfiguration);
    }

}
