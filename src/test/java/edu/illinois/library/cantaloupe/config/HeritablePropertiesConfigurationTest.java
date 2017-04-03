package edu.illinois.library.cantaloupe.config;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

public class HeritablePropertiesConfigurationTest extends FileConfigurationTest {

    private HeritablePropertiesConfiguration instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Instances won't work without an actual file backing them up.
        File directory = new File(".");
        String cwd = directory.getCanonicalPath();
        Path configPath = Paths.get(cwd, "src", "test", "resources",
                "heritable_level3.properties");
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT,
                configPath.toString());

        instance = new HeritablePropertiesConfiguration();
        instance.reload();
        instance.clear();
    }

    protected Configuration getInstance() {
        return instance;
    }

    /* getFiles() */

    @Test
    public void testGetFilesReturnsAllFiles() {
        assertEquals(3, instance.getFiles().size());
    }

    /* getKeys() */

    @Test
    public void testGetKeysReturnsKeysFromAllAllFiles() throws Exception {
        instance.reload();

        Iterator<String> it = instance.getKeys();
        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(8, count);
    }

    /* getProperty() */

    @Test
    public void testGetPropertyUsesChildmostProperty() throws Exception {
        instance.reload();
        assertEquals("birds", instance.getProperty("common_key"));
    }

    @Test
    public void testGetPropertyFallsBackToParentFileIfUndefinedInChildFile()
            throws Exception {
        instance.reload();
        assertEquals("dogs", instance.getProperty("level2_key"));
    }

    /* setProperty() */

    @Test
    public void testSetPropertySetsExistingPropertiesInSameFile()
            throws Exception {
        instance.reload();
        List<org.apache.commons.configuration.PropertiesConfiguration> commonsConfigs =
                instance.getConfigurationTree();
        instance.setProperty("level2_key", "bears");
        assertNull(commonsConfigs.get(0).getString("level2_key"));
        assertEquals("bears", commonsConfigs.get(1).getString("level2_key"));
        assertNull(commonsConfigs.get(2).getString("level2_key"));
    }

    @Test
    public void testSetPropertySetsNewPropertiesInChildmostFile()
            throws Exception {
        instance.reload();
        List<org.apache.commons.configuration.PropertiesConfiguration> commonsConfigs =
                instance.getConfigurationTree();
        instance.setProperty("newkey", "bears");
        assertEquals("bears", commonsConfigs.get(0).getString("newkey"));
        assertNull(commonsConfigs.get(1).getString("newkey"));
        assertNull(commonsConfigs.get(2).getString("newkey"));
    }

}
