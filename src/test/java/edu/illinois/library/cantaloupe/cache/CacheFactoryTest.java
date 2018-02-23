package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.config.Configuration;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

public class CacheFactoryTest extends BaseTest {

    public void setUp() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
        config.setProperty(Key.DERIVATIVE_CACHE,
                FilesystemCache.class.getSimpleName());
        config.setProperty(Key.SOURCE_CACHE_ENABLED, true);
        config.setProperty(Key.SOURCE_CACHE,
                FilesystemCache.class.getSimpleName());
    }

    /* getAllDerivativeCaches() */

    @Test
    public void testGetAllDerivativeCaches() {
        assertEquals(6, CacheFactory.getAllDerivativeCaches().size());
    }

    /* getAllSourceCaches() */

    @Test
    public void testGetAllSourceCaches() {
        assertEquals(1, CacheFactory.getAllSourceCaches().size());
    }

    /* getDerivativeCache() */

    @Test
    public void testGetDerivativeCache() {
        Configuration config = Configuration.getInstance();
        final Key key = Key.DERIVATIVE_CACHE;

        config.setProperty(key, FilesystemCache.class.getSimpleName());
        assertTrue(CacheFactory.getDerivativeCache() instanceof FilesystemCache);

        config.setProperty(key, "");
        assertNull(CacheFactory.getDerivativeCache());

        config.clearProperty(key);
        assertNull(CacheFactory.getDerivativeCache());

        config.setProperty(key, "bogus");
        assertNull(CacheFactory.getDerivativeCache());

        config.setProperty(key, HeapCache.class.getSimpleName());
        assertTrue(CacheFactory.getDerivativeCache() instanceof HeapCache);
    }

    @Test
    public void testGetDerivativeCacheWithFullyQualifiedClassName() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE, HeapCache.class.getName());

        assertTrue(CacheFactory.getDerivativeCache() instanceof HeapCache);
    }

    @Test
    public void testGetDerivativeCacheInitializesNewInstance() {
        Configuration config = Configuration.getInstance();
        final Key key = Key.DERIVATIVE_CACHE;

        config.setProperty(key, MockCache.class.getSimpleName());
        MockCache cache = (MockCache) CacheFactory.getDerivativeCache();

        assertTrue(cache.isInitializeCalled());
    }

    @Test
    public void testGetDerivativeCacheShutsDownPreviousInstance() {
        Configuration config = Configuration.getInstance();
        final Key key = Key.DERIVATIVE_CACHE;

        config.setProperty(key, MockCache.class.getSimpleName());
        MockCache cache1 = (MockCache) CacheFactory.getDerivativeCache();

        config.setProperty(key, FilesystemCache.class.getSimpleName());
        CacheFactory.getDerivativeCache();

        assertTrue(cache1.isShutdownCalled());
    }

    @Test
    public void testGetDerivativeCacheConcurrently() throws Exception {
        final Configuration config = Configuration.getInstance();
        final int numThreads = 1000;
        final CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            ThreadPool.getInstance().submit(() -> {
                assertNotNull(CacheFactory.getDerivativeCache());
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
    public void testGetSourceCache() {
        Configuration config = Configuration.getInstance();
        final Key key = Key.SOURCE_CACHE;

        config.setProperty(key, FilesystemCache.class.getSimpleName());
        assertTrue(CacheFactory.getSourceCache() instanceof FilesystemCache);

        config.setProperty(key, "");
        assertNull(CacheFactory.getSourceCache());

        config.clearProperty(key);
        assertNull(CacheFactory.getSourceCache());

        config.setProperty(key, "bogus");
        assertNull(CacheFactory.getSourceCache());
    }

    @Test
    public void testGetSourceCacheWithFullyQualifiedClassName() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_CACHE, FilesystemCache.class.getName());

        assertTrue(CacheFactory.getSourceCache() instanceof FilesystemCache);
    }

    @Test
    public void testGetSourceCacheInitializesNewInstance() {
        Configuration config = Configuration.getInstance();
        final Key key = Key.SOURCE_CACHE;

        config.setProperty(key, MockCache.class.getSimpleName());
        MockCache cache = (MockCache) CacheFactory.getSourceCache();

        assertTrue(cache.isInitializeCalled());
    }

    @Test
    public void testGetSourceCacheShutsDownPreviousInstance() {
        Configuration config = Configuration.getInstance();
        final Key key = Key.SOURCE_CACHE;

        config.setProperty(key, MockCache.class.getSimpleName());
        MockCache cache1 = (MockCache) CacheFactory.getSourceCache();

        config.setProperty(key, FilesystemCache.class.getSimpleName());
        CacheFactory.getSourceCache();

        assertTrue(cache1.isShutdownCalled());
    }

    @Test
    public void testGetSourceCacheConcurrently() throws Exception {
        final Configuration config = Configuration.getInstance();
        final int numThreads = 1000;
        final CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            ThreadPool.getInstance().submit(() -> {
                CacheFactory.getSourceCache();
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
