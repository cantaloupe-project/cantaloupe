package edu.illinois.library.cantaloupe.test;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.OutputFormat;

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
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @return Dummy OperationList object with no operations added.
     */
    public static OperationList newOperationList() {
        OperationList ops = new OperationList();
        ops.setIdentifier(new Identifier("dummy"));
        ops.setOutputFormat(OutputFormat.JPG);
        return ops;
    }

}
