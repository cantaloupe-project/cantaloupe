package edu.illinois.library.cantaloupe.resolver;

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
import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.AccessDeniedException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>Provides access to source content located on an HTTP(S) server.</p>
 *
 * <h3>Format Determination</h3>
 *
 * <p>For images with extensions, the extension will be assumed to correctly
 * denote the image format, based on the return value of
 * {@link Format#inferFormat(Identifier)}. For images with extensions that are
 * missing or unrecognized, the <code>Content-Type</code> header will be
 * checked to determine their format, which will incur a penalty of an extra
 * request. It is therefore more efficient to serve images with extensions.</p>
 *
 * <h3>Lookup Strategies</h3>
 *
 * <p>Two distinct lookup strategies are supported, defined by
 * {@link Key#HTTPRESOLVER_LOOKUP_STRATEGY}. BasicLookupStrategy locates
 * images by concatenating a pre-defined URL prefix and/or suffix.
 * ScriptLookupStrategy invokes a delegate method to retrieve a URL
 * dynamically.</p>
 *
 * @see <a href="http://www.eclipse.org/jetty/documentation/current/http-client.html">
 *     Jetty HTTP Client</a>
 */
class HttpResolver extends AbstractResolver implements StreamResolver {

    private class HTTPStreamSource implements StreamSource {

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
                        timeout(REQUEST_TIMEOUT, TimeUnit.SECONDS).
                        method(HttpMethod.GET).
                        send(listener);

                // Wait for the response headers to arrive.
                Response response = listener.get(5, TimeUnit.SECONDS);

                if (response.getStatus() == HttpStatus.OK_200) {
                    return listener.getInputStream();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            return null;
        }

    }

    private static Logger logger = LoggerFactory.getLogger(HttpResolver.class);

    private static final String GET_URL_DELEGATE_METHOD =
            "HttpResolver::get_url";
    private static final int REQUEST_TIMEOUT = 10;

    private static HttpClient client; // TODO: stop this at app shutdown

    private static synchronized HttpClient getHTTPClient(URI uri) {
        if (client == null) {
            SslContextFactory sslContextFactory = new SslContextFactory();
            client = new HttpClient(sslContextFactory);
            client.setFollowRedirects(true);

            // Configure Basic auth.
            // https://www.eclipse.org/jetty/documentation/9.4.x/http-client-authentication.html
            final Configuration config = Configuration.getInstance();
            final String username =
                    config.getString(Key.HTTPRESOLVER_BASIC_AUTH_USERNAME, "");
            final String secret =
                    config.getString(Key.HTTPRESOLVER_BASIC_AUTH_SECRET, "");
            if (username.length() > 0 && secret.length() > 0) {
                AuthenticationStore auth = client.getAuthenticationStore();
                auth.addAuthenticationResult(new BasicAuthentication.BasicResult(
                        uri, username, secret));
            }

            try {
                client.start();
            } catch (Exception e) {
                logger.error("getHTTPClient(): {}", e.getMessage());
            }
        }
        return client;
    }

    @Override
    public StreamSource newStreamSource() throws IOException {
        URI uri = null;
        try {
            uri = getURI();
            logger.info("Resolved {} to {}", identifier, uri);
        } catch (Exception e) {
            logger.error("newStreamSource(): {}", e.getMessage());
        }

        try {
            // Issue an HTTP HEAD request to check whether the underlying
            // resource is accessible.
            Response response = getHTTPClient(uri).newRequest(uri).
                    timeout(REQUEST_TIMEOUT, TimeUnit.SECONDS).
                    method(HttpMethod.HEAD).send();

            if (response.getStatus() == 401 || response.getStatus() == 403) {
                throw new AccessDeniedException(response.getReason());
            } else if (response.getStatus() == 404 || response.getStatus() == 410) {
                throw new FileNotFoundException(response.getReason());
            } else if (response.getStatus() >= 400) {
                throw new IOException(response.getReason());
            }
        } catch (EofException e) {
            logger.debug("newStreamSource(): {}", e.getMessage());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error(e.getMessage(), e);
            throw new IOException(e.getMessage(), e);
        }
        return new HTTPStreamSource(client, uri);
    }

    @Override
    public Format getSourceFormat() throws IOException {
        if (sourceFormat == null) {
            sourceFormat = Format.inferFormat(identifier);
            if (Format.UNKNOWN.equals(sourceFormat)) {
                sourceFormat = inferSourceFormatFromContentTypeHeader();
            }
            // This could throw a variety of exceptions if inaccessible, not
            // found, etc.
            try (InputStream is = newStreamSource().newInputStream()) {
                // no-op
            }
        }
        return sourceFormat;
    }

    URI getURI() throws Exception {
        final Configuration config = Configuration.getInstance();
        switch (config.getString(Key.HTTPRESOLVER_LOOKUP_STRATEGY)) {
            case "BasicLookupStrategy":
                return getURIWithBasicStrategy();
            case "ScriptLookupStrategy":
                return getURIWithScriptStrategy();
            default:
                throw new ConfigurationException(Key.HTTPRESOLVER_LOOKUP_STRATEGY +
                        " is invalid or not set");
        }
    }

    private URI getURIWithBasicStrategy() throws ConfigurationException {
        final Configuration config = Configuration.getInstance();
        final String prefix = config.getString(Key.HTTPRESOLVER_URL_PREFIX, "");
        final String suffix = config.getString(Key.HTTPRESOLVER_URL_SUFFIX, "");
        try {
            return new URI(prefix + identifier.toString() + suffix);
        } catch (URISyntaxException e) {
            throw new ConfigurationException(e.getMessage());
        }
    }

    /**
     * @throws URISyntaxException If {@link #GET_URL_DELEGATE_METHOD} returns
     *                            an invalid URI.
     * @throws IOException
     * @throws ScriptException If the script fails to execute.
     * @throws DelegateScriptDisabledException
     */
    private URI getURIWithScriptStrategy() throws URISyntaxException,
            IOException, ScriptException, DelegateScriptDisabledException {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final Object result = engine.invoke(GET_URL_DELEGATE_METHOD,
                identifier.toString());
        if (result == null) {
            throw new FileNotFoundException(GET_URL_DELEGATE_METHOD +
                    " returned nil for " + identifier);
        }
        return new URI((String) result);
    }

    /**
     * Issues an HTTP HEAD request and checks the response
     * <code>Content-Type</code> header to determine the source format.
     *
     * @return Inferred source format, or {@link Format#UNKNOWN} if unknown.
     */
    private Format inferSourceFormatFromContentTypeHeader() {
        Format format = Format.UNKNOWN;
        try {
            final URI uri = getURI();
            Request request = getHTTPClient(uri).newRequest(uri).
                    timeout(REQUEST_TIMEOUT, TimeUnit.SECONDS).
                    method(HttpMethod.HEAD);
            Response response = request.send();

            if (response.getStatus() >= 200 && response.getStatus() < 300) {
                HttpField field = response.getHeaders().getField("Content-Type");
                if (field != null && field.getValue() != null) {
                    format = new MediaType(field.getValue()).toFormat();
                } else {
                    logger.warn("No Content-Type header for HEAD {}", uri);
                }
            } else {
                logger.warn("HEAD returned status {} for {}",
                        response.getStatus(), uri);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return format;
    }

}
