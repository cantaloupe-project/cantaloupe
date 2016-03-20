package edu.illinois.library.cantaloupe;

import org.apache.commons.configuration.Configuration;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

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

    private static final int IDLE_TIMEOUT = 30000;

    private Server server;

    public void start() throws Exception {
        stop();

        final Configuration config = Application.getConfiguration();

        server = new Server();

        ServletContextHandler context = new ServletContextHandler(
                ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.setInitParameter("org.restlet.application",
                WebApplication.class.getName());
        context.setResourceBase(System.getProperty("java.io.tmpdir"));
        context.addServlet(EntryServlet.class, "/*");
        server.setHandler(context);

        // set up HTTP server
        if (config.getBoolean(HTTP_ENABLED_CONFIG_KEY, true)) {
            ServerConnector connector = new ServerConnector(server);
            connector.setHost("localhost");
            connector.setPort(config.getInt(HTTP_PORT_CONFIG_KEY));
            connector.setIdleTimeout(IDLE_TIMEOUT);
            server.addConnector(connector);
        }
        // set up HTTPS server
        if (config.getBoolean(HTTPS_ENABLED_CONFIG_KEY, false)) {
            HttpConfiguration httpsConfig = new HttpConfiguration();
            httpsConfig.addCustomizer(new SecureRequestCustomizer());
            SslContextFactory sslContextFactory = new SslContextFactory();

            sslContextFactory.setKeyStorePath(
                    config.getString(HTTPS_KEY_STORE_PATH_CONFIG_KEY));
            sslContextFactory.setKeyStorePassword(
                    config.getString(HTTPS_KEY_STORE_PASSWORD_CONFIG_KEY));
            sslContextFactory.setKeyManagerPassword(
                    config.getString(HTTPS_KEY_PASSWORD_CONFIG_KEY));
            ServerConnector sslConnector = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, "HTTP/1.1"),
                    new HttpConnectionFactory(httpsConfig));
            sslConnector.setPort(config.getInt(HTTPS_PORT_CONFIG_KEY));
            server.addConnector(sslConnector);
        }

        server.start();
    }

    public void stop() throws Exception {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

}
