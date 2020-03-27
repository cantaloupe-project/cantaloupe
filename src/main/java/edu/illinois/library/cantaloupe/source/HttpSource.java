package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.script.DelegateMethod;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import okhttp3.ConnectionSpec;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

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
 *     <li>{@literal HEAD}</li>
 *     <li>If server supports ranges:
 *         <ol>
 *             <li>If {@link FormatIterator#next()} } needs to check magic
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
 * @author Alex Dolski UIUC
 */
class HttpSource extends AbstractSource implements Source {

    /**
     * Encapsulates the status code and headers of a HEAD response.
     */
    private static class HEADResponseInfo {

        int status;
        Headers headers;

        static HEADResponseInfo fromResponse(Response response)
                throws IOException {
            HEADResponseInfo info = new HEADResponseInfo();
            info.status           = response.code();
            info.headers          = response.headers();
            return info;
        }

        boolean acceptsRanges() {
            return "bytes".equals(headers.get("Accept-Ranges"));
        }

        long getContentLength() {
            String value = headers.get("Content-Length");
            return (value != null) ? Long.parseLong(value) : 0;
        }

    }

    /**
     * Encapsulates the status code, headers, and body of a ranged GET
     * response. The range specifies a small part of the beginning of the
     * resource to use for the purpose of inferring its format.
     */
    private static class RangedGETResponseInfo extends HEADResponseInfo {

        private static final int RANGE_LENGTH = 32;

        /**
         * Ranged response entity, with a maximum length of {@link
         * #RANGE_LENGTH}.
         */
        private byte[] entity;

        static RangedGETResponseInfo fromResponse(Response response)
                throws IOException {
            RangedGETResponseInfo info = new RangedGETResponseInfo();
            info.status                = response.code();
            info.headers               = response.headers();
            if (response.body() != null) {
                info.entity = response.body().bytes();
            }
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
     * @param <T> {@link Format}.
     */
    class FormatIterator<T> implements Iterator<T> {

        /**
         * Infers a {@link Format} based on the {@literal Content-Type} header in
         * the {@link #fetchHEADResponseInfo() HEAD response}.
         */
        private class ContentTypeHeaderChecker implements FormatChecker {
            /**
             * @return Format from the {@literal Content-Type} header, or {@link
             *         Format#UNKNOWN} if that header is missing or invalid.
             */
            @Override
            public Format check() {
                try {
                    final HTTPRequestInfo requestInfo = getRequestInfo();
                    final HEADResponseInfo responseInfo = fetchHEADResponseInfo();

                    if (responseInfo.status >= 200 && responseInfo.status < 300) {
                        String field = responseInfo.headers.get("Content-Type");
                        if (field != null) {
                            Format format = MediaType.fromContentType(field).toFormat();
                            if (Format.UNKNOWN.equals(format)) {
                                LOGGER.debug("Unrecognized Content-Type header value for HEAD {}",
                                        requestInfo.getURI());
                            }
                            return format;
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
                return Format.UNKNOWN;
            }
        }

        private class ByteChecker implements FormatChecker {
            /**
             * If the {@link #fetchHEADResponseInfo() HEAD response} contains an
             * {@literal Accept-Ranges: bytes} header, issues an HTTP {@literal GET}
             * request for a small {@literal Range} of the beginning of the resource
             * and checks the magic bytes in the response body.
             *
             * @return Inferred source format, or {@link Format#UNKNOWN}.
             */
            @Override
            public Format check() {
                try {
                    final HTTPRequestInfo requestInfo = getRequestInfo();
                    if (fetchHEADResponseInfo().acceptsRanges()) {
                        final RangedGETResponseInfo responseInfo
                                = fetchRangedGETResponseInfo();
                        if (responseInfo.status >= 200 && responseInfo.status < 300) {
                            Format format = responseInfo.detectFormat();
                            if (!Format.UNKNOWN.equals(format)) {
                                LOGGER.debug("Inferred {} format from magic bytes for GET {}",
                                        format, requestInfo.getURI());
                                return format;
                            } else {
                                LOGGER.debug("Unable to infer a format from magic bytes for GET {}",
                                        requestInfo.getURI());
                            }
                        } else {
                            LOGGER.debug("GET {} returned status {}",
                                    requestInfo.getURI(), responseInfo.status);
                        }
                    } else {
                        LOGGER.info("Server did not supply an " +
                                        "`Accept-Ranges: bytes` header for HEAD {}, and all " +
                                        "other attempts to infer a format failed.",
                                requestInfo.getURI());
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

    private static final int DEFAULT_REQUEST_TIMEOUT = 30;

    private static OkHttpClient httpClient;

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
    private HTTPRequestInfo requestInfo;

    private FormatIterator<Format> formatIterator = new FormatIterator<>();

    static synchronized OkHttpClient getHTTPClient() {
        if (httpClient == null) {
            final OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .followRedirects(true)
                    .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
                    .connectTimeout(getRequestTimeout().getSeconds(), TimeUnit.SECONDS)
                    .readTimeout(getRequestTimeout().getSeconds(), TimeUnit.SECONDS)
                    .writeTimeout(getRequestTimeout().getSeconds(), TimeUnit.SECONDS);

            final Configuration config = Configuration.getInstance();
            final boolean allowInsecure = config.getBoolean(
                    Key.HTTPSOURCE_ALLOW_INSECURE, false);

            if (allowInsecure) {
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

    static String getUserAgent() {
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
    }

    @Override
    public FormatIterator<Format> getFormatIterator() {
        return formatIterator;
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
            fetchHEADResponseInfo();
            return new HTTPStreamFactory(
                    getHTTPClient(),
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
            try (Response response = request("HEAD")) {
                headResponseInfo = HEADResponseInfo.fromResponse(response);
            }
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
            Map<String,String> extraHeaders = Map.of("Range",
                    "bytes=0-" + (RangedGETResponseInfo.RANGE_LENGTH - 1));
            try (Response response = request("GET", extraHeaders)) {
                rangedGETResponseInfo =
                        RangedGETResponseInfo.fromResponse(response);
            }
        }
        return rangedGETResponseInfo;
    }

    private Response request(String method) throws IOException {
        return request(method, Collections.emptyMap());
    }

    private Response request(String method,
                             Map<String,String> extraHeaders) throws IOException {
        HTTPRequestInfo requestInfo;
        try {
            requestInfo = getRequestInfo();
        } catch (Exception e) {
            LOGGER.error("request(): {}", e.getMessage());
            throw new IOException(e.getMessage(), e);
        }

        Request.Builder builder = new Request.Builder()
                .method(method, null)
                .url(requestInfo.getURI())
                .addHeader("User-Agent", getUserAgent());
        // Add any additional headers.
        requestInfo.getHeaders().forEach(h ->
                builder.addHeader(h.getName(), h.getValue()));
        extraHeaders.forEach(builder::addHeader);

        if (requestInfo.getUsername() != null &&
                requestInfo.getSecret() != null) {
            builder.addHeader("Authorization",
                    "Basic " + requestInfo.getBasicAuthToken());
        }

        Request request = builder.build();

        LOGGER.debug("Requesting {} {} (extra headers: {})",
                method, requestInfo.getURI(), extraHeaders);

        return getHTTPClient().newCall(request).execute();
    }

    /**
     * @return Instance corresponding to {@link #identifier}. The result is
     *         cached.
     */
    HTTPRequestInfo getRequestInfo() throws Exception {
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

    private HTTPRequestInfo getRequestInfoUsingBasicStrategy() {
        final Configuration config = Configuration.getInstance();
        final String prefix = config.getString(Key.HTTPSOURCE_URL_PREFIX, "");
        final String suffix = config.getString(Key.HTTPSOURCE_URL_SUFFIX, "");
        return new HTTPRequestInfo(
                prefix + identifier.toString() + suffix,
                config.getString(Key.HTTPSOURCE_BASIC_AUTH_USERNAME),
                config.getString(Key.HTTPSOURCE_BASIC_AUTH_SECRET));
    }

    /**
     * @throws NoSuchFileException if the remote resource was not found.
     * @throws ScriptException     if the delegate method throws an exception.
     */
    private HTTPRequestInfo getRequestInfoUsingScriptStrategy()
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

        return new HTTPRequestInfo(uri, username, secret, headers);
    }

    @Override
    public void setIdentifier(Identifier identifier) {
        super.setIdentifier(identifier);
        reset();
    }

    private void reset() {
        requestInfo           = null;
        headResponseInfo      = null;
        rangedGETResponseInfo = null;
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
