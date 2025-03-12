package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.cache.CacheFacade;

import java.util.concurrent.Callable;

/**
 * @since 6.0
 */
final class PurgeInfosFromCacheCommand<T> extends Command
        implements Callable<T> {

    @Override
    public T call() throws Exception {
        new CacheFacade().purgeInfos();
        return null;
    }

    @Override
    String getVerb() {
        return "PurgeInfosFromCache";
    }

}
