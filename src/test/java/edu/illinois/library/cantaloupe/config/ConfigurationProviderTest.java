package edu.illinois.library.cantaloupe.config;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.junit.Assert.*;

public class ConfigurationProviderTest extends BaseTest {

    private static final double DELTA = 0.00000001;

    private ConfigurationProvider instance;
    private Configuration config1 = new MapConfiguration();
    private Configuration config2 = new MapConfiguration();

    @Before
    public void setUp() {
        instance = new ConfigurationProvider(
                new ArrayList<>(Arrays.asList(config1, config2)));
    }

    @Test
    public void testClear() {
        config1.setProperty("key", "value");
        config2.setProperty("key", "value");

        instance.clear();

        assertEquals(0, config1.toMap().size());
        assertEquals(0, config2.toMap().size());
    }

    @Test
    public void testClearProperty() {
        config1.setProperty("key1", "value");
        config1.setProperty("key2", "value");
        config2.setProperty("key1", "value");
        config2.setProperty("key2", "value");

        instance.clearProperty("key1");

        assertNull(config1.getString("key1"));
        assertNotNull(config1.getString("key2"));
        assertNull(config2.getString("key1"));
        assertNotNull(config2.getString("key2"));
    }

    @Test
    public void testGetBoolean1() {
        // true in config1
        config1.setProperty("key", true);
        assertTrue(instance.getBoolean("key"));

        // false in config1
        config1.setProperty("key", false);
        assertFalse(instance.getBoolean("key"));

        // not set in config1, true in config2
        config1.clear();
        config2.setProperty("key", true);
        assertTrue(instance.getBoolean("key"));

        // not set in either
        config1.clear();
        config2.clear();
        try {
            instance.getBoolean("key");
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    @Test
    public void testGetBoolean2() {
        // true in config1
        config1.setProperty("key", true);
        assertTrue(instance.getBoolean("key", false));

        // not set in config1, true in config2
        config1.clear();
        config2.setProperty("key", true);
        assertTrue(instance.getBoolean("key", false));

        // not set in either
        config1.clear();
        config2.clear();
        assertTrue(instance.getBoolean("key", true));
    }

    @Test
    public void testGetDouble1() {
        // set in config1
        config1.setProperty("key", 1.0);
        assertEquals(1.0, instance.getDouble("key"), DELTA);

        // not set in config1, set in config2
        config1.clear();
        config2.setProperty("key", 1.0);
        assertEquals(1.0, instance.getDouble("key"), DELTA);

        // not set in either
        config1.clear();
        config2.clear();
        try {
            instance.getDouble("key");
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    @Test
    public void testGetDouble2() {
        // set in config1
        config1.setProperty("key", 1.0);
        assertEquals(1.0, instance.getDouble("key", 2.0), DELTA);

        // not set in config1, set in config2
        config1.clear();
        config2.setProperty("key", 1.0);
        assertEquals(1.0, instance.getDouble("key", 2.0), DELTA);

        // not set in either
        config1.clear();
        config2.clear();
        assertEquals(2.0, instance.getDouble("key", 2.0), DELTA);
    }

    @Test
    public void testGetFloat1() {
        // set in config1
        config1.setProperty("key", 1f);
        assertEquals(1f, instance.getFloat("key"), DELTA);

        // not set in config1, set in config2
        config1.clear();
        config2.setProperty("key", 1f);
        assertEquals(1f, instance.getFloat("key"), DELTA);

        // not set in either
        config1.clear();
        config2.clear();
        try {
            instance.getFloat("key");
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    @Test
    public void testGetFloat2() {
        // set in config1
        config1.setProperty("key", 1f);
        assertEquals(1f, instance.getFloat("key", 2f), DELTA);

        // not set in config1, set in config2
        config1.clear();
        config2.setProperty("key", 1f);
        assertEquals(1f, instance.getFloat("key", 2f), DELTA);

        // not set in either
        config1.clear();
        config2.clear();
        assertEquals(2f, instance.getFloat("key", 2f), DELTA);
    }

    @Test
    public void testGetInt1() {
        // set in config1
        config1.setProperty("key", 1);
        assertEquals(1, instance.getInt("key"));

        // not set in config1, set in config2
        config1.clear();
        config2.setProperty("key", 1);
        assertEquals(1, instance.getInt("key"));

        // not set in either
        config1.clear();
        config2.clear();
        try {
            instance.getInt("key");
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    @Test
    public void testGetInt2() {
        // set in config1
        config1.setProperty("key", 1);
        assertEquals(1, instance.getInt("key", 2), DELTA);

        // not set in config1, set in config2
        config1.clear();
        config2.setProperty("key", 1);
        assertEquals(1, instance.getInt("key", 2), DELTA);

        // not set in either
        config1.clear();
        config2.clear();
        assertEquals(2, instance.getInt("key", 2), DELTA);
    }

    @Test
    public void testGetKeys() {
        Iterator<String> it = instance.getKeys();
        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(0, count);

        config1.setProperty("cats", "cats");
        config2.setProperty("dogs", "dogs");
        it = instance.getKeys();
        count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void testGetLong1() {
        // set in config1
        config1.setProperty("key", 1);
        assertEquals(1, instance.getLong("key"));

        // not set in config1, set in config2
        config1.clear();
        config2.setProperty("key", 1);
        assertEquals(1, instance.getLong("key"));

        // not set in either
        config1.clear();
        config2.clear();
        try {
            instance.getLong("key");
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    @Test
    public void testGetLong2() {
        // set in config1
        config1.setProperty("key", 1);
        assertEquals(1, instance.getLong("key", 2), DELTA);

        // not set in config1, set in config2
        config1.clear();
        config2.setProperty("key", 1);
        assertEquals(1, instance.getLong("key", 2), DELTA);

        // not set in either
        config1.clear();
        config2.clear();
        assertEquals(2, instance.getLong("key", 2), DELTA);
    }

    @Test
    public void testGetProperty() {
        // set in config1
        config1.setProperty("key", "value");
        assertEquals("value", instance.getProperty("key"));

        // not set in config1, set in config2
        config1.clear();
        config2.setProperty("key", "value");
        assertEquals("value", instance.getProperty("key"));

        // not set in either
        config1.clear();
        config2.clear();
        assertNull(instance.getProperty("key"));
    }

    @Test
    public void testGetString1() {
        // set in config1
        config1.setProperty("key", "value");
        assertEquals("value", instance.getString("key"));

        // not set in config1, set in config2
        config1.clear();
        config2.setProperty("key", "value");
        assertEquals("value", instance.getString("key"));

        // not set in either
        config1.clear();
        config2.clear();
        assertNull(instance.getString("key"));
    }

    @Test
    public void testGetString2() {
        // set in config1
        config1.setProperty("key", "value");
        assertEquals("value", instance.getString("key", "default"));

        // not set in config1, set in config2
        config1.clear();
        config2.setProperty("key", "value");
        assertEquals("value", instance.getString("key", "default"));

        // not set in either
        config1.clear();
        config2.clear();
        assertEquals("default", instance.getString("key", "default"));
    }

    @Test
    public void testGetWrappedConfigurations() {
        assertEquals(2, instance.getWrappedConfigurations().size());
    }

    @Test
    public void testReload() {
        // TODO: write this
    }

    @Test
    public void testSave() {
        // TODO: write this
    }

    @Test
    public void testSetProperty() {
        instance.setProperty("key", "cats");
        assertEquals("cats", config1.getProperty("key"));
        assertEquals("cats", config2.getProperty("key"));
    }

    @Test
    public void testStartWatching() {
        // TODO: write this
    }

    @Test
    public void testStopWatching() {
        // TODO: write this
    }

}
