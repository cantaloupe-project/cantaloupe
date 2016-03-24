package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Configuration;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class CacheWorkerTest {

    private CacheWorker worker;

    @Before
    public void setUp() {
        Configuration config = Configuration.getInstance();
        config.clear();
        config.setProperty(CacheWorker.ENABLED_CONFIG_KEY, true);
        config.setProperty(CacheWorker.INTERVAL_CONFIG_KEY, 1);

        worker = new CacheWorker();
    }

    // TODO: write some tests

}
