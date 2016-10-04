package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.ConfigurationFactory;

import java.io.File;
import java.net.URL;
import java.security.ProtectionDomain;

/**
 * <p>Serves as the main application class in a standalone context.</p>
 *
 * <p>This class will be unavailable in a Servlet container and should have
 * as few dependencies as possible.</p>
 */
public class StandaloneEntry {

    private static WebServer webServer;

    /**
     * Checks invocation arguments and starts the embedded Servlet container.
     *
     * @param args Ignored.
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        final File configFile = ConfigurationFactory.getInstance().getFile();
        if (configFile == null) {
            System.out.println("Usage: java " +
                    "-D" + ConfigurationFactory.CONFIG_FILE_VM_ARGUMENT +
                    "=cantaloupe.properties -jar " + getWarFile().getName());
            System.exit(-1);
        }
        if (!configFile.exists()) {
            System.out.println("Configuration file does not exist.");
            System.exit(-1);
        }
        if (!configFile.isFile()) {
            System.out.println("Configuration file is not a file.");
            System.exit(-1);
        }
        if (!configFile.canRead()) {
            System.out.println("Configuration file is not readable.");
            System.exit(-1);
        }
        getWebServer().start();
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
