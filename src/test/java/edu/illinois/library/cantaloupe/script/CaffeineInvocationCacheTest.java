package edu.illinois.library.cantaloupe.script;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class CaffeineInvocationCacheTest {

    private CaffeineInvocationCache instance;

    @Before
    public void setUp() {
        instance = new CaffeineInvocationCache();
    }

    @Test
    public void testGet() {
        assertNull(instance.get("bogus"));
        String key = "key";
        String value = "value";
        instance.put(key, value);
        assertSame(value, instance.get(key));
    }

    @Test
    public void testPurge() {
        instance.put("key", "value");
        assertEquals(1, instance.size());
        instance.purge();
        assertEquals(0, instance.size());
    }

    @Test
    public void testPut() {
        String key = "key";
        String value = "value";
        instance.put(key, value);
        assertSame(value, instance.get(key));
    }

    @Test
    public void testSize() {
        assertEquals(0, instance.size());
        instance.put("key", "value");
        assertEquals(1, instance.size());
    }

}