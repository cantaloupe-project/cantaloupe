package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheFactory {

    private static Logger logger = LoggerFactory.getLogger(CacheFactory.class);

    public static Cache getCache() {
        String cacheName = Application.getConfiguration().getString("cache");
        if (cacheName != null && cacheName.length() > 0) {
            try {
                String className = CacheFactory.class.getPackage().getName() +
                        "." + cacheName;
                Class class_ = Class.forName(className);
                return (Cache) class_.newInstance();
            } catch (Exception e) {
                logger.error("Error in cache configuration: {}", e.getMessage());
            }
        }
        return null;
    }

}
