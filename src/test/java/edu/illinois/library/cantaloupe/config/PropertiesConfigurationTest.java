package edu.illinois.library.cantaloupe.config;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class PropertiesConfigurationTest extends AbstractConfigurationTest {

    private PropertiesConfiguration instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new PropertiesConfiguration();
    }

    protected Configuration getInstance() {
        return instance;
    }

    /* getFile() */

    @Test
    public void testGetFile() {
        try {
            File directory = new File(".");
            String cwd = directory.getCanonicalPath();
            Path testPath = Paths.get(cwd, "src", "test", "java", "edu",
                    "illinois", "library", "cantaloupe", "test");

            String goodProps = testPath + File.separator + "cantaloupe.properties";
            System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, goodProps);
            assertEquals(new File(cwd + "/src/test/java/edu/illinois/library/cantaloupe/test/cantaloupe.properties"),
                    instance.getFile());
        } catch (IOException e) {
            fail("Failed to set " + ConfigurationFactory.CONFIG_VM_ARGUMENT);
        }
    }

}
