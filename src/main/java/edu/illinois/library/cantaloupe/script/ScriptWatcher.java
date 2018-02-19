package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.util.FilesystemWatcher;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * Listens for changes to a script file and reloads it when it has changed.
 */
final class ScriptWatcher implements Runnable {

    private static class CallbackImpl implements FilesystemWatcher.Callback {

        private String contentsChecksum = "";

        @Override
        public void created(Path path) {
            handle(path);
        }

        @Override
        public void deleted(Path path) {
            try {
                if (path.equals(DelegateProxyService.getScriptFile())) {
                    LOGGER.warn("Delegate script deleted (not by me!): {}", path);
                }
            } catch (NoSuchFileException ignore) {}
        }

        @Override
        public void modified(Path path) {
            handle(path);
        }

        private void handle(Path path) {
            try {
                if (path.equals(DelegateProxyService.getScriptFile())) {
                    // Calculate the checksum of the file contents and compare
                    // it to what has already been loaded. If the checksums
                    // match, skip the load.
                    final byte[] fileBytes = Files.readAllBytes(path);
                    final String newChecksum = DigestUtils.md5Hex(fileBytes);

                    if (newChecksum.equals(contentsChecksum)) {
                        LOGGER.debug("Script checksums match; skipping reload");
                        return;
                    }
                    contentsChecksum = newChecksum;

                    final String code = new String(fileBytes, "UTF-8");
                    DelegateProxy.load(code);
                }
            } catch (NoSuchFileException e) {
                LOGGER.error("File not found: {}", e.getMessage());
            } catch (ScriptException | IOException e) {
                LOGGER.error(e.getMessage());
            }
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ScriptWatcher.class);

    private FilesystemWatcher filesystemWatcher;

    ScriptWatcher() {
        try {
            Path scriptFile = DelegateProxyService.getScriptFile();
            if (scriptFile != null) {
                Path path = scriptFile.getParent();
                filesystemWatcher = new FilesystemWatcher(
                        path, new CallbackImpl());
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public void run() {
        if (filesystemWatcher != null) {
            filesystemWatcher.processEvents();
        }
    }

    public void stop() {
        if (filesystemWatcher != null) {
            filesystemWatcher.stop();
        }
    }

}