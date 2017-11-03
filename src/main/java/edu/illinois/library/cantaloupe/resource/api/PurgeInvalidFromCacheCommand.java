package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.cache.CacheFacade;

import java.util.concurrent.Callable;

final class PurgeInvalidFromCacheCommand<T> extends Command
        implements Callable<T> {

    @Override
    public T call() throws Exception {
        new CacheFacade().purgeExpired();
        return null;
    }

    @Override
    String getVerb() {
        return "PurgeInvalidFromCache";
    }

}
