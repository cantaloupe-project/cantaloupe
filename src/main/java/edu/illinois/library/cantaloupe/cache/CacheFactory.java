package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

    /**
     * @return Set of instances of all available derivative caches.
     */
    public static Set<DerivativeCache> getAllDerivativeCaches() {
        return new HashSet<>(Arrays.asList(
                new AmazonS3Cache(),
                new AzureStorageCache(),
                new FilesystemCache(),
                new HeapCache(),
                new JdbcCache(),
                new RedisCache()));
    }

    /**
     * @return Set of single instances of all available source caches.
     */
    public static Set<SourceCache> getAllSourceCaches() {
        return new HashSet<>(
                Collections.singletonList(new FilesystemCache()));
    }

    /**
     * <p>Provides access to the shared {@link DerivativeCache} instance.</p>
     *
     * <p>This method respects live changes in application configuration.</p>
     *
     * @return The shared DerivativeCache instance, or {@literal null} if a
     *         derivative cache is not available.
     */
    public static DerivativeCache getDerivativeCache() {
        // The implementation is explained in "Fixing Double-Checked Locking
        // using Volatile":
        // http://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html
        DerivativeCache cache = null;
        final Configuration config = Configuration.getInstance();
        if (config.getBoolean(Key.DERIVATIVE_CACHE_ENABLED, false)) {
            final String unqualifiedName = config.getString(Key.DERIVATIVE_CACHE);
            if (unqualifiedName != null && unqualifiedName.length() > 0) {
                final String qualifiedName =
                        CacheFactory.class.getPackage().getName() + "." +
                                unqualifiedName;
                cache = derivativeCache;
                if (cache == null ||
                        !cache.getClass().getName().equals(qualifiedName)) {
                    synchronized (CacheFactory.class) {
                        if (cache == null ||
                                !cache.getClass().getName().equals(qualifiedName)) {
                            LOGGER.debug("getDerivativeCache(): " +
                                    "implementation changed; creating a new " +
                                    "instance");
                            try {
                                Class<?> implClass = Class.forName(qualifiedName);
                                cache = (DerivativeCache) implClass.newInstance();
                                setDerivativeCache(cache);
                            } catch (ClassNotFoundException e) {
                                cache = null;
                                LOGGER.error("Class not found: {}", e.getMessage());
                            } catch (IllegalAccessException | InstantiationException e) {
                                cache = null;
                                LOGGER.error(e.getMessage());
                            }
                        }
                    }
                }
            }
        }
        return cache;
    }

    /**
     * <p>Provides access to the shared {@link SourceCache} instance.</p>
     *
     * <p>This method respects live changes in application configuration.</p>
     *
     * @return The shared SourceCache instance, or {@literal null} if a
     *         source cache is not available.
     */
    public static SourceCache getSourceCache() {
        // The implementation is explained in "Fixing Double-Checked Locking
        // using Volatile":
        // http://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html
        SourceCache cache = null;
        final Configuration config = Configuration.getInstance();
        if (config.getBoolean(Key.SOURCE_CACHE_ENABLED, false)) {
            final String unqualifiedName = config.getString(Key.SOURCE_CACHE);
            if (unqualifiedName != null && unqualifiedName.length() > 0) {
                final String qualifiedName =
                        CacheFactory.class.getPackage().getName() + "." +
                                unqualifiedName;
                cache = sourceCache;
                if (cache == null || !cache.getClass().getName().equals(qualifiedName)) {
                    synchronized (CacheFactory.class) {
                        if (cache == null ||
                                !cache.getClass().getName().equals(qualifiedName)) {
                            LOGGER.debug("getSourceCache(): implementation " +
                                    "changed; creating a new instance");
                            try {
                                Class<?> implClass = Class.forName(qualifiedName);
                                cache = (SourceCache) implClass.newInstance();
                                setSourceCache(cache);
                            } catch (ClassNotFoundException e) {
                                cache = null;
                                LOGGER.error("Class not found: {}", e.getMessage());
                            } catch (IllegalAccessException | InstantiationException e) {
                                cache = null;
                                LOGGER.error(e.getMessage());
                            }
                        }
                    }
                }
            }
        }
        return cache;
    }

    public static synchronized void shutdownCaches() {
        LOGGER.debug("Shutting down caches");

        if (derivativeCache != null) {
            derivativeCache.shutdown();
        }
        if (sourceCache != null) {
            sourceCache.shutdown();
        }
    }

    /**
     * Shuts down any existing derivative cache, then sets the current
     * derivative cache to the given instance and initializes it.
     *
     * @param cache Derivative cache to use.
     */
    private static synchronized void setDerivativeCache(DerivativeCache cache) {
        if (derivativeCache != null) {
            LOGGER.debug("setDerivativeCache(): shutting down the current instance");
            derivativeCache.shutdown();
        }

        derivativeCache = cache;

        LOGGER.debug("setDerivativeCache(): initializing the new instance");
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
            LOGGER.debug("setSourceCache(): shutting down the current instance");
            sourceCache.shutdown();
        }

        sourceCache = cache;

        LOGGER.debug("setSourceCache(): initializing the new instance");
        sourceCache.initialize();
    }

    private CacheFactory() {}

}
