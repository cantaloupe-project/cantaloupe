package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.async.Task;
import edu.illinois.library.cantaloupe.script.InvocationCache;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;

final class PurgeDelegateMethodInvocationCacheTask extends APITask
        implements Task {

    @Override
    public void run() throws Exception {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        if (engine != null) {
            final InvocationCache cache = engine.getInvocationCache();
            if (cache != null) {
                cache.purge();
            }
        }
    }

}
