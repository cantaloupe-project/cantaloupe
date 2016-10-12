package edu.illinois.library.cantaloupe.config;

import edu.illinois.library.cantaloupe.logging.LoggerUtil;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Listens for changes to a configuration file and reloads it when it has
 * changed.
 */
class ConfigurationWatcher implements Runnable {

    private FilesystemWatcher filesystemWatcher;

    @Override
    public void run() {
        final Configuration config = ConfigurationFactory.getInstance();
        FilesystemWatcher.Callback callback = new FilesystemWatcher.Callback() {
            @Override
            public void created(Path path) { handle(path); }

            @Override
            public void deleted(Path path) {}

            @Override
            public void modified(Path path) { handle(path); }

            private void handle(Path path) {
                if (path.toFile().equals(config.getFile())) {
                    try {
                        config.reload();
                        LoggerUtil.reloadConfiguration();
                    } catch (IOException e) {
                        System.err.println("ConfigurationWatcher.run(): " + e.getMessage());
                    }
                }
            }
        };
        try {
            Path path = config.getFile().toPath().getParent();
            filesystemWatcher = new FilesystemWatcher(path, callback);
            filesystemWatcher.processEvents();
        } catch (IOException e) {
            System.err.println("ConfigurationWatcher.run(): " + e.getMessage());
        }
    }

    public void stop() {
        if (filesystemWatcher != null) {
            filesystemWatcher.stop();
        }
    }
}