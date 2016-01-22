package edu.illinois.library.cantaloupe;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheException;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.logging.AccessLogService;
import edu.illinois.library.cantaloupe.logging.velocity.Slf4jLogChute;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.restlet.Component;
import org.restlet.Server;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.util.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Main application class.
 */
public class Application {

    private static Logger logger = LoggerFactory.getLogger(Application.class);

    public static final String CONFIG_FILE_VM_ARGUMENT = "cantaloupe.config";
    public static final String PURGE_CACHE_VM_ARGUMENT =
            "cantaloupe.cache.purge";
    public static final String PURGE_EXPIRED_FROM_CACHE_VM_ARGUMENT =
            "cantaloupe.cache.purge_expired";

    private static Component component;
    private static Configuration config;

    static {
        // Suppress a Dock icon in OS X
        System.setProperty("java.awt.headless", "true");

        initializeLogging();

        Velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        Velocity.setProperty("classpath.resource.loader.class",
                ClasspathResourceLoader.class.getName());
        Velocity.setProperty("class.resource.loader.cache", true);
        Velocity.setProperty("runtime.log.logsystem.class",
                Slf4jLogChute.class.getCanonicalName());
        Velocity.init();
    }

    /**
     * @return Application-wide Configuration object. Will be loaded from a
     * file at startup (see {@link #getConfigurationFile}), but can also be
     * overridden by {@link #setConfiguration}.
     */
    public static Configuration getConfiguration() {
        if (config == null) {
            reloadConfigurationFile();
        }
        return config;
    }

