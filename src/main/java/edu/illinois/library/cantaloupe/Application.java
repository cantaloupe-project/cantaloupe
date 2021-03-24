package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class representing the application. This is not the main application class,
 * which is actually {@link StandaloneEntry}.
 */
public final class Application {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(Application.class);

    /**
     * Set to {@code true} during testing.
     *
     * @see #isTesting()
     */
    public static final String TEST_VM_ARGUMENT = "cantaloupe.test";

    private static final String DEFAULT_NAME    = "Cantaloupe";
    private static final String DEFAULT_VERSION = "Unknown";

    /**
     * @return The application title from {@literal MANIFEST.MF}, or some other
     *         string if not running from a JAR.
     */
    public static String getName() {
        Package myPackage = Application.class.getPackage();
        String name = myPackage.getImplementationTitle();
        return (name != null) ? name : DEFAULT_NAME;
    }

    /**
     * @return The application version from {@literal MANIFEST.MF}, or some
     *         other string if not running from a JAR.
     */
    public static String getVersion() {
        Package myPackage = Application.class.getPackage();
        String version = myPackage.getImplementationVersion();
        return (version != null) ? version : DEFAULT_VERSION;
    }

    /**
     * @return Path to the temp directory used by the application. If it does
     *         not exist, it will be created.
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
                LOGGER.error("getTempPath(): {} (falling back to java.io.tmpdir)",
                        e.getMessage(), e);
            }
        }
        return Paths.get(System.getProperty("java.io.tmpdir"));
    }

    /**
     * @return Whether the application is running in test mode.
     * @see #TEST_VM_ARGUMENT
     */
    public static boolean isTesting() {
        return "true".equals(System.getProperty(TEST_VM_ARGUMENT));
    }

    private Application() {}

}
