package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.CacheWorkerRunner;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFileWatcher;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.delegate.DelegateProxyService;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.source.Source;
import edu.illinois.library.cantaloupe.source.SourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ServiceRegistry;
import javax.script.ScriptEngineManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>Spring Boot native application listener that performs various application
 * initialization and cleanup tasks.</p>
 *
 * <p>This replaces the servlet-based ApplicationContextListener and
 * IIOProviderContextListener for Spring Boot deployments.</p>
 */
@Component
public class SpringBootApplicationListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SpringBootApplicationListener.class);

    /**
     * Filter for identifying locally registered ImageIO service providers.
     */
    static class LocalFilter implements ServiceRegistry.Filter {
        private final ClassLoader loader;

        public LocalFilter(ClassLoader loader) {
            this.loader = loader;
        }

        public boolean filter(Object provider) {
            return provider.getClass().getClassLoader() == loader;
        }
    }

    static {
        // This is also set at startup in the main method, but doing it here
        // suppresses the icon when running the tests.
        System.setProperty("java.awt.headless", "true");
    }

    /**
     * Handles application startup after Spring Boot is fully initialized.
     * This is equivalent to ServletContextListener.contextInitialized().
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // Initialize ImageIO providers first
        initializeImageIOProviders();

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

    /**
     * Handles application shutdown.
     * This is equivalent to ServletContextListener.contextDestroyed().
     */
    @EventListener(ContextClosedEvent.class)
    public void onApplicationShutdown() {
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
        SourceFactory sourceFactory = new SourceFactory(Configuration.getInstance());
        sourceFactory.getAllSources().forEach(Source::shutdown);

        // Shut down the application thread pool.
        ThreadPool.getInstance().shutdown();

        // Clean up ImageIO providers
        cleanupImageIOProviders();
    }

    /**
     * Initializes ImageIO providers and settings.
     */
    private void initializeImageIOProviders() {
        ImageIO.scanForPlugins();

        // The application will handle caching itself, if so configured. The
        // ImageIO cache would be redundant.
        ImageIO.setUseCache(false);

        logImageIOReaders();
        logImageIOWriters();
    }

    /**
     * Cleans up locally registered ImageIO service providers to prevent class/resource leaks.
     */
    private void cleanupImageIOProviders() {
        // De-register any locally registered IIO plugins.
        // Relies on each web app having its own context class loader.
        final IIORegistry registry = IIORegistry.getDefaultInstance();
        final LocalFilter localFilter =
                new LocalFilter(Thread.currentThread().getContextClassLoader()); // scanForPlugins uses context class loader

        Iterator<Class<?>> categories = registry.getCategories();

        while (categories.hasNext()) {
            Class<?> category = categories.next();
            Iterator<?> providers = registry.getServiceProviders(category, localFilter, false);

            // Copy the providers, as de-registering while iterating over
            // providers will lead to ConcurrentModificationExceptions.
            List<Object> providersCopy = new ArrayList<>();
            while (providers.hasNext()) {
                providersCopy.add(providers.next());
            }

            for (Object provider : providersCopy) {
                registry.deregisterServiceProvider(provider);
                LOGGER.debug("Unregistered locally installed provider class: {}",
                        provider.getClass());
            }
        }
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

        LOGGER.info("\uD83C\uDF48 Starting Cantaloupe {}",
                Application.getVersion());
    }

    private static void logImageIOReaders() {
        final List<Format> imageFormats = Format.all()
                .stream()
                .filter(f -> !f.isVideo())
                .collect(Collectors.toList());
        final List<String> formatLines = new ArrayList<>(imageFormats.size());

        for (Format format : imageFormats) {
            Iterator<javax.imageio.ImageReader> it =
                    ImageIO.getImageReadersByMIMEType(format.getPreferredMediaType().toString());
            List<String> readerClasses = new ArrayList<>();

            while (it.hasNext()) {
                javax.imageio.ImageReader reader = it.next();
                readerClasses.add(reader.getClass().getName());
            }

            formatLines.add("\t" + Format.class.getSimpleName() + "." +
                    format.getName() + ": " +
                    String.join(", ", readerClasses));
        }
        LOGGER.debug("Image I/O readers (not in preference order):\n{}",
                String.join("\n", formatLines));
    }

    private static void logImageIOWriters() {
        final List<Format> imageFormats = Format.all()
                .stream()
                .filter(f -> !f.isVideo())
                .collect(Collectors.toList());
        final List<String> formatLines = new ArrayList<>(imageFormats.size());

        for (Format format : imageFormats) {
            Iterator<javax.imageio.ImageWriter> it =
                    ImageIO.getImageWritersByMIMEType(format.getPreferredMediaType().toString());
            List<String> writerClasses = new ArrayList<>();

            while (it.hasNext()) {
                javax.imageio.ImageWriter writer = it.next();
                writerClasses.add(writer.getClass().getName());
            }

            formatLines.add("\t" + Format.class.getSimpleName() + "." +
                    format.getName() + ": " +
                    String.join(", ", writerClasses));
        }
        LOGGER.debug("Image I/O writers (not in preference order):\n{}",
                String.join("\n", formatLines));
    }
}
