package edu.illinois.library.cantaloupe.http;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class KeyValuePairTest {

    private KeyValuePair instance;

    @Before
    public void setUp() throws Exception {
        instance = new KeyValuePair("key", "value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullKey() {
        new KeyValuePair(null, "value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithEmptyKey() {
        new KeyValuePair("", "value");
    }

    @Test
    public void testEqualsWithEqualObjects() {
        KeyValuePair instance2 = new KeyValuePair("key", "value");
        assertEquals(instance, instance2);
    }

    @Test
    public void testEqualsWithUnequalKeys() {
        KeyValuePair instance2 = new KeyValuePair("cats", "value");
        assertNotEquals(instance, instance2);
    }

    @Test
    public void testEqualsWithUnequalValues() {
        KeyValuePair instance2 = new KeyValuePair("key", "cats");
        assertNotEquals(instance, instance2);
    }

    @Test
    public void testHashCode() {
        assertEquals(instance.toString().hashCode(), instance.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("key=value", instance.toString());
    }

}