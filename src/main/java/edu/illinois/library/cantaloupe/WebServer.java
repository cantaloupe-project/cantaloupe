package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
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
            setHttpEnabled(config.getBoolean(Key.HTTP_ENABLED, false));
            setHttpHost(config.getString(Key.HTTP_HOST, "0.0.0.0"));
            setHttpPort(config.getInt(Key.HTTP_PORT, 8182));
            setHttpsEnabled(config.getBoolean(Key.HTTPS_ENABLED, false));
            setHttpsHost(config.getString(Key.HTTPS_HOST, "0.0.0.0"));
            setHttpsKeyPassword(config.getString(Key.HTTPS_KEY_PASSWORD));
            setHttpsKeyStorePassword(
                    config.getString(Key.HTTPS_KEY_STORE_PASSWORD));
            setHttpsKeyStorePath(
                    config.getString(Key.HTTPS_KEY_STORE_PATH));
            setHttpsKeyStoreType(
                    config.getString(Key.HTTPS_KEY_STORE_TYPE));
            setHttpsPort(config.getInt(Key.HTTPS_PORT, 8183));
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
