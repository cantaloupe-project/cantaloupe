package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.config.Configuration;
import org.junit.Test;

import static org.junit.Assert.*;

public class CacheFactoryTest extends BaseTest {

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
    public void testGetDerivativeCache() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);

        final Key key = Key.DERIVATIVE_CACHE;

        config.setProperty(key, "FilesystemCache");
        assertTrue(CacheFactory.getDerivativeCache() instanceof FilesystemCache);

        config.setProperty(key, "");
        assertNull(CacheFactory.getDerivativeCache());

        config.setProperty(key, "bogus");
        assertNull(CacheFactory.getDerivativeCache());
    }

    @Test
    public void testGetDerivativeCacheInitializesNewInstance() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);

        final Key key = Key.DERIVATIVE_CACHE;

        config.setProperty(key, "MockCache");
        MockCache cache = (MockCache) CacheFactory.getDerivativeCache();

        assertTrue(cache.isInitializeCalled());
    }

    @Test
    public void testGetDerivativeCacheShutsDownPreviousInstance() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);

        final Key key = Key.DERIVATIVE_CACHE;

        config.setProperty(key, "MockCache");
        MockCache cache1 = (MockCache) CacheFactory.getDerivativeCache();

        config.setProperty(key, "FilesystemCache");
        CacheFactory.getDerivativeCache();

        assertTrue(cache1.isShutdownCalled());
    }

    /* getSourceCache() */

    @Test
    public void testGetSourceCache() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_CACHE_ENABLED, true);

        final Key key = Key.SOURCE_CACHE;

        config.setProperty(key, "FilesystemCache");
        assertTrue(CacheFactory.getSourceCache() instanceof FilesystemCache);

        config.setProperty(key, "");
        assertNull(CacheFactory.getSourceCache());

        config.setProperty(key, "bogus");
        assertNull(CacheFactory.getSourceCache());
    }

    @Test
    public void testGetSourceCacheInitializesNewInstance() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_CACHE_ENABLED, true);

        final Key key = Key.SOURCE_CACHE;

        config.setProperty(key, "MockCache");
        MockCache cache = (MockCache) CacheFactory.getSourceCache();

        assertTrue(cache.isInitializeCalled());
    }

    @Test
    public void testGetSourceCacheShutsDownPreviousInstance() {
        // This is not really testable
    }

}
