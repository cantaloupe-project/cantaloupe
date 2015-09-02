package edu.illinois.library.cantaloupe;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.restlet.Component;
import org.restlet.data.Protocol;

/**
 * Main application class.
 */
public class Application {

    private static Configuration config;

    public static Configuration getConfiguration() {
        if (config == null) {
            config = new PropertiesConfiguration();
            config.load("cantaloupe.properties");
        }
        return config;
    }

    public static void main(String[] args) throws Exception {
        Component component = new Component();
        Integer port = getConfiguration().getInteger("http.port", 8182);
        component.getServers().add(Protocol.HTTP, port);
        component.getDefaultHost().attach("", new ImageServerApplication());
        component.start();
    }

}
