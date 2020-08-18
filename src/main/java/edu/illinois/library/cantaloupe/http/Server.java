package edu.illinois.library.cantaloupe.http;

import edu.illinois.library.cantaloupe.util.SocketUtils;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;

/**
 * <p>Simple HTTP server wrapping a Jetty server. Supports HTTP and HTTPS and
 * protocol versions 1.1 and 2.</p>
 *
 * <p>The default handler serves static filesystem content, but can be
 * overridden via {@link #setHandler(Handler)}.</p>
 *
 * @see <a href="http://www.eclipse.org/jetty/documentation/current/embedded-examples.html">
 *     Embedded Examples</a>
 */
public final class Server {

    private boolean isAcceptingRanges = true;
    private boolean isBasicAuthEnabled;
    private String authRealm;
    private String authUser;
    private String authSecret;

    private Handler handler;
    private int httpPort;
    private int httpsPort;
    private boolean isHTTP1Enabled = true;
    private boolean isHTTP2Enabled = true;
    private boolean isHTTPS1Enabled = false;
    private boolean isHTTPS2Enabled = false;
    private String keyManagerPassword;
    private String keyStorePassword;
    private Path keyStorePath;
    private Path root;
    private org.eclipse.jetty.server.Server server;

    /**
     * Initializes a static file HTTP(S) server using the image fixture path as
     * its root.
     */
    public Server() {
        httpPort = SocketUtils.getOpenPort();
        do {
            httpsPort = SocketUtils.getOpenPort();
        } while (httpPort == httpsPort);
    }

    private void initializeServer() {
        server = new org.eclipse.jetty.server.Server();

        ServerConnector connector;
        HttpConfiguration config = new HttpConfiguration();

        HttpConnectionFactory http1 = new HttpConnectionFactory(config);
        HTTP2CServerConnectionFactory http2c =
                new HTTP2CServerConnectionFactory(config);

        // Initialize HTTP/H2C.
        if (isHTTP1Enabled || isHTTP2Enabled) {
            if (isHTTP1Enabled && isHTTP2Enabled) {
                connector = new ServerConnector(server, http1, http2c);
            } else if (isHTTP1Enabled) {
                connector = new ServerConnector(server, http1);
            } else {
                connector = new ServerConnector(server, http2c);
            }

            connector.setPort(httpPort);
            server.addConnector(connector);
        }

        // Initialize HTTPS.
        if (isHTTPS1Enabled || isHTTPS2Enabled) {
            config = new HttpConfiguration();
            config.setSecureScheme("https");
            config.addCustomizer(new SecureRequestCustomizer());

            final SslContextFactory contextFactory =
                    new SslContextFactory.Server();
            contextFactory.setKeyStorePath(keyStorePath.toString());
            contextFactory.setKeyStorePassword(keyStorePassword);
            contextFactory.setKeyManagerPassword(keyManagerPassword);

            http1 = new HttpConnectionFactory(config);
            HTTP2ServerConnectionFactory http2 =
                    new HTTP2ServerConnectionFactory(config);

            if (isHTTPS1Enabled && isHTTPS2Enabled) {
                ALPNServerConnectionFactory alpn =
                        new ALPNServerConnectionFactory();
                alpn.setDefaultProtocol(http1.getProtocol());

                contextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
                contextFactory.setUseCipherSuitesOrder(true);

                SslConnectionFactory connectionFactory =
                        new SslConnectionFactory(contextFactory,
                                alpn.getProtocol());

                connector = new ServerConnector(server, connectionFactory, alpn,
                        http2, http1);
            } else if (isHTTPS2Enabled) {
                ALPNServerConnectionFactory alpn =
                        new ALPNServerConnectionFactory();
                alpn.setDefaultProtocol(http1.getProtocol());

                contextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
                contextFactory.setUseCipherSuitesOrder(true);

                SslConnectionFactory connectionFactory =
                        new SslConnectionFactory(contextFactory,
                                alpn.getProtocol());

                connector = new ServerConnector(server, connectionFactory, alpn,
                        http2);
            } else {
                connector = new ServerConnector(server,
                        new SslConnectionFactory(contextFactory, "HTTP/1.1"),
                        new HttpConnectionFactory(config));
            }

            connector.setPort(httpsPort);
            server.addConnector(connector);
        }

        // If a custom handler has not been set, use a static file server.
        if (handler == null) {
            ResourceHandler handler = new ResourceHandler();
            handler.setDirectoriesListed(false);
            handler.setAcceptRanges(isAcceptingRanges);
            handler.setResourceBase(root.toString());
            this.handler = handler;
        }

        if (isBasicAuthEnabled) {
            final String[] roles = new String[] { "user" };

            HashLoginService loginService = new HashLoginService(authRealm);
            UserStore userStore = new UserStore();
            userStore.addUser(authUser, new Password(authSecret), roles);
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

    public URI getHTTPURI() {
        try {
            return new URI("http://localhost:" + httpPort);
        } catch (URISyntaxException e) {
            // This should never happen.
        }
        return null;
    }

    public URI getHTTPSURI() {
        try {
            return new URI("https://localhost:" + httpsPort);
        } catch (URISyntaxException e) {
            // This should never happen.
        }
        return null;
    }

    public void setAcceptingRanges(boolean isAcceptingRanges) {
        this.isAcceptingRanges = isAcceptingRanges;
    }

    public void setAuthRealm(String realm) {
        this.authRealm = realm;
    }

    public void setAuthSecret(String secret) {
        this.authSecret = secret;
    }

    public void setAuthUser(String user) {
        this.authUser = user;
    }

    public void setBasicAuthEnabled(boolean enabled) {
        this.isBasicAuthEnabled = enabled;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void setHTTP1Enabled(boolean enabled) {
        this.isHTTP1Enabled = enabled;
    }

    public void setHTTP2Enabled(boolean enabled) {
        this.isHTTP2Enabled = enabled;
    }

    public void setHTTPS1Enabled(boolean enabled) {
        this.isHTTPS1Enabled = enabled;
    }

    public void setHTTPS2Enabled(boolean enabled) {
        this.isHTTPS2Enabled = enabled;
    }

    public void setKeyManagerPassword(String password) {
        this.keyManagerPassword = password;
    }

    public void setKeyStorePassword(String password) {
        this.keyStorePassword = password;
    }

    public void setKeyStorePath(Path path) {
        this.keyStorePath = path;
    }

    public void setRoot(Path root) {
        this.root = root;
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
