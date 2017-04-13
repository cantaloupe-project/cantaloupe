package edu.illinois.library.cantaloupe.cache;

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
        config.setProperty(CacheFactory.DERIVATIVE_CACHE_ENABLED_CONFIG_KEY, true);

        final String key = CacheFactory.DERIVATIVE_CACHE_CONFIG_KEY;

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
        config.setProperty(CacheFactory.DERIVATIVE_CACHE_ENABLED_CONFIG_KEY, true);

        final String key = CacheFactory.DERIVATIVE_CACHE_CONFIG_KEY;

        config.setProperty(key, "MockCache");
        MockCache cache = (MockCache) CacheFactory.getDerivativeCache();

        assertTrue(cache.isInitializeCalled());
    }

    @Test
    public void testGetDerivativeCacheShutsDownPreviousInstance() {
        Configuration config = Configuration.getInstance();
        config.setProperty(CacheFactory.DERIVATIVE_CACHE_ENABLED_CONFIG_KEY, true);

        final String key = CacheFactory.DERIVATIVE_CACHE_CONFIG_KEY;

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
        config.setProperty(CacheFactory.SOURCE_CACHE_ENABLED_CONFIG_KEY, true);

        final String key = CacheFactory.SOURCE_CACHE_CONFIG_KEY;

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
        config.setProperty(CacheFactory.SOURCE_CACHE_ENABLED_CONFIG_KEY, true);

        final String key = CacheFactory.SOURCE_CACHE_CONFIG_KEY;

        config.setProperty(key, "MockCache");
        MockCache cache = (MockCache) CacheFactory.getSourceCache();

        assertTrue(cache.isInitializeCalled());
    }

    @Test
    public void testGetSourceCacheShutsDownPreviousInstance() {
        // This is not really testable
    }

}
