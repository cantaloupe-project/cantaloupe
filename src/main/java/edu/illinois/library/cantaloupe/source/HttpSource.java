package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.delegate.DelegateMethod;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>Provides access to source content located on an HTTP(S) server. Backed by
 * an <a href="http://square.github.io/okhttp/">OkHttp</a> client.</p>
 *
 * <h1>Protocol Support</h1>
 *
 * <p>HTTP/1.x, HTTPS/1.x, and HTTPS/2.0 are supported.</p>
 *
 * <h1>Format Inference</h1>
 *
 * <p>See {@link FormatIterator}.</p>
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
 *     <li>If {@link Key#HTTPSOURCE_SEND_HEAD_REQUESTS} is {@code true}, or
 *     the delegate method returns {@code true} for the equivalent key, a
 *     {@code HEAD} request. Otherwise, a ranged {@code GET} request specifying
 *     a small range of the beginning of the resource.</li>
 *     <li>If a {@code HEAD} request was sent:
 *         <ol>
 *             <li>If {@link FormatIterator#next()} needs to check magic bytes,
 *             and the server supports ranges:
 *                 <ol>
 *                     <li>Ranged {@code GET}</li>
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
 *                     <li>{@code GET} to retrieve the full image bytes</li>
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
 * @author Alex Dolski UIUC
 */
class HttpSource extends AbstractSource implements Source {

    /**
     * Encapsulates the status code, headers, and body (if available) of a
     * {code HEAD} or ranged {@code GET} response. The range specifies a small
     * part of the beginning of the resource to use for the purpose of
     * inferring its format.
     */
    private static class ResourceInfo {

        private String requestMethod;
        private int status;
        private Headers headers;

        /**
         * Response entity with a maximum length of {@link #RANGE_LENGTH}.
         */
        private byte[] entity;

        static ResourceInfo fromResponse(Response response) throws IOException {
            ResourceInfo info = new ResourceInfo();
            info.requestMethod = response.request().method();
            info.status        = response.code();
            info.headers       = response.headers();
            if ("GET".equals(response.request().method()) &&
                    response.body() != null) {
                info.entity = response.body().bytes();
            }
            return info;
        }

        boolean acceptsRanges() {
            return "bytes".equals(headers.get("Accept-Ranges"));
        }

        long contentLength() {
            String value = headers.get("Content-Length");
            return (value != null) ? Long.parseLong(value) : 0;
        }

        String contentType() {
            return headers.get("Content-Type");
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

        Instant lastModified() {
            String str = headers.get("Last-Modified");
            if (str != null) {
                TemporalAccessor ta = DateTimeFormatter.RFC_1123_DATE_TIME
                        .withLocale(Locale.UK)
                        .withZone(ZoneId.systemDefault())
                        .parse(str);
                return Instant.from(ta);
            }
            return null;
        }

    }

    /**
     * <ol>
     *     <li>If the path component of the URI contains a recognized filename
     *     extension, the format is inferred from that.</li>
     *     <li>Otherwise, if the identifier contains a recognized filename
     *     extension, the format is inferred from that.</li>
     *     <li>Otherwise, if a {@code Content-Type} header is present in the
     *     {@link #getResourceInfo HEAD response}, and its value is
     *     specific enough (not {@code application/octet-stream}, for
     *     example), a format is inferred from that.</li>
     *     <li>Otherwise, if the {@literal HEAD} response contains an {@code
     *     Accept-Ranges: bytes} header, a {@code GET} request is sent with a
     *     {@code Range} header specifying a small range of data from the
     *     beginning of the resource, and a format is inferred from the magic
     *     bytes in the response entity.</li>
     *     <li>Otherwise, {@link Format#UNKNOWN} is returned.</li>
     * </ol>
     *
     * @param <T> {@link Format}.
     */
    class FormatIterator<T> implements Iterator<T> {

        /**
         * Infers a {@link Format} based on the {@code Content-Type} header in
         * the initial response..
         */
        private class ContentTypeHeaderChecker implements FormatChecker {
            /**
             * @return Format from the {@code Content-Type} header, or {@link
             *         Format#UNKNOWN} if that header is missing or invalid.
             */
            @Override
            public Format check() {
                try {
                    final HTTPRequestInfo requestInfo = getRequestInfo();
                    final ResourceInfo resourceInfo   = getResourceInfo();

                    if (resourceInfo.status >= 200 && resourceInfo.status < 300) {
                        String value = resourceInfo.contentType();
                        if (value != null) {
                            Format format = MediaType.fromContentType(value).toFormat();
                            if (Format.UNKNOWN.equals(format)) {
                                LOGGER.debug("Unrecognized Content-Type header value for {} {}: {}",
                                        resourceInfo.requestMethod,
                                        requestInfo.getURI(),
                                        value);
                            }
                            return format;
                        } else {
                            LOGGER.debug("No Content-Type header for {} {}",
                                    resourceInfo.requestMethod,
                                    requestInfo.getURI());
                        }
                    } else {
                        LOGGER.debug("{} {} returned status {}",
                                resourceInfo.requestMethod,
                                requestInfo.getURI(),
                                resourceInfo.status);
                    }
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
                return Format.UNKNOWN;
            }
        }

        private class ByteChecker implements FormatChecker {
            /**
             * If the {@link #getResourceInfo initial response} is from a
             * {@code HEAD} request, issues an HTTP {@code GET} request for a
             * small range of the beginning of the resource. (If it is a {@code
             * GET} response, that data has already been received.) Then, a
             * source format is inferred from the magic bytes in the response
             * entity.
             *
             * @return Inferred source format, or {@link Format#UNKNOWN}.
             */
            @Override
            public Format check() {
                try {
                    final HTTPRequestInfo requestInfo = getRequestInfo();
                    // If a request hasn't yet been sent, send one. This may be
                    // a HEAD or a ranged GET. It's not safe to send a ranged
                    // GET without first checking (via HEAD) whether the
                    // resource supports ranges--unless we are told via the
                    // configuration not to send HEADs.
                    if (resourceInfo == null) {
                        resourceInfo = getResourceInfo();
                    }
                    // If it was a HEAD, we need to know whether the resource
                    // supports ranged requests. If it does, send one.
                    if ("HEAD".equals(resourceInfo.requestMethod)) {
                        if (resourceInfo.acceptsRanges()) {
                            resourceInfo = fetchResourceInfoViaGET();
                        } else {
                            LOGGER.debug("Server did not supply an " +
                                            "`Accept-Ranges: bytes` header in response " +
                                            "to HEAD {}, and all other attempts to "+
                                            "infer a format failed.",
                                    requestInfo.getURI());
                            return Format.UNKNOWN;
                        }
                    }
                    if (resourceInfo.status >= 200 && resourceInfo.status < 300) {
                        Format format = resourceInfo.detectFormat();
                        if (!Format.UNKNOWN.equals(format)) {
                            LOGGER.debug("Inferred {} format from magic bytes for GET {}",
                                    format, requestInfo.getURI());
                            return format;
                        } else {
                            LOGGER.debug("Unable to infer a format from magic bytes for GET {}",
                                    requestInfo.getURI());
                        }
                    } else {
                        LOGGER.debug("{} {} returned status {}",
                                resourceInfo.requestMethod,
                                requestInfo.getURI(),
                                resourceInfo.status);
                    }
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
                return Format.UNKNOWN;
            }
        }

        private FormatChecker formatChecker;

        @Override
        public boolean hasNext() {
            return (formatChecker == null ||
                    formatChecker instanceof URIPathChecker ||
                    formatChecker instanceof IdentifierFormatChecker ||
                    formatChecker instanceof FormatIterator.ContentTypeHeaderChecker);
        }

        @Override
        public T next() {
            if (formatChecker == null) {
                formatChecker = new URIPathChecker();
            } else if (formatChecker instanceof URIPathChecker) {
                formatChecker = new IdentifierFormatChecker(getIdentifier());
            } else if (formatChecker instanceof IdentifierFormatChecker) {
                formatChecker = new ContentTypeHeaderChecker();
            } else if (formatChecker instanceof FormatIterator.ContentTypeHeaderChecker) {
                formatChecker = new ByteChecker();
            } else {
                throw new NoSuchElementException();
            }
            try {
                //noinspection unchecked
                return (T) formatChecker.check();
            } catch (IOException e) {
                LOGGER.warn("Error checking format: {}", e.getMessage());
                //noinspection unchecked
                return (T) Format.UNKNOWN;
            }
        }
    }

    /**
     * Infers a {@link Format} based on a filename extension in the URI path.
     */
    private class URIPathChecker implements FormatChecker {
        @Override
        public Format check() {
            try {
                return Format.inferFormat(
                        new URI(getRequestInfo().getURI()).getPath());
            } catch (URISyntaxException e) {
                LOGGER.warn("{}: {}",
                        getClass().getSimpleName(), e.getMessage());
            } catch (Exception ignore) {
                // This is better caught and handled elsewhere.
            }
            return Format.UNKNOWN;
        }
    }

    static final Logger LOGGER = LoggerFactory.getLogger(HttpSource.class);

    static final String USER_AGENT = String.format(
            "%s/%s (%s/%s; java/%s; %s/%s)",
            HttpSource.class.getSimpleName(),
            Application.getVersion(),
            Application.getName(),
            Application.getVersion(),
            System.getProperty("java.version"),
            System.getProperty("os.name"),
            System.getProperty("os.version"));

    private static final int DEFAULT_REQUEST_TIMEOUT = 30;
    private static final int RANGE_LENGTH            = 32;

    private static OkHttpClient httpClient;

    /**
     * Cached by {@link #getRequestInfo()}.
     */
    private HTTPRequestInfo requestInfo;

    /**
     * Cached {@link #getResourceInfo resource info} from the initial request,
     * which may be either a {@code HEAD} or ranged {@code GET}, depending on
     * the configuration.
     */
    private ResourceInfo resourceInfo;

    private final FormatIterator<Format> formatIterator =
            new FormatIterator<>();

    /**
     * @return Already-initialized instance shared by all threads.
     */
    static synchronized OkHttpClient getHTTPClient() {
        if (httpClient == null) {
            final OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .followRedirects(true)
                    .connectTimeout(getRequestTimeout().getSeconds(), TimeUnit.SECONDS)
                    .readTimeout(getRequestTimeout().getSeconds(), TimeUnit.SECONDS)
                    .writeTimeout(getRequestTimeout().getSeconds(), TimeUnit.SECONDS);
            final Configuration config = Configuration.getInstance();

            final String proxyHost =
                    config.getString(Key.HTTPSOURCE_HTTP_PROXY_HOST, "");
            if (!proxyHost.isBlank()) {
                final int proxyPort =
                        config.getInt(Key.HTTPSOURCE_HTTP_PROXY_PORT);
                if (proxyPort == 0) {
                    throw new RuntimeException("Proxy port setting " +
                            Key.HTTPSOURCE_HTTP_PROXY_PORT + " must be set");
                }
                LOGGER.debug("Using HTTP proxy: {}:{}", proxyHost, proxyPort);
                Proxy httpProxy = new Proxy(Proxy.Type.HTTP,
                        new InetSocketAddress(proxyHost, proxyPort));
                builder.proxy(httpProxy);
            }

            if (config.getBoolean(Key.HTTPSOURCE_ALLOW_INSECURE, false)) {
                try {
                    X509TrustManager[] tm = new X509TrustManager[]{
                            new X509TrustManager() {
                                @Override
                                public void checkClientTrusted(X509Certificate[] chain,
                                                               String authType) {}
                                @Override
                                public void checkServerTrusted(X509Certificate[] chain,
                                                               String authType) {}
                                @Override
                                public X509Certificate[] getAcceptedIssuers() {
                                    return new X509Certificate[0];
                                }
                            }};
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, tm, new SecureRandom());
                    builder.sslSocketFactory(sslContext.getSocketFactory(), tm[0]);
                    builder.hostnameVerifier((s, sslSession) -> true);
                } catch (KeyManagementException | NoSuchAlgorithmException e) {
                    LOGGER.error("getHTTPClient(): {}", e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }
            httpClient = builder.build();
        }
        return httpClient;
    }

    /**
     * @return Request timeout from the application configuration, or a
     *         reasonable default if not set.
     */
    private static Duration getRequestTimeout() {
        int timeout = Configuration.getInstance().getInt(
                Key.HTTPSOURCE_REQUEST_TIMEOUT,
                DEFAULT_REQUEST_TIMEOUT);
        return Duration.ofSeconds(timeout);
    }

    /**
     * @see #request(HTTPRequestInfo, String, Map)
     */
    static Response request(HTTPRequestInfo requestInfo,
                            String method) throws IOException {
        return request(requestInfo, method, Collections.emptyMap());
    }

    /**
     * Sends a request using the {@link #getHTTPClient() shared client},
     * respecting any credentials and/or extra headers set on {@code
     * requestInfo}.
     *
     * @param requestInfo Request info.
     * @param method HTTP method.
     * @param extraHeaders Any additional headers to send.
     * @return Response.
     */
    static Response request(HTTPRequestInfo requestInfo,
                            String method,
                            Map<String,String> extraHeaders) throws IOException {
        Request.Builder builder = new Request.Builder()
                .method(method, null)
                .url(requestInfo.getURI())
                .addHeader("User-Agent", USER_AGENT);
        // Add credentials.
        if (requestInfo.getUsername() != null &&
                requestInfo.getSecret() != null) {
            builder.addHeader("Authorization",
                    "Basic " + requestInfo.getBasicAuthToken());
        }
        // Add any additional headers.
        requestInfo.getHeaders().forEach(h ->
                builder.addHeader(h.getName(), h.getValue()));
        extraHeaders.forEach(builder::addHeader);

        Request request = builder.build();

        LOGGER.debug("Requesting {} {} [extra headers: {}]",
                method, requestInfo.getURI(), toString(request.headers()));

        return getHTTPClient().newCall(request).execute();
    }

    static String toString(Headers headers) {
        return headers.toMultimap().entrySet()
                .stream()
                .map(entry -> entry.getKey() + ": " +
                        ("authorization".equalsIgnoreCase(entry.getKey()) ?
                                "********" : entry.getValue()))
                .collect(Collectors.joining("; "));
    }

    @Override
    public StatResult stat() throws IOException {
        ResourceInfo info = getResourceInfo();
        final int status  = info.status;
        if (status >= 400) {
            final String statusLine = "HTTP " + status;
            if (status == 404 || status == 410) {        // not found or gone
                throw new NoSuchFileException(statusLine);
            } else if (status == 401 || status == 403) { // unauthorized or forbidden
                throw new AccessDeniedException(statusLine);
            } else {
                throw new IOException(statusLine);
            }
        }
        StatResult result = new StatResult();
        result.setLastModified(info.lastModified());
        return result;
    }

    @Override
    public FormatIterator<Format> getFormatIterator() {
        return formatIterator;
    }

    /**
     * Issues a {@code HEAD} or ranged {@code GET} request (depending on the
     * configuration) and caches the result in {@link #resourceInfo}.
     */
    private ResourceInfo getResourceInfo() throws IOException {
        if (resourceInfo == null) {
            try {
                requestInfo = getRequestInfo();
                if (requestInfo.isSendingHeadRequest()) {
                    fetchResourceInfoViaHEAD();
                } else {
                    fetchResourceInfoViaGET();
                }
            } catch (Exception e) {
                LOGGER.error("fetchResourceInfo(): {}", e.getMessage());
                throw new IOException(e.getMessage(), e);
            }
        }
        return resourceInfo;
    }

    private ResourceInfo fetchResourceInfoViaHEAD() throws Exception {
        requestInfo = getRequestInfo();
        try (Response response = request("HEAD", Collections.emptyMap())) {
            resourceInfo = ResourceInfo.fromResponse(response);
        }
        return resourceInfo;
    }

    private ResourceInfo fetchResourceInfoViaGET() throws Exception {
        requestInfo = getRequestInfo();
        var extraHeaders = Map.of("Range", "bytes=0-" + (RANGE_LENGTH - 1));
        try (Response response = request("GET", extraHeaders)) {
            resourceInfo = ResourceInfo.fromResponse(response);
        }
        return resourceInfo;
    }

    private Response request(String method,
                             Map<String,String> extraHeaders) throws IOException {
        return request(requestInfo, method, extraHeaders);
    }

    /**
     * @return Instance corresponding to {@link #identifier}. The result is
     *         cached.
     */
    HTTPRequestInfo getRequestInfo() throws Exception {
        if (requestInfo == null) {
            final LookupStrategy strategy =
                    LookupStrategy.from(Key.HTTPSOURCE_LOOKUP_STRATEGY);
            if (LookupStrategy.DELEGATE_SCRIPT.equals(strategy)) {
                requestInfo = newRequestInfoUsingScriptStrategy();
            } else {
                requestInfo = newRequestInfoUsingBasicStrategy();
            }
        }
        return requestInfo;
    }

    private HTTPRequestInfo newRequestInfoUsingBasicStrategy() {
        final var config    = Configuration.getInstance();
        final String prefix = config.getString(Key.HTTPSOURCE_URL_PREFIX, "");
        final String suffix = config.getString(Key.HTTPSOURCE_URL_SUFFIX, "");

        final HTTPRequestInfo info = new HTTPRequestInfo();
        info.setURI(prefix + identifier.toString() + suffix);
        info.setUsername(config.getString(Key.HTTPSOURCE_BASIC_AUTH_USERNAME));
        info.setSecret(config.getString(Key.HTTPSOURCE_BASIC_AUTH_SECRET));
        info.setSendingHeadRequest(config.getBoolean(Key.HTTPSOURCE_SEND_HEAD_REQUESTS, true));
        return info;
    }

    /**
     * @throws NoSuchFileException if the remote resource was not found.
     * @throws ScriptException     if the delegate method throws an exception.
     */
    private HTTPRequestInfo newRequestInfoUsingScriptStrategy()
            throws NoSuchFileException, ScriptException {
        final DelegateProxy proxy   = getDelegateProxy();
        final Map<String, ?> result = proxy.getHttpSourceResourceInfo();

        if (result.isEmpty()) {
            throw new NoSuchFileException(
                    DelegateMethod.HTTPSOURCE_RESOURCE_INFO +
                            " returned nil for " + identifier);
        }

        final String uri                 = (String) result.get("uri");
        final String username            = (String) result.get("username");
        final String secret              = (String) result.get("secret");
        @SuppressWarnings("unchecked")
        final Map<String,Object> headers = (Map<String,Object>) result.get("headers");
        final boolean isHeadEnabled      = !result.containsKey("send_head_request") ||
                (boolean) result.get("send_head_request");

        final HTTPRequestInfo info = new HTTPRequestInfo();
        info.setURI(uri);
        info.setUsername(username);
        info.setSecret(secret);
        info.setHeaders(headers);
        info.setSendingHeadRequest(isHeadEnabled);
        return info;
    }

    @Override
    public StreamFactory newStreamFactory() throws IOException {
        HTTPRequestInfo info;
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
            getResourceInfo();
            return new HTTPStreamFactory(
                    info,
                    resourceInfo.contentLength(),
                    resourceInfo.acceptsRanges());
        }
        return null;
    }

    @Override
    public void setIdentifier(Identifier identifier) {
        super.setIdentifier(identifier);
        reset();
    }

    private void reset() {
        requestInfo  = null;
        resourceInfo = null;
    }

    /**
     * Stops the shared HTTP client.
     */
    @Override
    public void shutdown() {
        synchronized (HttpSource.class) {
            if (httpClient != null) {
                httpClient.dispatcher().executorService().shutdown();
                httpClient.connectionPool().evictAll();
            }
        }
    }

}
