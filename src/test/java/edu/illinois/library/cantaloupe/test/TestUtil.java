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
        for (int port = 1024; port <= 65536; port++) {
            try {
                new ServerSocket(port);
                return port;
            } catch (IOException e) {
                // noop
            }
        }
        return null;
    }

}
