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
            "cache.derivative";
    public static final String SOURCE_CACHE_CONFIG_KEY =
            "cache.source";
    public static final String SOURCE_CACHE_ENABLED_CONFIG_KEY =
            "cache.source.enabled";

    /** Singleton instance */
    private static DerivativeCache derivativeCache;

    /** Singleton instance */
    private static SourceCache sourceCache;

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
     * <p>Provides access to the shared {@link Cache} instance.</p>
     *
     * <p>This method respects live changes in application configuration,
     * mostly for the sake of testing.</p>
     *
     * @return The shared Cache Singleton, or null if a cache is not available.
     */
    public static synchronized DerivativeCache getDerivativeCache() {
        try {
            String cacheName = Configuration.getInstance().
                    getString(DERIVATIVE_CACHE_CONFIG_KEY);
            if (cacheName != null && cacheName.length() > 0) {
                String className = CacheFactory.class.getPackage().getName() +
                        "." + cacheName;
                Class class_ = Class.forName(className);
                if (derivativeCache == null ||
                        !derivativeCache.getClass().getSimpleName().equals(className)) {
                    derivativeCache = (DerivativeCache) class_.newInstance();
                }
            } else {
                derivativeCache = null;
            }
        } catch (ClassNotFoundException e) {
            logger.error("Class not found: {}", e.getMessage());
            derivativeCache = null;
        } catch (IllegalAccessException | InstantiationException e) {
            logger.error(e.getMessage());
            derivativeCache = null;
        }
        return derivativeCache;
    }

    /**
     * <p>Provides access to the shared {@link SourceCache} instance.</p>
     *
     * <p>This method respects live changes in application configuration.</p>
     *
     * @return The shared SourceCache instance, or <code>null</code> if a
     *         source cache is not available.
     */
    public static synchronized SourceCache getSourceCache() {
        final Configuration config = Configuration.getInstance();
        final boolean enabled =
                config.getBoolean(SOURCE_CACHE_ENABLED_CONFIG_KEY, false);
        if (enabled) {
            String cacheName = config.getString(SOURCE_CACHE_CONFIG_KEY);
            if (cacheName != null && cacheName.length() > 0) {
                try {
                    String className = CacheFactory.class.getPackage().getName() +
                            "." + cacheName;
                    if (sourceCache == null ||
                            !sourceCache.getClass().getName().equals(className)) {
                        logger.debug("getSourceCache(): implementation " +
                                "changed; creating a new instance");
                        Class class_ = Class.forName(className);
                        sourceCache = (SourceCache) class_.newInstance();
                    }
                } catch (ClassNotFoundException e) {
                    logger.error("Class not found: {}", e.getMessage());
                    sourceCache = null;
                } catch (IllegalAccessException | InstantiationException e) {
                    logger.error(e.getMessage());
                    sourceCache = null;
                }
            } else {
                sourceCache = null;
            }
        } else {
            sourceCache = null;
        }
        return sourceCache;
    }

}
