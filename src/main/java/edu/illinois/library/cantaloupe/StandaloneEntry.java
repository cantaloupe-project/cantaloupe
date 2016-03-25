package edu.illinois.library.cantaloupe;

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
