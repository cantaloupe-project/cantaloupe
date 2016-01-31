package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.Application;
import org.apache.commons.configuration.BaseConfiguration;
import org.junit.Test;

import static org.junit.Assert.*;

public class CacheFactoryTest {

    @Test
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
