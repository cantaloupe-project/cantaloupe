package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;

import java.io.File;
import java.net.URL;
import java.security.ProtectionDomain;

/**
 * <p>Serves as the main application class in a standalone context.</p>
 *
 * <p>This class will be unavailable in a Servlet container, so should not be
 * referred to externally if at all possible. It should also have as few
 * dependencies as possible.</p>
 */
public class StandaloneEntry {

    private static WebServer webServer;

    /**
     * <p>Checks the configuration VM option and starts the embedded web
     * container. The following configuration options are supported:</p>
     *
     * <dl>
     *     <dt><code>-Dcantaloupe.config</code> option supplied:</dt>
     *     <dd>Will use the configuration file at the corresponding
     *     pathname.</dd>
     *     <dt>No <code>-Dcantaloupe.config</code> option supplied:</dt>
     *     <dd>Will attempt to get configuration from the environment.</dd>
     * </dl>
     *
     * @param args Ignored.
     * @throws Exception If there is a problem starting the web server.
     */
    public static void main(String[] args) throws Exception {
        final Configuration config = ConfigurationFactory.getInstance();
        if (config == null) {
            printUsage();
            System.exit(-1);
        }
        final File configFile = config.getFile();
        if (configFile == null) {
            printUsage();
            System.exit(-1);
        }
        if (!configFile.exists()) {
            System.out.println("Does not exist: " + configFile);
            printUsage();
            System.exit(-1);
        }
        if (!configFile.isFile()) {
            System.out.println("Not a file: " + configFile);
            printUsage();
            System.exit(-1);
        }
        if (!configFile.canRead()) {
            System.out.println("Not readable: " + configFile);
            printUsage();
            System.exit(-1);
        }
        getWebServer().start();
    }

    /**
     * Print program usage.
     */
    private static void printUsage() {
        System.out.println("Usage: java " +
                "-D" + ConfigurationFactory.CONFIG_VM_ARGUMENT +
                "=cantaloupe.properties -jar " + getWarFile().getName());
    }

    static File getWarFile() {
        ProtectionDomain protectionDomain =
                WebServer.class.getProtectionDomain();
        URL location = protectionDomain.getCodeSource().getLocation();
        return new File(location.getFile());
    }

    /**
     * @return Application web server instance.
     */
    public static synchronized WebServer getWebServer() {
        if (webServer == null) {
            webServer = new WebServer();
        }
        return webServer;
    }

}
