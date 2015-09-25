package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheFactory {

    private static Logger logger = LoggerFactory.getLogger(CacheFactory.class);

    /** Singleton instance */
    private static Cache instance;

    /**
     * @return The shared Cache Singleton, or null if a cache is not available.
     */
    public static Cache getInstance() {
        if (instance == null) {
            try {
                String cacheName = Application.getConfiguration().getString("cache.server");
                if (cacheName != null && cacheName.length() > 0) {
                    String className = CacheFactory.class.getPackage().getName() +
                            "." + cacheName;
                    Class class_ = Class.forName(className);
                    synchronized (CacheFactory.class) {
                        instance = (Cache) class_.newInstance();
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        return instance;
    }

}
