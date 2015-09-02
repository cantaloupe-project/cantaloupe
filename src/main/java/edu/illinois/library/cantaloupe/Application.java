package edu.illinois.library.cantaloupe;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.restlet.Component;
import org.restlet.data.Protocol;

/**
 * Main application class.
 */
public class Application {

    public static void main(String[] args) throws Exception {
        PropertiesConfiguration config = new PropertiesConfiguration();
        config.load("cantaloupe.properties");

        Component component = new Component();
        component.getServers().add(Protocol.HTTP,
                Integer.valueOf((String) config.getProperty("http.port")));
        component.getDefaultHost().attach("", new ImageServerApplication());
        component.start();
    }

}
