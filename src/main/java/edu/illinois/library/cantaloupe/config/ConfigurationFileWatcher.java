package edu.illinois.library.cantaloupe.config;

import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.util.FilesystemWatcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Watches the configuration file (if available) for changes.
 */
public final class ConfigurationFileWatcher {

    /**
     * Listens for changes to a configuration file and reloads it when it has
     * changed.
     */
    private static class FileChangeHandlerRunner implements Runnable {

        private File file;
        private FilesystemWatcher filesystemWatcher;

        FileChangeHandlerRunner(File file) {
            this.file = file;
        }

        @Override
        public void run() {
            try {
                Path path = file.toPath().getParent();
                filesystemWatcher = new FilesystemWatcher(path, new FileChangeHandler());
                filesystemWatcher.start();
            } catch (IOException e) {
                System.err.println("FileChangeHandlerRunner.run(): " + e.getMessage());
            }
        }

        void stop() {
            if (filesystemWatcher != null) {
                filesystemWatcher.stop();
            }
        }

    }

    private static final Set<FileChangeHandlerRunner> CHANGE_HANDLERS =
            ConcurrentHashMap.newKeySet();

    public static void startWatching() {
        final Configuration config = Configuration.getInstance();
        ((ConfigurationProvider) config).getWrappedConfigurations()
                .stream()
                .filter(c -> c instanceof FileConfiguration)
                .map(c -> (FileConfiguration) c)
                .forEach(c -> {
                    FileChangeHandlerRunner runner =
                            new FileChangeHandlerRunner(c.getFile());
                    CHANGE_HANDLERS.add(runner);
                    ThreadPool.getInstance().submit(runner, ThreadPool.Priority.LOW);
                });
    }

    public static void stopWatching() {
        CHANGE_HANDLERS.forEach(FileChangeHandlerRunner::stop);
        CHANGE_HANDLERS.clear();
    }

    private ConfigurationFileWatcher() {}

}
