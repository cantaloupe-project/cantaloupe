package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.config.Configuration;
import org.junit.Test;

import static org.junit.Assert.*;

public class CacheFactoryTest extends BaseTest {

    @Test
    public void testGetAllDerivativeCaches() {
        assertEquals(4, CacheFactory.getAllDerivativeCaches().size());
    }

    @Test
    public void testGetAllSourceCaches() {
        assertEquals(1, CacheFactory.getAllSourceCaches().size());
    }

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

}
