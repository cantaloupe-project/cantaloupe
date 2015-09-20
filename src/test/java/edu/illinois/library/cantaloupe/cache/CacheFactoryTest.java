package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.Application;
import junit.framework.TestCase;
import org.apache.commons.configuration.BaseConfiguration;

public class CacheFactoryTest extends TestCase {

    public void testGetCache() throws Exception {
        BaseConfiguration config = new BaseConfiguration();
        Application.setConfiguration(config);

        config.setProperty("cache", "FilesystemCache");
        assertTrue(CacheFactory.getCache() instanceof FilesystemCache);

        try {
            config.setProperty("cache", "");
            assertNull(CacheFactory.getCache());
            fail("Expected exception");
        } catch (ClassNotFoundException e) {
            // pass
        }

        try {
            config.setProperty("cache", null);
            assertNull(CacheFactory.getCache());
            fail("Expected exception");
        } catch (ClassNotFoundException e) {
            // pass
        }

        try {
            config.setProperty("cache", "bogus");
            assertNull(CacheFactory.getCache());
            fail("Expected exception");
        } catch (ClassNotFoundException e) {
            // pass
        }
    }

}
