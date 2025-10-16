package edu.illinois.library.cantaloupe.config.spring;

import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.cache.CacheWorkerRunner;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFileWatcher;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.delegate.DelegateProxyService;
import edu.illinois.library.cantaloupe.logging.LoggerUtil;
import edu.illinois.library.cantaloupe.processor.codec.IIOProviderContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import javax.script.ScriptEngineManager;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.stream.Collectors;

/**
 * Spring configuration class that handles application initialization,
 * replacing the functionality of ApplicationContextListener.
 */
@Component
@org.springframework.context.annotation.Configuration
public class ApplicationConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    static {
        // Suppress a Dock icon in macOS.
        System.setProperty("java.awt.headless", "true");
    }

    /**
     * Initialize the application when Spring Boot is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
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
            startCacheWorker();

            // Initialize ImageIO providers
            initializeImageIOProviders();

            LOGGER.info("Cantaloupe application initialization completed");

        } catch (Exception e) {
            LOGGER.error("Failed to initialize application", e);
            throw new RuntimeException("Application initialization failed", e);
        }
    }

    /**
     * Clean up resources when the application shuts down.
     */
    @PreDestroy
    public void destroy() {
        LOGGER.info("Shutting down application...");

        try {
            // Stop configuration file watcher
            ConfigurationFileWatcher.stopWatching();

            // Stop delegate script watcher
            DelegateProxyService.getInstance().stopWatching();

            // Shutdown thread pool
            ThreadPool.getInstance().shutdown();

            LOGGER.info("Application shutdown completed");
        } catch (Exception e) {
            LOGGER.error("Error during application shutdown", e);
        }
    }

    /**
     * Initialize ImageIO providers context listener functionality.
     */
    private void initializeImageIOProviders() {
        try {
            IIOProviderContextListener listener = new IIOProviderContextListener();
            listener.contextInitialized(null);
        } catch (Exception e) {
            LOGGER.warn("Failed to initialize ImageIO providers", e);
        }
    }

    /**
     * Start the cache worker if enabled in configuration.
     */
    private void startCacheWorker() {
        Configuration config = Configuration.getInstance();
        if (config.getBoolean(Key.CACHE_WORKER_ENABLED, false)) {
            CacheWorkerRunner.getInstance().start();
        }
    }

    /**
     * Log system information at startup.
     */
    private void logSystemInfo() {
        final RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();

        LOGGER.info("Starting Cantaloupe {}",
                ApplicationConfiguration.class.getPackage().getImplementationVersion());

        LOGGER.info("Java {} / {}",
                System.getProperty("java.version"),
                System.getProperty("java.vm.name"));

        LOGGER.info("Java home: {}", System.getProperty("java.home"));

        LOGGER.info("OS: {} {} / {}",
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"));

        LOGGER.info("Available processors: {}",
                Runtime.getRuntime().availableProcessors());

        final long maxMemory = Runtime.getRuntime().maxMemory();
        final String maxMemoryStr = (maxMemory == Long.MAX_VALUE) ?
                "no limit" : (maxMemory / 1024 / 1024) + " MB";
        LOGGER.info("JVM memory: initial: {} MB; max: {}",
                Runtime.getRuntime().totalMemory() / 1024 / 1024,
                maxMemoryStr);

        final String classpath = runtimeMxBean.getClassPath();
        if (classpath.length() < 2048) {
            LOGGER.debug("Classpath: {}", classpath);
        } else {
            LOGGER.debug("Classpath: {} (truncated)",
                    classpath.substring(0, 2048));
        }

        LOGGER.debug("JVM arguments: {}",
                runtimeMxBean.getInputArguments()
                        .stream()
                        .collect(Collectors.joining(" ")));

        // Log available script engines
        final ScriptEngineManager manager = new ScriptEngineManager();
        final String engineNames = manager.getEngineFactories()
                .stream()
                .flatMap(f -> f.getNames().stream())
                .collect(Collectors.joining(", "));
        LOGGER.debug("Available script engines: {}", engineNames);
    }
}
