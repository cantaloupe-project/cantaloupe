package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class InfoCacheTest extends BaseTest {

    private InfoCache instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        instance = new InfoCache();
    }

    /* get() */

    @Test
    public void testGetWithHit() {
        final Identifier identifier = new Identifier("jpg");
        final Info info = new Info(500, 300);

        instance.put(identifier, info);

        Info actualInfo = instance.get(identifier);
        assertEquals(info, actualInfo);
    }

    @Test
    public void testGetWithMiss() {
        final Identifier identifier = new Identifier("jpg");

        Info actualInfo = instance.get(identifier);
        assertNull(actualInfo);
    }

    /* maxSize() */

    @Test
    public void testMaxSize() {
        assertTrue(instance.maxSize() > 1000);
    }

    /* purge() */

    @Test
    public void testPurge() {
        final Identifier identifier = new Identifier("cats");
        final Info info = new Info(500, 300);
        instance.put(identifier, info);
        assertEquals(1, instance.size());

        instance.purge();
        assertEquals(0, instance.size());
    }

    /* purge(Identifier) */

    @Test
    public void testPurgeWithIdentifier() {
        final Identifier id1 = new Identifier("cats");
        final Identifier id2 = new Identifier("dogs");
        final Info info = new Info(500, 300);
        instance.put(id1, info);
        instance.put(id2, info);
        assertEquals(2, instance.size());

        instance.purge(id1);
        assertEquals(1, instance.size());
    }

    /* put() */

    @Test
    public void testPut() {
        assertEquals(0, instance.size());

        final Identifier identifier = new Identifier("cats");
        final Info info = new Info(500, 300);
        instance.put(identifier, info);
        assertEquals(1, instance.size());
    }

    /* size() */

    @Test
    public void testSize() {
        assertEquals(0, instance.size());

        final Identifier identifier = new Identifier("cats");
        final Info info = new Info(500, 300);
        instance.put(identifier, info);
        assertEquals(1, instance.size());
    }

}
