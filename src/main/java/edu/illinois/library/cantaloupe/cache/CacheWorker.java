package edu.illinois.library.cantaloupe.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purges expired items from the cache.
 */
public class CacheWorker implements Runnable {

    private static final Logger logger = LoggerFactory.
            getLogger(CacheWorker.class);

    public static final String ENABLED_CONFIG_KEY =
            "cache.server.worker.enabled";
    public static final String INTERVAL_CONFIG_KEY =
            "cache.server.worker.interval";

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
