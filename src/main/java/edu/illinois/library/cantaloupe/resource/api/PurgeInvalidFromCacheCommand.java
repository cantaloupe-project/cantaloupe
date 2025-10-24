package edu.illinois.library.cantaloupe.resource.api;

import java.util.concurrent.Callable;

import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.config.Configuration;

final class PurgeInvalidFromCacheCommand<T> extends Command
        implements Callable<T> {

    @Override
    public T call() throws Exception {
        new CacheFacade(Configuration.getInstance()).purgeInvalid();
        return null;
    }

    @Override
    String getVerb() {
        return "PurgeInvalidFromCache";
    }

}
