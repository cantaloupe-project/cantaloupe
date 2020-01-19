package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.script.DelegateProxyService;
import edu.illinois.library.cantaloupe.script.InvocationCache;

import java.util.concurrent.Callable;

final class PurgeDelegateMethodInvocationCacheCommand<T> extends Command
        implements Callable<T> {

    @Override
    public T call() {
        final InvocationCache cache = DelegateProxyService.getInvocationCache();;
        if (cache != null) {
            cache.purge();
        }
        return null;
    }

    @Override
    String getVerb() {
        return "PurgeDelegateMethodInvocationCache";
    }

}
