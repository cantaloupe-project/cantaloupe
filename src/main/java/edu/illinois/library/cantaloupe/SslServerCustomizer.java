package edu.illinois.library.cantaloupe;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.ee10.servlet.ServletHandler;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class SslServerCustomizer {
    private Ssl ssl;
    private int idleTimeout;
    private int acceptQueueLimit;

    public SslServerCustomizer(Ssl ssl, int idleTimeout, int acceptQueueLimit) {
        this.ssl = ssl;
        this.idleTimeout = idleTimeout;
        this.acceptQueueLimit = acceptQueueLimit;
    }

    public void customize(Server server) {
        final SslContextFactory.Server contextFactory = new SslContextFactory.Server();
        configureSsl(contextFactory);
        createConnector(server, contextFactory);
	}

        private ServerConnector createConnector(Server server, SslContextFactory.Server sslContextFactory) {
        HttpConfiguration config = new HttpConfiguration();
        config.setUriCompliance(
                UriCompliance.from("DEFAULT,SUSPICIOUS_PATH_CHARACTERS,AMBIGUOUS_PATH_SEPARATOR"));

        config.setSecureScheme("https");
        config.setSecurePort(ssl.getHTTPSPort());
        config.addCustomizer(new SecureRequestCustomizer());

        ServerConnector connector = createHttp2ServerConnector(config, sslContextFactory, server);
        connector.setHost(ssl.getHTTPSHost());
        connector.setPort(ssl.getHTTPSPort());
        connector.setIdleTimeout(idleTimeout);
        connector.setAcceptQueueSize(acceptQueueLimit);

        server.addConnector(connector);
        server.getContainedBeans(ServletHandler.class)
            .forEach(handler -> handler.setDecodeAmbiguousURIs(true));
        return connector;
	}

    private void configureSsl(SslContextFactory.Server contextFactory) {
        contextFactory.setKeyStorePath(ssl.getHTTPSKeyStorePath());
        if (ssl.getHTTPSKeyStorePassword() != null) {
            contextFactory.setKeyStorePassword(ssl.getHTTPSKeyStorePassword());
        }
        if (ssl.getHTTPSKeyPassword() != null) {
            contextFactory.setKeyManagerPassword(ssl.getHTTPSKeyPassword());
        }

    }

    private ServerConnector createHttp2ServerConnector(HttpConfiguration config,
			SslContextFactory.Server sslContextFactory, Server server) {
        HttpConnectionFactory http1 = new HttpConnectionFactory(config);
        HTTP2ServerConnectionFactory http2 = new HTTP2ServerConnectionFactory(config);

        ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
        alpn.setDefaultProtocol(http1.getProtocol());

        sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
        sslContextFactory.setUseCipherSuitesOrder(true);

        SslConnectionFactory connectionFactory =
                new SslConnectionFactory(sslContextFactory, alpn.getProtocol());

        return new ServerConnector(server, connectionFactory, alpn, http2, http1);
	}
}
