package edu.illinois.library.cantaloupe.config;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class ConfigurationTest {

    @Test
    public void testGetInstance() {
        try {
            File directory = new File(".");
            String cwd = directory.getCanonicalPath();
            Path testPath = Paths.get(cwd, "src", "test", "java", "edu",
                    "illinois", "library", "cantaloupe", "test");

            String goodProps = testPath + File.separator + "cantaloupe.properties";
            System.setProperty(edu.illinois.library.cantaloupe.config.Configuration.CONFIG_FILE_VM_ARGUMENT, goodProps);
            assertNotNull(Configuration.getInstance());
        } catch (IOException e) {
            fail("Failed to set " + edu.illinois.library.cantaloupe.config.Configuration.CONFIG_FILE_VM_ARGUMENT);
        }
    }

    @Test
    public void testGetConfigurationFile() {
        try {
            File directory = new File(".");
            String cwd = directory.getCanonicalPath();
            Path testPath = Paths.get(cwd, "src", "test", "java", "edu",
                    "illinois", "library", "cantaloupe", "test");

            String goodProps = testPath + File.separator + "cantaloupe.properties";
            System.setProperty(edu.illinois.library.cantaloupe.config.Configuration.CONFIG_FILE_VM_ARGUMENT, goodProps);
            assertEquals(new File(cwd + "/src/test/java/edu/illinois/library/cantaloupe/test/cantaloupe.properties"),
                    Configuration.getInstance().getConfigurationFile());
        } catch (IOException e) {
            fail("Failed to set " + edu.illinois.library.cantaloupe.config.Configuration.CONFIG_FILE_VM_ARGUMENT);
        }
    }

}
