package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CacheFactory {

    private static Logger logger = LoggerFactory.getLogger(CacheFactory.class);

    public static final String CACHE_CONFIG_KEY = "cache.server";

    /** Singleton instance */
    private static Cache instance;

    /**
     * Provides access to the shared Cache instance. This methods respects
     * live changes in application configuration, mostly for the sake of
     * testing.
     *
     * @return The shared Cache Singleton, or null if a cache is not available.
     */
    public static synchronized Cache getInstance() {
        try {
            String cacheName = Application.getConfiguration().
                    getString(CACHE_CONFIG_KEY);
            if (cacheName != null && cacheName.length() > 0) {
                String className = CacheFactory.class.getPackage().getName() +
                        "." + cacheName;
                Class class_ = Class.forName(className);
                if (instance == null ||
                        !instance.getClass().getSimpleName().equals(className)) {
                    instance = (Cache) class_.newInstance();
                }
            } else {
                instance = null;
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            instance = null;
        }
        return instance;
    }

}
