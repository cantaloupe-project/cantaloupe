package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HeapInvocationCacheTest extends BaseTest {

    private HeapInvocationCache instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new HeapInvocationCache();
    }

    @Test
    void testGet() {
        assertNull(instance.get("bogus"));
        String key = "key";
        String value = "value";
        instance.put(key, value);
        assertSame(value, instance.get(key));
    }

    @Test
    void testMaxSize() {
        assertTrue(instance.maxSize() > 100);
    }

    @Test
    void testPurge() {
        instance.put("key", "value");
        assertEquals(1, instance.size());
        instance.purge();
        assertEquals(0, instance.size());
    }

    @Test
    void testPut() {
        String key = "key";
        String value = "value";
        instance.put(key, value);
        assertSame(value, instance.get(key));
    }

    @Test
    void testSize() {
        assertEquals(0, instance.size());
        instance.put("key", "value");
        assertEquals(1, instance.size());
    }

}