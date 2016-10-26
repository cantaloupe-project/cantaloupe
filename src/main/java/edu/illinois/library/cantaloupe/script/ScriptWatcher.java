package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.util.FilesystemWatcher;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Listens for changes to a script file and reloads it when it has changed.
 */
class ScriptWatcher implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(ScriptWatcher.class);

    private FilesystemWatcher filesystemWatcher;

    @Override
    public void run() {

        FilesystemWatcher.Callback callback = new FilesystemWatcher.Callback() {

            private Logger logger = LoggerFactory.
                    getLogger(FilesystemWatcher.Callback.class);

            @Override
            public void created(Path path) { handle(path); }

            @Override
            public void deleted(Path path) {}

            @Override
            public void modified(Path path) { handle(path); }

            private void handle(Path path) {
                try {
                    if (path.toFile().equals(ScriptEngineFactory.getScriptFile())) {
                        try {
                            ScriptEngineFactory.getScriptEngine().
                                    load(FileUtils.readFileToString(path.toFile()));
                        } catch(DelegateScriptDisabledException e) {
                            logger.debug("handle(): {}", e.getMessage());
                        } catch (Exception e) {
                            logger.error("run(): {}", e.getMessage());
                        }
                    }
                } catch (FileNotFoundException e) {
                    logger.error("run(): {}", e.getMessage());
                }
            }
        };
        try {
            Path path = ScriptEngineFactory.getScriptFile().toPath().getParent();
            filesystemWatcher = new FilesystemWatcher(path, callback);
            filesystemWatcher.processEvents();
        } catch (IOException e) {
            logger.error("run(): {}", e.getMessage());
        }
    }

    public void stop() {
        if (filesystemWatcher != null) {
            filesystemWatcher.stop();
        }
    }
}