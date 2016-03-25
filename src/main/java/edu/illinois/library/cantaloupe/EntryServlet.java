package edu.illinois.library.cantaloupe;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheException;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.CacheWorker;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.logging.velocity.Slf4jLogChute;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.restlet.data.Protocol;
import org.restlet.ext.servlet.ServerServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Serves as the entry Servlet in both standalone and Servlet container
 * context.
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
    private static FilesystemWatcher fsWatcher;

    static {
        // Suppress a Dock icon in OS X
        System.setProperty("java.awt.headless", "true");

        initializeLogging();
        handleVmArguments();
        initializeVelocity();
        startConfigWatcher();

        final int mb = 1024 * 1024;
        Runtime runtime = Runtime.getRuntime();
        logger.info(System.getProperty("java.vm.name") + " / " +
                System.getProperty("java.vm.info"));
        logger.info("{} available processor cores",
                runtime.availableProcessors());
        logger.info("Heap total: {}MB; max: {}MB", runtime.totalMemory() / mb,
                runtime.maxMemory() / mb);
        logger.info("\uD83C\uDF48 Starting Cantaloupe {}",
                Application.getVersion());
    }

    private static void initializeLogging() {
        // Restlet normally uses JUL; we want it to use slf4j.
        System.getProperties().put("org.restlet.engine.loggerFacadeClass",
                "org.restlet.ext.slf4j.Slf4jLoggerFacade");

        Configuration appConfig = Configuration.getInstance();
        if (appConfig != null) {
            // At this point, Logback has already initialized itself, which is a
            // problem because logback.xml depends on application configuration
            // options, which have not been loaded yet. So, reset the logger
            // context...
            LoggerContext loggerContext = (LoggerContext)
                    LoggerFactory.getILoggerFactory();
            JoranConfigurator jc = new JoranConfigurator();
            jc.setContext(loggerContext);
            loggerContext.reset();
            // Then copy logging-related configuration key/values into logger
            // context properties...
            Iterator it = appConfig.getKeys();
            while (it.hasNext()) {
                String key = (String) it.next();
                if (key.startsWith("log.")) {
                    loggerContext.putProperty(key, appConfig.getString(key));
                }
            }
            // Finally, reload the Logback configuration.
            try {
                InputStream stream = Application.class.getClassLoader().
                        getResourceAsStream("logback.xml");
                jc.doConfigure(stream);
            } catch (JoranException je) {
                je.printStackTrace();
            }
            StatusPrinter.printIfErrorsOccured(loggerContext);
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

        // If the cache worker is enabled, run it in a low-priority
        // background thread.
        if (Configuration.getInstance().getBoolean(CacheWorker.ENABLED_CONFIG_KEY, false)) {
            startCacheWorker();
        }
    }

    private static void initializeVelocity() {
        Velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        Velocity.setProperty("classpath.resource.loader.class",
                ClasspathResourceLoader.class.getName());
        Velocity.setProperty("class.resource.loader.cache", true);
        Velocity.setProperty("runtime.log.logsystem.class",
                Slf4jLogChute.class.getCanonicalName());
        Velocity.init();
    }

    private static void startCacheWorker() {
        cacheWorkerExecutorService =
                Executors.newSingleThreadScheduledExecutor();
        cacheWorkerFuture = cacheWorkerExecutorService.scheduleAtFixedRate(
                new CacheWorker(), 5,
                Configuration.getInstance().getInt(CacheWorker.INTERVAL_CONFIG_KEY, -1),
                TimeUnit.SECONDS);
    }

    private static void startConfigWatcher() {
        if (Configuration.getInstance().getConfigurationFile() != null) {
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
                            if (path.toFile().equals(Configuration.getInstance().getConfigurationFile())) {
                                Configuration.getInstance().reloadConfigurationFile();
                            }
                        }
                    };
                    try {
                        Path path = Configuration.getInstance().
                                getConfigurationFile().toPath().getParent();
                        fsWatcher = new FilesystemWatcher(path, callback);
                        fsWatcher.processEvents();
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
        if (fsWatcher != null) {
            fsWatcher.stop();
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
