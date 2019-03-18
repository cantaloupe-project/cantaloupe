package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.script.DelegateMethod;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.IOException;
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
 * {@link Key#HTTPSOURCE_LOOKUP_STRATEGY}:</p>
 *
 * <ol>
 *     <li>{@link LookupStrategy#BASIC} locates images by concatenating a
 *     pre-defined URL prefix and/or suffix.</li>
 *     <li>{@link LookupStrategy#DELEGATE_SCRIPT} invokes a delegate method to
 *     retrieve a URL (and optional auth info) dynamically.</li>
 * </ol>
 *
 * <h1>Resource Access</h1>
 *
 * <p>While proceeding through the client request fulfillment flow, this source
 * issues the following server requests:</p>
 *
 * <ol>
 *     <li>{@literal HEAD}</li>
 *     <li>If server supports ranges:
 *         <ol>
 *             <li>If {@link #getFormat()} needs to check magic bytes:
 *                 <ol>
 *                     <li>Ranged {@literal GET}</li>
 *                 </ol>
 *             </li>
 *             <li>If {@link HTTPStreamFactory#newSeekableStream()} is used:
 *                 <ol>
 *                     <li>A series of ranged {@literal GET} requests (see {@link
 *                     edu.illinois.library.cantaloupe.source.stream.HTTPImageInputStream}
 *                     for details)</li>
 *                 </ol>
 *             </li>
 *             <li>Else if {@link HTTPStreamFactory#newInputStream()} is used:
 *                 <ol>
 *                     <li>{@literal GET} to retrieve the full image bytes</li>
 *                 </ol>
 *             </li>
 *         </ol>
 *     </li>
 *     <li>Else if server does not support ranges:
 *         <ol>
 *             <li>{@literal GET} to retrieve the full image bytes</li>
 *         </ol>
 *     </li>
 * </ol>
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
 * @author Alex Dolski UIUC
 */
class HttpSource extends AbstractSource implements StreamSource {

    static class RequestInfo {

        private Headers headers = new Headers();
        private String uri, username, secret;

        RequestInfo(String uri, String username, String secret) {
            this.uri = uri;
            this.username = username;
            this.secret = secret;
        }

        RequestInfo(String uri,
                    String username,
                    String secret,
                    Map<String,?> headers) {
            this(uri, username, secret);
            if (headers != null) {
                headers.forEach((key, value) ->
                        this.headers.add(key, value.toString()));
            }
        }

        Headers getHeaders() {
            return headers;
        }

        String getSecret() {
            return secret;
        }

        String getURI() {
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

    /**
     * Encapsulates some parts of a HEAD response.
     */
    private static class HEADResponseInfo {

        int status;
        HttpFields headers;

        static HEADResponseInfo fromResponse(ContentResponse response) {
            HEADResponseInfo info = new HEADResponseInfo();
            info.status  = response.getStatus();
            info.headers = response.getHeaders();
            return info;
        }

        boolean acceptsRanges() {
            return "bytes".equals(headers.get("Accept-Ranges"));
        }

        long getContentLength() {
            return headers.getLongField("Content-Length");
        }

    }

    /**
     * Encapsulates parts of a ranged GET response. The range specifies a small
     * part of the beginning of the resource to use for the purpose of
     * inferring its format.
     */
    private static class RangedGETResponseInfo extends HEADResponseInfo {

        private static final int RANGE_LENGTH = 32;

        /**
         * Ranged response entity, with a maximum length of {@link
         * #RANGE_LENGTH}.
         */
        byte[] entity;

        static RangedGETResponseInfo fromResponse(ContentResponse response) {
            RangedGETResponseInfo info = new RangedGETResponseInfo();
            info.status  = response.getStatus();
            info.headers = response.getHeaders();
            info.entity  = response.getContent();
            return info;
        }

        Format detectFormat() throws IOException {
            Format format = Format.UNKNOWN;
            if (entity != null) {
                List<MediaType> types = MediaType.detectMediaTypes(entity);
                if (!types.isEmpty()) {
                    format = types.get(0).toFormat();
                }
            }
            return format;
        }

    }

    static final Logger LOGGER = LoggerFactory.getLogger(HttpSource.class);

    private static final int DEFAULT_REQUEST_TIMEOUT = 30;

    private static HttpClient jettyClient;

    /**
     * Cached {@link #fetchHEADResponseInfo() HEAD response info}.
     */
    private HEADResponseInfo headResponseInfo;

    /**
     * Cached {@link #fetchRangedGETResponseInfo() ranged GET response info}.
     */
    private RangedGETResponseInfo rangedGETResponseInfo;

    /**
     * Cached by {@link #getRequestInfo()}.
     */
    private RequestInfo requestInfo;

    static synchronized HttpClient getHTTPClient(RequestInfo info) {
        if (jettyClient == null) {
            final Configuration config = Configuration.getInstance();
            final boolean trustInvalidCerts = config.getBoolean(
                    Key.HTTPSOURCE_TRUST_ALL_CERTS, false);
            SslContextFactory sslContextFactory =
                    new SslContextFactory(trustInvalidCerts);

            HttpClientTransport transport = new HttpClientTransportOverHTTP();
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
            try {
                auth.addAuthenticationResult(new BasicAuthentication.BasicResult(
                        new URI(info.getURI()), info.getUsername(), info.getSecret()));
            } catch (URISyntaxException e) {
                LOGGER.warn("getHTTPClient(): {}", e.getMessage());
            }
        }
        return jettyClient;
    }

    /**
     * @return Request timeout from the application configuration, or a
     *         reasonable default if not set.
     */
    static int getRequestTimeout() {
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
        fetchHEADResponseInfo();

        final int status = headResponseInfo.status;

        if (status >= HttpStatus.BAD_REQUEST_400) {
            final String statusLine = "HTTP " + status;

            if (status == HttpStatus.NOT_FOUND_404
                    || status == HttpStatus.GONE_410) {
                throw new NoSuchFileException(statusLine);
            } else if (status == HttpStatus.UNAUTHORIZED_401
                    || status == HttpStatus.FORBIDDEN_403) {
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
     *     <li>Otherwise, if a {@literal Content-Type} header is present in the
     *     {@link #fetchHEADResponseInfo() HEAD response}, and its value is
     *     specific enough (not {@literal application/octet-stream}, for
     *     example), a format is inferred from that.</li>
     *     <li>Otherwise, if the {@literal HEAD} response contains an {@literal
     *     Accept-Ranges: bytes} header, a {@literal GET} request is sent with
     *     a {@literal Range} header specifying a small range of data from the
     *     beginning of the resource, and a format is inferred from the magic
     *     bytes in the response entity.</li>
     *     <li>Otherwise, {@link Format#UNKNOWN} is returned.</li>
     * </ol>
     *
     * @return See above.
     */
    @Override
    public Format getFormat() {
        if (format == null) {
            // Try to infer a format from the path component of the URI.
            try {
                format = Format.inferFormat(
                        new URI(getRequestInfo().getURI()).getPath());
            } catch (URISyntaxException e) {
                LOGGER.warn("getFormat(): {}", e.getMessage());
            } catch (Exception ignore) {
                // This is better caught and handled elsewhere.
            }

            if (Format.UNKNOWN.equals(format)) {
                // Try to infer a format from the identifier.
                format = Format.inferFormat(identifier);
            }

            if (Format.UNKNOWN.equals(format)) {
                // Try to infer a format from the Content-Type header.
                format = inferSourceFormatFromHEADResponse();
            }

            if (Format.UNKNOWN.equals(format)) {
                // Try to infer a format from the entity magic bytes. This
                // will require another request.
                format = inferSourceFormatFromMagicBytes();
            }
        }
        return format;
    }

    /**
     * @return Best guess at a format based on the {@literal Content-Type}
     *         header in the {@link #fetchHEADResponseInfo() HEAD response}, or
     *         {@link Format#UNKNOWN} if that header is missing or invalid.
     */
    private Format inferSourceFormatFromHEADResponse() {
        Format format = Format.UNKNOWN;
        try {
            final RequestInfo requestInfo       = getRequestInfo();
            final HEADResponseInfo responseInfo = fetchHEADResponseInfo();

            if (responseInfo.status >= 200 && responseInfo.status < 300) {
                HttpField field = responseInfo.headers.getField("Content-Type");
                if (field != null && field.getValue() != null) {
                    format = MediaType.fromContentType(field.getValue()).toFormat();
                    if (Format.UNKNOWN.equals(format)) {
                        LOGGER.debug("Unrecognized Content-Type header value for HEAD {}",
                                requestInfo.getURI());
                    }
                } else {
                    LOGGER.debug("No Content-Type header for HEAD {}",
                            requestInfo.getURI());
                }
            } else {
                LOGGER.debug("HEAD {} returned status {}",
                        requestInfo.getURI(), responseInfo.status);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return format;
    }

    /**
     * If the {@link #fetchHEADResponseInfo() HEAD response} contains an
     * {@literal Accept-Ranges: bytes} header, issues an HTTP {@literal GET}
     * request for a small {@literal Range} of the beginning of the resource
     * and checks the magic bytes in the response body.
     *
     * @return Inferred source format, or {@link Format#UNKNOWN}.
     */
    private Format inferSourceFormatFromMagicBytes() {
        Format format = Format.UNKNOWN;
        try {
            final RequestInfo requestInfo = getRequestInfo();
            if (fetchHEADResponseInfo().acceptsRanges()) {
                final RangedGETResponseInfo responseInfo
                        = fetchRangedGETResponseInfo();
                if (responseInfo.status >= 200 && responseInfo.status < 300) {
                    format = responseInfo.detectFormat();
                    if (!Format.UNKNOWN.equals(format)) {
                        LOGGER.debug("Inferred {} format from magic bytes for GET {}",
                                format, requestInfo.getURI());
                    } else {
                        LOGGER.debug("Unable to infer a format from magic bytes for GET {}",
                                requestInfo.getURI());
                    }
                } else {
                    LOGGER.debug("GET {} returned status {}",
                            requestInfo.getURI(), responseInfo.status);
                }
            } else {
                LOGGER.warn("Server did not supply an " +
                        "`Accept-Ranges: bytes` header for HEAD {}, and all " +
                        "other attempts to infer a format failed.",
                        requestInfo.getURI());
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return format;
    }

    @Override
    public StreamFactory newStreamFactory() throws IOException {
        RequestInfo info;
        try {
            info = getRequestInfo();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("newStreamFactory(): {}", e.getMessage());
            throw new IOException(e);
        }

        if (info != null) {
            LOGGER.debug("Resolved {} to {}", identifier, info.getURI());
            fetchHEADResponseInfo();
            return new HTTPStreamFactory(
                    getHTTPClient(info),
                    info,
                    headResponseInfo.getContentLength(),
                    headResponseInfo.acceptsRanges());
        }
        return null;
    }

    /**
     * <p>Issues a {@literal HEAD} request and caches parts of the response in
     * {@link #headResponseInfo}.</p>
     */
    private HEADResponseInfo fetchHEADResponseInfo() throws IOException {
        if (headResponseInfo == null) {
            ContentResponse response = request(HttpMethod.HEAD);
            headResponseInfo = HEADResponseInfo.fromResponse(response);
        }
        return headResponseInfo;
    }

    /**
     * <p>Issues a {@literal GET} request specifying a small range of data and
     * caches parts of the response in {@link #rangedGETResponseInfo}.</p>
     */
    private RangedGETResponseInfo fetchRangedGETResponseInfo()
            throws IOException {
        if (rangedGETResponseInfo == null) {
            final Headers extraHeaders = new Headers();
            extraHeaders.add("Range",
                    "bytes=0-" + (RangedGETResponseInfo.RANGE_LENGTH - 1));

            ContentResponse response = request(HttpMethod.GET, extraHeaders);
            rangedGETResponseInfo =
                    RangedGETResponseInfo.fromResponse(response);
        }
        return rangedGETResponseInfo;
    }

    private ContentResponse request(HttpMethod method) throws IOException {
        return request(method, new Headers());
    }

    private ContentResponse request(HttpMethod method,
                                    Headers extraHeaders) throws IOException {
        RequestInfo requestInfo;
        try {
            requestInfo = getRequestInfo();
        } catch (InterruptedException | TimeoutException e) {
            LOGGER.error(e.getMessage(), e);
            throw new IOException(e);
        } catch (Exception e) {
            LOGGER.error("request(): {}", e.getMessage());
            throw new IOException(e.getMessage(), e);
        }

        Request request = getHTTPClient(requestInfo)
                .newRequest(requestInfo.getURI())
                .timeout(getRequestTimeout(), TimeUnit.SECONDS)
                .method(method);
        // Add any additional headers returned from the delegate method.
        extraHeaders.addAll(requestInfo.getHeaders());
        // Then add them all to the request.
        extraHeaders.forEach(h -> request.header(h.getName(), h.getValue()));

        LOGGER.debug("Requesting {} {} (extra headers: {})",
                method, requestInfo.getURI(), extraHeaders);

        try {
            return request.send();
        } catch (ExecutionException e) {
            LOGGER.debug("ExecutionException from Request.send(); generating AccessDeniedException.", e);
            throw new AccessDeniedException(requestInfo.getURI());
        } catch (InterruptedException | TimeoutException e) {
            throw new IOException(e);
        }
    }

    /**
     * @return Instance corresponding to {@link #identifier}. The result is
     *         cached.
     */
    RequestInfo getRequestInfo() throws Exception {
        if (requestInfo == null) {
            final LookupStrategy strategy =
                    LookupStrategy.from(Key.HTTPSOURCE_LOOKUP_STRATEGY);
            switch (strategy) {
                case DELEGATE_SCRIPT:
                    requestInfo = getRequestInfoUsingScriptStrategy();
                    break;
                default:
                    requestInfo = getRequestInfoUsingBasicStrategy();
                    break;
            }
        }
        return requestInfo;
    }

    private RequestInfo getRequestInfoUsingBasicStrategy() {
        final Configuration config = Configuration.getInstance();
        final String prefix = config.getString(Key.HTTPSOURCE_URL_PREFIX, "");
        final String suffix = config.getString(Key.HTTPSOURCE_URL_SUFFIX, "");
        return new RequestInfo(
                prefix + identifier.toString() + suffix,
                config.getString(Key.HTTPSOURCE_BASIC_AUTH_USERNAME),
                config.getString(Key.HTTPSOURCE_BASIC_AUTH_SECRET));
    }

    /**
     * @throws NoSuchFileException if the remote resource was not found.
     * @throws ScriptException     if the delegate method throws an exception.
     */
    private RequestInfo getRequestInfoUsingScriptStrategy()
            throws NoSuchFileException, ScriptException {
        final DelegateProxy proxy   = getDelegateProxy();
        final Map<String, ?> result = proxy.getHttpSourceResourceInfo();

        if (result.isEmpty()) {
            throw new NoSuchFileException(
                    DelegateMethod.HTTPSOURCE_RESOURCE_INFO +
                    " returned nil for " + identifier);
        }

        final String uri            = (String) result.get("uri");
        final String username       = (String) result.get("username");
        final String secret         = (String) result.get("secret");
        @SuppressWarnings("unchecked")
        final Map<String,?> headers = (Map<String,?>) result.get("headers");

        return new RequestInfo(uri, username, secret, headers);
    }

    @Override
    public void setIdentifier(Identifier identifier) {
        super.setIdentifier(identifier);
        requestInfo = null;
        headResponseInfo = null;
        rangedGETResponseInfo = null;
    }

    /**
     * Stops the shared Jetty client.
     */
    @Override
    public void shutdown() {
        synchronized (HttpSource.class) {
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

}
