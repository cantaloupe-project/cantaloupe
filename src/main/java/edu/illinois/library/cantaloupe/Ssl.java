package edu.illinois.library.cantaloupe;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;

public class Ssl {
    static final String DEFAULT_HTTPS_HOST = "0.0.0.0";
    static final int DEFAULT_HTTPS_PORT = 8183;

    private boolean isHTTPSEnabled;
    private String httpsHost                = DEFAULT_HTTPS_HOST;
    private String httpsKeyPassword;
    private String httpsKeyStorePassword;
    private String httpsKeyStorePath;
    private String httpsKeyStoreType;
    private int httpsPort                   = DEFAULT_HTTPS_PORT;

    static Ssl fromConfig(Configuration config) {
        Ssl ssl = new Ssl();
        ssl.setHTTPSEnabled(config.getBoolean(Key.HTTPS_ENABLED, false));
        ssl.setHTTPSHost(config.getString(Key.HTTPS_HOST, DEFAULT_HTTPS_HOST));
        ssl.setHTTPSKeyPassword(config.getString(Key.HTTPS_KEY_PASSWORD));
        ssl.setHTTPSKeyStorePassword(
                config.getString(Key.HTTPS_KEY_STORE_PASSWORD));
        ssl.setHTTPSKeyStorePath(
                config.getString(Key.HTTPS_KEY_STORE_PATH));
        ssl.setHTTPSKeyStoreType(
                config.getString(Key.HTTPS_KEY_STORE_TYPE));
        ssl.setHTTPSPort(config.getInt(Key.HTTPS_PORT, DEFAULT_HTTPS_PORT));
        return ssl;
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

    public boolean isHTTPSEnabled() {
        return isHTTPSEnabled;
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

}
