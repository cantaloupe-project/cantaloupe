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

    private int interval;

    CacheWorker(int interval) {
        this.interval = interval;
    }

    /**
     * Runs one sweep of the worker.
     */
    @Override
    public void run() {
        LOGGER.info("Working...");
        CacheFacade cacheFacade = new CacheFacade();

        // Purge invalid content.
        try {
            cacheFacade.purgeExpired();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

        // Clean up.
        try {
            cacheFacade.cleanUp();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

        // If the derivative cache is HeapCache, and its persistence is
        // enabled, dump it.
        cacheFacade.getDerivativeCache().ifPresent(cache -> {
            if (cache instanceof HeapCache) {
                HeapCache heapCache = (HeapCache) cache;
                if (heapCache.isPersistenceEnabled()) {
                    try {
                        heapCache.dumpToPersistentStore();
                    } catch (IOException e) {
                        LOGGER.error("Error while persisting {}: {}",
                                HeapCache.class.getSimpleName(),
                                e.getMessage());
                    }
                }
            }
        });

        LOGGER.info("Done working. Next shift starts in {} seconds.", interval);
    }

}
