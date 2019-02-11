package edu.illinois.library.cantaloupe.config;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

public class PropertiesDocumentTest extends BaseTest {

    private static Path FIXTURE;

    private PropertiesDocument instance;

    @Before
    public void setUp() throws Exception {
        instance = new PropertiesDocument();
        FIXTURE = TestUtil.getFixture("config.properties");
        instance.load(FIXTURE);
    }

    @Test
    public void testClear() {
        instance.clear();
        assertEquals(0, instance.size());
    }

    @Test
    public void testClearKey() {
        assertEquals(9, instance.size());
        instance.clearKey("bogus");
        assertEquals(9, instance.size());

        instance.clearKey("key1");
        assertEquals(8, instance.size());
    }

    @Test
    public void testContainsKey() {
        assertFalse(instance.containsKey("bogus"));
        assertTrue(instance.containsKey("key1"));
    }

    @Test
    public void testGet() {
        assertNull(instance.get("bogus"));
        assertEquals("value", instance.get("key1"));
        assertEquals("value=value", instance.get("key2"));
        assertEquals("value\\value", instance.get("key3"));
        assertEquals("value", instance.get("key4"));
        assertEquals("value", instance.get("key5"));
    }

    @Test
    public void testGetKeys() {
        Iterator<String> it = instance.getKeys();
        assertEquals("key1", it.next());
        assertEquals("key2", it.next());
        assertEquals("key3", it.next());
        assertEquals("key4", it.next());
        assertEquals("key5", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testLoad() {
        List<PropertiesDocument.Item> items = instance.items();
        assertEquals(9, items.size());

        // item 0
        PropertiesDocument.Comment comment =
                (PropertiesDocument.Comment) items.get(0);
        assertEquals(" This is a test document", comment.comment());

        // item 1
        comment = (PropertiesDocument.Comment) items.get(1);
        assertEquals("with another comment", comment.comment());

        // item 2
        comment = (PropertiesDocument.Comment) items.get(2);
        assertEquals(" and a different kind of comment", comment.comment());

        // item 3
        assertTrue(items.get(3) instanceof PropertiesDocument.EmptyLine);

        // item 4
        PropertiesDocument.KeyValuePair pair =
                (PropertiesDocument.KeyValuePair) items.get(4);
        assertEquals("key1", pair.key());
        assertEquals("value", pair.value());

        // item 5
        pair = (PropertiesDocument.KeyValuePair) items.get(5);
        assertEquals("key2 ", pair.key());
        assertEquals(" value=value", pair.value());

        // item 6
        pair = (PropertiesDocument.KeyValuePair) items.get(6);
        assertEquals("key3 ", pair.key());
        assertEquals(" value\\\\value", pair.value());

        // item 7
        pair = (PropertiesDocument.KeyValuePair) items.get(7);
        assertEquals("key4", pair.key());
        assertEquals("value", pair.value());

        // item 8
        pair = (PropertiesDocument.KeyValuePair) items.get(8);
        assertEquals("key5 ", pair.key());
        assertEquals(" value", pair.value());
    }

    @Test
    public void testSave() throws Exception {
        Path tempFile = Files.createTempFile("test", "tmp");
        try {
            instance.save(tempFile);

            byte[] bytes = Files.readAllBytes(FIXTURE);
            byte[] bytes2 = Files.readAllBytes(tempFile);

            String s1 = new String(bytes);
            String s2 = new String(bytes2);

            assertArrayEquals(bytes, bytes2);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void testSetWithExistingKey() {
        instance.set("key1", "new value");

        assertEquals(9, instance.size());
        PropertiesDocument.KeyValuePair item =
                (PropertiesDocument.KeyValuePair) instance.items().get(4);
        assertEquals("key1", item.key());
        assertEquals("new value", item.value());
    }

    @Test
    public void testSetWithNewKey() {
        instance.set("newKey", "value");

        assertEquals(10, instance.size());
        PropertiesDocument.KeyValuePair item =
                (PropertiesDocument.KeyValuePair) instance.items().get(9);
        assertEquals("newKey", item.key());
        assertEquals("value", item.value());
    }

    @Test
    public void testSize() {
        assertEquals(9, instance.size());
        instance.clear();
        assertEquals(0, instance.size());
    }

    @Test
    public void functionalTestOfLoadEditSave() throws Exception {
        instance.set("key1", "newValue");
        instance.set("key4", "newValue");
        instance.set("newKey", "value");

        Path tempFile = Files.createTempFile("test", "tmp");
        try {
            instance.save(tempFile);

            final String sep = System.lineSeparator();
            final String expected = "# This is a test document" + sep +
                    "#with another comment" + sep +
                    "! and a different kind of comment" + sep +
                    sep +
                    "key1=newValue" + sep +
                    "key2 = value=value" + sep +
                    "  key3 = value\\\\value" + sep +
                    "key4:newValue" + sep +
                    "key5 : value" + sep +
                    "newKey=value" + sep;

            byte[] bytes = Files.readAllBytes(tempFile);
            String actual = new String(bytes);

            assertEquals(expected, actual);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

}
