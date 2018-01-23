package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>Provides access to source content located on an HTTP(S) server.</p>
 *
 * <h1>Protocol Support</h1>
 *
 * <p>HTTP/1.1 and HTTPS/1.1 are supported.</p>
 *
 * <p>HTTP/2 is not supported.</p>
 *
 * <h1>Format Determination</h1>
 *
 * <p>For images with recognized name extensions, the format will be inferred
 * by {@link Format#inferFormat(Identifier)}. For images with unrecognized or
 * missing extensions, the <code>Content-Type</code> header will be checked to
 * determine their format, which will incur an extra <code>HEAD</code> request.
 * It is therefore more efficient to serve images with extensions.</p>
 *
 * <h1>Lookup Strategies</h1>
 *
 * <p>Two distinct lookup strategies are supported, defined by
 * {@link Key#HTTPRESOLVER_LOOKUP_STRATEGY}. BasicLookupStrategy locates
 * images by concatenating a pre-defined URL prefix and/or suffix.
 * ScriptLookupStrategy invokes a delegate method to retrieve a URL (and
 * optional auth info) dynamically.</p>
 *
 * <h1>Authentication Support</h1>
 *
 * <p>HTTP Basic authentication is supported.</p>
 *
 * <ul>
 *     <li>When using BasicLookupStrategy, auth info is set globally in the
 *     {@link Key#HTTPRESOLVER_BASIC_AUTH_USERNAME} and
 *     {@link Key#HTTPRESOLVER_BASIC_AUTH_SECRET} configuration keys.</li>
 *     <li>When using ScriptLookupStrategy, auth info can be returned from a
 *     delegate method.</li>
 * </ul>
 *
 * @see <a href="http://www.eclipse.org/jetty/documentation/current/http-client.html">
 *     Jetty HTTP Client</a>
 */
class HttpResolver extends AbstractResolver implements StreamResolver {

    private static class HTTPStreamSource implements StreamSource {

        private final HttpClient client;
        private final URI uri;

        HTTPStreamSource(HttpClient client, URI uri) {
            this.client = client;
            this.uri = uri;
        }

        @Override
        public ImageInputStream newImageInputStream() throws IOException {
            return ImageIO.createImageInputStream(newInputStream());
        }

        @Override
        public InputStream newInputStream() {
            try {
                InputStreamResponseListener listener =
                        new InputStreamResponseListener();
                client.newRequest(uri).
                        timeout(getRequestTimeout(), TimeUnit.SECONDS).
                        method(HttpMethod.GET).
                        send(listener);

                // Wait for the response headers to arrive.
                Response response = listener.get(getRequestTimeout(),
                        TimeUnit.SECONDS);

                if (response.getStatus() == HttpStatus.OK_200) {
                    return listener.getInputStream();
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
            return null;
        }

    }

    static class ResourceInfo {

        private URI uri;
        private String username;
        private String secret;

        ResourceInfo(URI uri) {
            this.uri = uri;
        }

        ResourceInfo(URI uri, String username, String secret) {
            this(uri);
            this.username = username;
            this.secret = secret;
        }

        String getSecret() {
            return secret;
        }

        URI getURI() {
            return uri;
        }

        String getUsername() {
            return username;
        }

        @Override
        public String toString() {
            return getURI() + "";
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HttpResolver.class);

    private static final String GET_URL_DELEGATE_METHOD =
            "HttpResolver::get_url";

    private static HttpClient jettyClient;

    /**
     * Cached HTTP HEAD response.
     */
    private Response headResponse;

    /**
     * Lazy-loaded by {@link #getResourceInfo()}.
     */
    private ResourceInfo resourceInfo;

    private static synchronized HttpClient getHTTPClient(ResourceInfo info) {
        if (jettyClient == null) {
            HttpClientTransport transport = new HttpClientTransportOverHTTP();

            Configuration config = Configuration.getInstance();
            final boolean trustInvalidCerts = config.getBoolean(
                    Key.HTTPRESOLVER_TRUST_ALL_CERTS, false);
            SslContextFactory sslContextFactory =
                    new SslContextFactory(trustInvalidCerts);

            jettyClient = new HttpClient(transport, sslContextFactory);
            jettyClient.setFollowRedirects(true);
            jettyClient.setUserAgentField(new HttpField("User-Agent", getUserAgent()));

            try {
                jettyClient.start();
            } catch (Exception e) {
                LOGGER.error("getHTTPClient(): {}", e.getMessage());
            }
        }

        // Add Basic auth credentials to the authentication store.
        // https://www.eclipse.org/jetty/documentation/9.4.x/http-client-authentication.html
        if (info.getUsername() != null && info.getSecret() != null) {
            AuthenticationStore auth = jettyClient.getAuthenticationStore();
            auth.addAuthenticationResult(new BasicAuthentication.BasicResult(
                    info.getURI(), info.getUsername(), info.getSecret()));
        }
        return jettyClient;
    }

    /**
     * @return Request timeout from the application configuration, or a
     *         reasonable default if not set.
     */
    private static int getRequestTimeout() {
        return Configuration.getInstance().
                getInt(Key.HTTPRESOLVER_REQUEST_TIMEOUT, 10);
    }

    private static String getUserAgent() {
        return String.format("%s/%s (%s/%s; java/%s; %s/%s)",
                HttpResolver.class.getSimpleName(),
                Application.getVersion(),
                Application.NAME,
                Application.getVersion(),
                System.getProperty("java.version"),
                System.getProperty("os.name"),
                System.getProperty("os.version"));
    }

    @Override
    public void checkAccess() throws IOException {
        Response response = retrieveHEADResponse();
        if (response.getStatus() >= HttpStatus.BAD_REQUEST_400) {
            final String statusLine = "HTTP " + headResponse.getStatus() +
                    ": " + headResponse.getReason();

            if (response.getStatus() == HttpStatus.NOT_FOUND_404
                    || response.getStatus() == HttpStatus.GONE_410) {
                throw new NoSuchFileException(statusLine);
            } else if (response.getStatus() == HttpStatus.UNAUTHORIZED_401
                    || response.getStatus() == HttpStatus.FORBIDDEN_403) {
                throw new AccessDeniedException(statusLine);
            } else {
                throw new IOException(statusLine);
            }
        }
    }

    @Override
    public Format getSourceFormat() {
        if (sourceFormat == null) {
            sourceFormat = Format.inferFormat(identifier);
            if (Format.UNKNOWN.equals(sourceFormat)) {
                sourceFormat = inferSourceFormatFromContentTypeHeader();
            }
        }
        return sourceFormat;
    }

    /**
     * Issues an HTTP <code>HEAD</code> request and checks the response
     * <code>Content-Type</code> header to determine the source format.
     *
     * @return Inferred source format, or {@link Format#UNKNOWN} if unknown.
     */
    private Format inferSourceFormatFromContentTypeHeader() {
        Format format = Format.UNKNOWN;
        try {
            final ResourceInfo info = getResourceInfo();
            final Response response = retrieveHEADResponse();

            if (response.getStatus() >= 200 && response.getStatus() < 300) {
                HttpField field = response.getHeaders().getField("Content-Type");
                if (field != null && field.getValue() != null) {
                    format = new MediaType(field.getValue()).toFormat();
                    if (Format.UNKNOWN.equals(format)) {
                        LOGGER.warn("Unrecognized Content-Type header value for HEAD {}",
                                info.getURI());
                    }
                } else {
                    LOGGER.warn("No Content-Type header for HEAD {}",
                            info.getURI());
                }
            } else {
                LOGGER.warn("HEAD returned status {} for {}",
                        response.getStatus(), info.getURI());
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return format;
    }


    @Override
    public StreamSource newStreamSource() throws IOException {
        ResourceInfo info;
        try {
            info = getResourceInfo();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("newStreamSource(): {}", e.getMessage());
            throw new IOException(e.getMessage(), e);
        }

        if (info != null) {
            LOGGER.info("Resolved {} to {}", identifier, info.getURI());
            return new HTTPStreamSource(getHTTPClient(info), info.getURI());
        }
        return null;
    }

    private Response retrieveHEADResponse() throws IOException {
        if (headResponse == null) {
            ResourceInfo info;
            try {
                info = getResourceInfo();
            } catch (InterruptedException | TimeoutException e) {
                LOGGER.error(e.getMessage(), e);
                throw new IOException(e.getMessage(), e);
            } catch (Exception e) {
                LOGGER.error("retrieveHEADResponse(): {}", e.getMessage());
                throw new IOException(e.getMessage(), e);
            }

            try {
                final HttpClient client = getHTTPClient(info);

                InputStreamResponseListener listener =
                        new InputStreamResponseListener();
                client.newRequest(info.getURI()).
                        timeout(getRequestTimeout(), TimeUnit.SECONDS).
                        method(HttpMethod.HEAD).send(listener);

                // Wait for the response headers to arrive.
                headResponse = listener.get(getRequestTimeout(),
                        TimeUnit.SECONDS);
            } catch (ExecutionException e ) {
                throw new AccessDeniedException(info.getURI().toString());
            } catch (InterruptedException | TimeoutException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
        return headResponse;
    }

    /**
     * @return Info corresponding to {@link #identifier}. The result is cached.
     */
    ResourceInfo getResourceInfo() throws Exception {
        if (resourceInfo == null) {
            final LookupStrategy strategy =
                    LookupStrategy.from(Key.HTTPRESOLVER_LOOKUP_STRATEGY);
            switch (strategy) {
                case DELEGATE_SCRIPT:
                    resourceInfo = getResourceInfoUsingScriptStrategy();
                    break;
                default:
                    resourceInfo = getResourceInfoUsingBasicStrategy();
                    break;
            }
        }
        return resourceInfo;
    }

    private ResourceInfo getResourceInfoUsingBasicStrategy()
            throws ConfigurationException {
        final Configuration config = Configuration.getInstance();
        final String prefix = config.getString(Key.HTTPRESOLVER_URL_PREFIX, "");
        final String suffix = config.getString(Key.HTTPRESOLVER_URL_SUFFIX, "");
        try {
            return new ResourceInfo(
                    new URI(prefix + identifier.toString() + suffix),
                    config.getString(Key.HTTPRESOLVER_BASIC_AUTH_USERNAME),
                    config.getString(Key.HTTPRESOLVER_BASIC_AUTH_SECRET));
        } catch (URISyntaxException e) {
            throw new ConfigurationException(e.getMessage());
        }
    }

    /**
     * @throws NoSuchFileException  If the remote resource was not found.
     * @throws URISyntaxException   If {@link #GET_URL_DELEGATE_METHOD}
     *                              returns an invalid URI.
     * @throws IOException
     * @throws ScriptException      If the delegate method throws an exception.
     * @throws DelegateScriptDisabledException
     */
    private ResourceInfo getResourceInfoUsingScriptStrategy()
            throws URISyntaxException, IOException, ScriptException,
            DelegateScriptDisabledException {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final Object result = engine.invoke(GET_URL_DELEGATE_METHOD,
                identifier.toString(), context.asMap());
        if (result == null) {
            throw new NoSuchFileException(GET_URL_DELEGATE_METHOD +
                    " returned nil for " + identifier);
        }
        // The return value may be a string URI, or a hash with "uri",
        // "username", and "secret" keys.
        if (result instanceof String) {
            return new ResourceInfo(new URI((String) result));
        } else {
            @SuppressWarnings("unchecked")
            final Map<String,String> info = (Map<String,String>) result;
            final String uri = info.get("uri");
            final String username = info.get("username");
            final String secret = info.get("secret");
            return new ResourceInfo(new URI(uri), username, secret);
        }
    }

    @Override
    public void setIdentifier(Identifier identifier) {
        headResponse = null;
        resourceInfo = null;
        sourceFormat = null;
        this.identifier = identifier;
    }

}
