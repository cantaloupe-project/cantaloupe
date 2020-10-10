package edu.illinois.library.cantaloupe.test;

import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.delegate.DelegateProxyService;
import edu.illinois.library.cantaloupe.delegate.UnavailableException;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashSet;

public final class TestUtil {

    public static File getCurrentWorkingDirectory() {
        try {
            return new File(".").getCanonicalFile();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Collection<Path> getImageFixtures(Format format)
            throws IOException {
        final Collection<Path> fixtures = new HashSet<>();

        Files.walkFileTree(getFixture("images"), new FileVisitor<>() {
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

    public static Path getFixture(String filename) {
        return getFixturePath().resolve(filename);
    }

    /**
     * @return Path of the fixtures directory.
     */
    public static Path getFixturePath() {
        return Paths.get(getCurrentWorkingDirectory().getAbsolutePath(),
                "src", "test", "resources");
    }

    public static Path getImage(String name) {
        return getFixture("images/" + name);
    }

    /**
     * @return Path of the image fixtures directory.
     */
    public static Path getImagesPath() {
        return getFixturePath().resolve("images");
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

    public static DelegateProxy newDelegateProxy() {
        edu.illinois.library.cantaloupe.config.Configuration config =
                edu.illinois.library.cantaloupe.config.Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").toString());

        DelegateProxyService service = DelegateProxyService.getInstance();
        try {
            return service.newDelegateProxy(new RequestContext());
        } catch (UnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    private TestUtil() {}

}
