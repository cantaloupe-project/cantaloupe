package edu.illinois.library.cantaloupe;


import java.io.File;
import java.net.URL;
import java.security.ProtectionDomain;

/**
 * <p>Serves as the main application class in a standalone context.</p>
 */
public class StandaloneEntry {

    private static WebServer webServer;

    /**
     * Starts the embedded Servlet container, which initializes
     * {@link EntryServlet}.</p>
     *
     * @param args Ignored.
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        getWebServer().start();
    }

    static File getWarFile() {
        ProtectionDomain protectionDomain =
                WebServer.class.getProtectionDomain();
        URL location = protectionDomain.getCodeSource().getLocation();
        return new File(location.toExternalForm());
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
