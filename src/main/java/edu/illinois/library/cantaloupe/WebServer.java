package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.util.SystemUtils;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NegotiatingServerConnectionFactory;
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
    private boolean isStarted = false;
    private Server server = new Server();

    /**
     * Initializes the instance with defaults from the application
     * configuration.
     */
    public WebServer() {
        final Configuration config = Configuration.getInstance();
        if (config != null) {
            setHTTPEnabled(config.getBoolean(Key.HTTP_ENABLED, false));
            setHTTPHost(config.getString(Key.HTTP_HOST, "0.0.0.0"));
            setHTTPPort(config.getInt(Key.HTTP_PORT, 8182));
            setHTTPSEnabled(config.getBoolean(Key.HTTPS_ENABLED, false));
            setHTTPSHost(config.getString(Key.HTTPS_HOST, "0.0.0.0"));
            setHTTPSKeyPassword(config.getString(Key.HTTPS_KEY_PASSWORD));
            setHTTPSKeyStorePassword(
                    config.getString(Key.HTTPS_KEY_STORE_PASSWORD));
            setHTTPSKeyStorePath(
                    config.getString(Key.HTTPS_KEY_STORE_PATH));
            setHTTPSKeyStoreType(
                    config.getString(Key.HTTPS_KEY_STORE_TYPE));
            setHTTPSPort(config.getInt(Key.HTTPS_PORT, 8183));
        }
    }

    public String getHTTPHost() {
        return httpHost;
    }

    public int getHTTPPort() {
        return httpPort;
    }

    public String getHTTPSHost() {
        return httpsHost;
    }

    public String getHTTPSKeyPassword() {
        return httpsKeyPassword;
    }

    public String getHTTPSKeyStorePassword() {
        return httpsKeyStorePassword;
    }

    public String getHTTPSKeyStorePath() {
        return httpsKeyStorePath;
    }

    public String getHTTPSKeyStoreType() {
        return httpsKeyStoreType;
    }

    public int getHTTPSPort() {
        return httpsPort;
    }

    /**
     * ALPN is built into Java 9. In earlier versions, it has to be provided by
     * a JAR on the boot classpath.
     */
    private boolean isALPNAvailable() {
        if (SystemUtils.getJavaVersion() < 1.9) {
            try {
                NegotiatingServerConnectionFactory.
                        checkProtocolNegotiationAvailable();
            } catch (IllegalStateException e) {
                return false;
            }
        }
        return true;
    }

    public boolean isHTTPEnabled() {
        return httpEnabled;
    }

    public boolean isHTTPSEnabled() {
        return httpsEnabled;
    }

    public boolean isStarted() {
        return server.isStarted();
    }

    public boolean isStopped() {
        return server.isStopped();
    }

    public void setHTTPEnabled(boolean enabled) {
        this.httpEnabled = enabled;
    }

    public void setHTTPHost(String host) {
        this.httpHost = host;
    }

    public void setHTTPPort(int port) {
        this.httpPort = port;
    }

    public void setHTTPSEnabled(boolean enabled) {
        this.httpsEnabled = enabled;
    }

    public void setHTTPSHost(String host) {
        this.httpsHost = host;
    }

    public void setHTTPSKeyPassword(String password) {
        this.httpsKeyPassword = password;
    }

    public void setHTTPSKeyStorePassword(String password) {
        this.httpsKeyStorePassword = password;
    }

    public void setHTTPSKeyStorePath(String path) {
        this.httpsKeyStorePath = path;
    }

    public void setHTTPSKeyStoreType(String type) {
        this.httpsKeyStoreType = type;
    }

    public void setHTTPSPort(int port) {
        this.httpsPort = port;
    }

    /**
     * Starts the HTTP and/or HTTPS servers.
     *
     * @throws Exception
     */
    public void start() throws Exception {
        if (!isStarted) {
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

            // Initialize the HTTP server, handling both HTTP/1.1 and plaintext
            // HTTP/2.
            if (isHTTPEnabled()) {
                HttpConfiguration config = new HttpConfiguration();
                HttpConnectionFactory http1 =
                        new HttpConnectionFactory(config);
                HTTP2CServerConnectionFactory http2 =
                        new HTTP2CServerConnectionFactory(config);

                ServerConnector connector = new ServerConnector(server,
                        http1, http2);
                connector.setHost(getHTTPHost());
                connector.setPort(getHTTPPort());
                connector.setIdleTimeout(IDLE_TIMEOUT);
                server.addConnector(connector);
            }
            // Initialize the HTTPS server.
            // N.B. HTTP/2 support requires an ALPN JAR on the boot classpath,
            // e.g.: -Xbootclasspath/p:/path/to/alpn-boot-8.1.5.v20150921.jar
            // https://www.eclipse.org/jetty/documentation/9.3.x/alpn-chapter.html
            if (isHTTPSEnabled()) {
                HttpConfiguration config = new HttpConfiguration();
                config.setSecureScheme("https");
                config.setSecurePort(getHTTPSPort());
                config.addCustomizer(new SecureRequestCustomizer());

                final SslContextFactory contextFactory = new SslContextFactory();
                contextFactory.setKeyStorePath(getHTTPSKeyStorePath());
                contextFactory.setKeyStorePassword(getHTTPSKeyStorePassword());
                contextFactory.setKeyManagerPassword(getHTTPSKeyPassword());

                if (isALPNAvailable()) {
                    System.out.println("WebServer.start(): ALPN is " +
                            "available; configuring HTTP/2");

                    HttpConnectionFactory http1 =
                            new HttpConnectionFactory(config);
                    HTTP2ServerConnectionFactory http2 =
                            new HTTP2ServerConnectionFactory(config);

                    ALPNServerConnectionFactory alpn =
                            new ALPNServerConnectionFactory();
                    alpn.setDefaultProtocol(http1.getProtocol());

                    contextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
                    contextFactory.setUseCipherSuitesOrder(true);

                    SslConnectionFactory connectionFactory =
                            new SslConnectionFactory(contextFactory,
                                    alpn.getProtocol());

                    ServerConnector connector = new ServerConnector(server,
                            connectionFactory, alpn, http2, http1);
                    connector.setHost(getHTTPSHost());
                    connector.setPort(getHTTPSPort());
                    connector.setIdleTimeout(IDLE_TIMEOUT);
                    server.addConnector(connector);
                } else {
                    System.err.println("WebServer.start(): ALPN is not " +
                            "available; falling back to HTTP/1.1");

                    ServerConnector connector = new ServerConnector(server,
                            new SslConnectionFactory(contextFactory, "HTTP/1.1"),
                            new HttpConnectionFactory(config));
                    connector.setHost(getHTTPSHost());
                    connector.setPort(getHTTPSPort());
                    connector.setIdleTimeout(IDLE_TIMEOUT);
                    server.addConnector(connector);
                }
            }
        }
        server.start();
        isStarted = true;
    }

    public void stop() throws Exception {
        server.stop();
    }

}
