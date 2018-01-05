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
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * <p>Provides the embedded Servlet container in standalone mode.</p>
 *
 * <p>This class is not used when running in an external Servlet container.</p>
 */
public class ApplicationServer {

    // N.B.: Due to the way the application is packaged, this class does not
    // have access to a logger.

    private static final int IDLE_TIMEOUT = 30000;

    private int acceptQueueLimit = 0;
    private boolean isHTTPEnabled;
    private String httpHost = "0.0.0.0";
    private int httpPort = 8182;
    private boolean isHTTPSEnabled;
    private String httpsHost = "0.0.0.0";
    private String httpsKeyPassword;
    private String httpsKeyStorePassword;
    private String httpsKeyStorePath;
    private String httpsKeyStoreType;
    private int httpsPort = 8183;
    private boolean isInsecureHTTP2Enabled = true;
    private boolean isSecureHTTP2Enabled = true;
    private boolean isStarted = false;
    private Server server;

    /**
     * Initializes the instance with arbitrary defaults.
     */
    public ApplicationServer() {
    }

    /**
     * Initializes the instance with defaults from a Configuration object.
     */
    public ApplicationServer(Configuration config) {
        this();

        setAcceptQueueLimit(config.getInt(Key.HTTP_ACCEPT_QUEUE_LIMIT, 0));
        setHTTPEnabled(config.getBoolean(Key.HTTP_ENABLED, false));
        setHTTPHost(config.getString(Key.HTTP_HOST, "0.0.0.0"));
        setHTTPPort(config.getInt(Key.HTTP_PORT, 8182));
        setInsecureHTTP2Enabled(
                config.getBoolean(Key.HTTP_HTTP2_ENABLED, true));
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
        setSecureHTTP2Enabled(
                config.getBoolean(Key.HTTPS_HTTP2_ENABLED, true));
    }

    private void createServer() {
        final WebAppContext context = new WebAppContext();
        context.setContextPath("/");

        // Jetty will extract the app into a temp directory that is, by
        // default, the same as java.io.tmpdir. The OS may periodically clean
        // out this directory, which will cause havoc if it happens while the
        // application is running, so here we can override Jetty's temp
        // directory location.
        // See: http://www.eclipse.org/jetty/documentation/current/ref-temporary-directories.html
        context.setAttribute("org.eclipse.jetty.webapp.basetempdir",
                Application.getTempPath().toString());

        // We also set it to NOT persist to avoid accumulating a bunch of
        // stale exploded apps.
        context.setPersistTempDirectory(false);

        // Give the WebAppContext a different "WAR" to use depending on
        // whether we are running from a WAR file or an IDE.
        final String warPath = StandaloneEntry.getWarFile().getAbsolutePath();
        if (warPath.endsWith(".war")) {
            context.setWar(warPath);
        } else {
            context.setWar("src/main/webapp");
        }

        server = new Server();
        context.setServer(server);
        server.setHandler(context);
    }

    public int getAcceptQueueLimit() {
        return acceptQueueLimit;
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

    public boolean isHTTPEnabled() {
        return isHTTPEnabled;
    }

    public boolean isHTTPSEnabled() {
        return isHTTPSEnabled;
    }

    public boolean isInsecureHTTP2Enabled() {
        return isInsecureHTTP2Enabled;
    }

    public boolean isSecureHTTP2Enabled() {
        return isSecureHTTP2Enabled;
    }

    public boolean isStarted() {
        return (server != null && server.isStarted());
    }

    public boolean isStopped() {
        return (server == null || server.isStopped());
    }

    public void setAcceptQueueLimit(int size) {
        this.acceptQueueLimit = size;
    }

    public void setHTTPEnabled(boolean enabled) {
        this.isHTTPEnabled = enabled;
    }

    public void setHTTPHost(String host) {
        this.httpHost = host;
    }

    public void setHTTPPort(int port) {
        this.httpPort = port;
    }

    public void setHTTPSEnabled(boolean enabled) {
        this.isHTTPSEnabled = enabled;
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

    public void setInsecureHTTP2Enabled(boolean enabled) {
        this.isInsecureHTTP2Enabled = enabled;
    }

    public void setSecureHTTP2Enabled(boolean enabled) {
        this.isSecureHTTP2Enabled = enabled;
    }

    /**
     * Starts the HTTP and/or HTTPS servers.
     *
     * @throws Exception
     */
    public void start() throws Exception {
        if (!isStarted) {
            createServer();

            // Initialize the HTTP server, handling both HTTP/1.1 and plaintext
            // HTTP/2.
            if (isHTTPEnabled()) {
                ServerConnector connector;
                HttpConfiguration config = new HttpConfiguration();
                config.setSendDateHeader(false); // Restlet will take care of this
                HttpConnectionFactory http1 =
                        new HttpConnectionFactory(config);

                if (isInsecureHTTP2Enabled()) {
                    HTTP2CServerConnectionFactory http2 =
                            new HTTP2CServerConnectionFactory(config);
                    connector = new ServerConnector(server, http1, http2);
                } else {
                    connector = new ServerConnector(server, http1);
                }

                connector.setHost(getHTTPHost());
                connector.setPort(getHTTPPort());
                connector.setIdleTimeout(IDLE_TIMEOUT);
                connector.setAcceptQueueSize(getAcceptQueueLimit());
                server.addConnector(connector);
            }

            // Initialize the HTTPS server.
            // N.B.: HTTP/2 support requires ALPN, which requires either Java 9,
            // or an ALPN JAR on the boot classpath,
            // e.g.: -Xbootclasspath/p:/path/to/alpn-boot-8.1.5.v20150921.jar
            // https://www.eclipse.org/jetty/documentation/9.3.x/alpn-chapter.html
            if (isHTTPSEnabled()) {
                HttpConfiguration config = new HttpConfiguration();
                config.setSendDateHeader(false); // Restlet will take care of this
                config.setSecureScheme("https");
                config.setSecurePort(getHTTPSPort());
                config.addCustomizer(new SecureRequestCustomizer());

                final SslContextFactory contextFactory = new SslContextFactory();
                contextFactory.setKeyStorePath(getHTTPSKeyStorePath());
                contextFactory.setKeyStorePassword(getHTTPSKeyStorePassword());
                contextFactory.setKeyManagerPassword(getHTTPSKeyPassword());

                ServerConnector connector = null;

                if (isSecureHTTP2Enabled()) {
                    if (SystemUtils.isALPNAvailable()) {
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

                        connector = new ServerConnector(server,
                                connectionFactory, alpn, http2, http1);
                    } else {
                        System.err.println(getClass().getSimpleName() +
                                ".start(): unable to initialize secure " +
                                "HTTP/2 (JRE <9 or no ALPN JAR on boot classpath)");
                    }
                }

                if (connector == null) { // Fall back to HTTP/1.1.
                    connector = new ServerConnector(server,
                            new SslConnectionFactory(contextFactory, "HTTP/1.1"),
                            new HttpConnectionFactory(config));
                }

                connector.setHost(getHTTPSHost());
                connector.setPort(getHTTPSPort());
                connector.setIdleTimeout(IDLE_TIMEOUT);
                connector.setAcceptQueueSize(getAcceptQueueLimit());
                server.addConnector(connector);
            }
            server.start();
            isStarted = true;
        }
    }

    public void stop() throws Exception {
        if (server != null) {
            server.stop();
        }
        server = null;
        isStarted = false;
    }

}
