package edu.illinois.library.cantaloupe.http;

import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class HeadersTest {

    private Headers instance;

    @Before
    public void setUp() throws Exception {
        instance = new Headers();
    }

    @Test
    public void testAddWithStrings() {
        assertEquals(0, instance.size());
        instance.add("name", "value");
        assertEquals(1, instance.size());
    }

    @Test
    public void testClear() {
        instance.add("name", "value");
        instance.add("name", "value");
        instance.clear();
        assertEquals(0, instance.size());
    }

    @Test
    public void testEqualsWithEqualObjects() {
        Headers h1 = new Headers();
        Headers h2 = new Headers();
        assertEquals(h1, h2);

        h1.add("test", "cats1");
        h1.add("test", "cats2");
        h2.add("test", "cats1");
        h2.add("test", "cats2");
        assertEquals(h1, h2);
    }

    @Test
    public void testEqualsWithUnequalObjects() {
        Headers h1 = new Headers();
        Headers h2 = new Headers();
        h1.add("test", "cats1");
        h1.add("test", "cats2");
        h2.add("test", "cats1");
        h2.add("test2", "cats2");
        assertNotEquals(h1, h2);
    }

    @Test
    public void testGetAll() {
        assertEquals(0, instance.getAll().size());

        instance.add("name", "value");
        instance.add("name", "value");
        assertEquals(2, instance.getAll().size());
    }

    @Test
    public void testGetAllWithString() {
        assertEquals(0, instance.getAll("name").size());

        instance.add("name", "value");
        instance.add("name", "value");
        assertEquals(2, instance.getAll("name").size());
    }

    @Test
    public void testGetFirstValue() {
        assertNull(instance.getFirstValue("name"));

        instance.add("name", "value1");
        instance.add("name", "value2");
        assertEquals("value1", instance.getFirstValue("name"));
    }

    @Test
    public void testIterator() {
        instance.add("name", "value1");
        instance.add("name", "value2");

        Iterator<Header> it = instance.iterator();
        assertTrue(it.hasNext());
        it.next();
        assertTrue(it.hasNext());
        it.next();
        assertFalse(it.hasNext());
    }

    @Test
    public void testRemoveAll() {
        instance.add("name1", "value1");
        instance.add("name1", "value2");
        instance.add("name2", "value1");
        instance.removeAll("name1");
        assertEquals(1, instance.size());
        assertNotNull(instance.getFirstValue("name2"));
    }

    @Test
    public void testSet() {
        instance.set("name1", "value1");
        instance.set("name2", "value1");
        instance.set("name2", "value2");
        assertEquals(1, instance.getAll("name2").size());
        assertEquals("value2", instance.getFirstValue("name2"));
    }

    @Test
    public void testSize() {
        assertEquals(0, instance.size());
        instance.add("name", "value");
        instance.add("name", "value");
        assertEquals(2, instance.size());
    }

    @Test
    public void testStream() {
        instance.add("name", "value");
        instance.add("name", "value");
        assertEquals(2, instance.stream().collect(Collectors.toList()).size());
    }

}
