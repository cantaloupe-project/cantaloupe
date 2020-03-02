package edu.illinois.library.cantaloupe.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HeritablePropertiesConfigurationTest extends AbstractFileConfigurationTest {

    private static final double DELTA = 0.00000001f;

    private HeritablePropertiesConfiguration instance;

    @BeforeEach
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

    /* getBoolean() */

    @Test
    void testGetBooleanUsesChildmostProperty() {
        instance.reload();
        assertTrue(instance.getBoolean("common_boolean_key"));
    }

    /* getDouble() */

    @Test
    void testGetDoubleUsesChildmostProperty() {
        instance.reload();
        assertEquals(3.0f, instance.getDouble("common_integer_key"), DELTA);
    }

    /* getFiles() */

    @Test
    void testGetFilesReturnsAllFiles() {
        assertEquals(3, instance.getFiles().size());
    }

    /* getFloat() */

    @Test
    void testGetFloatUsesChildmostProperty() {
        instance.reload();
        assertEquals(3.0f, instance.getDouble("common_integer_key"), DELTA);
    }

    /* getInt() */

    @Test
    void testGetIntUsesChildmostProperty() {
        instance.reload();
        assertEquals(3, instance.getInt("common_integer_key"));
    }

    /* getKeys() */

    @Test
    void testGetKeysReturnsKeysFromAllAllFiles() {
        instance.reload();
        Iterator<String> it = instance.getKeys();
        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(15, count);
    }

    /* getLong() */

    @Test
    public void testGetLongUsesChildmostProperty() {
        instance.reload();
        assertEquals(3, instance.getLong("common_integer_key"));
    }

    /* getProperty(Key) */

    /**
     * Override because this class stores all values internally as strings.
     */
    @Override
    @Test
    void testGetPropertyWithKeyWithPresentProperty() {
        final Configuration instance = getInstance();
        instance.setProperty(Key.IIIF_1_ENDPOINT_ENABLED, "1");
        instance.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, 2);
        assertEquals("1", instance.getProperty(Key.IIIF_1_ENDPOINT_ENABLED));
        assertEquals("2", instance.getProperty(Key.IIIF_2_ENDPOINT_ENABLED));
        assertNull(instance.getProperty(Key.MAX_PIXELS));
    }

    /* getProperty(String) */

    /**
     * Override because this class stores all values internally as strings.
     */
    @Override
    @Test
    void testGetPropertyWithStringWithPresentProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("cats", "1");
        instance.setProperty("dogs", 2);
        assertEquals("1", instance.getProperty("cats"));
        assertEquals("2", instance.getProperty("dogs"));
        assertNull(instance.getProperty("pigs"));
    }

    @Test
    void testGetPropertyUsesChildmostProperty() {
        instance.reload();
        assertEquals("birds", instance.getProperty("common_string_key"));
    }

    @Test
    void testGetPropertyFallsBackToParentFileIfUndefinedInChildFile() {
        instance.reload();
        assertEquals("dogs", instance.getProperty("level2_key"));
    }

    /* getString() */

    @Test
    void testGetStringUsesChildmostProperty() {
        instance.reload();
        assertEquals("birds", instance.getString("common_string_key"));
    }

    /* setProperty() */

    @Test
    void testSetPropertySetsExistingPropertiesInSameFile() {
        instance.reload();
        List<PropertiesDocument> docs = instance.getConfigurationTree();
        instance.setProperty("level2_key", "bears");
        assertNull(docs.get(0).get("level2_key"));
        assertEquals("bears", docs.get(1).get("level2_key"));
        assertNull(docs.get(2).get("level2_key"));
    }

    @Test
    void testSetPropertySetsNewPropertiesInChildmostFile() {
        instance.reload();
        List<PropertiesDocument> docs = instance.getConfigurationTree();
        instance.setProperty("newkey", "bears");
        assertEquals("bears", docs.get(0).get("newkey"));
        assertNull(docs.get(1).get("newkey"));
        assertNull(docs.get(2).get("newkey"));
    }

    @Test
    public void testGetPropertyReturnsNullIfKeyIsSpecifiedButNoValueIsPresent() {
        instance.reload();
        assertNull(instance.getProperty("key_without_value"));
    }

}
