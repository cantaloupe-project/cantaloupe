package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.Configuration;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;

import java.util.Arrays;

public class WebServer {

    static final String HTTP_ENABLED_CONFIG_KEY = "http.enabled";
    static final String HTTP_PORT_CONFIG_KEY = "http.port";
    static final String HTTPS_ENABLED_CONFIG_KEY = "https.enabled";
    static final String HTTPS_KEY_PASSWORD_CONFIG_KEY = "https.key_password";
    static final String HTTPS_KEY_STORE_PASSWORD_CONFIG_KEY =
            "https.key_store_password";
    static final String HTTPS_KEY_STORE_PATH_CONFIG_KEY =
            "https.key_store_path";
    static final String HTTPS_KEY_STORE_TYPE_CONFIG_KEY =
            "https.key_store_type";
    static final String HTTPS_PORT_CONFIG_KEY = "https.port";

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
        final Configuration config = Configuration.getInstance();
        if (config != null) {
            httpEnabled = config.getBoolean(HTTP_ENABLED_CONFIG_KEY, false);
            httpPort = config.getInt(HTTP_PORT_CONFIG_KEY, 8182);
            httpsEnabled = config.getBoolean(HTTPS_ENABLED_CONFIG_KEY, false);
            httpsKeyPassword = config.getString(HTTPS_KEY_PASSWORD_CONFIG_KEY);
            httpsKeyStorePassword =
                    config.getString(HTTPS_KEY_STORE_PASSWORD_CONFIG_KEY);
            httpsKeyStorePath =
                    config.getString(HTTPS_KEY_STORE_PATH_CONFIG_KEY);
            httpsKeyStoreType =
                    config.getString(HTTPS_KEY_STORE_TYPE_CONFIG_KEY);
            httpsPort = config.getInt(HTTPS_PORT_CONFIG_KEY, 8183);
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

        // If we are running from a WAR file, tell Jetty to load the war file
        // via a WebAppContext. Otherwise, use a ServletContextHandler.
        final String warLocation = StandaloneEntry.getWarFile().getName();
        if (warLocation.endsWith(".war")) {
            WebAppContext context = new WebAppContext();
            context.setServer(server);
            context.setConfigurationClasses(Arrays.asList(
                    "org.eclipse.jetty.webapp.WebInfConfiguration",
                    "org.eclipse.jetty.webapp.WebXmlConfiguration",
                    "org.eclipse.jetty.webapp.MetaInfConfiguration",
                    "org.eclipse.jetty.webapp.FragmentConfiguration",
                    "org.eclipse.jetty.webapp.JettyWebXmlConfiguration"));
            context.setContextPath("/");
            context.setWar(warLocation);
            server.setHandler(context);
        } else {
            ServletContextHandler context = new ServletContextHandler(
                    ServletContextHandler.NO_SESSIONS);
            context.setInitParameter("org.restlet.application",
                    WebApplication.class.getName());
            context.setResourceBase(System.getProperty("java.io.tmpdir"));
            context.addServlet(EntryServlet.class, "/*");
            server.setHandler(context);
        }

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
