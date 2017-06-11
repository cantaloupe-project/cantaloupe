package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.util.FilesystemWatcher;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Listens for changes to a script file and reloads it when it has changed.
 */
class ScriptWatcher implements Runnable {

    private static class CallbackImpl implements FilesystemWatcher.Callback {

        private String contentsChecksum = "";
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
                final File script = path.toFile();
                if (script.equals(ScriptEngineFactory.getScriptFile())) {
                    // Calculate the checksum of the file contents and compare
                    // it to what has already been loaded. If the checksums
                    // match, skip the load.
                    try (FileInputStream is = new FileInputStream(script)) {
                        final String newChecksum = DigestUtils.md5Hex(is);
                        if (newChecksum.equals(contentsChecksum)) {
                            return;
                        }
                        contentsChecksum = newChecksum;

                        ScriptEngineFactory.getScriptEngine().
                                load(FileUtils.readFileToString(script));
                    } catch (FileNotFoundException e) {
                        logger.error("File not found: {}", e.getMessage());
                    } catch (DelegateScriptDisabledException e) {
                            logger.debug(e.getMessage());
                    } catch (Exception e) {
                        logger.error(e.getMessage());
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