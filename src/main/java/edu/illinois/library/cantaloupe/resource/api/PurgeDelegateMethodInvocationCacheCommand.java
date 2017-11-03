package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.script.InvocationCache;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;

import java.util.concurrent.Callable;

final class PurgeDelegateMethodInvocationCacheCommand<T> extends Command
        implements Callable<T> {

    @Override
    public T call() throws Exception {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        if (engine != null) {
            final InvocationCache cache = engine.getInvocationCache();
            if (cache != null) {
                cache.purge();
            }
        }
        return null;
    }

    @Override
    String getVerb() {
        return "PurgeDelegateMethodInvocationCache";
    }

}
