package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import org.junit.Before;

public class CacheWorkerTest extends BaseTest {

    private CacheWorker worker;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.CACHE_WORKER_ENABLED, true);
        config.setProperty(Key.CACHE_WORKER_INTERVAL, 1);

        worker = new CacheWorker();
    }

    // TODO: write some tests

}
