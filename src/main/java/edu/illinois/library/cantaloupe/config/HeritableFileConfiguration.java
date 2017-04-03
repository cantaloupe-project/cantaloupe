package edu.illinois.library.cantaloupe.config;

import edu.illinois.library.cantaloupe.ThreadPool;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration that allows file-based inheritance.
 */
abstract class HeritableFileConfiguration extends FileConfiguration {

    private Map<File, FileConfigurationWatcher> watchers = new HashMap<>();

    abstract Collection<File> getFiles();

    /**
     * Starts watching the configuration files for changes.
     */
    @Override
    public synchronized void startWatching() {
        for (File file : getFiles()) {
            FileConfigurationWatcher watcher = new FileConfigurationWatcher(file);
            watchers.put(file, watcher);
            ThreadPool.getInstance().submit(watcher);
        }
    }

    /**
     * Stops watching the configuration files for changes.
     */
    @Override
    public synchronized void stopWatching() {
        for (FileConfigurationWatcher watcher : watchers.values()) {
            watcher.stop();
        }
    }

}
