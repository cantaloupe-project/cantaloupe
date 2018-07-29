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
            if (config instanceof ConfigurationProvider) {
                // If the ConfigurationProvider wraps any FileConfigurations or
                // AbstractHeritableFileConfigurations, check whether any of
                // their files have changed.
                ((ConfigurationProvider) config).getWrappedConfigurations()
                        .stream()
                        .filter(c -> c instanceof FileConfiguration)
                        .forEach(c -> {
                            if (c instanceof AbstractHeritableFileConfiguration) {
                                if (((AbstractHeritableFileConfiguration) c).getFiles().contains(path.toFile())) {
                                    reload(c);
                                }
                            } else if (path.toFile().equals(((FileConfiguration) c).getFile())) {
                                reload(c);
                            }
                        });
            } else {
                System.err.println("Configuration is an unexpected type. " +
                        "This is probably a bug.");
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