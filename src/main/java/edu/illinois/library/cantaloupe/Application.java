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
     * @return The application version from manifest.mf, or a string like
     *         "Non-Release" if not running from a jar.
     */
    public static String getVersion() {
        String versionStr = "Non-Release";
        Class clazz = Application.class;
        String className = clazz.getSimpleName() + ".class";
        String classPath = clazz.getResource(className).toString();
        if (classPath.startsWith("jar")) {
            String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) +
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
        return versionStr;
    }

}
