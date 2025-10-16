package edu.illinois.library.cantaloupe.config.spring;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Spring Boot configuration properties that map Cantaloupe's configuration
 * to Spring Boot server properties.
 */
@Component
@ConfigurationProperties(prefix = "server")
public class ServerConfigurationProperties {

    private int port = 8182;
    private String address = "0.0.0.0";
    private Ssl ssl = new Ssl();
    private Tomcat tomcat = new Tomcat();

    public ServerConfigurationProperties() {
        // Initialize from Cantaloupe configuration if available
        try {
            Configuration config = Configuration.getInstance();

            // HTTP configuration
            if (config.getBoolean(Key.HTTP_ENABLED, true)) {
                this.port = config.getInt(Key.HTTP_PORT, 8182);
                this.address = config.getString(Key.HTTP_HOST, "0.0.0.0");
            }

            // HTTPS configuration
            if (config.getBoolean(Key.HTTPS_ENABLED, false)) {
                ssl.setEnabled(true);
                ssl.setPort(config.getInt(Key.HTTPS_PORT, 8183));
                ssl.setKeyStore(config.getString(Key.HTTPS_KEY_STORE_PATH));
                ssl.setKeyStorePassword(config.getString(Key.HTTPS_KEY_STORE_PASSWORD));
                ssl.setKeyPassword(config.getString(Key.HTTPS_KEY_PASSWORD));
                ssl.setKeyStoreType(config.getString(Key.HTTPS_KEY_STORE_TYPE, "JKS"));
            }

            // Thread configuration
            tomcat.setMaxThreads(config.getInt(Key.HTTP_MAX_THREADS, 150));
            tomcat.setMinSpareThreads(config.getInt(Key.HTTP_MIN_THREADS, 8));
            tomcat.setAcceptCount(config.getInt(Key.HTTP_ACCEPT_QUEUE_LIMIT, 0));

        } catch (Exception e) {
            // Fall back to defaults if configuration is not available
        }
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Ssl getSsl() {
        return ssl;
    }

    public void setSsl(Ssl ssl) {
        this.ssl = ssl;
    }

    public Tomcat getTomcat() {
        return tomcat;
    }

    public void setTomcat(Tomcat tomcat) {
        this.tomcat = tomcat;
    }

    public static class Ssl {
        private boolean enabled = false;
        private int port = 8183;
        private String keyStore;
        private String keyStorePassword;
        private String keyPassword;
        private String keyStoreType = "JKS";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getKeyStore() {
            return keyStore;
        }

        public void setKeyStore(String keyStore) {
            this.keyStore = keyStore;
        }

        public String getKeyStorePassword() {
            return keyStorePassword;
        }

        public void setKeyStorePassword(String keyStorePassword) {
            this.keyStorePassword = keyStorePassword;
        }

        public String getKeyPassword() {
            return keyPassword;
        }

        public void setKeyPassword(String keyPassword) {
            this.keyPassword = keyPassword;
        }

        public String getKeyStoreType() {
            return keyStoreType;
        }

        public void setKeyStoreType(String keyStoreType) {
            this.keyStoreType = keyStoreType;
        }
    }

    public static class Tomcat {
        private int maxThreads = 150;
        private int minSpareThreads = 8;
        private int acceptCount = 0;

        public int getMaxThreads() {
            return maxThreads;
        }

        public void setMaxThreads(int maxThreads) {
            this.maxThreads = maxThreads;
        }

        public int getMinSpareThreads() {
            return minSpareThreads;
        }

        public void setMinSpareThreads(int minSpareThreads) {
            this.minSpareThreads = minSpareThreads;
        }

        public int getAcceptCount() {
            return acceptCount;
        }

        public void setAcceptCount(int acceptCount) {
            this.acceptCount = acceptCount;
        }
    }
}
