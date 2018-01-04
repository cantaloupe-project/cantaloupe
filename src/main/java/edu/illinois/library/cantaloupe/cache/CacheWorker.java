package edu.illinois.library.cantaloupe.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Purges invalid items from the cache.
 */
class CacheWorker implements Runnable {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(CacheWorker.class);

    /**
     * Runs one sweep of the worker.
     */
    @Override
    public void run() {
        LOGGER.info("Working...");
        CacheFacade cacheFacade = new CacheFacade();
        try {
            cacheFacade.purgeExpired();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
        try {
            cacheFacade.cleanUp();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
        LOGGER.info("Done working.");
    }

}
