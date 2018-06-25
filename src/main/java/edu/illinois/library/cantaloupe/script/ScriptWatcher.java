package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.util.FilesystemWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Listens for changes to a script file and reloads it when it has changed.
 */
final class ScriptWatcher implements Runnable {

    private static class CallbackImpl implements FilesystemWatcher.Callback {

        private byte[] currentChecksum = new byte[0];

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
                    // Some filesystems will generate multiple events that
                    // could result in this method being invoked multiple
                    // times. To avoid that, we will calculate the checksum of
                    // the file contents and compare it to what has already
                    // been loaded. If the checksums match, skip the load.
                    final byte[] fileBytes = Files.readAllBytes(path);
                    final MessageDigest md = MessageDigest.getInstance("SHA-1");
                    md.update(fileBytes);
                    byte[] newChecksum = md.digest();

                    if (!Arrays.equals(newChecksum, currentChecksum)) {
                        LOGGER.debug("Script checksums differ; reloading");
                        currentChecksum = newChecksum;
                        final String code = new String(fileBytes, "UTF-8");
                        DelegateProxy.load(code);
                    } else {
                        LOGGER.debug("Script checksums match; skipping reload");
                    }
                }
            } catch (NoSuchFileException e) {
                LOGGER.error("File not found: {}", e.getMessage());
            } catch (NoSuchAlgorithmException | ScriptException | IOException e) {
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
                Path path = scriptFile.toAbsolutePath().getParent();
                filesystemWatcher = new FilesystemWatcher(
                        path, new CallbackImpl());
            }
        } catch (IOException e) {
            if (Configuration.getInstance().getBoolean(Key.DELEGATE_SCRIPT_ENABLED)) {
                LOGGER.error(e.getMessage());
            }
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