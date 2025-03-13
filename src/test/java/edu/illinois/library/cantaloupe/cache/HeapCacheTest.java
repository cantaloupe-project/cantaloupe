package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class HeapCacheTest extends AbstractCacheTest {

    @Nested
    class KeyTest extends BaseTest {

        @Test
        void testEqualsWithInfoKey() {
            // Equal
            HeapCache.Key k1 = new HeapCache.Key("cats");
            HeapCache.Key k2 = new HeapCache.Key("cats");
            assertEquals(k1, k2);

            // Unequal
            k2 = new HeapCache.Key("dogs");
            assertNotEquals(k1, k2);
        }

        @Test
        void testEqualsWithImageKey() {
            // Equal
            HeapCache.Key k1 = new HeapCache.Key("cats", "birds");
            HeapCache.Key k2 = new HeapCache.Key("cats", "birds");
            assertEquals(k1, k2);

            // Unequal op lists
            k1 = new HeapCache.Key("cats", "birds");
            k2 = new HeapCache.Key("cats", "goats");
            assertNotEquals(k1, k2);

            // Unequal identifiers and op lists
            k1 = new HeapCache.Key("cats", "birds");
            k2 = new HeapCache.Key("dogs", "goats");
            assertNotEquals(k1, k2);
        }

        @Test
        void testEqualsWithMixedKeys() {
            HeapCache.Key k1 = new HeapCache.Key("cats", "birds");
            HeapCache.Key k2 = new HeapCache.Key("cats");
            assertNotEquals(k1, k2);
        }

    }

    private static final String IMAGE = "jpg-rgb-64x56x8-baseline.jpg";

    private HeapCache instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        instance = newInstance();
        assertFalse(instance.isDirty());
    }

    @Override
    HeapCache newInstance() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, Math.pow(1024, 2));

        return new HeapCache();
    }

    /* dumpToPersistentStore() */

    @Test
    void testDumpToPersistentStore() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HEAPCACHE_PERSIST, true);

        Path cacheFile = Files.createTempFile("heapcache", "tmp");
        try {
            Files.delete(cacheFile);
            config.setProperty(Key.HEAPCACHE_PATHNAME, cacheFile.toString());

            // Seed an image
            OperationList ops = new OperationList(new Identifier("cats"));
            try (CompletableOutputStream os =
                         instance.newDerivativeImageOutputStream(ops)) {
                Files.copy(TestUtil.getImage(IMAGE), os);
                os.setComplete(true);
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
    void testGetByteSize() throws Exception {
        // Initial size
        assertEquals(0, instance.getByteSize());

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, 10000);

        // Seed an image
        Identifier id1 = new Identifier("cats");
        OperationList ops = new OperationList(id1);
        try (CompletableOutputStream os =
                     instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(TestUtil.getImage(IMAGE), os);
            os.setComplete(true);
        }

        assertEquals(5439, instance.getByteSize());

        // Seed an info
        Info info = new Info();
        instance.put(id1, info);

        assertEquals(5439 + info.toJSON().length(), instance.getByteSize());
    }

    /* getInfo(Identifier) */

    /**
     * Override that does nothing as this cache does not invalidate on the
     * basis of age.
     */
    @Test
    @Override
    void testGetInfoWithExistingInvalidImage() {}

    /* getTargetByteSize() */

    @Test
    void testGetTargetByteSizeWithInvalidValue() {
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
    void testGetTargetByteSizeWithNumber() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, 1000);
        assertEquals(1000, instance.getTargetByteSize());
    }

    @Test
    void testGetTargetByteSizeWithUnitSuffix() throws Exception {
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
    void testIsPersistenceEnabled() {
        Configuration config = Configuration.getInstance();

        config.setProperty(Key.HEAPCACHE_PERSIST, true);
        assertTrue(instance.isPersistenceEnabled());

        config.setProperty(Key.HEAPCACHE_PERSIST, false);
        assertFalse(instance.isPersistenceEnabled());
    }

    /* loadFromPersistentStore() */

    @Test
    void testLoadFromPersistentStore() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HEAPCACHE_PERSIST, true);

        Path cacheFile = Files.createTempFile("heapcache", "tmp");
        try {
            Files.delete(cacheFile);
            config.setProperty(Key.HEAPCACHE_PATHNAME, cacheFile.toString());

            // Seed an image
            OperationList ops = new OperationList(new Identifier("cats"));
            try (CompletableOutputStream os =
                         instance.newDerivativeImageOutputStream(ops)) {
                Files.copy(TestUtil.getImage(IMAGE), os);
                os.setComplete(true);
            }

            instance.dumpToPersistentStore();

            instance = new HeapCache();
            instance.loadFromPersistentStore();
            assertEquals(1, instance.size());

            assertNotNull(instance.newDerivativeImageInputStream(ops));
        } finally {
            Files.deleteIfExists(cacheFile);
        }
    }

    /* newDerivativeImageInputStream(OperationList) */

    /**
     * Override that does nothing as this cache does not invalidate on the
     * basis of age.
     */
    @Override
    @Test
    void testNewDerivativeImageInputStreamWithNonzeroTTL() {}

    /* newDerivativeImageOutputStream(OperationList) */

    @Test
    void testNewDerivativeImageOutputStreamSetsDirtyFlag() {
        OperationList ops = new OperationList(new Identifier("cats"));

        instance.newDerivativeImageOutputStream(ops);
        assertTrue(instance.isDirty());
    }

    @Test
    void testOnCacheWorker() throws Exception {
        Path dir = Files.createTempDirectory("test");
        Path file = dir.resolve("dump");

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HEAPCACHE_PERSIST, true);
        config.setProperty(Key.HEAPCACHE_PATHNAME, file);

        instance.put(new Identifier("cats"), new Info());

        assertFalse(Files.exists(file));

        instance.onCacheWorker();

        assertTrue(Files.exists(file));
    }

    /* purgeExcess() */

    @Test
    void testPurgeExcessWithExcess() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, 5000);

        // Seed an image
        OperationList ops = new OperationList(new Identifier("cats"));
        try (CompletableOutputStream os =
                     instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(TestUtil.getImage(IMAGE), os);
            os.setComplete(true);
        }

        assertEquals(5439, instance.getByteSize());

        instance.purgeExcess();

        assertEquals(0, instance.getByteSize());
        assertTrue(instance.isDirty());
    }

    @Test
    void testPurgeExcessWithNoExcess() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, 10000);

        // Seed an image
        OperationList ops = new OperationList(new Identifier("cats"));
        try (CompletableOutputStream os =
                     instance.newDerivativeImageOutputStream(ops)) {
            Files.copy(TestUtil.getImage(IMAGE), os);
            os.setComplete(true);
        }

        long size = instance.getByteSize();

        assertEquals(size, instance.getByteSize());

        instance.purgeExcess();

        assertEquals(size, instance.getByteSize());
    }

    @Test
    void testPurgeExcessThrowsConfigurationExceptionWhenMaxSizeIsInvalid() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HEAPCACHE_TARGET_SIZE, 0);

        assertThrows(ConfigurationException.class, () -> instance.purgeExcess());
    }

    /* purgeInvalid() */

    /**
     * Override that does nothing as this cache does not invalidate on the
     * basis of age.
     */
    @Override
    @Test
    void testPurgeInvalid() {}

}
