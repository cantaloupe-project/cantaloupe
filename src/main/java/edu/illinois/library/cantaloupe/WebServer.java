package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.Configuration;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * <p>Provides the web server in standalone mode.</p>
 *
 * <p>This class is not used when running in a Servlet container.</p>
 */
public class WebServer {

    public static final String HTTP_ENABLED_CONFIG_KEY = "http.enabled";
    public static final String HTTP_HOST_CONFIG_KEY = "http.host";
    public static final String HTTP_PORT_CONFIG_KEY = "http.port";
    public static final String HTTPS_ENABLED_CONFIG_KEY = "https.enabled";
    public static final String HTTPS_HOST_CONFIG_KEY = "https.host";
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
    private String httpHost;
    private int httpPort;
    private boolean httpsEnabled;
    private String httpsHost;
    private String httpsKeyPassword;
    private String httpsKeyStorePassword;
    private String httpsKeyStorePath;
    private String httpsKeyStoreType;
    private int httpsPort;
    private boolean isInitialized = false;
    private Server server = new Server();

    /**
     * Initializes the instance with defaults from the application
     * configuration.
     */
    public WebServer() {
        final Configuration config = Configuration.getInstance();
        if (config != null) {
            setHttpEnabled(config.getBoolean(HTTP_ENABLED_CONFIG_KEY, false));
            setHttpHost(config.getString(HTTP_HOST_CONFIG_KEY, "0.0.0.0"));
            setHttpPort(config.getInt(HTTP_PORT_CONFIG_KEY, 8182));
            setHttpsEnabled(config.getBoolean(HTTPS_ENABLED_CONFIG_KEY, false));
            setHttpsHost(config.getString(HTTPS_HOST_CONFIG_KEY, "0.0.0.0"));
            setHttpsKeyPassword(config.getString(HTTPS_KEY_PASSWORD_CONFIG_KEY));
            setHttpsKeyStorePassword(
                    config.getString(HTTPS_KEY_STORE_PASSWORD_CONFIG_KEY));
            setHttpsKeyStorePath(
                    config.getString(HTTPS_KEY_STORE_PATH_CONFIG_KEY));
            setHttpsKeyStoreType(
                    config.getString(HTTPS_KEY_STORE_TYPE_CONFIG_KEY));
            setHttpsPort(config.getInt(HTTPS_PORT_CONFIG_KEY, 8183));
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

    public boolean isStarted() {
        return server.isStarted();
    }

    public boolean isStopped() {
        return server.isStopped();
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
        if (!isInitialized) {
            final WebAppContext context = new WebAppContext();
            context.setContextPath("/");
            context.setServer(server);
            server.setHandler(context);

            // Give the WebAppContext a different WAR to use depending on whether
            // we are running standalone or from a WAR file.
            final String warPath = StandaloneEntry.getWarFile().getAbsolutePath();
            if (warPath.endsWith(".war")) {
                context.setWar(warPath);
            } else {
                context.setWar("src/main/webapp");
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
        }
        isInitialized = true;
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }

}
