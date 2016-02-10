package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.Application;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class CacheWorkerTest {

    private CacheWorker worker;

    @Before
    public void setUp() {
        Configuration config = new BaseConfiguration();
        config.setProperty(CacheWorker.ENABLED_CONFIG_KEY, true);
        config.setProperty(CacheWorker.INTERVAL_CONFIG_KEY, 1);
        Application.setConfiguration(config);

        worker = new CacheWorker();
    }

    // TODO: write some tests

}
