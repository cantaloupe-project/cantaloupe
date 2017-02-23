package edu.illinois.library.cantaloupe.config;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.apache.commons.configuration.ConversionException;
import org.junit.Test;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.junit.Assert.*;

public abstract class AbstractConfigurationTest extends BaseTest {

    abstract protected Configuration getInstance();

    /* clear() */

    @Test
    public void testClear() {
        final Configuration instance = getInstance();
        instance.setProperty("cats", "yes");
        instance.clear();
        assertNull(instance.getString("cats"));
    }

    /* clearProperty() */

    @Test
    public void testClearProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("cats", "yes");
        instance.setProperty("dogs", "yes");
        instance.clearProperty("cats");
        assertNull(instance.getString("cats"));
        assertNotNull(instance.getString("dogs"));
    }

    /* getBoolean(String) */

    @Test
    public void testGetBooleanWithValidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", true);
        instance.setProperty("test2", false);
        assertTrue(instance.getBoolean("test1"));
        assertFalse(instance.getBoolean("test2"));
    }

    @Test
    public void testGetBooleanWithInvalidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", "cats");
        try {
            instance.getBoolean("test1");
            fail("Expected exception");
        } catch (ConversionException e) {
            // pass
        }
    }

    @Test
    public void testGetBooleanWithMissingProperty() {
        final Configuration instance = getInstance();
        try {
            instance.getBoolean("test3");
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    /* getBoolean(String, boolean) */

    @Test
    public void testGetBooleanWithDefaultWithValidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", true);
        assertTrue(instance.getBoolean("test1", true));
        assertTrue(instance.getBoolean("test1", false));

        instance.setProperty("test2", false);
        assertFalse(instance.getBoolean("test2", true));
        assertFalse(instance.getBoolean("test2", false));
    }

    @Test
    public void testGetBooleanWithDefaultWithInvalidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test3", "");
        assertFalse(instance.getBoolean("test3", false));
        assertTrue(instance.getBoolean("test3", true));

        instance.setProperty("test3", "cats");
        assertFalse(instance.getBoolean("test3", false));
        assertTrue(instance.getBoolean("test3", true));
    }

    @Test
    public void testGetBooleanWithDefaultWithMissingProperty() {
        final Configuration instance = getInstance();
        assertTrue(instance.getBoolean("test3", true));
        assertFalse(instance.getBoolean("test3", false));
    }

    /* getDouble(String) */

    @Test
    public void testGetDoubleWithValidProperty() {
        final Configuration instance = getInstance();
        final double delta = 0.0000001f;
        instance.setProperty("test1", 0.25f);
        instance.setProperty("test2", "0.55");
        assertEquals(0.25f, instance.getDouble("test1"), delta);
        assertEquals(0.55f, instance.getDouble("test2"), delta);
    }

    @Test
    public void testGetDoubleWithInvalidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", "cats");
        try {
            instance.getDouble("test1");
            fail("Expected exception");
        } catch (ConversionException e) {
            // pass
        }
    }

    @Test
    public void testGetDoubleWithMissingProperty() {
        final Configuration instance = getInstance();
        try {
            instance.getDouble("test3");
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    /* getDouble(String, double) */

    @Test
    public void testGetDoubleWithDefaultWithValidProperty() {
        final Configuration instance = getInstance();
        final float delta = 0.0000001f;
        instance.setProperty("test1", 0.5f);
        assertEquals(0.5f, instance.getDouble("test1", 0.65f), delta);
    }

    @Test
    public void testGetDoubleWithDefaultWithInvalidProperty() {
        final Configuration instance = getInstance();
        final float delta = 0.0000001f;
        instance.setProperty("test1", "cats");
        assertEquals(0.5f, instance.getDouble("test1", 0.5f), delta);
    }

    @Test
    public void testGetDoubleWithDefaultWithMissingProperty() {
        final Configuration instance = getInstance();
        final float delta = 0.0000001f;
        assertEquals(0.65f, instance.getDouble("test1", 0.65f), delta);
    }

    /* getFloat(String) */

    @Test
    public void testGetFloatWithValidProperty() {
        final Configuration instance = getInstance();
        final float delta = 0.0000001f;
        instance.setProperty("test1", 0.25f);
        instance.setProperty("test2", "0.55");
        assertEquals(0.25f, instance.getFloat("test1"), delta);
        assertEquals(0.55f, instance.getFloat("test2"), delta);
    }

    @Test
    public void testGetFloatWithInvalidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", "cats");
        try {
            instance.getFloat("test1");
            fail("Expected exception");
        } catch (ConversionException e) {
            // pass
        }
    }

    @Test
    public void testGetFloatWithMissingProperty() {
        final Configuration instance = getInstance();
        try {
            instance.getFloat("test3");
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    /* getFloat(String, float) */

    @Test
    public void testGetFloatWithDefaultWithValidProperty() {
        final Configuration instance = getInstance();
        final float delta = 0.0000001f;
        instance.setProperty("test1", 0.5f);
        assertEquals(0.5f, instance.getFloat("test1", 0.65f), delta);
    }

    @Test
    public void testGetFloatWithDefaultWithInvalidProperty() {
        final Configuration instance = getInstance();
        final float delta = 0.0000001f;
        instance.setProperty("test1", "cats");
        assertEquals(0.5f, instance.getFloat("test1", 0.5f), delta);
    }

    @Test
    public void testGetFloatWithDefaultWithMissingProperty() {
        final Configuration instance = getInstance();
        final float delta = 0.0000001f;
        assertEquals(0.65f, instance.getFloat("test1", 0.65f), delta);
    }

    /* getInt(String) */

    @Test
    public void testGetIntWithValidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", 25);
        instance.setProperty("test2", "55");
        assertEquals(25, instance.getInt("test1"));
        assertEquals(55, instance.getInt("test2"));
    }

    @Test
    public void testGetIntWithInvalidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", "cats");
        try {
            instance.getInt("test1");
            fail("Expected exception");
        } catch (ConversionException e) {
            // pass
        }
    }

    @Test
    public void testGetIntWithMissingProperty() {
        final Configuration instance = getInstance();
        try {
            instance.getInt("test3");
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    /* getInt(String, int) */

    @Test
    public void testGetIntWithDefaultWithValidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", 5);
        assertEquals(5, instance.getInt("test1", 6));
    }

    @Test
    public void testGetIntWithDefaultWithInvalidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", "cats");
        assertEquals(5, instance.getInt("test1", 5));
    }

    @Test
    public void testGetIntWithDefaultWithMissingProperty() {
        final Configuration instance = getInstance();
        assertEquals(6, instance.getInt("test1", 6));
    }

    /* getKeys() */

    @Test
    public void testGetKeys() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", "cats");
        instance.setProperty("test2", "cats");
        Iterator<String> it = instance.getKeys();
        assertNotNull(it.next());
        assertNotNull(it.next());
        try {
            it.next();
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    /* getLong(String) */

    @Test
    public void testGetLongWithValidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", 25);
        instance.setProperty("test2", "55");
        assertEquals(25, instance.getLong("test1"));
        assertEquals(55, instance.getLong("test2"));
    }

    @Test
    public void testGetLongWithInvalidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", "cats");
        try {
            instance.getLong("test1");
            fail("Expected exception");
        } catch (ConversionException e) {
            // pass
        }
    }

    @Test
    public void testGetLongWithMissingProperty() {
        final Configuration instance = getInstance();
        try {
            instance.getLong("test3");
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    /* getLong(String, int) */

    @Test
    public void testGetLongWithDefaultWithValidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", 5);
        assertEquals(5, instance.getLong("test1", 6));
    }

    @Test
    public void testGetLongWithDefaultWithInvalidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", "cats");
        assertEquals(5, instance.getLong("test1", 5));
    }

    @Test
    public void testGetLongWithDefaultWithMissingProperty() {
        final Configuration instance = getInstance();
        assertEquals(6, instance.getLong("test1", 6));
    }

    /* getProperty(String) */

    @Test
    public void testGetPropertyWithPresentProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("cats", "1");
        instance.setProperty("dogs", 2);
        assertEquals("1", instance.getProperty("cats"));
        assertEquals(2, instance.getProperty("dogs"));
        assertNull(instance.getProperty("pigs"));
    }

    @Test
    public void testGetPropertyWithMissingProperty() {
        final Configuration instance = getInstance();
        assertNull(instance.getProperty("cats"));
    }

    /* getString(String) */

    @Test
    public void testGetStringWithValidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", "cats");
        assertEquals("cats", instance.getString("test1"));
    }

    @Test
    public void testGetStringWithMissingProperty() {
        final Configuration instance = getInstance();
        assertNull(instance.getString("bogus"));
    }

    /* getString(String, String) */

    @Test
    public void testGetStringWithDefaultWithValidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", "cats");
        assertEquals("cats", instance.getString("test1", "dogs"));
    }

    @Test
    public void testGetStringWithDefaultWithMissingProperty() {
        final Configuration instance = getInstance();
        assertEquals("cats", instance.getString("test1", "cats"));
    }

}
