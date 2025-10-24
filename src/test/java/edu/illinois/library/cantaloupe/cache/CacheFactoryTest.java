package edu.illinois.library.cantaloupe.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.BaseTest;

public class CacheFactoryTest extends BaseTest {
    private CacheFactory instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
        config.setProperty(Key.DERIVATIVE_CACHE,
                FilesystemCache.class.getSimpleName());
        config.setProperty(Key.SOURCE_CACHE,
                FilesystemCache.class.getSimpleName());
        instance = new CacheFactory(config);
    }

    /* getAllDerivativeCaches() */

    @Test
    void testGetAllDerivativeCaches() {
        assertEquals(6, instance.getAllDerivativeCaches().size());
    }

    /* getAllSourceCaches() */

    @Test
    void testGetAllSourceCaches() {
        assertEquals(1, instance.getAllSourceCaches().size());
    }

    /* getDerivativeCache() */

    @Test
    void testGetDerivativeCache() {
        Configuration config = Configuration.getInstance();
        final Key key = Key.DERIVATIVE_CACHE;

        config.setProperty(key, FilesystemCache.class.getSimpleName());
        assertTrue(instance.getDerivativeCache().get() instanceof FilesystemCache);

        config.setProperty(key, "");
        assertFalse(instance.getDerivativeCache().isPresent());

        config.clearProperty(key);
        assertFalse(instance.getDerivativeCache().isPresent());

        config.setProperty(key, "bogus");
        assertFalse(instance.getDerivativeCache().isPresent());

        config.setProperty(key, HeapCache.class.getSimpleName());
        assertTrue(instance.getDerivativeCache().get() instanceof HeapCache);
    }

    @Test
    void testGetDerivativeCacheWithFullyQualifiedClassName() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE, HeapCache.class.getName());

        assertTrue(instance.getDerivativeCache().get() instanceof HeapCache);
    }

    @Test
    void testGetDerivativeCacheInitializesNewInstance() {
        Configuration config = Configuration.getInstance();
        final Key key = Key.DERIVATIVE_CACHE;

        config.setProperty(key, MockCache.class.getSimpleName());
        MockCache cache = (MockCache) instance.getDerivativeCache().get();

        assertTrue(cache.isInitializeCalled());
    }

    @Test
    void testGetDerivativeCacheShutsDownPreviousInstance() {
        Configuration config = Configuration.getInstance();
        final Key key = Key.DERIVATIVE_CACHE;

        config.setProperty(key, MockCache.class.getSimpleName());
        MockCache cache1 = (MockCache) instance.getDerivativeCache().get();

        config.setProperty(key, FilesystemCache.class.getSimpleName());
        instance.getDerivativeCache();

        assertTrue(cache1.isShutdownCalled());
    }

    @Test
    void testGetDerivativeCacheConcurrently() throws Exception {
        final Configuration config = Configuration.getInstance();
        final int numThreads = 1000;
        final CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            ThreadPool.getInstance().submit(() -> {
                assertNotNull(instance.getDerivativeCache());
                latch.countDown();

                // Introduce some "writers" to try and mess things up.
                if (latch.getCount() % 3 == 0) {
                    config.setProperty(Key.SOURCE_CACHE,
                            FilesystemCache.class.getSimpleName());
                } else if (latch.getCount() % 5 == 0) {
                    config.setProperty(Key.SOURCE_CACHE, "");
                }

                return null;
            });
        }
        latch.await();
    }

    /* getSourceCache() */

    @Test
    void testGetSourceCache() {
        Configuration config = Configuration.getInstance();
        final Key key = Key.SOURCE_CACHE;

        config.setProperty(key, FilesystemCache.class.getSimpleName());
        assertTrue(instance.getSourceCache().get() instanceof FilesystemCache);

        config.setProperty(key, "");
        assertFalse(instance.getSourceCache().isPresent());

        config.clearProperty(key);
        assertFalse(instance.getSourceCache().isPresent());

        config.setProperty(key, "bogus");
        assertFalse(instance.getSourceCache().isPresent());
    }

    @Test
    void testGetSourceCacheWithFullyQualifiedClassName() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_CACHE, FilesystemCache.class.getName());

        assertTrue(instance.getSourceCache().get() instanceof FilesystemCache);
    }

    @Test
    void testGetSourceCacheInitializesNewInstance() {
        Configuration config = Configuration.getInstance();
        final Key key = Key.SOURCE_CACHE;

        config.setProperty(key, MockCache.class.getSimpleName());
        MockCache cache = (MockCache) instance.getSourceCache().get();

        assertTrue(cache.isInitializeCalled());
    }

    @Test
    void testGetSourceCacheShutsDownPreviousInstance() {
        Configuration config = Configuration.getInstance();
        final Key key = Key.SOURCE_CACHE;

        config.setProperty(key, MockCache.class.getSimpleName());
        MockCache cache1 = (MockCache) instance.getSourceCache().get();

        config.setProperty(key, FilesystemCache.class.getSimpleName());
        instance.getSourceCache();

        assertTrue(cache1.isShutdownCalled());
    }

    @Test
    void testGetSourceCacheConcurrently() throws Exception {
        final Configuration config = Configuration.getInstance();
        final int numThreads = 1000;
        final CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            ThreadPool.getInstance().submit(() -> {
                instance.getSourceCache();
                latch.countDown();

                // Introduce some "writers" to try and mess things up.
                if (latch.getCount() % 3 == 0) {
                    config.setProperty(Key.SOURCE_CACHE,
                            FilesystemCache.class.getSimpleName());
                } else if (latch.getCount() % 5 == 0) {
                    config.setProperty(Key.SOURCE_CACHE, "");
                }
                return null;
            });
        }
        latch.await();
    }

}
