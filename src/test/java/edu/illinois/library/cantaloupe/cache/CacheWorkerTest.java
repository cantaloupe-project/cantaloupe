package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.config.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CacheWorkerTest extends BaseTest {

    private CacheWorker instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
        config.setProperty(Key.DERIVATIVE_CACHE, MockCache.class.getName());
        config.setProperty(Key.SOURCE_CACHE, MockCache.class.getName());

        instance = new CacheWorker(-1); // we aren't using the interval
    }

    @Test
    void testRunCallsCleanUp() {
        MockCache cache = (MockCache) CacheFactory.getDerivativeCache();
        instance.run();
        assertTrue(cache.isCleanUpCalled());
    }

    @Test
    void testCallsPurgeInvalid() {
        MockCache cache = (MockCache) CacheFactory.getSourceCache().get();
        instance.run();
        assertTrue(cache.isPurgeInvalidCalled());
    }

    @Test
    void testRunCallsOnCacheWorkerCallback() {
        MockCache cache = (MockCache) CacheFactory.getDerivativeCache();
        instance.run();
        assertTrue(cache.isOnCacheWorkerCalled());
    }

}
