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

    public static final String HTTP_ENABLED_CONFIG_KEY = "http.enabled";
    public static final String HTTP_PORT_CONFIG_KEY = "http.port";
    public static final String HTTPS_ENABLED_CONFIG_KEY = "https.enabled";
    public static final String HTTPS_KEY_PASSWORD_CONFIG_KEY =
            "https.key_password";
    public static final String HTTPS_KEY_STORE_PASSWORD_CONFIG_KEY =
            "https.key_store_password";
    public static final String HTTPS_KEY_STORE_PATH_CONFIG_KEY =
            "https.key_store_path";
    public static final String HTTPS_KEY_STORE_TYPE_CONFIG_KEY =
            "https.key_store_type";
    public static final String HTTPS_PORT_CONFIG_KEY = "https.port";

    private Component component;

    public void start() throws Exception {
        stop();

        final Configuration config = Application.getConfiguration();
        component = new Component();

        // set up HTTP server
        if (config.getBoolean(HTTP_ENABLED_CONFIG_KEY, true)) {
            final int port = config.getInteger(HTTP_PORT_CONFIG_KEY, 8182);
            final Server server = component.getServers().
                    add(Protocol.HTTP, port);
            server.getContext().getParameters().
                    add("useForwardedForHeader", "true");
        }
        // set up HTTPS server
        if (config.getBoolean(HTTPS_ENABLED_CONFIG_KEY, false)) {
            final int port = config.getInteger(HTTPS_PORT_CONFIG_KEY, 8183);
            final Server server = component.getServers().
                    add(Protocol.HTTPS, port);
            server.getContext().getParameters().
                    add("useForwardedForHeader", "true");
            Series<Parameter> parameters = server.getContext().getParameters();
            parameters.add("sslContextFactory",
                    "org.restlet.engine.ssl.DefaultSslContextFactory");
            parameters.add("keyStorePath",
                    config.getString(HTTPS_KEY_STORE_PATH_CONFIG_KEY));
            parameters.add("keyStorePassword",
                    config.getString(HTTPS_KEY_STORE_PASSWORD_CONFIG_KEY));
            parameters.add("keyPassword",
                    config.getString(HTTPS_KEY_PASSWORD_CONFIG_KEY));
            parameters.add("keyStoreType",
                    config.getString(HTTPS_KEY_STORE_TYPE_CONFIG_KEY));
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
