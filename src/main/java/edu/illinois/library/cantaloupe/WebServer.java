package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.logging.AccessLogService;
import org.apache.commons.configuration.Configuration;
import org.restlet.Component;
import org.restlet.Server;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.util.Series;

import javax.net.ssl.KeyManagerFactory;

public class WebServer {

    private static Component component;

    public void start() throws Exception {
        stop();

        final Configuration config = Application.getConfiguration();
        component = new Component();

        // set up HTTP server
        if (config.getBoolean("http.enabled", true)) {
            final int port = config.getInteger("http.port", 8182);
            final Server server = component.getServers().
                    add(Protocol.HTTP, port);
            server.getContext().getParameters().
                    add("useForwardedForHeader", "true");
        }
        // set up HTTPS server
        if (config.getBoolean("https.enabled", false)) {
            final int port = config.getInteger("https.port", 8183);
            final Server server = component.getServers().
                    add(Protocol.HTTPS, port);
            server.getContext().getParameters().
                    add("useForwardedForHeader", "true");
            Series<Parameter> parameters = server.getContext().getParameters();
            parameters.add("sslContextFactory",
                    "org.restlet.engine.ssl.DefaultSslContextFactory");
            parameters.add("keyStorePath",
                    config.getString("https.key_store_path"));
            parameters.add("keyStorePassword",
                    config.getString("https.key_store_password"));
            parameters.add("keyPassword",
                    config.getString("https.key_password"));
            parameters.add("keyStoreType",
                    config.getString("https.key_store_type"));
            parameters.add("keyManagerAlgorithm",
                    KeyManagerFactory.getDefaultAlgorithm());
        }
        component.getClients().add(Protocol.CLAP);
        component.getDefaultHost().attach("", new WebApplication());
        component.setLogService(new AccessLogService());
        component.start();
    }

    public void stop() throws Exception {
        if (component != null) {
            component.stop();
            component = null;
        }
    }

}
