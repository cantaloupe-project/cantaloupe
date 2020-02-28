package edu.illinois.library.cantaloupe.config;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

public class HeritablePropertiesConfigurationTest extends AbstractFileConfigurationTest {

    private static final double DELTA = 0.00000001f;

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

    /* getBoolean() */

    @Test
    public void testGetBooleanUsesChildmostProperty() throws Exception {
        instance.reload();
        assertTrue(instance.getBoolean("common_boolean_key"));
    }

    /* getDouble() */

    @Test
    public void testGetDoubleUsesChildmostProperty() throws Exception {
        instance.reload();
        assertEquals(3.0f, instance.getDouble("common_integer_key"), DELTA);
    }

    /* getFiles() */

    @Test
    public void testGetFilesReturnsAllFiles() {
        assertEquals(3, instance.getFiles().size());
    }

    /* getFloat() */

    @Test
    public void testGetFloatUsesChildmostProperty() throws Exception {
        instance.reload();
        assertEquals(3.0f, instance.getDouble("common_integer_key"), DELTA);
    }

    /* getInt() */

    @Test
    public void testGetIntUsesChildmostProperty() throws Exception {
        instance.reload();
        assertEquals(3, instance.getInt("common_integer_key"));
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
        assertEquals(15, count);
    }

    /* getLong() */

    @Test
    public void testGetLongUsesChildmostProperty() throws Exception {
        instance.reload();
        assertEquals(3, instance.getLong("common_integer_key"));
    }

    /* getProperty(Key) */

    /**
     * Override because this class stores all values internally as strings.
     */
    @Override
    @Test
    public void testGetPropertyWithKeyWithPresentProperty() {
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
    public void testGetPropertyWithStringWithPresentProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("cats", "1");
        instance.setProperty("dogs", 2);
        assertEquals("1", instance.getProperty("cats"));
        assertEquals("2", instance.getProperty("dogs"));
        assertNull(instance.getProperty("pigs"));
    }

    @Test
    public void testGetPropertyUsesChildmostProperty() throws Exception {
        instance.reload();
        assertEquals("birds", instance.getProperty("common_string_key"));
    }

    @Test
    public void testGetPropertyFallsBackToParentFileIfUndefinedInChildFile()
            throws Exception {
        instance.reload();
        assertEquals("dogs", instance.getProperty("level2_key"));
    }

    /* getString() */

    @Test
    public void testGetStringUsesChildmostProperty() throws Exception {
        instance.reload();
        assertEquals("birds", instance.getString("common_string_key"));
    }

    /* setProperty() */

    @Test
    public void testSetPropertySetsExistingPropertiesInSameFile()
            throws Exception {
        instance.reload();
        List<PropertiesDocument> docs = instance.getConfigurationTree();
        instance.setProperty("level2_key", "bears");
        assertNull(docs.get(0).get("level2_key"));
        assertEquals("bears", docs.get(1).get("level2_key"));
        assertNull(docs.get(2).get("level2_key"));
    }

    @Test
    public void testSetPropertySetsNewPropertiesInChildmostFile()
            throws Exception {
        instance.reload();
        List<PropertiesDocument> docs = instance.getConfigurationTree();
        instance.setProperty("newkey", "bears");
        assertEquals("bears", docs.get(0).get("newkey"));
        assertNull(docs.get(1).get("newkey"));
        assertNull(docs.get(2).get("newkey"));
    }

    @Test
    public void testGetPropertyReturnsNullIfKeyIsSpecifiedButNoValueIsPresent() throws ConfigurationException {
        instance.reload();

        assertNull(instance.getProperty("key_without_value"));
    }
}
