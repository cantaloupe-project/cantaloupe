package edu.illinois.library.cantaloupe.config;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

abstract class AbstractFileConfigurationTest extends AbstractConfigurationTest {

    @Test
    void testGetFile() throws Exception {
        File directory = new File(".");
        String cwd = directory.getCanonicalPath();
        Path testPath = Paths.get(cwd, "src", "test", "java", "edu",
                "illinois", "library", "cantaloupe", "test");

        String goodProps = testPath + File.separator + "cantaloupe.properties";
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, goodProps);
        assertEquals(
                Optional.of(Paths.get(cwd, "/src/test/java/edu/illinois/library/cantaloupe/test/cantaloupe.properties")),
                getInstance().getFile());
    }

    /* toMap() */

    @Test
    void testToMap() {
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
