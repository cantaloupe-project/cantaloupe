package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.Application;
import junit.framework.TestCase;
import org.apache.commons.configuration.BaseConfiguration;

public class CacheFactoryTest extends TestCase {

    public void testGetInstance() throws Exception {
        BaseConfiguration config = new BaseConfiguration();
        Application.setConfiguration(config);

        final String key = CacheFactory.CACHE_CONFIG_KEY;

        config.setProperty(key, "FilesystemCache");
        assertTrue(CacheFactory.getInstance() instanceof FilesystemCache);

        config.setProperty(key, "");
        assertNull(CacheFactory.getInstance());

        config.setProperty(key, null);
        assertNull(CacheFactory.getInstance());

        config.setProperty(key, "bogus");
        assertNull(CacheFactory.getInstance());
    }

}
