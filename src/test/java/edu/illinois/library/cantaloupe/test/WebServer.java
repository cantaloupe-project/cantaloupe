package edu.illinois.library.cantaloupe.test;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;

import java.io.File;
import java.io.IOException;

/**
 * HTTP server that serves static content using the fixture path as its root.
 * Call {@link #start()} and then {@link #getUri()} to get its URL.
 *
 * @see <a href="http://www.eclipse.org/jetty/documentation/current/embedded-examples.html">
 *     Embedded Examples</a>
 */
public class WebServer {

    private int port = TestUtil.getOpenPort();
    private File root;
    private Server server;

    /**
     * Initializes a static file HTTP server using the fixture path as its
     * root.
     *
     * @throws IOException
     */
    public WebServer() throws IOException {
        String path = TestUtil.getFixturePath().toAbsolutePath() + "/images";
        this.root = new File(path);
        server = new Server(port);

        ResourceHandler handler = new ResourceHandler();
        handler.setDirectoriesListed(false);
        handler.setResourceBase(root.getAbsolutePath());

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { handler, new DefaultHandler() });
        server.setHandler(handlers);
    }

    public int getPort() {
        return this.port;
    }

    public String getUri() {
        return "http://localhost:" + getPort();
    }

    public void start() throws Exception {
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }

}
