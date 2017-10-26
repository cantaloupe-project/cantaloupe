package edu.illinois.library.cantaloupe.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purges expired items from the cache.
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
        } catch (CacheException e) {
            LOGGER.error(e.getMessage());
        }
        try {
            cacheFacade.cleanUp();
        } catch (CacheException e) {
            LOGGER.error(e.getMessage());
        }
        LOGGER.info("Done working.");
    }

}
