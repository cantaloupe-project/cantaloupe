package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class CacheWorkerRunner {

    private static final Logger logger = LoggerFactory.getLogger(
            CacheWorkerRunner.class);

    private static ScheduledExecutorService executorService;
    private static ScheduledFuture<?> future;

    public static synchronized void start() {
        final Configuration config = ConfigurationFactory.getInstance();
        if (config.getBoolean(CacheWorker.ENABLED_CONFIG_KEY, false)) {
            executorService = Executors.newSingleThreadScheduledExecutor();
            future = executorService.scheduleAtFixedRate(
                    new CacheWorker(), 5,
                    config.getInt(CacheWorker.INTERVAL_CONFIG_KEY, -1),
                    TimeUnit.SECONDS);
        }
    }

    public static synchronized void stop() {
        logger.info("Stopping the cache worker...");
        future.cancel(true);
        executorService.shutdown();
    }

}
