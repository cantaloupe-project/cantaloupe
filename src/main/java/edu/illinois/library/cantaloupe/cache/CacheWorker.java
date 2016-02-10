package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.Application;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

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

    private static AtomicBoolean running = new AtomicBoolean(false);

    private int startupDelay = 0;

    /**
     * Runs a new CacheWorker instance in a low-priority background thread.
     *
     * @param delay Number of seconds to wait before running.
     */
    public static synchronized void runInBackground(int delay) {
        if (!running.get()) {
            CacheWorker worker = new CacheWorker();
            worker.setStartupDelay(delay);
            Thread workerThread = new Thread(worker);
            workerThread.setName(worker.getClass().getSimpleName());
            workerThread.setPriority(Thread.MIN_PRIORITY);
            workerThread.start();
            running.set(true);
        }
    }

    public int getStartupDelay() {
        return startupDelay;
    }

    @Override
    public void run() {
        final Configuration config = Application.getConfiguration();

        if (!config.getBoolean(ENABLED_CONFIG_KEY, false)) {
            logger.info("Cache worker is disabled. To enable it, set {} to " +
                    "true and restart.", ENABLED_CONFIG_KEY);
            return;
        }

        try {
            logger.info("Will start in {} seconds", getStartupDelay());
            Thread.sleep(getStartupDelay() * 1000);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }

        while (true) {
            // If caching is disabled, this will be null.
            final Cache cache = CacheFactory.getInstance();
            try {
                final int interval = config.getInt(INTERVAL_CONFIG_KEY, -1);
                if (cache != null) {
                    logger.info("Working...");
                    try {
                        if (interval < 0) {
                            throw new CacheException("Invalid interval (" +
                                    INTERVAL_CONFIG_KEY + "). Aborting.");
                        }
                        cache.purgeExpired();
                        logger.info("Done working.");
                    } catch (CacheException e) {
                        logger.error(e.getMessage());
                    }
                } else {
                    logger.info("Caching is disabled. Nothing to do.");
                }
                logger.info("Sleeping for {} seconds...", interval);
                Thread.sleep(interval * 1000);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }
    }

    public void setStartupDelay(int delay) {
        startupDelay = delay;
    }

}
