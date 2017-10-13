package edu.illinois.library.cantaloupe.test;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.image.Format;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public final class TestUtil {

    public static int countFiles(File dir) {
        List<File> allFiles = new ArrayList<>();
        Queue<File> dirs = new LinkedList<>();
        dirs.add(dir);
        while (!dirs.isEmpty()) {
            for (File f : dirs.poll().listFiles()) {
                if (f.isDirectory()) {
                    dirs.add(f);
                } else if (f.isFile()) {
                    allFiles.add(f);
                }
            }
        }
        return allFiles.size();
    }

    public static File getCurrentWorkingDirectory() throws IOException {
        File directory = new File(".");
        return directory.getCanonicalFile();
    }

    public static Collection<File> getImageFixtures(Format format)
            throws IOException {
        return FileUtils.listFiles(getFixture("images"), null, false);
    }

    public static File getFixture(String filename) throws IOException {
        return new File(getFixturePath() + File.separator + filename);
    }

    public static Path getFixturePath() throws IOException {
        return Paths.get(getCurrentWorkingDirectory().getAbsolutePath(),
                "src", "test", "resources");
    }

    public static File getImage(String name) throws IOException {
        return getFixture("images/" + name);
    }

    public static int getOpenPort() {
        return getOpenPorts(1)[0];
    }

    public static Integer[] getOpenPorts(int howMany) {
        final Set<ServerSocket> triedSockets = new HashSet<>();
        final Integer[] ports = new Integer[howMany];

        for (int i = 0; i < howMany; i++) {
            try {
                ServerSocket socket = new ServerSocket(0);
                ports[i] = socket.getLocalPort();
                triedSockets.add(socket);
                // Leave it open for now so it isn't returned again on the next
                // iteration.
            } catch (IOException e) {
                System.err.println("TestUtil.getOpenPorts(): " + e.getMessage());
            }
        }

        for (ServerSocket socket : triedSockets) {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("TestUtil.getOpenPort(): " + e.getMessage());
            }
        }

        return ports;
    }

    public static File getTempFolder() throws IOException {
        return Files.createTempDirectory("test").toFile();
    }

    public static Configuration getTestConfig() {
        try {
            return new PropertiesConfiguration("./test.properties");
        } catch (ConfigurationException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        return new BaseConfiguration();
    }

    /**
     * @return Dummy OperationList object with no operations added.
     */
    public static OperationList newOperationList() {
        OperationList ops = new OperationList(new Identifier("dummy"), Format.JPG);
        ops.add(new Encode(Format.JPG));
        return ops;
    }

    private TestUtil() {}

}
