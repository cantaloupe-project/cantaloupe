package edu.illinois.library.cantaloupe;

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
        Velocity.init();
    }

    public static void main(String[] args) throws Exception {
        start();
    }

    /**
     * @return The application-wide Configuration object.
     */
    public static Configuration getConfiguration() {
        if (config == null) {
            try {
                String configFilePath = System.getProperty("cantaloupe.config");
                logger.debug("Using config file: {}", configFilePath);
                PropertiesConfiguration propConfig = new PropertiesConfiguration();
                propConfig.load(configFilePath);
                config = propConfig;
            } catch (ConfigurationException e) {
                logger.error("Failed to load the config file. Re-run with " +
                        "the -Dcantaloupe.config=/path/to/cantaloupe.properties " +
                        "option. (See the readme.)");
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
