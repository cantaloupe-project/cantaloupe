package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import org.junit.Before;

public class CacheWorkerTest {

    private CacheWorker worker;

    @Before
    public void setUp() {
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        Configuration config = ConfigurationFactory.getInstance();
        config.clear();
        config.setProperty(CacheWorker.ENABLED_CONFIG_KEY, true);
        config.setProperty(CacheWorker.INTERVAL_CONFIG_KEY, 1);

        worker = new CacheWorker();
    }

    // TODO: write some tests

}
