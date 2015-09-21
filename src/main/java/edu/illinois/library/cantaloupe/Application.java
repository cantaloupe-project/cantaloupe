package edu.illinois.library.cantaloupe;

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

/**
 * Main application class.
 */
public class Application {

    private static Logger logger = LoggerFactory.getLogger(Application.class);
    private static Component component = new Component();
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

    public static void main(String[] args) throws Exception {
        if (System.getProperty("cantaloupe.cache.flush") != null) {
            CacheFactory.getCache().flush();
        } else if (System.getProperty("cantaloupe.cache.flush_expired") != null) {
            CacheFactory.getCache().flushExpired();
        } else {
            start();
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

    public static void start() throws Exception {
        Integer port = getConfiguration().getInteger("http.port", 8182);
        component.getServers().add(Protocol.HTTP, port);
        component.getDefaultHost().attach("", new ImageServerApplication());
        component.start();
    }

    public static void stop() throws Exception {
        component.stop();
    }

}
