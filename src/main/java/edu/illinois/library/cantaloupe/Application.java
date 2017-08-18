package edu.illinois.library.cantaloupe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Class representing the application. This is not the main application class,
 * which is actually {@link StandaloneEntry}.
 */
public final class Application {

    private static Logger logger = LoggerFactory.getLogger(Application.class);

    /**
     * Thread-safely reads and caches the version.
     */
    private static class LazyVersionReader {

        private static String cachedVersion = null;

        static {
            cachedVersion = readVersionFromManifest();
            if (cachedVersion == null) {
                cachedVersion = "Unknown";
            }
        }
    }

    /**
     * @return The application version from MANIFEST.MF, or a string like
     *         "Unknown" if not running from a JAR/WAR. The return value is
     *         cached.
     */
    public static String getVersion() {
        return LazyVersionReader.cachedVersion;
    }

    /**
     * @return The version. May be null.
     */
    private static String readVersionFromManifest() {
        String versionStr = null;
        final Class<Application> clazz = Application.class;
        final String className = clazz.getSimpleName() + ".class";
        final URL classUrl = clazz.getResource(className);

        if (classUrl != null) {
            final String classPath = classUrl.toString();
            if (classPath.startsWith("file")) {
                // The classpath will contain /WEB-INF only when running from a
                // JAR.
                final int webInfIndex = classPath.lastIndexOf("/WEB-INF");
                if (webInfIndex > -1) {
                    final String manifestPath =
                            classPath.substring(0, webInfIndex) +
                                    "/META-INF/MANIFEST.MF";
                    try (InputStream urlStream = new URL(manifestPath).openStream()) {
                        final Manifest manifest = new Manifest(urlStream);
                        final Attributes attr = manifest.getMainAttributes();
                        final String version = attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                        if (version != null) {
                            versionStr = version;
                        }
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                } else {
                    logger.debug("readVersionFromManifest(): apparently not " +
                            "running from a JAR, so no version to read");
                }
            }
        } else {
            logger.error("readVersionFromManifest(): unable to get the " +
                    "{} resource", className);
        }
        return versionStr;
    }

}
