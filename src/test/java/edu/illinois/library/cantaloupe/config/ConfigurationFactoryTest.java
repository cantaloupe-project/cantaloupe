package edu.illinois.library.cantaloupe.config;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class ConfigurationFactoryTest extends BaseTest {

    @Test
    public void testGetInstanceReturnsMemoryConfiguration() throws Exception {
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        assertTrue(Configuration.getInstance() instanceof MemoryConfiguration);
    }

    @Test
    public void testGetInstanceReturnsPropertiesConfiguration() throws Exception {
        File directory = new File(".");
        String cwd = directory.getCanonicalPath();
        Path testPath = Paths.get(cwd, "src", "test", "java", "edu",
                "illinois", "library", "cantaloupe", "test");
        String opt = testPath + File.separator + "cantaloupe.properties";
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, opt);

        Configuration config = Configuration.getInstance();
        assertTrue(config instanceof HeritablePropertiesConfiguration);
    }

}
