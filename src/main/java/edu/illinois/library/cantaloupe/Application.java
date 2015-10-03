package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.restlet.Component;
import org.restlet.data.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Main application class.
 */
public class Application {

    private static Logger logger = LoggerFactory.getLogger(Application.class);
    private static Component component;
    private static Configuration config;

    static {
        Velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        Velocity.setProperty("classpath.resource.loader.class",
                ClasspathResourceLoader.class.getName());
        Velocity.setProperty("class.resource.loader.cache", true);
        // we don't really care about velocity log messages
        Velocity.setProperty("runtime.log.logsystem.class",
                "org.apache.velocity.runtime.log.NullLogChute");
        Velocity.init();
    }

    private static void flushCacheAtLaunch() throws IOException {
        Cache cache = CacheFactory.getInstance();
        if (cache != null) {
            cache.flush();
        } else {
            System.out.println("Cache is not specified or is improperly configured.");
            System.exit(-1);
        }
    }

    private static void flushExpiredFromCacheAtLaunch() throws IOException {
        Cache cache = CacheFactory.getInstance();
        if (cache != null) {
            cache.flushExpired();
        } else {
            System.out.println("Cache is not specified or is improperly configured.");
            System.exit(-1);
        }
    }

    public static void main(String[] args) throws Exception {
        logger.info("Starting Cantaloupe {}", getVersion());

        if (System.getProperty("cantaloupe.cache.flush") != null) {
            flushCacheAtLaunch();
        } else if (System.getProperty("cantaloupe.cache.flush_expired") != null) {
            flushExpiredFromCacheAtLaunch();
        } else {
            startServer();
        }
    }

    /**
     * @return The application-wide Configuration object.
     */
    public static Configuration getConfiguration() {
        if (config == null) {
            try {
                String configFilePath = System.getProperty("cantaloupe.config");
                if (configFilePath == null) {
                    throw new ConfigurationException("No configuration file " +
                            "specified. Try again with the " +
                            "-Dcantaloupe.config=/path/to/cantaloupe.properties " +
                            "option.");
                }
                configFilePath = configFilePath.replaceFirst("^~",
                        System.getProperty("user.home"));
                PropertiesConfiguration propConfig = new PropertiesConfiguration();
                propConfig.load(configFilePath);
                config = propConfig;
            } catch (ConfigurationException e) {
                logger.error(e.getMessage());
            }
        }
        return config;
    }

    /**
     * Overrides the configuration, mainly for testing purposes.
     */
    public static void setConfiguration(Configuration c) {
        config = c;
    }

    /**
     * @return The application version from manifest.mf, or a string like
     * "Non-Release" if running from a jar.
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
                // noop
            }
        }
        return versionStr;
    }

    public static void startServer() throws Exception {
        stopServer();
        component = new Component();
        Integer port = getConfiguration().getInteger("http.port", 8182);
        component.getServers().add(Protocol.HTTP, port);
        component.getClients().add(Protocol.CLAP);
        component.getDefaultHost().attach("", new ImageServerApplication());
        component.start();
    }

    public static void stopServer() throws Exception {
        if (component != null) {
            component.stop();
            component = null;
        }
    }

}
