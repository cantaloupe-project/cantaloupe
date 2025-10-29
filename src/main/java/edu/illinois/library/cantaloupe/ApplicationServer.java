package edu.illinois.library.cantaloupe;

import java.lang.management.ManagementFactory;

import org.eclipse.jetty.ee10.servlet.ListenerHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHandler;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.processor.codec.IIOProviderContextListener;
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

    private boolean isStarted;
    private int minThreads                  = DEFAULT_MIN_THREADS;
    private int maxThreads                  = DEFAULT_MAX_THREADS;
    private Server server;

    private Ssl ssl;

    /**
     * Initializes the instance with arbitrary defaults.
     */
    public ApplicationServer() {
        setSsl(new Ssl());
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
        setMaxThreads(config.getInt(Key.HTTP_MAX_THREADS, DEFAULT_MAX_THREADS));
        setMinThreads(config.getInt(Key.HTTP_MIN_THREADS, DEFAULT_MIN_THREADS));
        setAcceptQueueLimit(config.getInt(Key.HTTP_ACCEPT_QUEUE_LIMIT,
                DEFAULT_ACCEPT_QUEUE_LIMIT));
        setSsl(Ssl.fromConfig(config));
    }

    private void createServer() {
        final ServletContextHandler context = new ServletContextHandler(
                ServletContextHandler.NO_SESSIONS);

        // Disable directory listing.
        context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed",
                "false");

        context.setContextPath("/");
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


    public int getMaxThreads() {
        return maxThreads;
    }

    public int getMinThreads() {
        return minThreads;
    }

    public boolean isHTTPEnabled() {
        return isHTTPEnabled;
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

    public void setSsl(Ssl ssl) {
        this.ssl = ssl;
    }

    public Ssl getSsl() {
        return ssl;
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
                config.setUriCompliance(
                        UriCompliance.from("DEFAULT,SUSPICIOUS_PATH_CHARACTERS,AMBIGUOUS_PATH_SEPARATOR"));
                HttpConnectionFactory http1 = new HttpConnectionFactory(config);

                HTTP2CServerConnectionFactory http2 =
                        new HTTP2CServerConnectionFactory(config);
                ServerConnector connector = new ServerConnector(server, http1, http2);
                connector.setHost(getHTTPHost());
                connector.setPort(getHTTPPort());
                connector.setIdleTimeout(IDLE_TIMEOUT);
                connector.setAcceptQueueSize(getAcceptQueueLimit());
                server.addConnector(connector);
                server.getContainedBeans(ServletHandler.class)
                    .forEach(handler -> handler.setDecodeAmbiguousURIs(true));
            }

            // Initialize the HTTPS server.
            if (ssl.isHTTPSEnabled()) {
                new SslServerCustomizer(ssl, IDLE_TIMEOUT, getAcceptQueueLimit()).customize(server);
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
