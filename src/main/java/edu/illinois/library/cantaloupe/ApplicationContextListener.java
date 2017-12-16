package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.cache.CacheWorkerRunner;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.logging.LoggerUtil;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.awt.GraphicsEnvironment;
import java.io.FileNotFoundException;
import java.io.IOException;

import static edu.illinois.library.cantaloupe.StandaloneEntry.CLEAN_CACHE_VM_ARGUMENT;
import static edu.illinois.library.cantaloupe.StandaloneEntry.LIST_FONTS_VM_ARGUMENT;
import static edu.illinois.library.cantaloupe.StandaloneEntry.PURGE_CACHE_VM_ARGUMENT;
import static edu.illinois.library.cantaloupe.StandaloneEntry.PURGE_EXPIRED_FROM_CACHE_VM_ARGUMENT;
import static edu.illinois.library.cantaloupe.StandaloneEntry.exitUnlessTesting;

/**
 * <p>Performs application initialization that cannot be performed
 * in {@link StandaloneEntry} as that class is not available in a Servlet
 * container context.</p>
 */
public class ApplicationContextListener implements ServletContextListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ApplicationContextListener.class);

    static {
        // Suppress a Dock icon in OS X
        System.setProperty("java.awt.headless", "true");

        // Tell Restlet to use SLF4J instead of java.util.logging. This needs
        // to be performed before Restlet has been initialized.
        System.setProperty("org.restlet.engine.loggerFacadeClass",
                "org.restlet.ext.slf4j.Slf4jLoggerFacade");
    }

    private void handleVmArguments() {
        final CacheFacade cacheFacade = new CacheFacade();
        final boolean isAnyCacheAvailable =
                cacheFacade.isSourceCacheAvailable() ||
                cacheFacade.isDerivativeCacheAvailable();

        try {
            if (System.getProperty(LIST_FONTS_VM_ARGUMENT) != null) {
                GraphicsEnvironment ge =
                        GraphicsEnvironment.getLocalGraphicsEnvironment();
                for (String family : ge.getAvailableFontFamilyNames()) {
                    System.out.println(family);
                }
                exitUnlessTesting(0);
            } else if (System.getProperty(CLEAN_CACHE_VM_ARGUMENT) != null) {
                System.err.println("DEPRECATION NOTICE: this VM argument will "+
                        "be removed in version 4.0. Use the HTTP API instead.");
                if (isAnyCacheAvailable) {
                    System.out.println("Cleaning the source and/or derivative caches...");
                    cacheFacade.cleanUp();
                    System.out.println("Done.");
                } else {
                    System.out.println("Source and derivative caches are " +
                            "disabled. Nothing to do.");
                }
                exitUnlessTesting(0);
            } else if (System.getProperty(PURGE_CACHE_VM_ARGUMENT) != null) {
                // Two variants of this argument are supported:
                // -Dcantaloupe.cache.purge (purge everything)
                // -Dcantaloupe.cache.purge=identifier (purge all content
                // related to the given identifier)
                System.err.println("DEPRECATION NOTICE: this VM argument will "+
                        "be removed in version 4.0. Use the HTTP API instead.");
                final String purgeArg =
                        System.getProperty(PURGE_CACHE_VM_ARGUMENT);
                if (purgeArg.length() > 0) {
                    Identifier identifier = new Identifier(purgeArg);
                    if (isAnyCacheAvailable) {
                        System.out.println("Purging " + identifier +
                                " from the source and/or derivative caches...");
                        cacheFacade.purge(identifier);
                        System.out.println("Done.");
                    } else {
                        System.out.println("Source and derivative caches are " +
                                "disabled. Nothing to do.");
                    }
                } else {
                    if (isAnyCacheAvailable) {
                        System.out.println("Purging the source and/or " +
                                "derivative caches...");
                        cacheFacade.purge();
                        System.out.println("Done.");
                    } else {
                        System.out.println("Source and derivative caches are " +
                                "disabled. Nothing to do.");
                    }
                }
                exitUnlessTesting(0);
            } else if (System.getProperty(PURGE_EXPIRED_FROM_CACHE_VM_ARGUMENT) != null) {
                System.err.println("DEPRECATION NOTICE: this VM argument will "+
                        "be removed in version 4.0. Use the HTTP API instead.");
                if (isAnyCacheAvailable) {
                    System.out.println("Purging expired items from the " +
                            "source and/or derivative caches...");
                    cacheFacade.purgeExpired();
                    System.out.println("Done.");
                } else {
                    System.out.println("Source and derivative caches are " +
                            "disabled. Nothing to do.");
                }
                exitUnlessTesting(0);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            exitUnlessTesting(-1);
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Logback has already initialized itself, which is a problem because
        // logback.xml depends on the application configuration, which at the
        // time, had not been initialized yet. So, reload it.
        LoggerUtil.reloadConfiguration();

        final int mb = 1024 * 1024;
        final Runtime runtime = Runtime.getRuntime();
        LOGGER.info(System.getProperty("java.vendor") + " " +
                System.getProperty("java.vm.name") + " " +
                System.getProperty("java.version") + " / " +
                System.getProperty("java.vm.info"));
        LOGGER.info("Java home: {}", System.getProperty("java.home"));
        LOGGER.info("{} available processor cores",
                runtime.availableProcessors());
        LOGGER.info("Heap total: {}MB; max: {}MB",
                runtime.totalMemory() / mb,
                runtime.maxMemory() / mb);
        LOGGER.info("Effective temp directory: {}", Application.getTempPath());
        LOGGER.info("\uD83C\uDF48 Starting Cantaloupe {}",
                Application.getVersion());

        handleVmArguments();

        Configuration.getInstance().startWatching();
        CacheWorkerRunner.start();
        try {
            ScriptEngineFactory.getScriptEngine().startWatching();
        } catch (DelegateScriptDisabledException e) {
            LOGGER.debug(e.getMessage());
        } catch (FileNotFoundException e) {
            LOGGER.error("contextInitialized(): file not found: {}",
                    e.getMessage());
        } catch (Exception e) {
            LOGGER.error("contextInitialized(): {}", e.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("Shutting down...");
        CacheWorkerRunner.stop();
        Configuration.getInstance().stopWatching();
        ThreadPool.getInstance().shutdown();
        try {
            ScriptEngineFactory.getScriptEngine().stopWatching();
        } catch (DelegateScriptDisabledException e) {
            LOGGER.debug("contextDestroyed(): {}", e.getMessage());
        } catch (FileNotFoundException e) {
            LOGGER.error("contextDestroyed(): file not found: {}", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("contextDestroyed(): {}", e.getMessage());
        }
    }

}
