package edu.illinois.library.cantaloupe.test;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.image.Format;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashSet;

public final class TestUtil {

    public static File getCurrentWorkingDirectory() throws IOException {
        File directory = new File(".");
        return directory.getCanonicalFile();
    }

    public static Collection<Path> getImageFixtures(Format format)
            throws IOException {
        final Collection<Path> fixtures = new HashSet<>();

        Files.walkFileTree(getFixture("images"), new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                                                     BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(format.getPreferredExtension())) {
                    fixtures.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });

        return fixtures;
    }

    public static Path getFixture(String filename) throws IOException {
        return getFixturePath().resolve(filename);
    }

    public static Path getFixturePath() throws IOException {
        return Paths.get(getCurrentWorkingDirectory().getAbsolutePath(),
                "src", "test", "resources");
    }

    public static Path getImage(String name) throws IOException {
        return getFixture("images/" + name);
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
