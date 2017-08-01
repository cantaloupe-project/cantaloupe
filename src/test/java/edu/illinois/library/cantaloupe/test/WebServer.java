package edu.illinois.library.cantaloupe.test;

import edu.illinois.library.cantaloupe.util.SystemUtils;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.File;
import java.io.IOException;

/**
 * HTTP(S) server that serves static content using the fixture path as its root.
 * Call {@link #start()} and then {@link #getHTTPURI()} to get its root URL.
 *
 * @see <a href="http://www.eclipse.org/jetty/documentation/current/embedded-examples.html">
 *     Embedded Examples</a>
 */
public class WebServer {

    private int httpPort;
    private int httpsPort;
    private File root;
    private Server server;

    /**
     * Initializes a static file HTTP(S) server using the fixture path as its
     * root.
     *
     * @throws IOException
     */
    public WebServer() throws IOException {
        httpPort = TestUtil.getOpenPort();
        do {
            httpsPort = TestUtil.getOpenPort();
        } while (httpPort == httpsPort);

        String path = TestUtil.getFixturePath().toAbsolutePath() + "/images";
        this.root = new File(path);
        server = new Server();

        // Initialize the HTTP connector.
        ServerConnector connector;
        HttpConfiguration config = new HttpConfiguration();
        HttpConnectionFactory http1 =
                new HttpConnectionFactory(config);

        HTTP2CServerConnectionFactory http2c =
                new HTTP2CServerConnectionFactory(config);
        connector = new ServerConnector(server, http1, http2c);
        connector.setPort(httpPort);
        server.addConnector(connector);

        // Initialize the HTTPS connector.
        config = new HttpConfiguration();
        config.setSecureScheme("https");
        config.addCustomizer(new SecureRequestCustomizer());

        final SslContextFactory contextFactory = new SslContextFactory();
        contextFactory.setKeyStorePath(
                TestUtil.getFixture("keystore.jks").getAbsolutePath());
        contextFactory.setKeyStorePassword("password");
        contextFactory.setKeyManagerPassword("password");

        if (SystemUtils.isALPNAvailable()) {
            http1 = new HttpConnectionFactory(config);
            HTTP2ServerConnectionFactory http2 =
                    new HTTP2ServerConnectionFactory(config);

            ALPNServerConnectionFactory alpn =
                    new ALPNServerConnectionFactory();
            alpn.setDefaultProtocol(http1.getProtocol());

            contextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
            contextFactory.setUseCipherSuitesOrder(true);

            SslConnectionFactory connectionFactory =
                    new SslConnectionFactory(contextFactory,
                            alpn.getProtocol());

            connector = new ServerConnector(server, connectionFactory,
                    alpn, http2, http1);
        } else {
            connector = new ServerConnector(server,
                    new SslConnectionFactory(contextFactory, "HTTP/1.1"),
                    new HttpConnectionFactory(config));
        }

        connector.setPort(httpsPort);
        server.addConnector(connector);

        // Initialize the static file server.
        ResourceHandler handler = new ResourceHandler();
        handler.setDirectoriesListed(false);
        handler.setResourceBase(root.getAbsolutePath());

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { handler, new DefaultHandler() });
        server.setHandler(handlers);
    }

    public int getHTTPPort() {
        return this.httpPort;
    }

    public int getHTTPSPort() {
        return this.httpsPort;
    }

    public String getHTTPURI() {
        return "http://localhost:" + getHTTPPort();
    }

    public String getHTTPSURI() {
        return "https://localhost:" + getHTTPSPort();
    }

    public void start() throws Exception {
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }

}
