package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheFactory {

    private static Logger logger = LoggerFactory.getLogger(CacheFactory.class);

    public static final String CACHE_CONFIG_KEY = "cache.server";

    /**
     * @return Cache, or null if not available.
     */
    public static Cache getCache() {
        try {
            String cacheName = Application.getConfiguration().
                    getString(CACHE_CONFIG_KEY);
            if (cacheName != null && cacheName.length() > 0) {
                String className = CacheFactory.class.getPackage().getName() +
                        "." + cacheName;
                Class class_ = Class.forName(className);
                return (Cache) class_.newInstance();
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return null;
    }

}
