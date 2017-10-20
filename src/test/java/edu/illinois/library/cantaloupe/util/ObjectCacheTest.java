package edu.illinois.library.cantaloupe.util;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ObjectCacheTest extends BaseTest {

    private static final long MAX_SIZE = 100;

    private ObjectCache<String,String> instance;

    @Before
    public void setUp() {
        instance = new ObjectCache<>(MAX_SIZE);
    }

    @Test
    public void testCleanUp() {
        final long excessiveSize = MAX_SIZE + 1;
        for (long i = 0; i < excessiveSize; i++) {
            instance.put("" + i, "cats");
        }
        instance.cleanUp();
        assertEquals(MAX_SIZE, instance.size());
    }

    @Test
    public void testGet() {
        final String key = "cats";
        final String value = "yes";
        instance.put(key, value);
        assertSame(value, instance.get(key));
    }

    @Test
    public void testPurge() {
        instance.put("1", "1");
        instance.put("2", "2");
        instance.put("3", "3");
        assertEquals(3, instance.size());

        instance.purge();
        assertEquals(0, instance.size());
    }

    @Test
    public void testPut() {
        final String key = "cats";
        final String value = "yes";
        instance.put(key, value);
        assertSame(value, instance.get(key));
    }

    @Test
    public void testPutRespectsMaxSize() throws Exception {
        for (int i = 0; i < MAX_SIZE * 2; i++) {
            instance.put("" + i, "cats");
        }
        instance.cleanUp();
        assertEquals(MAX_SIZE, instance.size());
    }

    @Test
    public void testSize() {
        assertEquals(0, instance.size());
        instance.put("1", "1");
        instance.put("2", "2");
        instance.put("3", "3");
        assertEquals(3, instance.size());
    }

}
