package edu.illinois.library.cantaloupe.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purges expired items from the cache.
 */
class CacheWorker implements Runnable {

    private static final Logger logger = LoggerFactory.
            getLogger(CacheWorker.class);

    /**
     * Runs one sweep of the worker.
     */
    @Override
    public void run() {
        // Disabled caches will be null.
        final Cache sourceCache = CacheFactory.getSourceCache();
        final Cache derivativeCache = CacheFactory.getDerivativeCache();
        logger.info("Working...");

        if (sourceCache != null) {
            // Purge expired items from the source cache.
            try {
                sourceCache.purgeExpired();
            } catch (CacheException e) {
                logger.error(e.getMessage());
            }
            // Clean up the source cache.
            try {
                sourceCache.cleanUp();
            } catch (CacheException e) {
                logger.error(e.getMessage());
            }
        }
        if (derivativeCache != null) {
            // Purge expired items from the derivative cache.
            try {
                derivativeCache.purgeExpired();
            } catch (CacheException e) {
                logger.error(e.getMessage());
            }
            // Clean up the derivative cache.
            try {
                derivativeCache.cleanUp();
            } catch (CacheException e) {
                logger.error(e.getMessage());
            }
        } else {
            logger.info("Caching is disabled. Nothing to do.");
        }

        logger.info("Done working.");
    }

}
