package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.restlet.Client;
import org.restlet.Response;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.util.Arrays;

/**
 * <p>Provides access to source content located on an HTTP(S) server.</p>
 *
 * <h3>Format Determination</h3>
 *
 * <p>For images with extensions, the extension will be assumed to correctly
 * denote the image format, based on the return value of
 * {@link Format#inferFormat(Identifier)}. For images with extensions that are
 * missing or unrecognized, the Content-Type header will be checked to
 * determine their format (in a separate request), which will incur a small
 * performance penalty. It is therefore more efficient to serve images with
 * extensions.</p>
 *
 * <h3>Lookup Strategies</h3>
 *
 * <p>Two distinct lookup strategies are supported, defined by
 * {@link #LOOKUP_STRATEGY_CONFIG_KEY}. BasicLookupStrategy locates images by
 * concatenating a pre-defined URL prefix and/or suffix. ScriptLookupStrategy
 * invokes a delegate method to retrieve a URL dynamically.</p>
 */
class HttpResolver extends AbstractResolver implements StreamResolver {

    private static class HttpStreamSource implements StreamSource {

        private final Client client;
        private final Reference url;

        HttpStreamSource(Client client, Reference url) {
            this.client = client;
            this.url = url;
        }

        @Override
        public ImageInputStream newImageInputStream() throws IOException {
            return ImageIO.createImageInputStream(newInputStream());
        }

        @Override
        public InputStream newInputStream() throws IOException {
            ClientResource resource = newClientResource(url);
            resource.setNext(client);
            try {
                return resource.get().getStream();
            } catch (ResourceException e) {
                throw new IOException(e.getMessage(), e);
            } finally {
                resource.release();
            }
        }

    }

    private static Logger logger = LoggerFactory.getLogger(HttpResolver.class);

    static final String BASIC_AUTH_SECRET_CONFIG_KEY =
            "HttpResolver.auth.basic.secret";
    static final String BASIC_AUTH_USERNAME_CONFIG_KEY =
            "HttpResolver.auth.basic.username";
    static final String LOOKUP_STRATEGY_CONFIG_KEY =
            "HttpResolver.lookup_strategy";
    static final String URL_PREFIX_CONFIG_KEY =
            "HttpResolver.BasicLookupStrategy.url_prefix";
    static final String URL_SUFFIX_CONFIG_KEY =
            "HttpResolver.BasicLookupStrategy.url_suffix";

    static final String GET_URL_DELEGATE_METHOD = "HttpResolver::get_url";

    private final Client client = new Client(
            Arrays.asList(Protocol.HTTP, Protocol.HTTPS));

    private ResourceException headRequestException;
    private Response headResponse;

    /**
     * Factory method. Be sure to call {@link ClientResource#release()} when
     * done with the instance.
     *
     * @param url
     * @return New ClientResource respecting HttpResolver configuration
     * options.
     */
    private static ClientResource newClientResource(final Reference url) {
        final ClientResource resource = new ClientResource(url);
        final Configuration config = ConfigurationFactory.getInstance();
        final String username = config.getString(BASIC_AUTH_USERNAME_CONFIG_KEY, "");
        final String secret = config.getString(BASIC_AUTH_SECRET_CONFIG_KEY, "");
        if (username.length() > 0 && secret.length() > 0) {
            resource.setChallengeResponse(ChallengeScheme.HTTP_BASIC,
                    username, secret);
        }
        return resource;
    }

    @Override
    public StreamSource newStreamSource() throws IOException {
        // Check whether the underlying resource is accessible.
        getHEADResponse();

        Reference url = getUrl();
        ClientResource resource = newClientResource(url);
        resource.setNext(client);
        return new HttpStreamSource(client, url);
    }

    @Override
    public Format getSourceFormat() throws IOException {
        if (sourceFormat == null) {
            sourceFormat = Format.inferFormat(identifier);
            if (sourceFormat == Format.UNKNOWN) {
                sourceFormat = getSourceFormatFromContentTypeHeader();
            }
            newStreamSource(); // throws IOException if not found etc.
        }
        return sourceFormat;
    }

