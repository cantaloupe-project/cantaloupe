package edu.illinois.library.cantaloupe.config;

import edu.illinois.library.cantaloupe.logging.LoggerUtil;
import edu.illinois.library.cantaloupe.util.FilesystemWatcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Listens for changes to a configuration file and reloads it when it has
 * changed.
 */
class ConfigurationWatcher implements Runnable {

    private static class CallbackImpl implements FilesystemWatcher.Callback {

        private static final long MIN_INTERVAL_MSEC = 1000;

        private long lastHandled = System.currentTimeMillis();

        @Override
        public void created(Path path) {
            handle(path);
        }

        @Override
        public void deleted(Path path) {}

        @Override
        public void modified(Path path) {
            handle(path);
        }

        private void handle(Path path) {
            final Configuration config = Configuration.getInstance();
            if (path.toFile().equals(config.getFile())) {
                // Handle the event only if it happened at least
                // MIN_INTERVAL_MSEC after the last one.
                final long now = System.currentTimeMillis();
                if (now - lastHandled >= MIN_INTERVAL_MSEC) {
                    lastHandled = now;
                    try {
                        config.reload();
                        LoggerUtil.reloadConfiguration();
                    } catch (FileNotFoundException e) {
                        System.err.println("ConfigurationWatcher$CallbackImpl: " +
                                "file not found: " + e.getMessage());
                    } catch (IOException e) {
                        System.err.println("ConfigurationWatcher$CallbackImpl: " +
                                e.getMessage());
                    }
                } else {
                    System.out.println("ConfigurationWatcher$CallbackImpl: " +
                            "multiple events < " + MIN_INTERVAL_MSEC + "ms apart");
                }
            }
        }

    }

    private FilesystemWatcher filesystemWatcher;

    @Override
    public void run() {
        final Configuration config = Configuration.getInstance();
        try {
            Path path = config.getFile().toPath().getParent();
            filesystemWatcher = new FilesystemWatcher(path, new CallbackImpl());
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