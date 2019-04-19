package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InfoCacheTest extends BaseTest {

    private InfoCache instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        instance = new InfoCache();
    }

    /* get() */

    @Test
    void testGetWithHit() {
        final Identifier identifier = new Identifier("jpg");
        final Info info = new Info();

        instance.put(identifier, info);

        Info actualInfo = instance.get(identifier);
        assertEquals(info, actualInfo);
    }

    @Test
    void testGetWithMiss() {
        final Identifier identifier = new Identifier("jpg");

        Info actualInfo = instance.get(identifier);
        assertNull(actualInfo);
    }

    /* maxSize() */

    @Test
    void testMaxSize() {
        assertTrue(instance.maxSize() > 1000);
    }

    /* purge() */

    @Test
    void testPurge() {
        final Identifier identifier = new Identifier("cats");
        final Info info = new Info();
        instance.put(identifier, info);
        assertEquals(1, instance.size());

        instance.purge();
        assertEquals(0, instance.size());
    }

    /* purge(Identifier) */

    @Test
    void testPurgeWithIdentifier() {
        final Identifier id1 = new Identifier("cats");
        final Identifier id2 = new Identifier("dogs");
        final Info info = new Info();
        instance.put(id1, info);
        instance.put(id2, info);
        assertEquals(2, instance.size());

        instance.purge(id1);
        assertEquals(1, instance.size());
    }

    /* put() */

    @Test
    void testPut() {
        assertEquals(0, instance.size());

        final Identifier identifier = new Identifier("cats");
        final Info info = new Info();
        instance.put(identifier, info);
        assertEquals(1, instance.size());
    }

    /* size() */

    @Test
    void testSize() {
        assertEquals(0, instance.size());

        final Identifier identifier = new Identifier("cats");
        final Info info = new Info();
        instance.put(identifier, info);
        assertEquals(1, instance.size());
    }

}
