package edu.illinois.library.cantaloupe.config;

import edu.illinois.library.cantaloupe.logging.LoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Listens for changes to the configuration file and reloads it when it has
 * changed.
 */
public class ConfigurationWatcher implements Runnable {

    private static Logger logger =
            LoggerFactory.getLogger(ConfigurationWatcher.class);

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
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        };
        try {
            Path path = config.getFile().toPath().getParent();
            filesystemWatcher = new FilesystemWatcher(path, callback);
            filesystemWatcher.processEvents();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void stop() {
        if (filesystemWatcher != null) {
            filesystemWatcher.stop();
        }
    }
}