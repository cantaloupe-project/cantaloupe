package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
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

/**
 * <p>Provides the web server in standalone mode.</p>
 *
 * <p>This class is not used when running in a Servlet container.</p>
 */
public class WebServer {

    static final String HTTP_ENABLED_CONFIG_KEY = "http.enabled";
    static final String HTTP_HOST_CONFIG_KEY = "http.host";
    static final String HTTP_PORT_CONFIG_KEY = "http.port";
    static final String HTTPS_ENABLED_CONFIG_KEY = "https.enabled";
    static final String HTTPS_HOST_CONFIG_KEY = "https.host";
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
    private String httpHost;
    private int httpPort;
    private boolean httpsEnabled;
    private String httpsHost;
    private String httpsKeyPassword;
    private String httpsKeyStorePassword;
    private String httpsKeyStorePath;
    private String httpsKeyStoreType;
    private int httpsPort;
    private Server server;

    static {
        // Tell Restlet to use SLF4J instead of java.util.logging. This needs
        // to be performed before Restlet has been initialized.
        System.setProperty("org.restlet.engine.loggerFacadeClass",
                "org.restlet.ext.slf4j.Slf4jLoggerFacade");
    }

    /**
     * Initializes the instance with defaults from the application
     * configuration.
     */
    public WebServer() {
        final Configuration config = ConfigurationFactory.getInstance();
        if (config != null) {
            httpEnabled = config.getBoolean(HTTP_ENABLED_CONFIG_KEY, false);
            httpHost = config.getString(HTTP_HOST_CONFIG_KEY, "0.0.0.0");
            httpPort = config.getInt(HTTP_PORT_CONFIG_KEY, 8182);
            httpsEnabled = config.getBoolean(HTTPS_ENABLED_CONFIG_KEY, false);
            httpsHost = config.getString(HTTPS_HOST_CONFIG_KEY, "0.0.0.0");
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

    public String getHttpHost() {
        return httpHost;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public String getHttpsHost() {
        return httpsHost;
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

    public void setHttpHost(String host) {
        this.httpHost = host;
    }

    public void setHttpPort(int port) {
        this.httpPort = port;
    }

    public void setHttpsEnabled(boolean enabled) {
        this.httpsEnabled = enabled;
    }

    public void setHttpsHost(String host) {
        this.httpsHost = host;
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

    /**
     * Starts the HTTP and/or HTTPS servers.
     *
     * @throws Exception
     */
    public void start() throws Exception {
        stop();
        server = new Server();

        // If we are running from a WAR file, tell Jetty to load the war file
        // via a WebAppContext. Otherwise, use a ServletContextHandler.
        final String warPath = StandaloneEntry.getWarFile().getAbsolutePath();
        if (warPath.endsWith(".war")) {
            WebAppContext context = new WebAppContext();
            context.setServer(server);
            context.setConfigurationClasses(Arrays.asList(
                    "org.eclipse.jetty.webapp.WebInfConfiguration",
                    "org.eclipse.jetty.webapp.WebXmlConfiguration",
                    "org.eclipse.jetty.webapp.MetaInfConfiguration",
                    "org.eclipse.jetty.webapp.FragmentConfiguration",
                    "org.eclipse.jetty.webapp.JettyWebXmlConfiguration"));
            context.setContextPath("/");
            context.setWar(warPath);
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
            connector.setHost(getHttpHost());
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
            sslConnector.setHost(getHttpsHost());
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
