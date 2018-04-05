package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.script.DelegateProxy;
import edu.illinois.library.cantaloupe.script.InvocationCache;

import java.util.concurrent.Callable;

final class PurgeDelegateMethodInvocationCacheCommand<T> extends Command
        implements Callable<T> {

    @Override
    public T call() {
        final InvocationCache cache = DelegateProxy.getInvocationCache();;
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
