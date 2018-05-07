package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class CacheWorkerRunner {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CacheWorkerRunner.class);

    private static CacheWorkerRunner instance;

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> future;

    /**
     * For testing only!
     */
    static synchronized void clearInstance() {
        instance = null;
    }

    /**
     * @return Singleton instance.
     */
    public static synchronized CacheWorkerRunner getInstance() {
        if (instance == null) {
            instance = new CacheWorkerRunner();
        }
        return instance;
    }

    public synchronized void start() {
        final Configuration config = Configuration.getInstance();
        final int delay = 5;
        final int interval = config.getInt(Key.CACHE_WORKER_INTERVAL, -1);

        LOGGER.info("Starting the cache worker with {} second delay, {} second interval",
                delay, interval);

        executorService = Executors.newSingleThreadScheduledExecutor();
        future = executorService.scheduleWithFixedDelay(
                new CacheWorker(interval),
                delay,
                interval,
                TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        LOGGER.info("Stopping the cache worker...");

        if (future != null) {
            future.cancel(true);
            future = null;
        }
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
    }

    private CacheWorkerRunner() {}

}
