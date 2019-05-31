package edu.illinois.library.cantaloupe.http;

import edu.illinois.library.cantaloupe.image.MediaType;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;

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

    public Client() {
        // The default is 3 in JDK 11
        System.setProperty("jdk.httpclient.auth.retrylimit", "0");
    }

    public Builder builder() {
        return new Builder(this);
    }

    public Headers getHeaders() {
        return headers;
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

    public Response send() throws Exception {
        if (client == null) {
            client = newClient();
        }
        HttpRequest request = newRequest();
        try {
            HttpResponse<byte[]> response = client.send(
                    request, HttpResponse.BodyHandlers.ofByteArray());
            Response customResponse = Response.fromHttpClientResponse(response);
            if (customResponse.getStatus() >= 400) {
                throw new ResourceException(customResponse);
            }
            return customResponse;
        } catch (IOException e) {
            if (e.getMessage().startsWith("too many authentication attempts")) {
                Response customResponse = new Response();
                customResponse.setStatus(401);
                throw new ResourceException(customResponse);
            } else {
                throw e;
            }
        }
    }

    private HttpClient newClient() {
        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .followRedirects(followRedirects ?
                        HttpClient.Redirect.ALWAYS : HttpClient.Redirect.NEVER)
                .version(Transport.HTTP1_1.equals(getTransport()) ?
                        HttpClient.Version.HTTP_1_1 : HttpClient.Version.HTTP_2);

        if (trustAll) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new X509TrustManager[]{
                        new X509TrustManager() {
                            public void checkClientTrusted(X509Certificate[] chain,
                                                           String authType) {
                            }

                            public void checkServerTrusted(X509Certificate[] chain,
                                                           String authType) {
                            }

                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }
                        }}, new SecureRandom());
                clientBuilder.sslContext(sslContext);
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        // Set Basic auth info
        if (username != null && secret != null) {
            clientBuilder.authenticator(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, secret.toCharArray());
                }
            });
        }
        return clientBuilder.build();
    }

    private HttpRequest newRequest() {
        // Assemble body
        HttpRequest.BodyPublisher pub = HttpRequest.BodyPublishers.noBody();
        if (entity != null) {
            pub = HttpRequest.BodyPublishers.ofString(entity);
        }

        var requestBuilder = HttpRequest.newBuilder()
                .method(method.toString(), pub)
                .uri(uri);

        // Send the authorization header preemptively without a challenge,
        // which the client does not do by default as of JDK 11.
        if (username != null && secret != null) {
            requestBuilder.header("Authorization", basicAuthToken());
        }

        // Add headers
        for (Header header : headers) {
            requestBuilder.setHeader(header.getName(), header.getValue());
        }
        return requestBuilder.build();
    }

    private String basicAuthToken() {
        return "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + secret).getBytes());
    }

    /**
     * Adds a {@code Content-Type} header to the {@link #getHeaders()} map.
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
        client = null;
        this.transport = transport;
    }

    public void setTrustAll(boolean trustAll) {
        client = null;
        this.trustAll = trustAll;
    }

    public void setURI(URI uri) {
        this.uri = uri;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * This is currently a no-op, but clients should call it anyway in case the
     * wrapped client ever changes to one that requires manual stoppage.
     */
    public void stop() throws Exception {
    }

}