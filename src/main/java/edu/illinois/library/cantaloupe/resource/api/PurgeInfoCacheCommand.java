package edu.illinois.library.cantaloupe.resource.api;

import java.util.concurrent.Callable;

import edu.illinois.library.cantaloupe.cache.InfoService;
import edu.illinois.library.cantaloupe.config.Configuration;

final class PurgeInfoCacheCommand<T> extends Command implements Callable<T> {

    @Override
    public T call() throws Exception {
        InfoService.getInstance(Configuration.getInstance()).purgeObjectCache();
        return null;
    }

    @Override
    String getVerb() {
        return "PurgeInfoCache";
    }

}
