package edu.illinois.library.cantaloupe.http;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class KeyValuePairTest extends BaseTest {

    private KeyValuePair instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new KeyValuePair("key", "value");
    }

    @Test
    void testConstructorWithNullKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new KeyValuePair(null, "value"));
    }

    @Test
    void testConstructorWithEmptyKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new KeyValuePair("", "value"));
    }

    @Test
    void testEqualsWithEqualObjects() {
        KeyValuePair instance2 = new KeyValuePair("key", "value");
        assertEquals(instance, instance2);
    }

    @Test
    void testEqualsWithUnequalKeys() {
        KeyValuePair instance2 = new KeyValuePair("cats", "value");
        assertNotEquals(instance, instance2);
    }

    @Test
    void testEqualsWithUnequalValues() {
        KeyValuePair instance2 = new KeyValuePair("key", "cats");
        assertNotEquals(instance, instance2);
    }

    @Test
    void testHashCode() {
        assertEquals(instance.toString().hashCode(), instance.hashCode());
    }

    @Test
    void testToString() {
        assertEquals("key=value", instance.toString());
    }

}