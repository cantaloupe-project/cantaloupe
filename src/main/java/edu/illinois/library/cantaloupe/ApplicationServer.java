package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;

/**
 * <p>Compatibility stub for the original ApplicationServer class.</p>
 *
 * <p>This class maintains API compatibility with the original ApplicationServer
 * but delegates actual server functionality to Spring Boot's embedded server.
 * It's kept for backward compatibility with existing code that might reference it.</p>
 */
public class ApplicationServer {

    private static final int DEFAULT_ACCEPT_QUEUE_LIMIT = 0;
    static final String DEFAULT_HTTP_HOST = "0.0.0.0";
    static final int DEFAULT_HTTP_PORT = 8182;
    static final String DEFAULT_HTTPS_HOST = "0.0.0.0";
    static final int DEFAULT_HTTPS_PORT = 8183;
    static final int DEFAULT_MIN_THREADS = 8;
    static final int DEFAULT_MAX_THREADS = 150;

    private int acceptQueueLimit = DEFAULT_ACCEPT_QUEUE_LIMIT;
    private boolean isHTTPEnabled;
    private String httpHost = DEFAULT_HTTP_HOST;
    private int httpPort = DEFAULT_HTTP_PORT;
    private boolean isHTTPSEnabled;
    private String httpsHost = DEFAULT_HTTPS_HOST;
    private String httpsKeyPassword;
    private String httpsKeyStorePassword;
    private String httpsKeyStorePath;
    private String httpsKeyStoreType;
    private int httpsPort = DEFAULT_HTTPS_PORT;
    private boolean isStarted;
    private int minThreads = DEFAULT_MIN_THREADS;
    private int maxThreads = DEFAULT_MAX_THREADS;

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
        // In Spring Boot context, assume server is started when Spring context is active
        return isStarted;
    }

    public boolean isStopped() {
        return !isStarted;
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
     * Starts the server.
     * In Spring Boot context, this is a no-op as Spring Boot handles server startup.
     */
    public void start() throws Exception {
        // Spring Boot handles server startup automatically
        isStarted = true;
    }

    /**
     * Stops the server.
     * In Spring Boot context, this is a no-op as Spring Boot handles server shutdown.
     */
    public void stop() throws Exception {
        // Spring Boot handles server shutdown automatically
        isStarted = false;
    }
}
