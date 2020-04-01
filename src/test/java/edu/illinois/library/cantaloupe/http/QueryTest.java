package edu.illinois.library.cantaloupe.http;

import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.Assert.*;

public class QueryTest {

    private Query instance;

    @Before
    public void setUp() throws Exception {
        instance = new Query("key1=value1&key2=value2");
    }

    @Test
    public void testCopyConstructor() {
        Query query2 = new Query(instance);
        assertEquals("value1", query2.getFirstValue("key1"));
        assertEquals("value2", query2.getFirstValue("key2"));
    }

    @Test
    public void testConstructorWithString() {
        assertEquals("value1", instance.getFirstValue("key1"));
        assertEquals("value2", instance.getFirstValue("key2"));
    }

    @Test
    public void testAdd1() {
        instance.add("key1");
        assertEquals(2, instance.getAll("key1").size());
    }

    @Test
    public void testAdd2() {
        instance.add("key1", "dogs");
        assertEquals(2, instance.getAll("key1").size());
    }

    @Test
    public void testClear() {
        instance.clear();
        assertTrue(instance.isEmpty());
    }

    @Test
    public void testEqualsWithEqualObjects() {
        Query instance2 = new Query("key1=value1&key2=value2");
        assertEquals(instance, instance2);
    }

    @Test
    public void testEqualsWithUnequalObjects() {
        Query instance2 = new Query("key1=value1&key3=value3");
        assertNotEquals(instance, instance2);
    }

    @Test
    public void testGetAll() {
        List<KeyValuePair> actual = instance.getAll();
        assertEquals(2, actual.size());
    }

    @Test
    public void testGetAllWithString() {
        List<KeyValuePair> actual = instance.getAll("key1");
        assertEquals(1, actual.size());
        assertEquals("key1", actual.get(0).getKey());
        assertEquals("value1", actual.get(0).getValue());
    }

    @Test
    public void getFirstValue() {
        assertEquals("value1", instance.getFirstValue("key1"));
        assertNull(instance.getFirstValue("bogus"));
    }

    @Test
    public void getFirstValueWithDefaultValue() {
        assertEquals("value1", instance.getFirstValue("key1", "default"));
        assertEquals("default", instance.getFirstValue("bogus", "default"));
    }

    @Test
    public void testHashCode() {
        assertEquals(instance.toString().hashCode(), instance.hashCode());
    }

    @Test
    public void testIsEmpty() {
        assertFalse(instance.isEmpty());
        instance.removeAll("key1");
        instance.removeAll("key2");
        assertTrue(instance.isEmpty());
    }

    @Test(expected = NoSuchElementException.class)
    public void testIterator() {
        Iterator<KeyValuePair> it = instance.iterator();
        assertNotNull(it.next());
        assertNotNull(it.next());
        it.next();
    }

    @Test
    public void testRemoveAll() {
        instance.removeAll("key1");
        assertNull(instance.getFirstValue("key1"));
    }

    @Test
    public void testSet() {
        assertNull(instance.getFirstValue("test"));
        instance.set("test", "cats");
        assertEquals("cats", instance.getFirstValue("test"));
    }

    @Test
    public void testSize() {
        assertEquals(2, instance.size());
    }

    @Test
    public void testStream() {
        assertNotNull(instance.stream());
    }

    @Test
    public void testToMap() {
        Map<String,String> actual = instance.toMap();
        assertEquals(2, actual.size());
        assertEquals("value1", actual.get("key1"));
        assertEquals("value2", actual.get("key2"));
    }

    @Test
    public void testToString() {
        assertEquals("key1=value1&key2=value2", instance.toString());
    }

    @Test
    public void testToStringWithEmptyInstance() {
        instance = new Query();
        assertEquals("", instance.toString());
    }

}