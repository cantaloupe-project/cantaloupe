package edu.illinois.library.cantaloupe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class Application {

    private static Logger logger = LoggerFactory.getLogger(Application.class);

    /**
     * @return The application version from MANIFEST.MF, or a string like
     *         "SNAPSHOT" if not running from a jar.
     */
    public static String getVersion() {
        String versionStr = "SNAPSHOT";
        final Class clazz = Application.class;
        final String className = clazz.getSimpleName() + ".class";
        final URL classUrl = clazz.getResource(className);
        if (classUrl != null) {
            final String classPath = classUrl.toString();
            if (classPath.startsWith("file")) {
                final int webInfIndex = classPath.lastIndexOf("/WEB-INF");
                if (webInfIndex > -1) {
                    String manifestPath = classPath.substring(0, webInfIndex) +
                            "/META-INF/MANIFEST.MF";
                    try {
                        Manifest manifest = new Manifest(new URL(manifestPath).openStream());
                        Attributes attr = manifest.getMainAttributes();
                        String version = attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                        if (version != null) {
                            versionStr = version;
                        }
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        } else {
            versionStr = "Unknown";
            logger.error("Unable to get the {} resource", className);
        }
        return versionStr;
    }

}
