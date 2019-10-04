package edu.illinois.library.cantaloupe.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purges invalid items from the cache.
 */
final class CacheWorker implements Runnable {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(CacheWorker.class);

    private int interval;

    /**
     * @param interval Shift interval in seconds.
     */
    CacheWorker(int interval) {
        this.interval = interval;
    }

    /**
     * Runs one sweep of the worker.
     */
    @Override
    public void run() {
        LOGGER.info("Working...");

        DerivativeCache dCache = CacheFactory.getDerivativeCache().orElse(null);
        if (dCache != null) {
            dCache.onCacheWorker();
        }

        SourceCache sCache = CacheFactory.getSourceCache().orElse(null);
        if (sCache != null && sCache != dCache) {
            sCache.onCacheWorker();
        }

        LOGGER.info("Done working. Next shift starts in {} seconds.", interval);
    }

}
