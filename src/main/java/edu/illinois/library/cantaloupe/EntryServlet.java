package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheException;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.CacheWorker;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.logging.LoggerUtil;
import org.restlet.data.Protocol;
import org.restlet.ext.servlet.ServerServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * <p>Serves as the entry Servlet in both standalone and Servlet container
 * context. Also performs application initialization that cannot be performed
 * in {@link StandaloneEntry} as that class is not available in a Servlet
 * container context.</p>
 *
 * <p>Because this is a Restlet application, there are no other Servlet
 * classes. Instead there are Restlet resource classes residing in
 * {@link edu.illinois.library.cantaloupe.resource}.</p>
 */
public class EntryServlet extends ServerServlet {

    private static Logger logger = LoggerFactory.getLogger(EntryServlet.class);

    public static final String CLEAN_CACHE_VM_ARGUMENT =
            "cantaloupe.cache.clean";
    public static final String PURGE_CACHE_VM_ARGUMENT =
            "cantaloupe.cache.purge";
    public static final String PURGE_EXPIRED_FROM_CACHE_VM_ARGUMENT =
            "cantaloupe.cache.purge_expired";

    private static ScheduledExecutorService cacheWorkerExecutorService;
    private static ScheduledFuture<?> cacheWorkerFuture;
    private static Thread configWatcher;
    private static FilesystemWatcher filesystemWatcher;

    static {
        // Suppress a Dock icon in OS X
        System.setProperty("java.awt.headless", "true");

        // Logback has already initialized itself, which is a problem because
        // logback.xml depends on the application configuration, which has
        // not been initialized yet. So, reload it.
        LoggerUtil.reloadConfiguration();
        handleVmArguments();

        final int mb = 1024 * 1024;
        final Runtime runtime = Runtime.getRuntime();
        logger.info(System.getProperty("java.vm.name") + " / " +
                System.getProperty("java.vm.info"));
        logger.info("{} available processor cores",
                runtime.availableProcessors());
        logger.info("Heap total: {}MB; max: {}MB", runtime.totalMemory() / mb,
                runtime.maxMemory() / mb);
        logger.info("\uD83C\uDF48 Starting Cantaloupe {}",
                Application.getVersion());

        startConfigWatcher();

        // If the cache worker is enabled, run it in a background thread.
        final Configuration config = Configuration.getInstance();
        if (config.getBoolean(CacheWorker.ENABLED_CONFIG_KEY, false)) {
            cacheWorkerExecutorService =
                    Executors.newSingleThreadScheduledExecutor();
            cacheWorkerFuture = cacheWorkerExecutorService.scheduleAtFixedRate(
                    new CacheWorker(), 5,
                    config.getInt(CacheWorker.INTERVAL_CONFIG_KEY, -1),
                    TimeUnit.SECONDS);
        }
    }

    private static void handleVmArguments() {
        try {
            if (System.getProperty(CLEAN_CACHE_VM_ARGUMENT) != null) {
                Cache cache = CacheFactory.getSourceCache();
                if (cache != null) {
                    System.out.println("Cleaning the source cache...");
                    cache.cleanUp();
                } else {
                    System.out.println("Source cache is disabled.");
                }
                cache = CacheFactory.getDerivativeCache();
                if (cache != null) {
                    System.out.println("Cleaning the derivative cache...");
                    cache.cleanUp();
                } else {
                    System.out.println("Derivative cache is disabled.");
                }
                System.out.println("Done.");
                System.exit(0);
            } else if (System.getProperty(PURGE_CACHE_VM_ARGUMENT) != null) {
                Cache cache = CacheFactory.getSourceCache();
                if (cache != null) {
                    System.out.println("Purging the source cache...");
                    cache.purge();
                } else {
                    System.out.println("Source cache is disabled.");
                }
                cache = CacheFactory.getDerivativeCache();
                if (cache != null) {
                    System.out.println("Purging the derivative cache...");
                    cache.purge();
                } else {
                    System.out.println("Derivative cache is disabled.");
                }
                System.out.println("Done.");
                System.exit(0);
            } else if (System.getProperty(PURGE_EXPIRED_FROM_CACHE_VM_ARGUMENT) != null) {
                Cache cache = CacheFactory.getSourceCache();
                if (cache != null) {
                    System.out.println("Purging expired items from the source cache...");
                    cache.purgeExpired();
                } else {
                    System.out.println("Source cache is disabled.");
                }
                cache = CacheFactory.getDerivativeCache();
                if (cache != null) {
                    System.out.println("Purging expired items from the derivative cache...");
                    cache.purgeExpired();
                } else {
                    System.out.println("Derivative cache is disabled.");
                }
                System.out.println("Done.");
                System.exit(0);
            }
        } catch (CacheException e) {
            System.out.println(e.getMessage());
            System.exit(-1);
        }
    }

    private static void startConfigWatcher() {
        final Configuration config = Configuration.getInstance();
        if (config.getConfigurationFile() != null) {
            // Use FilesystemWatcher to listen for changes to the directory
            // containing the configuration file. When the config file is found to
            // have been changed, reload it.
            configWatcher = new Thread() {
                public void run() {
                    FilesystemWatcher.Callback callback = new FilesystemWatcher.Callback() {
                        public void created(Path path) { handle(path); }
                        public void deleted(Path path) {}
                        public void modified(Path path) { handle(path); }
                        private void handle(Path path) {
                            if (path.toFile().equals(
                                    config.getConfigurationFile())) {
                                config.reloadConfigurationFile();
                            }
                        }
                    };
                    try {
                        Path path = config.getConfigurationFile().toPath().getParent();
                        filesystemWatcher = new FilesystemWatcher(path, callback);
                        filesystemWatcher.processEvents();
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            };
            configWatcher.setPriority(Thread.MIN_PRIORITY);
            configWatcher.setName("ConfigWatcher");
            configWatcher.start();
        }
    }

    @Override
    public void init() throws ServletException {
        super.init();
        getComponent().getClients().add(Protocol.CLAP);
    }

    @Override
    public void destroy() {
        super.destroy();
        logger.info("Stopping cache worker...");
        stopCacheWorker();
        logger.info("Stopping config watcher...");
        stopConfigWatcher();
    }

    private void stopCacheWorker() {
        cacheWorkerFuture.cancel(true);
        cacheWorkerExecutorService.shutdown();
    }

    private void stopConfigWatcher() {
        if (filesystemWatcher != null) {
            filesystemWatcher.stop();
        }
        if (configWatcher != null) {
            try {
                configWatcher.join();
            } catch (InterruptedException e) {
                // expected
            }
        }
    }

}