    public Reference getUrl() throws IOException {
        final Configuration config = ConfigurationFactory.getInstance();
        switch (config.getString(LOOKUP_STRATEGY_CONFIG_KEY)) {
            case "BasicLookupStrategy":
                return getUrlWithBasicStrategy();
            case "ScriptLookupStrategy":
                try {
                    return getUrlWithScriptStrategy();
                } catch (ScriptException | DelegateScriptDisabledException e) {
                    logger.error(e.getMessage(), e);
                    throw new IOException(e);
                }
            default:
                throw new IOException(LOOKUP_STRATEGY_CONFIG_KEY +
                        " is invalid or not set");
        }
    }

    /**
     * Issues an HTTP HEAD request and checks the Content-Type header in the
     * response to determine the source format.
     *
     * @return A source format, or {@link Format#UNKNOWN} if unknown.
     * @throws IOException
     */
    private Format getSourceFormatFromContentTypeHeader() throws IOException {
        Format format = Format.UNKNOWN;
        String contentType = "";
        Reference url = getUrl();
        try {
            Response response = getHEADResponse();
            contentType = response.getHeaders().
                    getFirstValue("Content-Type", true);
            if (contentType != null) {
                format = new MediaType(contentType).toFormat();
            }
        } catch (ResourceException e) {
            // nothing we can do but log it
            if (contentType.length() > 0) {
                logger.warn("Failed to determine the source format of the " +
                        "resource at {} based on a Content-Type of {}.",
                        url, contentType);
            } else {
                logger.warn("Failed to determine the source format of the " +
                        "resource at {}. The web server's response must " +
                        "include a Content-Type header with the value of " +
                        "the media (MIME) type of the source image.", url);
            }
        }
        return format;
    }

    private Reference getUrlWithBasicStrategy() {
        final Configuration config = ConfigurationFactory.getInstance();
        final String prefix = config.getString(URL_PREFIX_CONFIG_KEY, "");
        final String suffix = config.getString(URL_SUFFIX_CONFIG_KEY, "");
        return new Reference(prefix + identifier.toString() + suffix);
    }

    /**
     * @return
     * @throws FileNotFoundException If the delegate script does not exist
     * @throws IOException
     * @throws ScriptException If the script fails to execute
     * @throws DelegateScriptDisabledException
     */
    private Reference getUrlWithScriptStrategy()
            throws IOException, ScriptException,
            DelegateScriptDisabledException {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final Object result = engine.invoke(GET_URL_DELEGATE_METHOD,
                identifier.toString());
        if (result == null) {
            throw new FileNotFoundException(GET_URL_DELEGATE_METHOD +
                    " returned nil for " + identifier);
        }
        return new Reference((String) result);
    }

    /**
     * @return HEAD response. The result is cached.
     * @throws FileNotFoundException If the resource is not found (HTTP 404 or
     *                               410).
     * @throws AccessDeniedException If the resource is not accessible (HTTP
     *                               401 or 403).
     * @throws IOException           If there is some other error (HTTP 400 or
     *                               above).
     */
    private Response getHEADResponse() throws IOException {
        if (headResponse == null) {
            Reference url = getUrl();
            logger.info("Resolved {} to {}", identifier, url);

            ClientResource resource = newClientResource(url);
            resource.setNext(client);
            try {
                resource.head();
                headResponse = resource.getResponse();
            } catch (ResourceException e) {
                headRequestException = e;
            } finally {
                resource.release();
            }
        }

        if (headRequestException != null) {
            final Status status = headRequestException.getStatus();
            if (Status.CLIENT_ERROR_NOT_FOUND.equals(status) ||
                    Status.CLIENT_ERROR_GONE.equals(status)) {
                throw new FileNotFoundException(headRequestException.getMessage());
            } else if (Status.CLIENT_ERROR_UNAUTHORIZED.equals(status) ||
                    Status.CLIENT_ERROR_FORBIDDEN.equals(status)) {
                throw new AccessDeniedException(headRequestException.getMessage());
            } else {
                throw new IOException(headRequestException.getMessage(),
                        headRequestException);
            }
        }

        return headResponse;
    }

    @Override
    public void setIdentifier(Identifier identifier) {
        super.setIdentifier(identifier);
        headRequestException = null;
        headResponse = null;
    }

}
