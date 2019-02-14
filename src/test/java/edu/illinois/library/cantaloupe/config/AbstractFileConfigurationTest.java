package edu.illinois.library.cantaloupe.config;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public abstract class AbstractFileConfigurationTest extends AbstractConfigurationTest {

    @Test
    public void testGetFile() {
        try {
            File directory = new File(".");
            String cwd = directory.getCanonicalPath();
            Path testPath = Paths.get(cwd, "src", "test", "java", "edu",
                    "illinois", "library", "cantaloupe", "test");

            String goodProps = testPath + File.separator + "cantaloupe.properties";
            System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, goodProps);
            assertEquals(Paths.get(cwd, "/src/test/java/edu/illinois/library/cantaloupe/test/cantaloupe.properties"),
                    ((FileConfiguration) getInstance()).getFile());
        } catch (IOException e) {
            fail("Failed to set " + ConfigurationFactory.CONFIG_VM_ARGUMENT);
        }
    }

    /* toMap() */

    @Test
    public void testToMap() {
        final FileConfiguration instance = (FileConfiguration) getInstance();
        Map<String,Object> expected      = new LinkedHashMap<>();
        Iterator<String> keys            = instance.getKeys();
        while (keys.hasNext()) {
            String key = keys.next();
            expected.put(key, instance.getProperty(key));
        }

        assertEquals(expected, instance.toMap());
    }

}
