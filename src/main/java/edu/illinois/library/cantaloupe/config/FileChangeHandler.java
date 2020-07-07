package edu.illinois.library.cantaloupe.config;

import edu.illinois.library.cantaloupe.logging.LoggerUtil;
import edu.illinois.library.cantaloupe.util.FilesystemWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.nio.file.Path;

class FileChangeHandler implements FilesystemWatcher.Callback {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(FileChangeHandler.class);

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
        // If the ConfigurationProvider wraps any FileConfigurations, check
        // whether any of their files have changed.
        ((ConfigurationProvider) config).getWrappedConfigurations()
                .stream()
                .filter(c -> c instanceof FileConfiguration)
                .forEach(c -> {
                    if (c instanceof MultipleFileConfiguration) {
                        if (((MultipleFileConfiguration) c).getFiles().contains(path)) {
                            reload(c);
                        }
                    } else if (path.equals(c.getFile().get())) {
                        reload(c);
                    }
                });
    }

    private void reload(Configuration config) {
        try {
            config.reload();
            LoggerUtil.reloadConfiguration();
        } catch (FileNotFoundException e) {
            LOGGER.error("reload(): file not found: {}", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("reload(): {}", e.getMessage());
        }
    }

}
