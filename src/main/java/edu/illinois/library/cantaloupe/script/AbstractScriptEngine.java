package edu.illinois.library.cantaloupe.script;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

abstract class AbstractScriptEngine {

    private static ScriptWatcher scriptWatcher = new ScriptWatcher();
    private static ScheduledExecutorService watcherExecutorService;
    private static Future<?> watcherFuture;

    /**
     * Starts watching the configuration file for changes.
     */
    public synchronized void startWatching() {
        watcherExecutorService =
                Executors.newSingleThreadScheduledExecutor();
        watcherFuture = watcherExecutorService.submit(scriptWatcher);
    }

    /**
     * Stops watching the configuration file for changes.
     */
    public synchronized void stopWatching() {
        scriptWatcher.stop();
        if (watcherFuture != null) {
            watcherFuture.cancel(true);
        }
        if (watcherExecutorService != null) {
            watcherExecutorService.shutdown();
        }
    }

}
