package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.processor.codec.IIOProviderContextListener;
import edu.illinois.library.cantaloupe.resource.FileServlet;
import edu.illinois.library.cantaloupe.resource.HandlerServlet;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ListenerHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.lang.management.ManagementFactory;

/**
 * <p>Provides the embedded Servlet container in standalone mode.</p>
 *
 * <p>This class is not used when running in an external Servlet container.</p>
 */
public class ApplicationServer {

    // N.B.: Due to the way the application is packaged, this class does not
    // have access to a logger.

    private static final int IDLE_TIMEOUT = 30000;
    private static final String REMOTE_JMX_PORT_PARAMETER = "com.sun.management.jmxremote.port";

    /**
     * {@literal 0} tells Jetty to use an OS default.
     */
    static final int DEFAULT_ACCEPT_QUEUE_LIMIT = 0;

    static final String DEFAULT_HTTP_HOST = "0.0.0.0";

    static final int DEFAULT_HTTP_PORT = 8182;

    static final String DEFAULT_HTTPS_HOST = "0.0.0.0";

    static final int DEFAULT_HTTPS_PORT = 8183;

    /**
     * Minimum number of threads in the pool. {@literal 8} is the default in
     * Jetty 9.4.
     */
    static final int DEFAULT_MIN_THREADS = 8;

    /**
     * Maximum number of threads in the pool. {@literal 200} is the default in
     * Jetty 9.4, but, this being a resource-intensive application, we will
     * lower that a bit.
     */
    static final int DEFAULT_MAX_THREADS = 150;

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
    private boolean isStarted;
    private int minThreads                  = DEFAULT_MIN_THREADS;
    private int maxThreads                  = DEFAULT_MAX_THREADS;
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
        setMaxThreads(config.getInt(Key.HTTP_MAX_THREADS, DEFAULT_MAX_THREADS));
        setMinThreads(config.getInt(Key.HTTP_MIN_THREADS, DEFAULT_MIN_THREADS));
        setAcceptQueueLimit(config.getInt(Key.HTTP_ACCEPT_QUEUE_LIMIT,
                DEFAULT_ACCEPT_QUEUE_LIMIT));
    }

    private void createServer() {
        final ServletContextHandler context = new ServletContextHandler(
                ServletContextHandler.NO_SESSIONS);

        // Disable directory listing.
        context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed",
                "false");

        context.setContextPath("/");
        context.addServlet(HandlerServlet.class, "/*");
        context.addServlet(FileServlet.class, "/static/*");
        context.getServletHandler().addListener(new ListenerHolder(ApplicationContextListener.class));
        context.getServletHandler().addListener(new ListenerHolder(IIOProviderContextListener.class));

        QueuedThreadPool pool = new QueuedThreadPool(
                getMaxThreads(), getMinThreads());

        server = new Server(pool);
        context.setServer(server);
        server.setHandler(context);

        // This is technically "NCSA Combined" format.
        RequestLog log = new CustomRequestLog(
                new Slf4jRequestLogWriter(),
                CustomRequestLog.EXTENDED_NCSA_FORMAT);
        server.setRequestLog(log);
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

    public int getMaxThreads() {
        return maxThreads;
    }

    public int getMinThreads() {
        return minThreads;
    }

    public boolean isHTTPEnabled() {
        return isHTTPEnabled;
    }

    public boolean isHTTPSEnabled() {
        return isHTTPSEnabled;
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

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public void setMinThreads(int minThreads) {
        this.minThreads = minThreads;
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
                HttpConfiguration config = new HttpConfiguration();
                HttpConnectionFactory http1 = new HttpConnectionFactory();

                HTTP2CServerConnectionFactory http2 =
                        new HTTP2CServerConnectionFactory(config);
                ServerConnector connector = new ServerConnector(server, http1, http2);
                connector.setHost(getHTTPHost());
                connector.setPort(getHTTPPort());
                connector.setIdleTimeout(IDLE_TIMEOUT);
                connector.setAcceptQueueSize(getAcceptQueueLimit());
                server.addConnector(connector);
            }

            // Initialize the HTTPS server.
            if (isHTTPSEnabled()) {
                HttpConfiguration config = new HttpConfiguration();
                config.setSecureScheme("https");
                config.setSecurePort(getHTTPSPort());
                config.addCustomizer(new SecureRequestCustomizer());

                final SslContextFactory contextFactory = new SslContextFactory();
                contextFactory.setKeyStorePath(getHTTPSKeyStorePath());
                contextFactory.setKeyStorePassword(getHTTPSKeyStorePassword());
                contextFactory.setKeyManagerPassword(getHTTPSKeyPassword());

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
                connector.setAcceptQueueSize(getAcceptQueueLimit());
                server.addConnector(connector);
            }

            // If the Cantaloupe server is started with jmxremote, add the Jetty
            // jmx extensions to the MBeanServer
            if (System.getProperties().containsKey(REMOTE_JMX_PORT_PARAMETER) &&
                    !"".equals(System.getProperty(REMOTE_JMX_PORT_PARAMETER))) {
                MBeanContainer mbeanContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
                server.addBean(mbeanContainer);
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
