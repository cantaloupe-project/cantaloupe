package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.Set;

/**
 * Used to obtain {@link Cache} instances according to the application
 * configuration.
 */
public final class CacheFactory {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CacheFactory.class);

    private static final Set<DerivativeCache> ALL_DERIVATIVE_CACHES = Set.of(
            new AzureStorageCache(),
            new FilesystemCache(),
            new HeapCache(),
            new JdbcCache(),
            new RedisCache(),
            new S3Cache());

    private static final Set<SourceCache> ALL_SOURCE_CACHES = Set.of(
            new FilesystemCache());

    /**
     * Initialized by {@link #getDerivativeCache()}.
     */
    private static volatile DerivativeCache derivativeCache;

    /**
     * Initialized by {@link #getSourceCache()}.
     */
    private static volatile SourceCache sourceCache;

    /**
     * @return Set of instances of all available derivative caches.
     */
    public static Set<DerivativeCache> getAllDerivativeCaches() {
        return ALL_DERIVATIVE_CACHES;
    }

    /**
     * @return Set of single instances of all available source caches.
     */
    public static Set<SourceCache> getAllSourceCaches() {
        return ALL_SOURCE_CACHES;
    }

    /**
     * <p>Provides access to the shared {@link DerivativeCache} instance.</p>
     *
     * <p>This method respects live changes in application configuration.</p>
     *
     * @return The shared instance, or {@code null} if a derivative cache
     *         is not available.
     */
    public static Optional<DerivativeCache> getDerivativeCache() {
        DerivativeCache cache = null;

        if (isDerivativeCacheEnabled()) {
            final Configuration config = Configuration.getInstance();
            final String unqualifiedName = config.getString(Key.DERIVATIVE_CACHE, "");

            if (!unqualifiedName.isEmpty()) {
                final String qualifiedName = getQualifiedName(unqualifiedName);
                cache = derivativeCache;
                if (cache == null ||
                        !cache.getClass().getName().equals(qualifiedName)) {
                    synchronized (CacheFactory.class) {
                        if (cache == null ||
                                !cache.getClass().getName().equals(qualifiedName)) {
                            LOGGER.trace("getDerivativeCache(): " +
                                    "implementation changed; creating a new " +
                                    "instance");
                            try {
                                Class<?> implClass = Class.forName(qualifiedName);
                                cache = (DerivativeCache)
                                        implClass.getDeclaredConstructor().newInstance();
                                setDerivativeCache(cache);
                            } catch (ClassNotFoundException e) {
                                cache = null;
                                LOGGER.error("Class not found: {}", e.getMessage());
                            } catch (NoSuchMethodException |
                                    IllegalAccessException |
                                    InstantiationException |
                                    InvocationTargetException e) {
                                cache = null;
                                LOGGER.error(e.getMessage());
                            }
                        }
                    }
                }
            } else {
                LOGGER.warn("Derivative cache is enabled, but {} is not set",
                        Key.DERIVATIVE_CACHE);
                shutdownDerivativeCache();
            }
        }
        return Optional.ofNullable(cache);
    }

    /**
     * <p>Provides access to the shared {@link SourceCache} instance.</p>
     *
     * <p>This method respects live changes in application configuration.</p>
     *
     * @return The shared instance, or {@code null} if the source cache
     *         implementation specified in the configuration is invalid or not
     *         specified.
     */
    public static Optional<SourceCache> getSourceCache() {
        SourceCache cache = null;

        final Configuration config = Configuration.getInstance();
        final String unqualifiedName = config.getString(Key.SOURCE_CACHE, "");

        if (!unqualifiedName.isEmpty()) {
            final String qualifiedName = getQualifiedName(unqualifiedName);
            cache = sourceCache;
            if (cache == null ||
                    !cache.getClass().getName().equals(qualifiedName)) {
                synchronized (CacheFactory.class) {
                    if (cache == null ||
                            !cache.getClass().getName().equals(qualifiedName)) {
                        LOGGER.trace("getSourceCache(): implementation " +
                                "changed; creating a new instance");
                        try {
                            Class<?> implClass = Class.forName(qualifiedName);
                            cache = (SourceCache)
                                    implClass.getDeclaredConstructor().newInstance();
                            setSourceCache(cache);
                        } catch (ClassNotFoundException e) {
                            cache = null;
                            LOGGER.error("Class not found: {}", e.getMessage());
                        } catch (NoSuchMethodException |
                                IllegalAccessException |
                                InstantiationException |
                                InvocationTargetException e) {
                            cache = null;
                            LOGGER.error(e.getMessage());
                        }
                    }
                }
            }
        }
        return Optional.ofNullable(cache);
    }

    private static String getQualifiedName(String unqualifiedName) {
        return unqualifiedName.contains(".") ?
                unqualifiedName :
                CacheFactory.class.getPackage().getName() + "." +
                        unqualifiedName;
    }

    private static boolean isDerivativeCacheEnabled() {
        final Configuration config = Configuration.getInstance();
        return config.getBoolean(Key.DERIVATIVE_CACHE_ENABLED, false);
    }

    /**
     * Shuts down any existing derivative cache, then sets the current
     * derivative cache to the given instance and initializes it.
     *
     * @param cache Derivative cache to use.
     */
    private static synchronized void setDerivativeCache(DerivativeCache cache) {
        if (derivativeCache != null) {
            LOGGER.trace("setDerivativeCache(): shutting down the current instance");
            derivativeCache.shutdown();
        }

        derivativeCache = cache;

        LOGGER.trace("setDerivativeCache(): initializing the new instance");
        derivativeCache.initialize();
    }

    /**
     * Shuts down any existing source cache, then sets the current source cache
     * to the given instance and initializes it.
     *
     * @param cache Source cache to use.
     */
    private static synchronized void setSourceCache(SourceCache cache) {
        if (sourceCache != null) {
            LOGGER.trace("setSourceCache(): shutting down the current instance");
            sourceCache.shutdown();
        }

        sourceCache = cache;

        LOGGER.trace("setSourceCache(): initializing the new instance");
        sourceCache.initialize();
    }

    public static synchronized void shutdownCaches() {
        shutdownDerivativeCache();
        shutdownSourceCache();
    }

    private static synchronized void shutdownDerivativeCache() {
        LOGGER.trace("Shutting down the derivative cache");
        if (derivativeCache != null) {
            derivativeCache.shutdown();
            derivativeCache = null;
        }
    }

    private static synchronized  void shutdownSourceCache() {
        LOGGER.trace("Shutting down the source cache");
        if (sourceCache != null) {
            sourceCache.shutdown();
            sourceCache = null;
        }
    }

    private CacheFactory() {}

}
