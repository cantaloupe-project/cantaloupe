package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.script.DelegateMethod;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
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

import javax.script.ScriptException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.util.List;
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
 * <h1>Format Inference</h1>
 *
 * <p>See {@link #getFormat()}.</p>
 *
 * <h1>Lookup Strategies</h1>
 *
 * <p>Two distinct lookup strategies are supported, defined by
 * {@link Key#HTTPSOURCE_LOOKUP_STRATEGY}. {@link LookupStrategy#BASIC}
 * locates images by concatenating a pre-defined URL prefix and/or suffix.
 * {@link LookupStrategy#DELEGATE_SCRIPT} invokes a delegate method to
 * retrieve a URL (and optional auth info) dynamically.</p>
 *
 * <h1>Authentication Support</h1>
 *
 * <p>HTTP Basic authentication is supported.</p>
 *
 * <ul>
 *     <li>When using {@link LookupStrategy#BASIC}, auth info is set globally
 *     in the {@link Key#HTTPSOURCE_BASIC_AUTH_USERNAME} and
 *     {@link Key#HTTPSOURCE_BASIC_AUTH_SECRET} configuration keys.</li>
 *     <li>When using {@link LookupStrategy#DELEGATE_SCRIPT}, auth info can be
 *     returned from the delegate method.</li>
 * </ul>
 *
 * @see <a href="http://www.eclipse.org/jetty/documentation/current/http-client.html">
 *     Jetty HTTP Client</a>
 */
class HttpSource extends AbstractSource implements StreamSource {

    private static class HTTPStreamFactory implements StreamFactory {

        private final HttpClient client;
        private final URI uri;

        HTTPStreamFactory(HttpClient client, URI uri) {
            this.client = client;
            this.uri = uri;
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
            LoggerFactory.getLogger(HttpSource.class);

    private static final int DEFAULT_REQUEST_TIMEOUT = 30;

    /**
     * Byte length of the range used to infer the source image format.
     */
    private static final int FORMAT_INFERENCE_RANGE_LENGTH = 32;

    private static HttpClient jettyClient;

    private InputStreamResponseListener rangedGETResponseListener;

    /**
     * Cached HTTP {@literal GET} response with a maximum body length of {@link
     * #FORMAT_INFERENCE_RANGE_LENGTH}.
     */
    private Response rangedGETResponse;

    /**
     * Lazy-loaded by {@link #getResourceInfo()}.
     */
    private ResourceInfo resourceInfo;

    private static synchronized HttpClient getHTTPClient(ResourceInfo info) {
        if (jettyClient == null) {
            HttpClientTransport transport = new HttpClientTransportOverHTTP();

            Configuration config = Configuration.getInstance();
            final boolean trustInvalidCerts = config.getBoolean(
                    Key.HTTPSOURCE_TRUST_ALL_CERTS, false);
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
        return Configuration.getInstance().getInt(
                Key.HTTPSOURCE_REQUEST_TIMEOUT,
                DEFAULT_REQUEST_TIMEOUT);
    }

    private static String getUserAgent() {
        return String.format("%s/%s (%s/%s; java/%s; %s/%s)",
                HttpSource.class.getSimpleName(),
                Application.getVersion(),
                Application.getName(),
                Application.getVersion(),
                System.getProperty("java.version"),
                System.getProperty("os.name"),
                System.getProperty("os.version"));
    }

    @Override
    public void checkAccess() throws IOException {
        Response response = retrieveRangedGETResponse();

        if (response.getStatus() >= HttpStatus.BAD_REQUEST_400) {
            final String statusLine = "HTTP " + rangedGETResponse.getStatus() +
                    ": " + rangedGETResponse.getReason();

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

    /**
     * <ol>
     *     <li>If the path component of the URI contains a recognized filename
     *     extension, the format is inferred from that.</li>
     *     <li>Otherwise, if the identifier contains a recognized filename
     *     extension, the format is inferred from that.</li>
     *     <li>Otherwise, a {@literal GET} request is sent with a {@literal
     *     Range} header specifying a small range of data from the beginning of
     *     the resource.
     *         <ol>
     *             <li>If a {@literal Content-Type} header is present in the
     *             response, and its value is specific enough (i.e. not
     *             {@literal application/octet-stream}), a format is inferred
     *             from it.</li>
     *             <li>Otherwise, a format is inferred from the magic bytes in
     *             the response entity.</li>
     *         </ol>
     *     </li>
     * </ol>
     *
     * @return Best attempt at determining the format.
     */
    @Override
    public Format getFormat() {
        if (format == null) {
            // Try to infer a format from the path component of the URI.
            try {
                format = Format.inferFormat(
                        getResourceInfo().getURI().getPath());
            } catch (Exception ignore) {
                // This is better caught and handled elsewhere.
            }

            if (Format.UNKNOWN.equals(format)) {
                // Try to infer a format from the identifier.
                format = Format.inferFormat(identifier);
            }

            if (Format.UNKNOWN.equals(format)) {
                // Try to infer a format from the magic bytes.
                format = inferSourceFormatFromResponse();
            }
        }
        return format;
    }

    /**
     * Issues an HTTP {@literal GET} request for a small {@literal Range} of
     * the beginning of the resource and checks for the response {@literal
     * Content-Type} header. If it is not present or specific enough (i.e.
     * {@literal application/octet-stream}), the magic bytes in the response
     * body are checked.
     *
     * @return Inferred source format, or {@link Format#UNKNOWN} if unknown.
     */
    private Format inferSourceFormatFromResponse() {
        Format format = Format.UNKNOWN;
        try {
            final ResourceInfo info = getResourceInfo();
            final Response response = retrieveRangedGETResponse();

            if (response.getStatus() >= 200 && response.getStatus() < 300) {
                HttpField field = response.getHeaders().getField("Content-Type");

                if (field != null && field.getValue() != null) {
                    format = mediaTypeFromContentType(field.getValue()).toFormat();

                    if (Format.UNKNOWN.equals(format)) {
                        LOGGER.debug("Unrecognized Content-Type header value for GET {}",
                                info.getURI());
                    }
                } else {
                    LOGGER.debug("No Content-Type header for GET {}",
                            info.getURI());
                }

                if (Format.UNKNOWN.equals(format)) {
                    LOGGER.debug("Attempting to infer format from magic bytes for {}",
                            info.getURI());
                    try (InputStream bodyStream =
                                 new BufferedInputStream(rangedGETResponseListener.getInputStream())) {
                        List<MediaType> types =
                                MediaType.detectMediaTypes(bodyStream);
                        if (!types.isEmpty()) {
                            format = types.get(0).toFormat();
                            LOGGER.debug("Inferred {} format from magic bytes for {}",
                                    format, info.getURI());
                        } else {
                            LOGGER.debug("Unable to infer a format from magic bytes for GET {}",
                                    info.getURI());
                        }
                    }
                }
            } else {
                LOGGER.warn("GET {} returned status {}",
                        info.getURI(), response.getStatus());
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return format;
    }

    /**
     * @param contentType {@literal Content-Type} header value.
     * @return Media type corresponding to the given header value.
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.1.5">RFC
     * 7231</a>
     */
    static MediaType mediaTypeFromContentType(String contentType) {
        String[] parts = contentType.split(";");
        if (parts.length > 0) {
            return new MediaType(parts[0].trim());
        }
        throw new IllegalArgumentException("Unrecognized Content-Type");
    }

    @Override
    public StreamFactory newStreamFactory() throws IOException {
        ResourceInfo info;
        try {
            info = getResourceInfo();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("newStreamFactory(): {}", e.getMessage());
            throw new IOException(e.getMessage(), e);
        }

        if (info != null) {
            LOGGER.info("Resolved {} to {}", identifier, info.getURI());
            return new HTTPStreamFactory(getHTTPClient(info), info.getURI());
        }
        return null;
    }

    /**
     * Issues a {@literal GET} request specifying a small range of data and
     * caches the result in {@link #rangedGETResponse}.
     */
    private Response retrieveRangedGETResponse() throws IOException {
        if (rangedGETResponse == null) {
            ResourceInfo info;
            try {
                info = getResourceInfo();
            } catch (InterruptedException | TimeoutException e) {
                LOGGER.error(e.getMessage(), e);
                throw new IOException(e.getMessage(), e);
            } catch (Exception e) {
                LOGGER.error("retrieveRangedGETResponse(): {}", e.getMessage());
                throw new IOException(e.getMessage(), e);
            }

            try {
                final HttpClient client = getHTTPClient(info);

                rangedGETResponseListener = new InputStreamResponseListener();

                client.newRequest(info.getURI())
                        .timeout(getRequestTimeout(), TimeUnit.SECONDS)
                        .header("Range", "bytes=0-" + (FORMAT_INFERENCE_RANGE_LENGTH - 1))
                        .method(HttpMethod.GET)
                        .send(rangedGETResponseListener);

                rangedGETResponse = rangedGETResponseListener.get(
                        getRequestTimeout(), TimeUnit.SECONDS);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Malformed URI: " + info.getURI());
            } catch (ExecutionException e ) {
                throw new AccessDeniedException(info.getURI().toString());
            } catch (InterruptedException | TimeoutException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
        return rangedGETResponse;
    }

    /**
     * @return Info corresponding to {@link #identifier}. The result is cached.
     */
    ResourceInfo getResourceInfo() throws Exception {
        if (resourceInfo == null) {
            final LookupStrategy strategy =
                    LookupStrategy.from(Key.HTTPSOURCE_LOOKUP_STRATEGY);
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
        final String prefix = config.getString(Key.HTTPSOURCE_URL_PREFIX, "");
        final String suffix = config.getString(Key.HTTPSOURCE_URL_SUFFIX, "");
        try {
            return new ResourceInfo(
                    new URI(prefix + identifier.toString() + suffix),
                    config.getString(Key.HTTPSOURCE_BASIC_AUTH_USERNAME),
                    config.getString(Key.HTTPSOURCE_BASIC_AUTH_SECRET));
        } catch (URISyntaxException e) {
            throw new ConfigurationException(e.getMessage());
        }
    }

    /**
     * @throws NoSuchFileException if the remote resource was not found.
     * @throws URISyntaxException  if {@link
     *                             DelegateMethod#HTTPSOURCE_RESOURCE_INFO}
     *                             returns an invalid URI.
     * @throws ScriptException     if the delegate method throws an exception.
     */
    private ResourceInfo getResourceInfoUsingScriptStrategy()
            throws URISyntaxException, NoSuchFileException, ScriptException {
        final DelegateProxy proxy = getDelegateProxy();

        final Map<String, String> infoProps = proxy.getHttpSourceResourceInfo();

        if (infoProps.isEmpty()) {
            throw new NoSuchFileException(
                    DelegateMethod.HTTPSOURCE_RESOURCE_INFO +
                    " returned nil for " + identifier);
        }

        final String uri = infoProps.get("uri");
        final String username = infoProps.get("username");
        final String secret = infoProps.get("secret");
        return new ResourceInfo(new URI(uri), username, secret);
    }

    @Override
    public void setIdentifier(Identifier identifier) {
        rangedGETResponse = null;
        rangedGETResponseListener = null;
        resourceInfo = null;
        format = null;
        this.identifier = identifier;
    }

    /**
     * Stops the shared Jetty client.
     */
    @Override
    public synchronized void shutdown() {
        if (jettyClient != null) {
            try {
                jettyClient.stop();
                jettyClient = null;
            } catch (Exception e) {
                LOGGER.error("shutdown(): {}", e.getMessage());
            }
        }
    }

}
