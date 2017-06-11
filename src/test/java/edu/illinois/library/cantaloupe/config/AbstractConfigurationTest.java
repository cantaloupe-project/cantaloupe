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

    /* clearProperty(Key) */

    @Test
    public void testClearPropertyWithKey() {
        final Configuration instance = getInstance();
        instance.setProperty(Key.MAX_PIXELS, 123);
        instance.setProperty(Key.SLASH_SUBSTITUTE, "cats");
        instance.clearProperty(Key.MAX_PIXELS);
        assertNull(instance.getString(Key.MAX_PIXELS));
        assertNotNull(instance.getString(Key.SLASH_SUBSTITUTE));
    }

    /* clearProperty(String) */

    @Test
    public void testClearPropertyWithString() {
        final Configuration instance = getInstance();
        instance.setProperty("cats", "yes");
        instance.setProperty("dogs", "yes");
        instance.clearProperty("cats");
        assertNull(instance.getString("cats"));
        assertNotNull(instance.getString("dogs"));
    }

    /* getBoolean(Key) */

    @Test
    public void testGetBooleanWithKeyWithValidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty(Key.IIIF_1_ENDPOINT_ENABLED, true);
        instance.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, false);
        assertTrue(instance.getBoolean(Key.IIIF_1_ENDPOINT_ENABLED));
        assertFalse(instance.getBoolean(Key.IIIF_2_ENDPOINT_ENABLED));
    }

    @Test
    public void testGetBooleanWithKeyWithInvalidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty(Key.SLASH_SUBSTITUTE, "cats");
        try {
            instance.getBoolean(Key.SLASH_SUBSTITUTE);
            fail("Expected exception");
        } catch (ConversionException e) {
            // pass
        }
    }

    @Test
    public void testGetBooleanWithKeyWithMissingProperty() {
        final Configuration instance = getInstance();
        try {
            instance.getBoolean(Key.SLASH_SUBSTITUTE);
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    /* getBoolean(String) */

    @Test
    public void testGetBooleanWithStringWithValidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", true);
        instance.setProperty("test2", false);
        assertTrue(instance.getBoolean("test1"));
        assertFalse(instance.getBoolean("test2"));
    }

    @Test
    public void testGetBooleanWithStringWithInvalidProperty() {
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
    public void testGetBooleanWithStringWithMissingProperty() {
        final Configuration instance = getInstance();
        try {
            instance.getBoolean("test3");
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    /* getBoolean(Key, boolean) */

    @Test
    public void testGetBooleanWithKeyWithDefaultWithValidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty(Key.IIIF_1_ENDPOINT_ENABLED, true);
        assertTrue(instance.getBoolean(Key.IIIF_1_ENDPOINT_ENABLED, true));
        assertTrue(instance.getBoolean(Key.IIIF_1_ENDPOINT_ENABLED, false));

        instance.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, false);
        assertFalse(instance.getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, true));
        assertFalse(instance.getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, false));
    }

    @Test
    public void testGetBooleanWithKeyWithDefaultWithInvalidProperty() {
        final Key key = Key.IIIF_1_ENDPOINT_ENABLED;
        final Configuration instance = getInstance();
        instance.setProperty(key, "");
        assertFalse(instance.getBoolean(key, false));
        assertTrue(instance.getBoolean(key, true));

        instance.setProperty(key, "cats");
        assertFalse(instance.getBoolean(key, false));
        assertTrue(instance.getBoolean(key, true));
    }

    @Test
    public void testGetBooleanWithKeyWithDefaultWithMissingProperty() {
        final Configuration instance = getInstance();
        assertTrue(instance.getBoolean(Key.IIIF_1_ENDPOINT_ENABLED, true));
        assertFalse(instance.getBoolean(Key.IIIF_1_ENDPOINT_ENABLED, false));
    }

    /* getBoolean(String, boolean) */

    @Test
    public void testGetBooleanWithStringWithDefaultWithValidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", true);
        assertTrue(instance.getBoolean("test1", true));
        assertTrue(instance.getBoolean("test1", false));

        instance.setProperty("test2", false);
        assertFalse(instance.getBoolean("test2", true));
        assertFalse(instance.getBoolean("test2", false));
    }

    @Test
    public void testGetBooleanWithStringWithDefaultWithInvalidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test3", "");
        assertFalse(instance.getBoolean("test3", false));
        assertTrue(instance.getBoolean("test3", true));

        instance.setProperty("test3", "cats");
        assertFalse(instance.getBoolean("test3", false));
        assertTrue(instance.getBoolean("test3", true));
    }

    @Test
    public void testGetBooleanWithStringWithDefaultWithMissingProperty() {
        final Configuration instance = getInstance();
        assertTrue(instance.getBoolean("test3", true));
        assertFalse(instance.getBoolean("test3", false));
    }

    /* getDouble(Key) */

    @Test
    public void testGetDoubleWithKeyWithValidProperty() {
        final Configuration instance = getInstance();
        final double delta = 0.0000001f;
        instance.setProperty(Key.IIIF_1_ENDPOINT_ENABLED, 0.25f);
        instance.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, "0.55");
        assertEquals(0.25f, instance.getDouble(Key.IIIF_1_ENDPOINT_ENABLED), delta);
        assertEquals(0.55f, instance.getDouble(Key.IIIF_2_ENDPOINT_ENABLED), delta);
    }

    @Test
    public void testGetDoubleWithKeyWithInvalidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty(Key.IIIF_1_ENDPOINT_ENABLED, "cats");
        try {
            instance.getDouble(Key.IIIF_1_ENDPOINT_ENABLED);
            fail("Expected exception");
        } catch (ConversionException e) {
            // pass
        }
    }

    @Test
    public void testGetDoubleWithKeyWithMissingProperty() {
        final Configuration instance = getInstance();
        try {
            instance.getDouble(Key.IIIF_1_ENDPOINT_ENABLED);
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    /* getDouble(String) */

    @Test
    public void testGetDoubleWithStringWithValidProperty() {
        final Configuration instance = getInstance();
        final double delta = 0.0000001f;
        instance.setProperty("test1", 0.25f);
        instance.setProperty("test2", "0.55");
        assertEquals(0.25f, instance.getDouble("test1"), delta);
        assertEquals(0.55f, instance.getDouble("test2"), delta);
    }

    @Test
    public void testGetDoubleWithStringWithInvalidProperty() {
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
    public void testGetDoubleWithStringWithMissingProperty() {
        final Configuration instance = getInstance();
        try {
            instance.getDouble("test3");
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    /* getDouble(Key, double) */

    @Test
    public void testGetDoubleWithKeyWithDefaultWithValidProperty() {
        final Configuration instance = getInstance();
        final float delta = 0.0000001f;
        instance.setProperty(Key.IIIF_1_ENDPOINT_ENABLED, 0.5f);
        assertEquals(0.5f, instance.getDouble(Key.IIIF_1_ENDPOINT_ENABLED, 0.65f), delta);
    }

    @Test
    public void testGetDoubleWithKeyWithDefaultWithInvalidProperty() {
        final Configuration instance = getInstance();
        final float delta = 0.0000001f;
        instance.setProperty(Key.IIIF_1_ENDPOINT_ENABLED, "cats");
        assertEquals(0.5f, instance.getDouble(Key.IIIF_1_ENDPOINT_ENABLED, 0.5f), delta);
    }

    @Test
    public void testGetDoubleWithKeyWithDefaultWithMissingProperty() {
        final Configuration instance = getInstance();
        final float delta = 0.0000001f;
        assertEquals(0.65f, instance.getDouble(Key.IIIF_1_ENDPOINT_ENABLED, 0.65f), delta);
    }

    /* getDouble(String, double) */

    @Test
    public void testGetDoubleWithStringWithDefaultWithValidProperty() {
        final Configuration instance = getInstance();
        final float delta = 0.0000001f;
        instance.setProperty("test1", 0.5f);
        assertEquals(0.5f, instance.getDouble("test1", 0.65f), delta);
    }

    @Test
    public void testGetDoubleWithStringWithDefaultWithInvalidProperty() {
        final Configuration instance = getInstance();
        final float delta = 0.0000001f;
        instance.setProperty("test1", "cats");
        assertEquals(0.5f, instance.getDouble("test1", 0.5f), delta);
    }

    @Test
    public void testGetDoubleWithStringWithDefaultWithMissingProperty() {
        final Configuration instance = getInstance();
        final float delta = 0.0000001f;
        assertEquals(0.65f, instance.getDouble("test1", 0.65f), delta);
    }

    /* getFloat(Key) */

    @Test
    public void testGetFloatWithKeyWithValidProperty() {
        final Configuration instance = getInstance();
        final float delta = 0.0000001f;
        instance.setProperty(Key.IIIF_1_ENDPOINT_ENABLED, 0.25f);
        instance.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, "0.55");
        assertEquals(0.25f, instance.getFloat(Key.IIIF_1_ENDPOINT_ENABLED), delta);
        assertEquals(0.55f, instance.getFloat(Key.IIIF_2_ENDPOINT_ENABLED), delta);
    }

    @Test
    public void testGetFloatWithKeyWithInvalidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty(Key.MAX_PIXELS, "cats");
        try {
            instance.getFloat(Key.MAX_PIXELS);
            fail("Expected exception");
        } catch (ConversionException e) {
            // pass
        }
    }

    @Test
    public void testGetFloatWithKeyWithMissingProperty() {
        final Configuration instance = getInstance();
        try {
            instance.getFloat(Key.MAX_PIXELS);
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
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

    /* getFloat(Key, float) */

    @Test
    public void testGetFloatWithKeyWithDefaultWithValidProperty() {
        final Configuration instance = getInstance();
        final float delta = 0.0000001f;
        instance.setProperty(Key.MAX_PIXELS, 0.5f);
        assertEquals(0.5f, instance.getFloat(Key.MAX_PIXELS, 0.65f), delta);
    }

    @Test
    public void testGetFloatWithKeyWithDefaultWithInvalidProperty() {
        final Configuration instance = getInstance();
        final float delta = 0.0000001f;
        instance.setProperty(Key.MAX_PIXELS, "cats");
        assertEquals(0.5f, instance.getFloat(Key.MAX_PIXELS, 0.5f), delta);
    }

    @Test
    public void testGetFloatWithKeyWithDefaultWithMissingProperty() {
        final Configuration instance = getInstance();
        final float delta = 0.0000001f;
        assertEquals(0.65f, instance.getFloat(Key.MAX_PIXELS, 0.65f), delta);
    }

    /* getFloat(String, float) */

    @Test
    public void testGetFloatWithStringWithDefaultWithValidProperty() {
        final Configuration instance = getInstance();
        final float delta = 0.0000001f;
        instance.setProperty("test1", 0.5f);
        assertEquals(0.5f, instance.getFloat("test1", 0.65f), delta);
    }

    @Test
    public void testGetFloatWithStringWithDefaultWithInvalidProperty() {
        final Configuration instance = getInstance();
        final float delta = 0.0000001f;
        instance.setProperty("test1", "cats");
        assertEquals(0.5f, instance.getFloat("test1", 0.5f), delta);
    }

    @Test
    public void testGetFloatWithStringWithDefaultWithMissingProperty() {
        final Configuration instance = getInstance();
        final float delta = 0.0000001f;
        assertEquals(0.65f, instance.getFloat("test1", 0.65f), delta);
    }

    /* getInt(Key) */

    @Test
    public void testGetIntWithKeyWithValidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty(Key.IIIF_1_ENDPOINT_ENABLED, 25);
        instance.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, "55");
        assertEquals(25, instance.getInt(Key.IIIF_1_ENDPOINT_ENABLED));
        assertEquals(55, instance.getInt(Key.IIIF_2_ENDPOINT_ENABLED));
    }

    @Test
    public void testGetIntWithKeyWithInvalidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty(Key.MAX_PIXELS, "cats");
        try {
            instance.getInt(Key.MAX_PIXELS);
            fail("Expected exception");
        } catch (ConversionException e) {
            // pass
        }
    }

    @Test
    public void testGetIntWithKeyWithMissingProperty() {
        final Configuration instance = getInstance();
        try {
            instance.getInt(Key.MAX_PIXELS);
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    /* getInt(String) */

    @Test
    public void testGetIntWithStringWithValidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", 25);
        instance.setProperty("test2", "55");
        assertEquals(25, instance.getInt("test1"));
        assertEquals(55, instance.getInt("test2"));
    }

    @Test
    public void testGetIntWithStringWithInvalidProperty() {
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
    public void testGetIntWithStringWithMissingProperty() {
        final Configuration instance = getInstance();
        try {
            instance.getInt("test3");
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    /* getInt(Key, int) */

    @Test
    public void testGetIntWithKeyWithDefaultWithValidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty(Key.MAX_PIXELS, 5);
        assertEquals(5, instance.getInt(Key.MAX_PIXELS, 6));
    }

    @Test
    public void testGetIntWithKeyWithDefaultWithInvalidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty(Key.MAX_PIXELS, "cats");
        assertEquals(5, instance.getInt(Key.MAX_PIXELS, 5));
    }

    @Test
    public void testGetIntWithKeyWithDefaultWithMissingProperty() {
        final Configuration instance = getInstance();
        assertEquals(6, instance.getInt(Key.MAX_PIXELS, 6));
    }

    /* getInt(String, int) */

    @Test
    public void testGetIntWithStringWithDefaultWithValidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", 5);
        assertEquals(5, instance.getInt("test1", 6));
    }

    @Test
    public void testGetIntWithStringWithDefaultWithInvalidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", "cats");
        assertEquals(5, instance.getInt("test1", 5));
    }

    @Test
    public void testGetIntWithStringWithDefaultWithMissingProperty() {
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

    /* getLong(Key) */

    @Test
    public void testGetLongWithKeyWithValidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty(Key.IIIF_1_ENDPOINT_ENABLED, 25);
        instance.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, "55");
        assertEquals(25, instance.getLong(Key.IIIF_1_ENDPOINT_ENABLED));
        assertEquals(55, instance.getLong(Key.IIIF_2_ENDPOINT_ENABLED));
    }

    @Test
    public void testGetLongWithKeyWithInvalidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty(Key.MAX_PIXELS, "cats");
        try {
            instance.getLong(Key.MAX_PIXELS);
            fail("Expected exception");
        } catch (ConversionException e) {
            // pass
        }
    }

    @Test
    public void testGetLongWithKeyWithMissingProperty() {
        final Configuration instance = getInstance();
        try {
            instance.getLong(Key.MAX_PIXELS);
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    /* getLong(String) */

    @Test
    public void testGetLongWithStringWithValidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", 25);
        instance.setProperty("test2", "55");
        assertEquals(25, instance.getLong("test1"));
        assertEquals(55, instance.getLong("test2"));
    }

    @Test
    public void testGetLongWithStringWithInvalidProperty() {
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
    public void testGetLongWithStringWithMissingProperty() {
        final Configuration instance = getInstance();
        try {
            instance.getLong("test3");
            fail("Expected exception");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    /* getLong(Key, int) */

    @Test
    public void testGetLongWithKeyWithDefaultWithValidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty(Key.MAX_PIXELS, 5);
        assertEquals(5, instance.getLong(Key.MAX_PIXELS, 6));
    }

    @Test
    public void testGetLongWithKeyWithDefaultWithInvalidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty(Key.MAX_PIXELS, "cats");
        assertEquals(5, instance.getLong(Key.MAX_PIXELS, 5));
    }

    @Test
    public void testGetLongWithKeyWithDefaultWithMissingProperty() {
        final Configuration instance = getInstance();
        assertEquals(6, instance.getLong(Key.MAX_PIXELS, 6));
    }

    /* getLong(String, int) */

    @Test
    public void testGetLongWithStringWithDefaultWithValidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", 5);
        assertEquals(5, instance.getLong("test1", 6));
    }

    @Test
    public void testGetLongWithStringWithDefaultWithInvalidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", "cats");
        assertEquals(5, instance.getLong("test1", 5));
    }

    @Test
    public void testGetLongWithStringWithDefaultWithMissingProperty() {
        final Configuration instance = getInstance();
        assertEquals(6, instance.getLong("test1", 6));
    }

    /* getProperty(Key) */

    @Test
    public void testGetPropertyWithKeyWithPresentProperty() {
        final Configuration instance = getInstance();
        instance.setProperty(Key.IIIF_1_ENDPOINT_ENABLED, "1");
        instance.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, 2);
        assertEquals("1", instance.getProperty(Key.IIIF_1_ENDPOINT_ENABLED));
        assertEquals(2, instance.getProperty(Key.IIIF_2_ENDPOINT_ENABLED));
        assertNull(instance.getProperty(Key.MAX_PIXELS));
    }

    @Test
    public void testGetPropertyWithKeyWithMissingProperty() {
        final Configuration instance = getInstance();
        assertNull(instance.getProperty(Key.MAX_PIXELS));
    }

    /* getProperty(String) */

    @Test
    public void testGetPropertyWithStringWithPresentProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("cats", "1");
        instance.setProperty("dogs", 2);
        assertEquals("1", instance.getProperty("cats"));
        assertEquals(2, instance.getProperty("dogs"));
        assertNull(instance.getProperty("pigs"));
    }

    @Test
    public void testGetPropertyWithStringWithMissingProperty() {
        final Configuration instance = getInstance();
        assertNull(instance.getProperty("cats"));
    }

    /* getString(Key) */

    @Test
    public void testGetStringWithKeyWithValidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty(Key.MAX_PIXELS, "cats");
        assertEquals("cats", instance.getString(Key.MAX_PIXELS));
    }

    @Test
    public void testGetStringWithKeyWithMissingProperty() {
        final Configuration instance = getInstance();
        assertNull(instance.getString(Key.MAX_PIXELS));
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

    /* getString(Key, String) */

    @Test
    public void testGetStringWithKeyWithDefaultWithValidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty(Key.MAX_PIXELS, "cats");
        assertEquals("cats", instance.getString(Key.MAX_PIXELS, "dogs"));
    }

    @Test
    public void testGetStringWithKeyWithDefaultWithMissingProperty() {
        final Configuration instance = getInstance();
        assertEquals("cats", instance.getString(Key.MAX_PIXELS, "cats"));
    }

    /* getString(String, String) */

    @Test
    public void testGetStringWithStringWithDefaultWithValidProperty() {
        final Configuration instance = getInstance();
        instance.setProperty("test1", "cats");
        assertEquals("cats", instance.getString("test1", "dogs"));
    }

    @Test
    public void testGetStringWithStringWithDefaultWithMissingProperty() {
        final Configuration instance = getInstance();
        assertEquals("cats", instance.getString("test1", "cats"));
    }

}
