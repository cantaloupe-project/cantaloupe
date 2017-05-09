package edu.illinois.library.cantaloupe.config;

import edu.illinois.library.cantaloupe.logging.LoggerUtil;
import edu.illinois.library.cantaloupe.util.FilesystemWatcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Listens for changes to a configuration file and reloads it when it has
 * changed.
 */
class FileConfigurationWatcher implements Runnable {

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
            // If the configuration is heritable, check whether any of its
            // files changed.
            if (config instanceof HeritableFileConfiguration) {
                HeritableFileConfiguration hfc =
                        (HeritableFileConfiguration) config;
                if (hfc.getFiles().contains(path.toFile())) {
                    // Handle the event only if it happened at least
                    // MIN_INTERVAL_MSEC after the last one.
                    final long now = System.currentTimeMillis();
                    if (now - lastHandled >= MIN_INTERVAL_MSEC) {
                        lastHandled = now;
                        reload(config);
                    } else {
                        System.out.println("FileConfigurationWatcher$CallbackImpl: " +
                                "multiple events < " + MIN_INTERVAL_MSEC + "ms apart");
                    }
                }
            } else if (path.toFile().equals(config.getFile())) {
                reload(config);
            }
        }

        private void reload(Configuration config) {
            try {
                config.reload();
                LoggerUtil.reloadConfiguration();
            } catch (FileNotFoundException e) {
                System.err.println("FileConfigurationWatcher$CallbackImpl: " +
                        "file not found: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("FileConfigurationWatcher$CallbackImpl: " +
                        e.getMessage());
            }
        }

    }

    private File file;
    private FilesystemWatcher filesystemWatcher;

    FileConfigurationWatcher(File file) {
        this.file = file;
    }

    @Override
    public void run() {
        try {
            Path path = file.toPath().getParent();
            filesystemWatcher = new FilesystemWatcher(path, new CallbackImpl());
            filesystemWatcher.processEvents();
        } catch (IOException e) {
            System.err.println("FileConfigurationWatcher.run(): " + e.getMessage());
        }
    }

    public void stop() {
        if (filesystemWatcher != null) {
            filesystemWatcher.stop();
        }
    }
}