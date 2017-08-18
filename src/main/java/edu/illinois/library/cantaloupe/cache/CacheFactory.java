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
 * Used to obtain an instance of the current {@link Cache} according to the
 * application configuration.
 */
public abstract class CacheFactory {

    private static Logger logger = LoggerFactory.getLogger(CacheFactory.class);

    /** Lazy-initialized by {@link #getDerivativeCache()}. */
    private static volatile DerivativeCache derivativeCache;

    private static Thread derivativeCacheShutdownHook;

    /** Lazy-initialized by {@link #getSourceCache()}. */
    private static volatile SourceCache sourceCache;

    private static Thread sourceCacheShutdownHook;

    /**
     * @return Set of single instances of all available derivative caches.
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
     * @return The shared DerivativeCache instance, or <code>null</code> if a
     *         derivative cache is not available.
     */
    public static DerivativeCache getDerivativeCache() {
        // The implementation is explained in "Fixing Double-Checked Locking
        // using Volatile":
        // http://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html
        DerivativeCache cache = null;
        final Configuration config = Configuration.getInstance();
        if (config.getBoolean(Key.DERIVATIVE_CACHE_ENABLED, false)) {
            final String unqualifiedName =
                    config.getString(Key.DERIVATIVE_CACHE);
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
                            logger.debug("getDerivativeCache(): " +
                                    "implementation changed; creating a new " +
                                    "instance");
                            try {
                                Class<?> implClass = Class.forName(qualifiedName);
                                cache = (DerivativeCache) implClass.newInstance();
                                setDerivativeCache(cache);
                            } catch (ClassNotFoundException e) {
                                cache = null;
                                logger.error("Class not found: {}", e.getMessage());
                            } catch (IllegalAccessException | InstantiationException e) {
                                cache = null;
                                logger.error(e.getMessage());
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
     * @return The shared SourceCache instance, or <code>null</code> if a
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
                if (cache == null ||
                        !cache.getClass().getName().equals(qualifiedName)) {
                    synchronized (CacheFactory.class) {
                        if (cache == null ||
                                !cache.getClass().getName().equals(qualifiedName)) {
                            logger.debug("getSourceCache(): implementation " +
                                    "changed; creating a new instance");
                            try {
                                Class<?> implClass = Class.forName(qualifiedName);
                                cache = (SourceCache) implClass.newInstance();
                                setSourceCache(cache);
                            } catch (ClassNotFoundException e) {
                                cache = null;
                                logger.error("Class not found: {}", e.getMessage());
                            } catch (IllegalAccessException | InstantiationException e) {
                                cache = null;
                                logger.error(e.getMessage());
                            }
                        }
                    }
                }
            }
        }
        return cache;
    }

    /**
     * Shuts down any existing derivative cache and removes its shutdown hook,
     * then sets the current derivative cache to the given instance and adds
     * a shutdown hook for it; then initializes it.
     *
     * @param cache Derivative cache to use.
     */
    private static synchronized void setDerivativeCache(DerivativeCache cache) {
        final Runtime runtime = Runtime.getRuntime();
        if (derivativeCacheShutdownHook != null) {
            runtime.removeShutdownHook(derivativeCacheShutdownHook);
        }
        if (derivativeCache != null) {
            logger.debug("setDerivativeCache(): calling Cache.shutdown()");
            derivativeCache.shutdown();
        }

        derivativeCache = cache;
        derivativeCacheShutdownHook =
                new Thread(() -> derivativeCache.shutdown());
        runtime.addShutdownHook(derivativeCacheShutdownHook);

        logger.debug("setDerivativeCache(): calling Cache.initialize()");
        derivativeCache.initialize();
    }

    /**
     * Shuts down any existing source cache and removes its shutdown hook,
     * then sets the current source cache to the given instance and adds
     * a shutdown hook for it; then initializes it.
     *
     * @param cache Source cache to use.
     */
    private static synchronized void setSourceCache(SourceCache cache) {
        final Runtime runtime = Runtime.getRuntime();
        if (sourceCacheShutdownHook != null) {
            runtime.removeShutdownHook(sourceCacheShutdownHook);
        }
        if (sourceCache != null) {
            logger.debug("setSourceCache(): calling Cache.shutdown()");
            sourceCache.shutdown();
        }

        sourceCache = cache;
        sourceCacheShutdownHook = new Thread(sourceCache::shutdown);
        runtime.addShutdownHook(sourceCacheShutdownHook);

        logger.debug("setSourceCache(): calling Cache.initialize()");
        sourceCache.initialize();
    }

}
