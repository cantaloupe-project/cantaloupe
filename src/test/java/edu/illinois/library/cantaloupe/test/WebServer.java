package edu.illinois.library.cantaloupe.test;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.resource.Directory;
import org.restlet.routing.Router;
import org.restlet.routing.Template;

import java.io.File;
import java.io.IOException;

/**
 * HTTP server that serves static content using the fixture path as its root.
 * Call {@link #start()} and then {@link #getUri()} to get its URL.
 */
public class WebServer {

    private static class WebApplication extends Application {

        private File root;

        public WebApplication(File root) {
            super();
            this.root = root;
        }

        /**
         * Creates a root Restlet that will receive all incoming calls.
         */
        @Override
        public Restlet createInboundRoot() {
            final Router router = new Router(getContext());
            router.setDefaultMatchingMode(Template.MODE_EQUALS);
            final Directory dir = new Directory(getContext(),
                    "file://" + root.getAbsolutePath());
            dir.setDeeplyAccessible(true);
            dir.setListingAllowed(false);
            dir.setNegotiatingContent(false);
            router.attach("", dir);
            return router;
        }
    }

    private Component component;
    private int port = TestUtil.getOpenPort();
    private File root;

    /**
     * Initializes a static file HTTP server using the fixture path as its
     * root.
     *
     * @throws IOException
     */
    public WebServer() throws IOException {
        String path = TestUtil.getFixturePath().toAbsolutePath() + "/images";
        this.root = new File(path);
    }

    public WebServer(File root) {
        this.root = root;
    }

    public int getPort() {
        return this.port;
    }

    public String getUri() {
        return "http://localhost:" + getPort();
    }

    public void start() throws Exception {
        component = new Component();
        component.getServers().add(Protocol.HTTP, port);
        component.getClients().add(Protocol.FILE);
        component.getDefaultHost().attach("", new WebApplication(this.root));
        component.start();
    }

    public void stop() throws Exception {
        if (!component.isStopped()) {
            component.stop();
        }
    }

}
