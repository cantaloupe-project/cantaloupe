package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ServiceRegistry;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>Takes care of registering and de-registering local ImageIO plugins
 * (service providers) for the servlet context.</p>
 *
 * <p>Registers all available plugins on {@code contextInitialized} event,
 * using {@code ImageIO.scanForPlugins()}, to make sure they are available to
 * the current servlet context. De-registers all plugins which have the {@link
 * Thread#getContextClassLoader() current thread's context class loader} as its
 * class loader on {@code contextDestroyed} event, to avoid class/resource
 * leak.</p>
 *
 * <p>Forked from:
 * <a href="https://github.com/haraldk/TwelveMonkeys/commit/14e12eb2c192ab15dfc043c6f75bb9f27689af48">IIOProviderContextListener.java</a></p>
 *
 * @author Original author: <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author Last modified by Alex Dolski UIUC
 * @see javax.imageio.ImageIO#scanForPlugins()
 */
public final class IIOProviderContextListener implements ServletContextListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(IIOProviderContextListener.class);

    static class LocalFilter implements ServiceRegistry.Filter {
        private final ClassLoader loader;

        public LocalFilter(ClassLoader loader) {
            this.loader = loader;
        }

        public boolean filter(Object provider) {
            return provider.getClass().getClassLoader() == loader;
        }
    }

    public void contextInitialized(final ServletContextEvent event) {
        // ImageIO will automatically scan for plugins once, the first time
        // it's used. If our app is initialized after another ImageIO-using
        // app in the same JVM, any additional plugins bundled within our
        // app won't be picked up unless we scan again.
        ImageIO.scanForPlugins();

        // The application will handle caching itself, if so configured. The
        // ImageIO cache would be redundant.
        ImageIO.setUseCache(false);

        logImageIOReaders();
    }

    public void contextDestroyed(final ServletContextEvent event) {
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
                event.getServletContext().log(
                        String.format("Unregistered locally installed provider class: %s",
                                provider.getClass()));
            }
        }
    }

    private static void logImageIOReaders() {
        final List<Format> imageFormats = Arrays.stream(Format.values()).
                filter(f -> Format.Type.IMAGE.equals(f.getType())).
                collect(Collectors.toList());

        for (Format format : imageFormats) {
            Iterator<javax.imageio.ImageReader> it =
                    ImageIO.getImageReadersByMIMEType(format.getPreferredMediaType().toString());
            List<String> readerClasses = new ArrayList<>();

            while (it.hasNext()) {
                javax.imageio.ImageReader reader = it.next();
                readerClasses.add(reader.getClass().getName());
            }

            LOGGER.info("ImageIO readers for {}.{}: {}",
                    Format.class.getSimpleName(),
                    format.getName(),
                    readerClasses.stream().collect(Collectors.joining(", ")));
        }
    }

}
