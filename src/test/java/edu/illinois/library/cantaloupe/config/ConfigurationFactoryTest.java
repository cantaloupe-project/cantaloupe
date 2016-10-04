package edu.illinois.library.cantaloupe.config;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class ConfigurationFactoryTest {

    /* getInstance() */

    @Test
    public void testGetInstance() {
        try {
            File directory = new File(".");
            String cwd = directory.getCanonicalPath();
            Path testPath = Paths.get(cwd, "src", "test", "java", "edu",
                    "illinois", "library", "cantaloupe", "test");

            String goodProps = testPath + File.separator + "cantaloupe.properties";
            System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, goodProps);
            assertNotNull(ConfigurationFactory.getInstance());
        } catch (IOException e) {
            fail("Failed to set " + ConfigurationFactory.CONFIG_VM_ARGUMENT);
        }
    }

}
