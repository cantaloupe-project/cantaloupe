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

    private static class CallbackImpl implements FilesystemWatcher.Callback {

        private static final long MIN_INTERVAL_MSEC = 1000;

        private long lastHandled = System.currentTimeMillis();
        private Logger logger = LoggerFactory.getLogger(CallbackImpl.class);

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
            try {
                if (path.toFile().equals(ScriptEngineFactory.getScriptFile())) {
                    // Handle the event only if it happened at least
                    // MIN_INTERVAL_MSEC after the last one.
                    final long now = System.currentTimeMillis();
                    if (now - lastHandled >= MIN_INTERVAL_MSEC) {
                        lastHandled = now;
                        try {
                            ScriptEngineFactory.getScriptEngine().
                                    load(FileUtils.readFileToString(path.toFile()));
                        } catch (DelegateScriptDisabledException e) {
                            logger.debug(e.getMessage());
                        } catch (Exception e) {
                            logger.error(e.getMessage());
                        }
                    } else {
                        logger.debug("Multiple events < " + MIN_INTERVAL_MSEC +
                                "ms apart");
                    }
                }
            } catch (FileNotFoundException e) {
                logger.error("File not found: {}", e.getMessage());
            }
        }

    }

    private static Logger logger = LoggerFactory.getLogger(ScriptWatcher.class);

    private FilesystemWatcher filesystemWatcher;

    @Override
    public void run() {
        try {
            Path path = ScriptEngineFactory.getScriptFile().toPath().getParent();
            filesystemWatcher = new FilesystemWatcher(path, new CallbackImpl());
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