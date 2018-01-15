package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class HeapCacheTest extends BaseTest {

    public static class KeyTest extends BaseTest {

        @Test
        public void testEqualsWithInfoKey() {
            // Equal
            HeapCache.Key k1 = new HeapCache.Key("cats");
            HeapCache.Key k2 = new HeapCache.Key("cats");
            assertTrue(k1.equals(k2));

            // Unequal
            k2 = new HeapCache.Key("dogs");
            assertFalse(k1.equals(k2));
        }

        @Test
        public void testEqualsWithImageKey() {
            // Equal
            HeapCache.Key k1 = new HeapCache.Key("cats", "birds");
            HeapCache.Key k2 = new HeapCache.Key("cats", "birds");
            assertTrue(k1.equals(k2));

            // Unequal op lists
            k1 = new HeapCache.Key("cats", "birds");
            k2 = new HeapCache.Key("cats", "goats");
            assertFalse(k1.equals(k2));

            // Unequal identifiers and op lists
            k1 = new HeapCache.Key("cats", "birds");
            k2 = new HeapCache.Key("dogs", "goats");
            assertFalse(k1.equals(k2));
        }

        @Test
        public void testEqualsWithMixedKeys() {
            HeapCache.Key k1 = new HeapCache.Key("cats", "birds");
            HeapCache.Key k2 = new HeapCache.Key("cats");
            assertFalse(k1.equals(k2));
        }

    }

    private static final String IMAGE = "jpg-rgb-64x56x8-baseline.jpg";

    private HeapCache instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, Math.pow(1024, 2));
        config.setProperty(Key.CACHE_SERVER_TTL, 0);

        instance = new HeapCache();
        assertFalse(instance.isDirty());
    }

    /* dumpToPersistentStore() */

    @Test
    public void testDumpToPersistentStore() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HEAPCACHE_PERSIST, true);

        Path cacheFile = Files.createTempFile("heapcache", "tmp");
        try {
            Files.delete(cacheFile);
            config.setProperty(Key.HEAPCACHE_PATHNAME, cacheFile.toString());

            // Seed an image
            Identifier id1 = new Identifier("cats");
            OperationList ops1 = new OperationList(id1, Format.JPG);
            try (OutputStream os = instance.newDerivativeImageOutputStream(ops1)) {
                Files.copy(TestUtil.getImage(IMAGE), os);
            }

            instance.dumpToPersistentStore();

            assertTrue(Files.exists(cacheFile));
            long size = Files.size(cacheFile);
            assertTrue(size > 5000);
        } finally {
            Files.deleteIfExists(cacheFile);
        }
    }

    /* getByteSize() */

    @Test
    public void testGetByteSize() throws Exception {
        // Initial size
        assertEquals(0, instance.getByteSize());

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, 10000);

        // Seed an image
        Identifier id1 = new Identifier("cats");
        OperationList ops1 = new OperationList(id1, Format.JPG);
        try (OutputStream os = instance.newDerivativeImageOutputStream(ops1)) {
            Files.copy(TestUtil.getImage(IMAGE), os);
        }

        assertEquals(5439, instance.getByteSize());

        // Seed an info
        Info info = new Info(52, 52);
        instance.put(id1, info);

        assertEquals(5439 + info.toJSON().length(), instance.getByteSize());
    }

    /* getImageInfo(Identifier) */

    @Test
    public void testGetImageInfo() throws Exception {
        // existing image
        Identifier identifier = new Identifier("cats");
        Info info = new Info(50, 40);
        instance.put(identifier, info);
        Info actual = instance.getImageInfo(identifier);
        assertEquals(actual, info);

        // nonexistent image
        assertNull(instance.getImageInfo(new Identifier("bogus")));
    }

    /* getTargetByteSize() */

    @Test
    public void testGetTargetByteSizeWithInvalidValue() {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, "");
        try {
            instance.getTargetByteSize();
            fail("Expected exception");
        } catch (ConfigurationException e) {
            // pass
        }
    }

    @Test
    public void testGetTargetByteSizeWithNumber() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, 1000);
        assertEquals(1000, instance.getTargetByteSize());
    }

    @Test
    public void testGetTargetByteSizeWithUnitSuffix() throws Exception {
        final Configuration config = Configuration.getInstance();
        final float base = 500.5f;
        final float delta = 0.0001f;

        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, base + "M");
        assertEquals(base * (long) Math.pow(1024, 2), instance.getTargetByteSize(), delta);

        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, base + "MB");
        assertEquals(base * (long) Math.pow(1024, 2), instance.getTargetByteSize(), delta);

        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, base + "G");
        assertEquals(base * (long) Math.pow(1024, 3), instance.getTargetByteSize(), delta);

        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, base + "GB");
        assertEquals(base * (long) Math.pow(1024, 3), instance.getTargetByteSize(), delta);

        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, base + "T");
        assertEquals(base * (long) Math.pow(1024, 4), instance.getTargetByteSize(), delta);

        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, base + "TB");
        assertEquals(base * (long) Math.pow(1024, 4), instance.getTargetByteSize(), delta);

        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, base + "P");
        assertEquals(base * (long) Math.pow(1024, 5), instance.getTargetByteSize(), delta);

        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, base + "PB");
        assertEquals(base * (long) Math.pow(1024, 5), instance.getTargetByteSize(), delta);
    }

    /* isPersistenceEnabled() */

    @Test
    public void testIsPersistenceEnabled() {
        Configuration config = Configuration.getInstance();

        config.setProperty(Key.HEAPCACHE_PERSIST, true);
        assertTrue(instance.isPersistenceEnabled());

        config.setProperty(Key.HEAPCACHE_PERSIST, false);
        assertFalse(instance.isPersistenceEnabled());
    }

    /* loadFromPersistentStore() */

    @Test
    public void testLoadFromPersistentStore() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HEAPCACHE_PERSIST, true);

        Path cacheFile = Files.createTempFile("heapcache", "tmp");
        try {
            Files.delete(cacheFile);
            config.setProperty(Key.HEAPCACHE_PATHNAME, cacheFile.toString());

            // Seed an image
            Identifier id1 = new Identifier("cats");
            OperationList ops1 = new OperationList(id1, Format.JPG);
            try (OutputStream os = instance.newDerivativeImageOutputStream(ops1)) {
                Files.copy(TestUtil.getImage(IMAGE), os);
            }

            instance.dumpToPersistentStore();

            instance = new HeapCache();
            instance.loadFromPersistentStore();
            assertEquals(1, instance.size());

            assertNotNull(instance.newDerivativeImageInputStream(ops1));
        } finally {
            Files.deleteIfExists(cacheFile);
        }
    }

    /* newDerivativeImageInputStream(OperationList) */

    @Test
    public void testNewDerivativeImageInputStream() throws Exception {
        OperationList ops = new OperationList(new Identifier("cats"), Format.JPG);

        // Doesn't exist yet
        assertNull(instance.newDerivativeImageInputStream(ops));

        try (OutputStream os = instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(TestUtil.getImage(IMAGE), os);
        }

        // Now it should.
        assertNotNull(instance.newDerivativeImageInputStream(ops));
    }

    /* newDerivativeImageOutputStream(OperationList) */

    @Test
    public void testNewDerivativeImageOutputStream() throws Exception {
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("cats"));
        assertNotNull(instance.newDerivativeImageOutputStream(ops));
    }

    @Test
    public void testNewDerivativeImageOutputStreamSetsDirtyFlag()
            throws Exception {
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("cats"));
        instance.newDerivativeImageOutputStream(ops);
        assertTrue(instance.isDirty());
    }

    /* purge() */

    @Test
    public void testPurge() throws Exception {
        // Seed a derivative image
        Identifier id1 = new Identifier("cats");
        OperationList ops1 = new OperationList(id1, Format.JPG);
        try (OutputStream os = instance.newDerivativeImageOutputStream(ops1)) {
            Files.copy(TestUtil.getImage(IMAGE), os);
        }
        // Seed an info
        instance.put(id1, new Info(50, 40));

        instance.purge();

        // assert that only the expired derivative images were purged
        assertNull(instance.getImageInfo(id1));
        assertNull(instance.newDerivativeImageInputStream(ops1));
    }

    /* purge(OperationList) */

    @Test
    public void testPurgeWithOperationList() throws Exception {
        // Seed a derivative image
        Identifier id1 = new Identifier("cats");
        OperationList ops = new OperationList(id1, Format.JPG);
        try (OutputStream os = instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(TestUtil.getImage(IMAGE), os);
        }

        instance.purge(ops);

        assertNull(instance.newDerivativeImageInputStream(ops));
    }

    /* purge(Identifier) */

    @Test
    public void testPurgeWithIdentifier() throws Exception {
        // Seed a derivative image
        Identifier id1 = new Identifier("cats");
        OperationList ops = new OperationList(id1, Format.JPG);

        try (OutputStream os = instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(TestUtil.getImage(IMAGE), os);
        }

        // Seed an info
        instance.put(id1, new Info(60, 40));

        instance.purge(id1);

        assertNull(instance.getImageInfo(id1));
        assertNull(instance.newDerivativeImageInputStream(ops));
    }

    /* purgeExcess() */

    @Test
    public void testPurgeExcessWithExcess() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, 5000);

        // Seed an image
        Identifier id1 = new Identifier("cats");
        OperationList ops1 = new OperationList(id1, Format.JPG);
        try (OutputStream os = instance.newDerivativeImageOutputStream(ops1)) {
            Files.copy(TestUtil.getImage(IMAGE), os);
        }

        assertEquals(5439, instance.getByteSize());

        instance.purgeExcess();

        assertEquals(0, instance.getByteSize());
        assertTrue(instance.isDirty());
    }

    @Test
    public void testPurgeExcessWithNoExcess() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, 10000);

        // Seed an image
        Identifier id1 = new Identifier("cats");
        OperationList ops1 = new OperationList(id1, Format.JPG);
        try (OutputStream os = instance.newDerivativeImageOutputStream(ops1)) {
            Files.copy(TestUtil.getImage(IMAGE), os);
        }

        long size = instance.getByteSize();

        assertEquals(size, instance.getByteSize());

        instance.purgeExcess();

        assertEquals(size, instance.getByteSize());
    }

    @Test
    public void testPurgeExcessThrowsConfigurationExceptionWhenMaxSizeIsInvalid() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, 0);
        try {
            instance.purgeExcess();
            fail("Expected exception");
        } catch (ConfigurationException e) {
            // pass
        }
    }

    /* put(Identifier, Info) */

    @Test
    public void testPut() throws Exception {
        // Test with existing info
        Identifier identifier = new Identifier("birds");
        Info info = new Info(52, 52);
        instance.put(identifier, info);
        assertEquals(info, instance.getImageInfo(identifier));
        assertTrue(instance.isDirty());

        // Test with missing info
        assertNull(instance.getImageInfo(new Identifier("bogus")));
    }

}
