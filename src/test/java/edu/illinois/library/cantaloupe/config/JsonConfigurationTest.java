package edu.illinois.library.cantaloupe.config;

import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;

import static org.junit.Assert.*;

public class JsonConfigurationTest {

    private JsonConfiguration instance;

    @Before
    public void setUp() {
        instance = new JsonConfiguration();
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

    /* getFile() */

    @Test
    public void testGetFile() {
        try {
            File directory = new File(".");
            String cwd = directory.getCanonicalPath();
            Path testPath = Paths.get(cwd, "src", "test", "java", "edu",
                    "illinois", "library", "cantaloupe", "test");

            String goodProps = testPath + File.separator + "cantaloupe.json";
            System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, goodProps);
            assertEquals(new File(cwd + "/src/test/java/edu/illinois/library/cantaloupe/test/cantaloupe.json"),
                    instance.getFile());
        } catch (IOException e) {
            fail("Failed to set " + ConfigurationFactory.CONFIG_VM_ARGUMENT);
        }
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

    @Test
    public void testReload() throws Exception {
        File tempFile = File.createTempFile("cantaloupe", ".json");
        tempFile.deleteOnExit();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT,
                tempFile.getAbsolutePath());

        final String json = "{\n" +
                "    \"boolean\": true,\n" +
                "    \"string\": \"cats\",\n" +
                "    \"float\": 123.123,\n" +
                "    \"int\": 151\n" +
                "}";
        FileUtils.writeStringToFile(instance.getFile(), json);
        instance.reload();

        assertEquals("true", instance.getProperty("boolean"));
        assertEquals("123.123", instance.getProperty("float"));
        assertEquals("151", instance.getProperty("int"));
        assertEquals("cats", instance.getProperty("string"));
    }

    @Test
    public void testSave() throws Exception {
        File tempFile = File.createTempFile("cantaloupe", ".json");
        tempFile.deleteOnExit();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT,
                tempFile.getAbsolutePath());
        instance.save();

        String contents = FileUtils.readFileToString(tempFile);
        assertEquals("{", contents.substring(0, 1));
    }

    @Test
    public void testToJson() {
        instance.setProperty("boolean", true);
        instance.setProperty("float", 123.123);
        instance.setProperty("int", 151);
        instance.setProperty("string", "cats");

        final String expected = "{\n" +
                "    \"boolean\": true,\n" +
                "    \"string\": \"cats\",\n" +
                "    \"float\": 123.123,\n" +
                "    \"int\": 151\n" +
                "}";
        assertEquals(expected, instance.toJson());
    }

}
