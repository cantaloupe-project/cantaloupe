package edu.illinois.library.cantaloupe.test;

import edu.illinois.library.cantaloupe.util.SocketUtils;
import edu.illinois.library.cantaloupe.util.SystemUtils;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

/**
 * HTTP(S) server that serves static content using the fixture images path as
 * its root. Call {@link #start()} and then {@link #getHTTPURI()} to get its
 * root URI.
 *
 * @see <a href="http://www.eclipse.org/jetty/documentation/current/embedded-examples.html">
 *     Embedded Examples</a>
 */
public class WebServer {

    public static final String BASIC_REALM = "Test Realm";
    public static final String BASIC_USER = "user";
    public static final String BASIC_SECRET = "secret";

    private int httpPort;
    private int httpsPort;
    private boolean isBasicAuthEnabled = false;
    private File root;
    private Server server;

    /**
     * Initializes a static file HTTP(S) server using the image fixture path as
     * its root.
     */
    public WebServer() throws IOException {
        httpPort = SocketUtils.getOpenPort();
        do {
            httpsPort = SocketUtils.getOpenPort();
        } while (httpPort == httpsPort);

        String path = TestUtil.getFixturePath().toAbsolutePath() + "/images";
        this.root = new File(path);
    }

    private void initializeServer() throws IOException {
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

        if (isBasicAuthEnabled) {
            final String[] roles = new String[] { "user" };

            HashLoginService loginService = new HashLoginService(BASIC_REALM);
            UserStore userStore = new UserStore();
            userStore.addUser(BASIC_USER, new Password(BASIC_SECRET), roles);
            loginService.setUserStore(userStore);
            server.addBean(loginService);

            Constraint constraint = new Constraint();
            constraint.setName("auth");
            constraint.setAuthenticate(true);
            constraint.setRoles(roles);

            ConstraintMapping mapping = new ConstraintMapping();
            mapping.setPathSpec("/*");
            mapping.setConstraint(constraint);

            ConstraintSecurityHandler security = new ConstraintSecurityHandler();
            server.setHandler(security);

            security.setConstraintMappings(Collections.singletonList(mapping));
            security.setAuthenticator(new BasicAuthenticator());
            security.setLoginService(loginService);

            security.setHandler(handler);
        } else {
            server.setHandler(handler);
        }
    }

    public int getHTTPPort() {
        return this.httpPort;
    }

    public int getHTTPSPort() {
        return this.httpsPort;
    }

    public URI getHTTPURI() {
        try {
            return new URI("http://localhost:" + getHTTPPort());
        } catch (URISyntaxException e) {
            // This should never happen.
        }
        return null;
    }

    public URI getHTTPSURI() {
        try {
            return new URI("https://localhost:" + getHTTPSPort());
        } catch (URISyntaxException e) {
            // This should never happen.
        }
        return null;
    }

    public void setBasicAuthEnabled(boolean enabled) {
        this.isBasicAuthEnabled = enabled;
    }

    public void start() throws Exception {
        initializeServer();
        server.start();
    }

    public void stop() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

}
