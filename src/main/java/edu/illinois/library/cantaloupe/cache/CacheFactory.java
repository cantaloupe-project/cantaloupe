package edu.illinois.library.cantaloupe.cache;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;

/**
 * Used to obtain {@link Cache} instances according to the application
 * configuration.
 */
public final class CacheFactory {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CacheFactory.class);

    /**
     * Initialized by {@link #getDerivativeCache()}.
     */
    private static volatile DerivativeCache derivativeCache;

    /**
     * Initialized by {@link #getSourceCache()}.
     */
    private static volatile SourceCache sourceCache;

    private Configuration configuration;
    private Set<DerivativeCache> allDerivativeCaches;
    private final Set<SourceCache> allSourceCaches;

    public CacheFactory(Configuration configuration) {
        this.configuration = configuration;
        this.allDerivativeCaches = Set.of(
            new AzureStorageCache(configuration),
            new FilesystemCache(configuration),
            new HeapCache(configuration),
            new JdbcCache(configuration),
            new RedisCache(configuration),
            new S3Cache(configuration));
        this.allSourceCaches = Set.of(
            new FilesystemCache(configuration));
    }

    /**
     * @return Set of instances of all available derivative caches.
     */
    public Set<DerivativeCache> getAllDerivativeCaches() {
        return allDerivativeCaches;
    }

    /**
     * @return Set of single instances of all available source caches.
     */
    public Set<SourceCache> getAllSourceCaches() {
        return allSourceCaches;
    }

    /**
     * <p>Provides access to the shared {@link DerivativeCache} instance.</p>
     *
     * <p>This method respects live changes in application configuration.</p>
     *
     * @return The shared instance, or {@code null} if a derivative cache
     *         is not available.
     */
    public Optional<DerivativeCache> getDerivativeCache() {
        DerivativeCache cache = null;

        if (isDerivativeCacheEnabled()) {
            final String unqualifiedName = configuration.getString(Key.DERIVATIVE_CACHE, "");

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
    public Optional<SourceCache> getSourceCache() {
        SourceCache cache = null;

        final String unqualifiedName = configuration.getString(Key.SOURCE_CACHE, "");

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

    private boolean isDerivativeCacheEnabled() {
        return configuration.getBoolean(Key.DERIVATIVE_CACHE_ENABLED, false);
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
}
