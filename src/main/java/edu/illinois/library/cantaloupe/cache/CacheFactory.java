package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Configuration;
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

    public static final String DERIVATIVE_CACHE_CONFIG_KEY =
            "cache.server.derivative";
    public static final String DERIVATIVE_CACHE_ENABLED_CONFIG_KEY =
            "cache.server.derivative.enabled";
    public static final String SOURCE_CACHE_CONFIG_KEY =
            "cache.server.source";
    public static final String SOURCE_CACHE_ENABLED_CONFIG_KEY =
            "cache.server.source.enabled";

    /** Shared instance initialized by {@link #getDerivativeCache()}. */
    private static volatile DerivativeCache derivativeCache;

    /** Shared instance initialized by {@link #getSourceCache()}. */
    private static volatile SourceCache sourceCache;

    /**
     * @return Set of single instances of all available derivative caches.
     */
    public static Set<DerivativeCache> getAllDerivativeCaches() {
        return new HashSet<>(Arrays.asList(
                new AmazonS3Cache(),
                new AzureStorageCache(),
                new FilesystemCache(),
                new JdbcCache()));
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
        if (config.getBoolean(DERIVATIVE_CACHE_ENABLED_CONFIG_KEY, false)) {
            final String unqualifiedName =
                    config.getString(DERIVATIVE_CACHE_CONFIG_KEY);
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
                                Class implClass = Class.forName(qualifiedName);
                                cache = (DerivativeCache) implClass.newInstance();
                                derivativeCache = cache;
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
        if (config.getBoolean(SOURCE_CACHE_ENABLED_CONFIG_KEY, false)) {
            final String unqualifiedName =
                    config.getString(SOURCE_CACHE_CONFIG_KEY);
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
                                Class implClass = Class.forName(qualifiedName);
                                cache = (SourceCache) implClass.newInstance();
                                sourceCache = cache;
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

}
