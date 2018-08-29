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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * <p>Provides the embedded Servlet container in standalone mode.</p>
 *
 * <p>This class is not used when running in an external Servlet container.</p>
 */
public class ApplicationServer {

    // N.B.: Due to the way the application is packaged, this class does not
    // have access to a logger.

    private static final int IDLE_TIMEOUT = 30000;

    /**
     * {@literal 0} tells Jetty to use an OS default.
     */
    static final int DEFAULT_ACCEPT_QUEUE_LIMIT = 0;

    static final String DEFAULT_HTTP_HOST = "0.0.0.0";

    static final int DEFAULT_HTTP_PORT = 8182;

    static final String DEFAULT_HTTPS_HOST = "0.0.0.0";

    static final int DEFAULT_HTTPS_PORT = 8183;

    private int acceptQueueLimit            = DEFAULT_ACCEPT_QUEUE_LIMIT;
    private boolean isHTTPEnabled;
    private String httpHost                 = DEFAULT_HTTP_HOST;
    private int httpPort                    = DEFAULT_HTTP_PORT;
    private boolean isHTTPSEnabled;
    private String httpsHost                = DEFAULT_HTTPS_HOST;
    private String httpsKeyPassword;
    private String httpsKeyStorePassword;
    private String httpsKeyStorePath;
    private String httpsKeyStoreType;
    private int httpsPort                   = DEFAULT_HTTPS_PORT;
    private boolean isInsecureHTTP2Enabled;
    private boolean isSecureHTTP2Enabled;
    private boolean isStarted;
    private Server server;

    /**
     * Initializes the instance with arbitrary defaults.
     */
    public ApplicationServer() {
    }

    /**
     * Initializes the instance with defaults from a {@link Configuration}
     * object.
     */
    public ApplicationServer(Configuration config) {
        this();

        setHTTPEnabled(config.getBoolean(Key.HTTP_ENABLED, false));
        setHTTPHost(config.getString(Key.HTTP_HOST, DEFAULT_HTTP_HOST));
        setHTTPPort(config.getInt(Key.HTTP_PORT, DEFAULT_HTTP_PORT));
        setInsecureHTTP2Enabled(
                config.getBoolean(Key.HTTP_HTTP2_ENABLED, true));

        setHTTPSEnabled(config.getBoolean(Key.HTTPS_ENABLED, false));
        setHTTPSHost(config.getString(Key.HTTPS_HOST, DEFAULT_HTTPS_HOST));
        setHTTPSKeyPassword(config.getString(Key.HTTPS_KEY_PASSWORD));
        setHTTPSKeyStorePassword(
                config.getString(Key.HTTPS_KEY_STORE_PASSWORD));
        setHTTPSKeyStorePath(
                config.getString(Key.HTTPS_KEY_STORE_PATH));
        setHTTPSKeyStoreType(
                config.getString(Key.HTTPS_KEY_STORE_TYPE));
        setHTTPSPort(config.getInt(Key.HTTPS_PORT, DEFAULT_HTTPS_PORT));
        setSecureHTTP2Enabled(
                config.getBoolean(Key.HTTPS_HTTP2_ENABLED, true));

        setAcceptQueueLimit(config.getInt(Key.HTTP_ACCEPT_QUEUE_LIMIT,
                DEFAULT_ACCEPT_QUEUE_LIMIT));
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

        // Disable directory listing.
        context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed",
                "false");

        // We also set it to NOT persist to avoid accumulating a bunch of
        // stale exploded apps.
        // N.B.: WebAppContext.setPersistTempDirectory() is supposed to be the
        // way to accomplish this, but testing indicates that it does not work
        // reliably as of Jetty 9.4.9.v20180320, after sending either SIGINT
        // or SIGTERM. So, instead, we will use a shutdown hook.
        // See: http://www.eclipse.org/jetty/documentation/current/ref-temporary-directories.html#_setting_a_specific_temp_directory
        //context.setPersistTempDirectory(false);
        if (!"true".equals(System.getProperty(Application.TEST_VM_ARGUMENT))) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Files.walk(context.getTempDirectory().toPath())
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }));
        }

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
     */
    public void start() throws Exception {
        if (!isStarted) {
            createServer();

            // Initialize the HTTP server, handling both HTTP/1.1 and plaintext
            // HTTP/2.
            if (isHTTPEnabled()) {
                ServerConnector connector;
                HttpConfiguration config = new HttpConfiguration();
                HttpConnectionFactory http1 = new HttpConnectionFactory();

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
            // N.B.: HTTP/2 requires ALPN, which requires Java 9.
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
