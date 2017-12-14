package edu.illinois.library.cantaloupe.http;

import edu.illinois.library.cantaloupe.image.MediaType;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.File;
import java.net.ConnectException;
import java.net.URI;
import java.util.concurrent.ExecutionException;

/**
 * <p>Thinly wraps a Jetty HTTP client with support for HTTP/2 and convenient
 * initialization using the Builder pattern.</p>
 */
public final class Client {

    public static final class Builder {

        private Client clientInstance;

        Builder(Client clientInstance) {
            this.clientInstance = clientInstance;
        }

        public Client build() {
            return clientInstance;
        }

        public Builder contentType(MediaType type) {
            clientInstance.setContentType(type);
            return this;
        }

        public Builder entity(String entity) {
            clientInstance.setEntity(entity);
            return this;
        }

        public Builder followRedirects(boolean followRedirects) {
            clientInstance.setFollowRedirects(followRedirects);
            return this;
        }

        public Builder header(String key, String value) {
            clientInstance.getHeaders().add(key, value);
            return this;
        }

        public Builder method(Method method) {
            clientInstance.setMethod(method);
            return this;
        }

        public Builder realm(String realm) {
            clientInstance.setRealm(realm);
            return this;
        }

        public Builder secret(String secret) {
            clientInstance.setSecret(secret);
            return this;
        }

        public Builder transport(Transport transport) {
            clientInstance.setTransport(transport);
            return this;
        }

        public Builder uri(URI uri) {
            clientInstance.setURI(uri);
            return this;
        }

        public Builder username(String username) {
            clientInstance.setUsername(username);
            return this;
        }

    }

    private HttpClient client;
    private String entity;
    private boolean followRedirects = false;
    private final Headers headers = new Headers();
    private File keyStore;
    private String keyStorePassword = "password";
    private Method method = Method.GET;
    private String realm;
    private String secret;
    private Transport transport = Transport.HTTP1_1;
    private boolean trustAll = false;
    private URI uri;
    private String username;

    public Builder builder() {
        return new Builder(this);
    }

    public Headers getHeaders() {
        return headers;
    }

    private HttpClientTransport getJettyTransport() {
        switch (transport) {
            case HTTP2_0:
                return new HttpClientTransportOverHTTP2(new HTTP2Client());
            default:
                return new HttpClientTransportOverHTTP();
        }
    }

    public Method getMethod() {
        return method;
    }

    public Transport getTransport() {
        return transport;
    }

    public URI getURI() {
        return uri;
    }

    private SslContextFactory newSSLContextFactory() {
        SslContextFactory cf = new SslContextFactory();
        if (trustAll) {
            cf.setTrustAll(true);
        } else if (keyStore != null && keyStorePassword != null) {
            cf.setNeedClientAuth(false);
            cf.setKeyStorePath(keyStore.getAbsolutePath());
            cf.setKeyStorePassword(keyStorePassword);
            cf.setKeyStoreType("JKS");
            cf.setTrustStorePath(keyStore.getAbsolutePath());
            cf.setTrustStorePassword(keyStorePassword);
            cf.setTrustStoreType("JKS");
            cf.setKeyManagerPassword(keyStorePassword);
        }
        return cf;
    }

    public Response send() throws Exception {
        HttpClientTransport transport = getJettyTransport();
        SslContextFactory sslContextFactory = newSSLContextFactory();
        client = new HttpClient(transport, sslContextFactory);
        client.start();
        client.setFollowRedirects(followRedirects);

        // Set Basic auth info
        if (username != null && secret != null) {
            AuthenticationStore auth = client.getAuthenticationStore();
            auth.addAuthentication(new BasicAuthentication(
                    getURI(), realm, username, secret));
        }

        Request request = client.newRequest(getURI());

        // Set method
        request.method(method.toJettyMethod());

        // Add headers
        for (Header header : headers) {
            request.getHeaders().add(header.getName(), header.getValue());
        }

        // Add body
        if (entity != null) {
            request.content(new StringContentProvider(entity));
        }

        ContentResponse response;
        try {
            response = request.send();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ConnectException) {
                throw (Exception) e.getCause();
            }
            throw (Exception) e.getCause();
        }

        if (response != null) {
            if (response.getStatus() >= 400) {
                throw new ResourceException(response);
            }
            return Response.fromJettyResponse(response);
        }
        return null;
    }

    /**
     * Adds a <code>Content-Type</code> header to the {@link #getHeaders()} map.
     */
    public void setContentType(MediaType type) {
        getHeaders().set("Content-Type", type.toString());
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public void setKeyStore(File keyStore) {
        this.keyStore = keyStore;
    }

    public void setKeyStorePassword(String password) {
        this.keyStorePassword = password;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public void setRealm(String realm) {
        this.realm = realm;

    }
    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setTransport(Transport transport) {
        this.transport = transport;
    }

    public void setTrustAll(boolean trustAll) {
        this.trustAll = trustAll;
    }

    public void setURI(URI uri) {
        this.uri = uri;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void stop() throws Exception {
        if (client != null) {
            client.stop();
        }
    }

}