package edu.illinois.library.cantaloupe.resource.api;

import java.util.concurrent.Callable;

import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.config.Configuration;

/**
 * @since 6.0
 */
final class PurgeInfosFromCacheCommand<T> extends Command
        implements Callable<T> {

    @Override
    public T call() throws Exception {
        new CacheFacade(Configuration.getInstance()).purgeInfos();
        return null;
    }

    @Override
    String getVerb() {
        return "PurgeInfosFromCache";
    }

}
