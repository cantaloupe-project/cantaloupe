package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.Application;
import junit.framework.TestCase;
import org.apache.commons.configuration.BaseConfiguration;

public class CacheFactoryTest extends TestCase {

    public void testGetInstance() throws Exception {
        BaseConfiguration config = new BaseConfiguration();
        Application.setConfiguration(config);

        config.setProperty("cache", "FilesystemCache");
        assertTrue(CacheFactory.getInstance() instanceof FilesystemCache);

        config.setProperty("cache", "");
        assertNull(CacheFactory.getInstance());

        config.setProperty("cache", null);
        assertNull(CacheFactory.getInstance());

        config.setProperty("cache", "bogus");
        assertNull(CacheFactory.getInstance());
    }

}
