package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.Application;

public class CacheFactory {

    public static Cache getCache() throws Exception {
        String cacheName = Application.getConfiguration().getString("cache");
        String className = CacheFactory.class.getPackage().getName() +
                "." + cacheName;
        Class class_ = Class.forName(className);
        return (Cache) class_.newInstance();
    }

}
