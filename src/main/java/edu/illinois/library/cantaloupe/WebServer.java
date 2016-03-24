package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.logging.AccessLogService;
import org.restlet.Component;
import org.restlet.Server;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.util.Series;

import javax.net.ssl.KeyManagerFactory;

public class WebServer {

    public static final String HTTP_ENABLED_CONFIG_KEY = "http.enabled";
    public static final String HTTP_PORT_CONFIG_KEY = "http.port";
    public static final String HTTPS_ENABLED_CONFIG_KEY = "https.enabled";
    public static final String HTTPS_KEY_PASSWORD_CONFIG_KEY =
            "https.key_password";
    public static final String HTTPS_KEY_STORE_PASSWORD_CONFIG_KEY =
            "https.key_store_password";
    public static final String HTTPS_KEY_STORE_PATH_CONFIG_KEY =
            "https.key_store_path";
    public static final String HTTPS_KEY_STORE_TYPE_CONFIG_KEY =
            "https.key_store_type";
    public static final String HTTPS_PORT_CONFIG_KEY = "https.port";

    private Component component;
    private boolean httpEnabled;
    private int httpPort;
    private boolean httpsEnabled;
    private String httpsKeyPassword;
    private String httpsKeyStorePassword;
    private String httpsKeyStorePath;
    private String httpsKeyStoreType;
    private int httpsPort;

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

        component = new Component();

        // set up HTTP server
        if (isHttpEnabled()) {
            final Server server = component.getServers().
                    add(Protocol.HTTP, getHttpPort());
            server.getContext().getParameters().
                    add("useForwardedForHeader", "true");
        }
        // set up HTTPS server
        if (isHttpsEnabled()) {
            final Server server = component.getServers().
                    add(Protocol.HTTPS, getHttpsPort());
            server.getContext().getParameters().
                    add("useForwardedForHeader", "true");
            Series<Parameter> parameters = server.getContext().getParameters();
            parameters.add("sslContextFactory",
                    "org.restlet.engine.ssl.DefaultSslContextFactory");
            parameters.add("keyStorePath", getHttpsKeyStorePath());
            parameters.add("keyStorePassword", getHttpsKeyStorePassword());
            parameters.add("keyPassword", getHttpsKeyPassword());
            parameters.add("keyStoreType", getHttpsKeyStoreType());
            parameters.add("keyManagerAlgorithm",
                    KeyManagerFactory.getDefaultAlgorithm());
        }
        component.getClients().add(Protocol.CLAP);
        component.getDefaultHost().attach("", new WebApplication());
        component.setLogService(new AccessLogService());
        component.start();
    }

    public void stop() throws Exception {
        if (component != null) {
            component.stop();
            component = null;
        }
    }

}
