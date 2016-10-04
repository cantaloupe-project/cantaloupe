package edu.illinois.library.cantaloupe.config;

import org.junit.Before;
import org.junit.Test;

import java.util.NoSuchElementException;

import static org.junit.Assert.*;

public class MemoryConfigurationTest {

    private MemoryConfiguration instance;

    @Before
    public void setUp() {
        instance = new MemoryConfiguration();
    }

    /* clear() */

    @Test
    public void testClear() {
        instance.setProperty("cats", "yes");
        instance.clear();
        assertNull(instance.getString("cats"));
    }

    /* getBoolean(String) */

    @Test
    public void testGetBoolean() {
        instance.setProperty("test1", true);
        instance.setProperty("test2", false);
        assertTrue(instance.getBoolean("test1"));
        assertFalse(instance.getBoolean("test2"));
        try {
            instance.getBoolean("test3");
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    /* getBoolean(String, boolean) */

    @Test
    public void testGetBooleanWithDefault() {
        instance.setProperty("test1", true);
        instance.setProperty("test2", false);
        assertTrue(instance.getBoolean("test1"));
        assertFalse(instance.getBoolean("test2"));
        assertTrue(instance.getBoolean("test3", true));
        assertFalse(instance.getBoolean("test3", false));
    }

    /* getDouble(String) */

    @Test
    public void testGetDouble() {
        final double delta = 0.0000001f;
        instance.setProperty("test1", 0.25f);
        instance.setProperty("test2", "0.55");
        assertEquals(0.25f, instance.getDouble("test1"), delta);
        assertEquals(0.55f, instance.getDouble("test2"), delta);
        try {
            instance.getDouble("test3");
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    /* getDouble(String, boolean) */

    @Test
    public void testGetDoubleWithDefault() {
        final float delta = 0.0000001f;
        assertEquals(0.65f, instance.getDouble("test1", 0.65f), delta);
    }

    /* getFloat(String) */

    @Test
    public void testGetFloat() {
        final float delta = 0.0000001f;
        instance.setProperty("test1", 0.25f);
        instance.setProperty("test2", "0.55");
        assertEquals(0.25f, instance.getFloat("test1"), delta);
        assertEquals(0.55f, instance.getFloat("test2"), delta);
        try {
            instance.getFloat("test3");
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    /* getFloat(String, boolean) */

    @Test
    public void testGetFloatWithDefault() {
        final float delta = 0.0000001f;
        assertEquals(0.65f, instance.getFloat("test1", 0.65f), delta);
    }

    /* getProperty(String) */

    @Test
    public void testGetProperty() {
        instance.setProperty("cats", "1");
        instance.setProperty("dogs", 2);
        assertEquals("1", instance.getProperty("cats"));
        assertEquals(2, instance.getProperty("dogs"));
        assertNull(instance.getProperty("pigs"));
    }

    /* getString(String) */

    @Test
    public void testGetString() {
        instance.setProperty("test1", "cats");
        assertEquals("cats", instance.getString("test1"));
        assertNull(instance.getString("bogus"));
    }

    /* getString(String, String) */

    @Test
    public void testGetStringWithDefault() {
        instance.setProperty("test1", "cats");
        assertEquals("cats", instance.getString("test1", "dogs"));
        assertEquals("cats", instance.getString("test5", "cats"));
    }

}
