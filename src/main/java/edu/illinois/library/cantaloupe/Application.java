package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Class representing the application. This is not the main application class,
 * which is actually {@link StandaloneEntry}.
 */
public final class Application {

    // N.B.: Due to the way the application is packaged, this class does not
    // have access to a logger.

    /**
     * Reads information from the manifest.
     */
    private static class ManifestReader {

        private static String name, version;

        static {
            readManifest();

            if (name == null) {
                name = "Cantaloupe";
            }
            if (version == null) {
                version = "Unknown";
            }
        }

        private static void readManifest() {
            final Class<Application> clazz = Application.class;
            final String className = clazz.getSimpleName() + ".class";
            final URL classUrl = clazz.getResource(className);
            final String classPath = classUrl.toString();

            if (classPath.startsWith("file")) {
                // The classpath will contain /WEB-INF only when running from a JAR.
                final int webInfIndex = classPath.lastIndexOf("/WEB-INF");
                if (webInfIndex > -1) {
                    final String manifestPath =
                            classPath.substring(0, webInfIndex) +
                                    "/META-INF/MANIFEST.MF";
                    try (InputStream urlStream = new URL(manifestPath).openStream()) {
                        final Manifest manifest = new Manifest(urlStream);
                        final Attributes attr = manifest.getMainAttributes();

                        name = attr.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
                        version = attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                    } catch (IOException e) {
                        System.err.println(e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * @return The application title from {@literal MANIFEST.MF}, or some other
     *         string if not running from a JAR/WAR.
     */
    public static String getName() {
        return ManifestReader.name;
    }

    /**
     * @return The application version from {@literal MANIFEST.MF}, or some
     *         other string if not running from a JAR/WAR.
     */
    public static String getVersion() {
        return ManifestReader.version;
    }

    /**
     * @return Path to the temp directory used by the application. If it does
     *         not exist, it will be created.
     * @see #isUsingSystemTempPath()
     */
    public static Path getTempPath() {
        final Configuration config = Configuration.getInstance();
        final String pathStr = config.getString(Key.TEMP_PATHNAME, "");

        if (!pathStr.isEmpty()) {
            Path dir = Paths.get(pathStr);
            try {
                Files.createDirectories(dir);
                return dir;
            } catch (FileAlreadyExistsException ignore) {
                // This is fine.
            } catch (IOException e) {
                System.err.println("Application.getTempPath(): " + e.getMessage());
                System.err.println("Application.getTempPath(): " +
                        "falling back to java.io.tmpdir");
            }
        }
        return Paths.get(System.getProperty("java.io.tmpdir"));
    }

    /**
     * @see #getTempPath()
     */
    public static boolean isUsingSystemTempPath() {
        return Paths.get(System.getProperty("java.io.tmpdir")).equals(getTempPath());
    }

    private Application() {}

}
