package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.cache.CacheFacade;

import java.util.concurrent.Callable;

final class PurgeCacheCommand<T> extends Command implements Callable<T> {

    @Override
    public T call() throws Exception {
        new CacheFacade().purge();
        return null;
    }

    @Override
    String getVerb() {
        return "PurgeCache";
    }

}
