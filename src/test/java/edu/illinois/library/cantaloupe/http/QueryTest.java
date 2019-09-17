package edu.illinois.library.cantaloupe.http;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public class QueryTest extends BaseTest {

    private Query instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Query("key1=value1&key2=value2");
    }

    @Test
    void testStringConstructor() {
        assertEquals("value1", instance.getFirstValue("key1"));
        assertEquals("value2", instance.getFirstValue("key2"));
    }

    @Test
    void testCopyConstructor() {
        Query q2 = new Query(instance);
        assertEquals(q2, instance);
    }

    @Test
    void testAdd1() {
        instance.add("key1");
        assertEquals(2, instance.getAll("key1").size());
    }

    @Test
    void testAdd2() {
        instance.add("key1", "dogs");
        assertEquals(2, instance.getAll("key1").size());
    }

    @Test
    void testClear() {
        instance.clear();
        assertTrue(instance.isEmpty());
    }

    @Test
    void testEqualsWithEqualObjects() {
        Query instance2 = new Query("key1=value1&key2=value2");
        assertEquals(instance, instance2);
    }

    @Test
    void testEqualsWithUnequalObjects() {
        Query instance2 = new Query("key1=value1&key3=value3");
        assertNotEquals(instance, instance2);
    }

    @Test
    void testGetAll() {
        List<KeyValuePair> actual = instance.getAll();
        assertEquals(2, actual.size());
    }

    @Test
    void testGetAllWithString() {
        List<KeyValuePair> actual = instance.getAll("key1");
        assertEquals(1, actual.size());
        assertEquals("key1", actual.get(0).getKey());
        assertEquals("value1", actual.get(0).getValue());
    }

    @Test
    void testGetFirstValue() {
        assertEquals("value1", instance.getFirstValue("key1"));
        assertNull(instance.getFirstValue("bogus"));
    }

    @Test
    void testGetFirstValueWithDefaultValue() {
        assertEquals("value1", instance.getFirstValue("key1", "default"));
        assertEquals("default", instance.getFirstValue("bogus", "default"));
    }

    @Test
    void testHashCode() {
        assertEquals(instance.toString().hashCode(), instance.hashCode());
    }

    @Test
    void testIsEmpty() {
        assertFalse(instance.isEmpty());
        instance.removeAll("key1");
        instance.removeAll("key2");
        assertTrue(instance.isEmpty());
    }

    @Test
    void testIterator() {
        Iterator<KeyValuePair> it = instance.iterator();
        assertNotNull(it.next());
        assertNotNull(it.next());

        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void testRemoveOnNonExistingKey() {
        int size = instance.size();
        instance.remove("bogus");
        assertEquals(size, instance.size());
    }

    @Test
    void testRemoveOnExistingKey() {
        int size = instance.size();
        instance.remove("key1");
        assertEquals(size - 1, instance.size());
    }

    @Test
    void testRemoveAll() {
        instance.removeAll("key1");
        assertNull(instance.getFirstValue("key1"));
    }

    @Test
    void testSet() {
        assertNull(instance.getFirstValue("test"));
        instance.set("test", "cats");
        assertEquals("cats", instance.getFirstValue("test"));
    }

    @Test
    void testSize() {
        assertEquals(2, instance.size());
    }

    @Test
    void testStream() {
        assertNotNull(instance.stream());
    }

    @Test
    void testToMap() {
        Map<String,String> actual = instance.toMap();
        assertEquals(2, actual.size());
        assertEquals("value1", actual.get("key1"));
        assertEquals("value2", actual.get("key2"));
    }

    @Test
    void testToString() {
        assertEquals("key1=value1&key2=value2", instance.toString());
    }

    @Test
    void testToStringWithEmptyInstance() {
        instance = new Query();
        assertEquals("", instance.toString());
    }

}