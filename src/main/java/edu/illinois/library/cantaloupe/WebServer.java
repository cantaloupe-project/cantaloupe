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

    private boolean httpEnabled;
    private int httpPort;
    private boolean httpsEnabled;
    private String httpsKeyPassword;
    private String httpsKeyStorePassword;
    private String httpsKeyStorePath;
    private String httpsKeyStoreType;
    private int httpsPort;
    private Server server;

    /**
     * Initializes the instance with defaults from the application
     * configuration.
     */
    public WebServer() {
        final Configuration config = Application.getConfiguration();
        if (config != null) {
            httpEnabled = config.getBoolean(HTTP_ENABLED_CONFIG_KEY, false);
            httpPort = config.getInteger(HTTP_PORT_CONFIG_KEY, 8182);
            httpsEnabled = config.getBoolean(HTTPS_ENABLED_CONFIG_KEY, false);
            httpsKeyPassword = config.getString(HTTPS_KEY_PASSWORD_CONFIG_KEY);
            httpsKeyStorePassword =
                    config.getString(HTTPS_KEY_STORE_PASSWORD_CONFIG_KEY);
            httpsKeyStorePath =
                    config.getString(HTTPS_KEY_STORE_PATH_CONFIG_KEY);
            httpsKeyStoreType =
                    config.getString(HTTPS_KEY_STORE_TYPE_CONFIG_KEY);
            httpsPort = config.getInteger(HTTPS_PORT_CONFIG_KEY, 8183);
        }
    }

    public int getHttpPort() {
        return httpPort;
    }

    public String getHttpsKeyPassword() {
        return httpsKeyPassword;
    }

    public String getHttpsKeyStorePassword() {
        return httpsKeyStorePassword;
    }

    public String getHttpsKeyStorePath() {
        return httpsKeyStorePath;
    }

    public String getHttpsKeyStoreType() {
        return httpsKeyStoreType;
    }

    public int getHttpsPort() {
        return httpsPort;
    }

    public boolean isHttpEnabled() {
        return httpEnabled;
    }

    public boolean isHttpsEnabled() {
        return httpsEnabled;
    }

    public void setHttpEnabled(boolean enabled) {
        this.httpEnabled = enabled;
    }

    public void setHttpPort(int port) {
        this.httpPort = port;
    }

    public void setHttpsEnabled(boolean enabled) {
        this.httpsEnabled = enabled;
    }

    public void setHttpsKeyPassword(String password) {
        this.httpsKeyPassword = password;
    }

    public void setHttpsKeyStorePassword(String password) {
        this.httpsKeyStorePassword = password;
    }

    public void setHttpsKeyStorePath(String path) {
        this.httpsKeyStorePath = path;
    }

    public void setHttpsKeyStoreType(String type) {
        this.httpsKeyStoreType = type;
    }

    public void setHttpsPort(int port) {
        this.httpsPort = port;
    }

    public void start() throws Exception {
        stop();
        server = new Server();

        ServletContextHandler context = new ServletContextHandler(
                ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.setInitParameter("org.restlet.application",
                WebApplication.class.getName());
        context.setResourceBase(System.getProperty("java.io.tmpdir"));
        context.addServlet(EntryServlet.class, "/*");
        server.setHandler(context);

        // Initialize the HTTP server
        if (isHttpEnabled()) {
            ServerConnector connector = new ServerConnector(server);
            connector.setHost("localhost");
            connector.setPort(getHttpPort());
            connector.setIdleTimeout(IDLE_TIMEOUT);
            server.addConnector(connector);
        }
        // Initialize the HTTPS server
        if (isHttpsEnabled()) {
            HttpConfiguration httpsConfig = new HttpConfiguration();
            httpsConfig.addCustomizer(new SecureRequestCustomizer());
            SslContextFactory sslContextFactory = new SslContextFactory();

            sslContextFactory.setKeyStorePath(getHttpsKeyStorePath());
            sslContextFactory.setKeyStorePassword(getHttpsKeyStorePassword());
            sslContextFactory.setKeyManagerPassword(getHttpsKeyPassword());
            ServerConnector sslConnector = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, "HTTP/1.1"),
                    new HttpConnectionFactory(httpsConfig));
            sslConnector.setPort(getHttpsPort());
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
