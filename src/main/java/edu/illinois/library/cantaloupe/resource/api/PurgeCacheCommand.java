package edu.illinois.library.cantaloupe.resource.api;

import java.util.concurrent.Callable;

import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.config.Configuration;

final class PurgeCacheCommand<T> extends Command implements Callable<T> {

    @Override
    public T call() throws Exception {
        new CacheFacade(Configuration.getInstance()).purge();
        return null;
    }

    @Override
    String getVerb() {
        return "PurgeCache";
    }

}
