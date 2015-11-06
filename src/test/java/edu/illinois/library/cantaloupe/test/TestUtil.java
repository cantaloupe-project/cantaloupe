package edu.illinois.library.cantaloupe.test;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class TestUtil {

    public static File getFixture(String filename) throws IOException {
        File directory = new File(".");
        String cwd = directory.getCanonicalPath();
        Path testPath = Paths.get(cwd, "src", "test", "resources");
        return new File(testPath + File.separator + filename);
    }

    public static Integer getOpenPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            // noop
        }
        return null;
    }

}
