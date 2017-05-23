package edu.illinois.library.cantaloupe.config;

import edu.illinois.library.cantaloupe.ThreadPool;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Future;

abstract class FileConfiguration extends AbstractConfiguration {

    private FileConfigurationWatcher watcher;
    private Future<?> watcherFuture;

    public File getFile() {
        String configFilePath = System.
                getProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT);
        if (configFilePath != null) {
            try {
                // expand paths that start with "~"
                configFilePath = configFilePath.replaceFirst("^~",
                        System.getProperty("user.home"));
                return new File(configFilePath).getCanonicalFile();
            } catch (IOException e) {
                // The logger may not have been initialized yet, as it depends
                // on a working configuration. (Also, we don't want to
                // introduce a dependency on the logger, because of the way
                // the application is packaged.)
                System.out.println(e.getMessage());
            }
        }
        return null;
    }

    /**
     * Starts watching the configuration file for changes.
     */
    public synchronized void startWatching() {
        watcher = new FileConfigurationWatcher(getFile());
        watcherFuture = ThreadPool.getInstance().submit(watcher);
    }

    /**
     * Stops watching the configuration file for changes.
     */
    public synchronized void stopWatching() {
        if (watcher != null) {
            watcher.stop();
        }
        if (watcherFuture != null) {
            watcherFuture.cancel(true);
        }
    }

}