    /**
     * @return File object corresponding to the active configuration file, or
     * null if there is no configuration file.
     */
    public static File getConfigurationFile() {
        String configFilePath = System.getProperty(CONFIG_FILE_VM_ARGUMENT);
        if (configFilePath != null) {
            try {
                // expand paths that start with "~"
                configFilePath = configFilePath.replaceFirst("^~",
                                System.getProperty("user.home"));
                return new File(configFilePath).getCanonicalFile();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * @return The application version from manifest.mf, or a string like
     * "Non-Release" if not running from a jar.
     */
    public static String getVersion() {
        String versionStr = "Non-Release";
        Class clazz = Application.class;
        String className = clazz.getSimpleName() + ".class";
        String classPath = clazz.getResource(className).toString();
        if (classPath.startsWith("jar")) {
            String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) +
                    "/META-INF/MANIFEST.MF";
            try {
                Manifest manifest = new Manifest(new URL(manifestPath).openStream());
                Attributes attr = manifest.getMainAttributes();
                String version = attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                if (version != null) {
                    versionStr = version;
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return versionStr;
    }

    private static void purgeCacheAtLaunch() throws CacheException {
        Cache cache = CacheFactory.getInstance();
        if (cache != null) {
            cache.purge();
        } else {
            System.out.println("Cache is not specified or is improperly configured.");
            System.exit(-1);
        }
    }

    private static void purgeExpiredFromCacheAtLaunch() throws CacheException {
        Cache cache = CacheFactory.getInstance();
        if (cache != null) {
            cache.purgeExpired();
        } else {
            System.out.println("Cache is not specified or is improperly configured.");
            System.exit(-1);
        }
    }

    private static void initializeLogging() {
        // Restlet normally uses JUL; we want it to use slf4j.
        System.getProperties().put("org.restlet.engine.loggerFacadeClass",
                "org.restlet.ext.slf4j.Slf4jLoggerFacade");

        Configuration appConfig = getConfiguration();
        if (appConfig != null) {
            // At this point, Logback has already initialized itself, which is a
            // problem because logback.xml depends on application configuration
            // options, which have not been loaded yet. So, reset the logger
            // context...
            LoggerContext loggerContext = (LoggerContext)
                    LoggerFactory.getILoggerFactory();
            JoranConfigurator jc = new JoranConfigurator();
            jc.setContext(loggerContext);
            loggerContext.reset();
            // Then copy logging-related configuration key/values into logger
            // context properties...
            Iterator it = getConfiguration().getKeys();
            while (it.hasNext()) {
                String key = (String) it.next();
                if (key.startsWith("log.")) {
                    loggerContext.putProperty(key, getConfiguration().getString(key));
                }
            }
            // Finally, reload the Logback configuration.
            try {
                InputStream stream = Application.class.getClassLoader().
                        getResourceAsStream("logback.xml");
                jc.doConfigure(stream);
            } catch (JoranException je) {
                je.printStackTrace();
            }
            StatusPrinter.printIfErrorsOccured(loggerContext);
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            validateConfiguration();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("Exiting.");
            System.exit(-1);
        }

        if (getConfigurationFile() != null) {
            // Use FilesystemWatcher to listen for changes to the directory
            // containing the configuration file. When the config file is found to
            // have been changed, reload it.
            final Thread configWatcher = new Thread() {
                public void run() {
                    FilesystemWatcher.Callback callback = new FilesystemWatcher.Callback() {
                        public void created(Path path) { handle(path); }
                        public void deleted(Path path) {}
                        public void modified(Path path) { handle(path); }
                        private void handle(Path path) {
                            if (path.toFile().equals(getConfigurationFile())) {
                                reloadConfigurationFile();
                            }
                        }
                    };
                    try {
                        Path path = getConfigurationFile().toPath().getParent();
                        new FilesystemWatcher(path, callback).processEvents();
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            };
            configWatcher.start();
        }

        final int mb = 1024 * 1024;
        Runtime runtime = Runtime.getRuntime();
        logger.info(System.getProperty("java.vm.name") + " / " +
                System.getProperty("java.vm.info"));
        logger.info("{} available processor cores",
                runtime.availableProcessors());
        logger.info("Heap total: {}MB; max: {}MB", runtime.totalMemory() / mb,
                runtime.maxMemory() / mb);
        logger.info("\uD83C\uDF48 Starting Cantaloupe {}", getVersion());

        if (System.getProperty(PURGE_CACHE_VM_ARGUMENT) != null) {
            purgeCacheAtLaunch();
        } else if (System.getProperty(PURGE_EXPIRED_FROM_CACHE_VM_ARGUMENT) != null) {
            purgeExpiredFromCacheAtLaunch();
        } else {
            startServer();
        }
    }

    public static synchronized void reloadConfigurationFile() {
        try {
            File configFile = getConfigurationFile();
            if (configFile != null) {
                if (config != null) {
                    logger.info("Reloading configuration file: {}", configFile);
                } else {
                    // the logger has probably not been initialized yet
                    System.out.println("Loading configuration file: " +
                            configFile);
                }
                PropertiesConfiguration propConfig = new PropertiesConfiguration();
                propConfig.load(configFile);
                config = propConfig;
            }
        } catch (ConfigurationException e) {
            // The logger has probably not been initialized yet, as it
            // depends on a working configuration.
            System.out.println(e.getMessage());
        }
    }

    /**
     * Overrides the configuration, mainly for testing purposes.
     */
    public static synchronized void setConfiguration(Configuration c) {
        config = c;
    }

    public static synchronized void startServer() throws Exception {
        stopServer();
        final Configuration config = getConfiguration();
        component = new Component();
        // set up HTTP server
        if (config.getBoolean("http.enabled", true)) {
            final int port = config.getInteger("http.port", 8182);
            component.getServers().add(Protocol.HTTP, port);
        }
        // set up HTTPS server
        if (getConfiguration().getBoolean("https.enabled", false)) {
            final int port = config.getInteger("https.port", 8183);
            Server server = component.getServers().add(Protocol.HTTPS, port);
            Series<Parameter> parameters = server.getContext().getParameters();
            parameters.add("sslContextFactory",
                    "org.restlet.engine.ssl.DefaultSslContextFactory");
            parameters.add("keyStorePath",
                    config.getString("https.key_store_path"));
            parameters.add("keyStorePassword",
                    config.getString("https.key_store_password"));
            parameters.add("keyPassword",
                    config.getString("https.key_password"));
            parameters.add("keyStoreType",
                    config.getString("https.key_store_type"));
            parameters.add("keyManagerAlgorithm",
                    KeyManagerFactory.getDefaultAlgorithm());
        }
        component.getClients().add(Protocol.CLAP);
        component.getDefaultHost().attach("", new WebApplication());
        component.setLogService(new AccessLogService());
        component.start();
    }

    public static synchronized void stopServer() throws Exception {
        if (component != null) {
            component.stop();
            component = null;
        }
    }

    /**
     * @throws Exception If the configuration is invalid.
     */
    private static void validateConfiguration() throws Exception {
        // check that a configuration file exists
        if (getConfiguration() == null) {
            throw new edu.illinois.library.cantaloupe.ConfigurationException(
                    "No configuration file specified. Try again with the " +
                            "-D" + CONFIG_FILE_VM_ARGUMENT +
                            "=/path/to/cantaloupe.properties argument.");
        }
    }

}
