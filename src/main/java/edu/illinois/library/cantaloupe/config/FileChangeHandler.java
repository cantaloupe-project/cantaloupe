package edu.illinois.library.cantaloupe.config;

import edu.illinois.library.cantaloupe.logging.LoggerUtil;
import edu.illinois.library.cantaloupe.util.FilesystemWatcher;

import java.io.FileNotFoundException;
import java.nio.file.Path;

class FileChangeHandler implements FilesystemWatcher.Callback {

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
    }

    private void reload(Configuration config) {
        try {
            config.reload();
            LoggerUtil.reloadConfiguration();
        } catch (FileNotFoundException e) {
            System.err.println("FileChangeHandlerRunner$CallbackImpl: " +
                    "file not found: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("FileChangeHandlerRunner$CallbackImpl: " +
                    e.getMessage());
        }
    }

}
