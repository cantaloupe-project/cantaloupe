package edu.illinois.library.cantaloupe;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ApplicationTest extends TestCase {

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(ApplicationTest.class);
    }

    public void testGetConfiguration() {
        try {
            File directory = new File(".");
            String cwd = directory.getCanonicalPath();
            Path testPath = Paths.get(cwd, "src", "test", "java", "edu",
                    "illinois", "library", "cantaloupe", "test");

            String goodProps = testPath + File.separator + "cantaloupe.properties";
            System.setProperty("cantaloupe.config", goodProps);
            assertNotNull(Application.getConfiguration());
        } catch (IOException e) {
            fail("Failed to set cantaloupe.config");
        }
    }

    public void testSetConfiguration() {
        Configuration newConfig = new BaseConfiguration();
        Application.setConfiguration(newConfig);
        assertSame(newConfig, Application.getConfiguration());
    }

}
