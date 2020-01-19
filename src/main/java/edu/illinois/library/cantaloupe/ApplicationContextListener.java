package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.CacheWorkerRunner;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFileWatcher;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.logging.LoggerUtil;
import edu.illinois.library.cantaloupe.source.Source;
import edu.illinois.library.cantaloupe.source.SourceFactory;
import edu.illinois.library.cantaloupe.script.DelegateProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngineManager;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.stream.Collectors;

/**
 * <p>Performs application initialization that cannot be performed
 * in {@link StandaloneEntry} as that class is not available in a container
 * context.</p>
 */
public class ApplicationContextListener implements ServletContextListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ApplicationContextListener.class);

    static {
        // This is also set at startup in StandaloneEntry, but doing it here
        // suppresses the icon when running the tests.
        System.setProperty("java.awt.headless", "true");
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Logback has already initialized itself, which is a problem because
        // logback.xml depends on the application configuration, which at the
        // time, had not been initialized yet. So, reload it.
        LoggerUtil.reloadConfiguration();

        logSystemInfo();

        // Start watching configuration files.
        ConfigurationFileWatcher.startWatching();

        // Start the delegate script file watcher, if necessary.
        DelegateProxyService.getInstance().startWatching();

        // Start the cache worker, if necessary.
        final Configuration config = Configuration.getInstance();
        if (config.getBoolean(Key.CACHE_WORKER_ENABLED, false)) {
            CacheWorkerRunner.getInstance().start();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("Shutting down...");

        // Stop the cache worker runner.
        CacheWorkerRunner.getInstance().stop();

        // Stop the configuration file watcher.
        ConfigurationFileWatcher.stopWatching();

        // Stop the delegate script file watcher.
        DelegateProxyService.getInstance().stopWatching();

        // Shut down all caches.
        CacheFactory.shutdownCaches();

        // Shut down all sources.
        SourceFactory.getAllSources().forEach(Source::shutdown);

        // Shut down the application thread pool.
        ThreadPool.getInstance().shutdown();
    }

    private void logSystemInfo() {
        final int mb = 1024 * 1024;
        final Runtime runtime = Runtime.getRuntime();

        LOGGER.info(System.getProperty("java.vendor") + " " +
                System.getProperty("java.vm.name") + " " +
                System.getProperty("java.version") + " / " +
                System.getProperty("java.vm.info"));
        LOGGER.info("{} available processor cores",
                runtime.availableProcessors());
        LOGGER.info("Heap total: {}MB; max: {}MB",
                runtime.totalMemory() / mb,
                runtime.maxMemory() / mb);
        LOGGER.info("Java home: {}",
                System.getProperty("java.home"));
        LOGGER.info("Java library path: {}",
                System.getProperty("java.library.path"));
        LOGGER.info("JSR-223 script engines: {}",
                new ScriptEngineManager().getEngineFactories()
                        .stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(", ")));
        LOGGER.info("Effective temp directory: {}",
                Application.getTempPath());

        if (Application.isUsingSystemTempPath()) {
            LOGGER.warn("The effective temp directory is the system temp " +
                    "directory. Severe errors may result if the application " +
                    "is left running for a long time. Consider setting `" +
                    Key.TEMP_PATHNAME + "` for long-running servers.");
        }

        LOGGER.info("\uD83C\uDF48 Starting Cantaloupe {}",
                Application.getVersion());
    }

}
