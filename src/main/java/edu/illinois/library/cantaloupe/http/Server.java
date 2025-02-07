package edu.illinois.library.cantaloupe.http;

import edu.illinois.library.cantaloupe.util.SocketUtils;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.security.Constraint.Authorization;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

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

        // HTTP Configuration (if enabled)
        if (isHTTP1Enabled || isHTTP2Enabled) {
            HttpConfiguration httpConfig = new HttpConfiguration();
            HttpConnectionFactory http1 = new HttpConnectionFactory(httpConfig);
            HTTP2CServerConnectionFactory http2c = new HTTP2CServerConnectionFactory(httpConfig);

            ServerConnector httpConnector;
            if (isHTTP1Enabled && isHTTP2Enabled) {
                httpConnector = new ServerConnector(server, http1, http2c);
            } else if (isHTTP1Enabled) {
                httpConnector = new ServerConnector(server, http1);
            } else {
                httpConnector = new ServerConnector(server, http2c);
            }

            httpConnector.setPort(httpPort);
            server.addConnector(httpConnector);
        }

        // HTTPS Configuration (if enabled)
        if (isHTTPS1Enabled || isHTTPS2Enabled) {
            HttpConfiguration httpsConfig = new HttpConfiguration();
            httpsConfig.setSecureScheme("https");
            httpsConfig.addCustomizer(new SecureRequestCustomizer());

            SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
            sslContextFactory.setKeyStorePath(keyStorePath.toString());
            sslContextFactory.setKeyStorePassword(keyStorePassword);
            sslContextFactory.setKeyManagerPassword(keyManagerPassword);

            HttpConnectionFactory http1 = new HttpConnectionFactory(httpsConfig);
            HTTP2ServerConnectionFactory http2 = new HTTP2ServerConnectionFactory(httpsConfig);
            ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
            alpn.setDefaultProtocol(http1.getProtocol()); // Set default protocol for ALPN

            sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
            sslContextFactory.setUseCipherSuitesOrder(true);

            SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());


            ServerConnector httpsConnector;
            if (isHTTPS1Enabled && isHTTPS2Enabled) {
                httpsConnector = new ServerConnector(server, sslConnectionFactory, alpn, http2, http1);
            } else if (isHTTPS2Enabled) {
                httpsConnector = new ServerConnector(server, sslConnectionFactory, alpn, http2);
            } else {
                httpsConnector = new ServerConnector(server, sslConnectionFactory, http1);
            }

            httpsConnector.setPort(httpsPort);
            server.addConnector(httpsConnector);
        }


        // Default Resource Handler (if no custom handler is set)
        if (handler == null) {
            ResourceHandler resourceHandler = new ResourceHandler();
            resourceHandler.setDirAllowed(false);
            resourceHandler.setAcceptRanges(isAcceptingRanges);
            resourceHandler.setBaseResourceAsString(root.toString());
            handler = resourceHandler; // Assign the resource handler
        }


         // Security Handler (if Basic Auth is enabled)
        if (isBasicAuthEnabled) {
            final String[] roles = new String[] { "user" };
            HashLoginService loginService = new HashLoginService(authRealm);
            UserStore userStore = new UserStore();
            userStore.addUser(authUser, new Password(authSecret), roles);
            loginService.setUserStore(userStore);
            server.addBean(loginService);

            Constraint constraint = Constraint.from("auth", Authorization.KNOWN_ROLE, roles);
            SecurityHandler.PathMapped securityHandler = new SecurityHandler.PathMapped();
            securityHandler.put("/*", constraint);
            securityHandler.setAuthenticator(new BasicAuthenticator());   
            securityHandler.setLoginService(loginService); 
            securityHandler.setHandler(handler);
            server.setHandler(securityHandler);

        } else {
            // Set the handler directly if no authentication
            ContextHandler contextHandler = new ContextHandler("/");
            contextHandler.setHandler(handler);
            server.setHandler(contextHandler);
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
