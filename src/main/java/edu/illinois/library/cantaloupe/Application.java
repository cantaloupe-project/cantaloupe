package edu.illinois.library.cantaloupe;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import edu.illinois.library.cantaloupe.cache.Cache;
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
import org.restlet.data.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Main application class.
 */
public class Application {

    private static Logger logger = LoggerFactory.getLogger(Application.class);
    private static Component component;
    private static Configuration config;

    static {
        initializeLogging();

        Velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        Velocity.setProperty("classpath.resource.loader.class",
                ClasspathResourceLoader.class.getName());
        Velocity.setProperty("class.resource.loader.cache", true);
        Velocity.setProperty("runtime.log.logsystem.class",
                Slf4jLogChute.class.getCanonicalName());
        Velocity.init();
    }

    private static void flushCacheAtLaunch() throws IOException {
        Cache cache = CacheFactory.getInstance();
        if (cache != null) {
            cache.flush();
        } else {
            System.out.println("Cache is not specified or is improperly configured.");
            System.exit(-1);
        }
    }

    private static void flushExpiredFromCacheAtLaunch() throws IOException {
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
        if (getConfiguration() == null) {
            System.out.println("No configuration file specified. Try again " +
                    "with the -Dcantaloupe.config=/path/to/cantaloupe.properties option.");
            System.exit(0);
        }
        final int mb = 1024 * 1024;
        Runtime runtime = Runtime.getRuntime();
        logger.info(System.getProperty("java.vm.name") + " / " +
                System.getProperty("java.vm.info"));
        logger.info("Heap total: {}MB; max: {}MB", runtime.totalMemory() / mb,
                runtime.maxMemory() / mb);
        logger.info("Starting Cantaloupe {}", getVersion());

        if (System.getProperty("cantaloupe.cache.flush") != null) {
            flushCacheAtLaunch();
        } else if (System.getProperty("cantaloupe.cache.flush_expired") != null) {
            flushExpiredFromCacheAtLaunch();
        } else {
            startServer();
        }
    }

    /**
     * @return The application-wide Configuration object.
     */
    public static Configuration getConfiguration() {
        if (config == null) {
            try {
                String configFilePath = System.getProperty("cantaloupe.config");
                if (configFilePath != null) {
                    configFilePath = configFilePath.replaceFirst("^~",
                            System.getProperty("user.home"));
                    PropertiesConfiguration propConfig = new PropertiesConfiguration();
                    propConfig.load(configFilePath);
                    config = propConfig;
                }
            } catch (ConfigurationException e) {
                // The logger has probably not been initialized yet, as it
                // depends on a working configuration.
                System.out.println(e.getMessage());
            }
        }
        return config;
    }

    /**
     * Overrides the configuration, mainly for testing purposes.
     */
    public static void setConfiguration(Configuration c) {
        config = c;
    }

    /**
     * @return The application version from manifest.mf, or a string like
     * "Non-Release" if running from a jar.
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

    public static void startServer() throws Exception {
        stopServer();
        component = new Component();
        Integer port = getConfiguration().getInteger("http.port", 8182);
        component.getServers().add(Protocol.HTTP, port);
        component.getClients().add(Protocol.CLAP);
        component.getDefaultHost().attach("", new ImageServerApplication());
        component.setLogService(new AccessLogService());
        component.start();
    }

    public static void stopServer() throws Exception {
        if (component != null) {
            component.stop();
            component = null;
        }
    }

}
