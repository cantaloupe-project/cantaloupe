package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.apache.commons.configuration.Configuration;
import org.restlet.Client;
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

class HttpResolver extends AbstractResolver implements StreamResolver {

    private static class HttpStreamSource implements StreamSource {

        private final Client client;
        private final Reference url;

        public HttpStreamSource(Client client, Reference url) {
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

    public static final String BASIC_AUTH_SECRET_CONFIG_KEY =
            "HttpResolver.auth.basic.secret";
    public static final String BASIC_AUTH_USERNAME_CONFIG_KEY =
            "HttpResolver.auth.basic.username";
    public static final String LOOKUP_STRATEGY_CONFIG_KEY =
            "HttpResolver.lookup_strategy";
    public static final String URL_PREFIX_CONFIG_KEY =
            "HttpResolver.BasicLookupStrategy.url_prefix";
    public static final String URL_SUFFIX_CONFIG_KEY =
            "HttpResolver.BasicLookupStrategy.url_suffix";

    private final Client client = new Client(
            Arrays.asList(Protocol.HTTP, Protocol.HTTPS));

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
        final Configuration config = Application.getConfiguration();
        final String username = config.getString(BASIC_AUTH_USERNAME_CONFIG_KEY, "");
        final String secret = config.getString(BASIC_AUTH_SECRET_CONFIG_KEY, "");
        if (username.length() > 0 && secret.length() > 0) {
            resource.setChallengeResponse(ChallengeScheme.HTTP_BASIC,
                    username, secret);
        }
        return resource;
    }

    @Override
    public StreamSource getStreamSource() throws IOException {
        Reference url = getUrl();
        logger.info("Resolved {} to {}", identifier, url);
        ClientResource resource = newClientResource(url);
        resource.setNext(client);
        try {
            // Issue an HTTP HEAD request to check whether the underlying
            // resource is accessible.
            resource.head();
            return new HttpStreamSource(client, url);
        } catch (ResourceException e) {
            if (e.getStatus().equals(Status.CLIENT_ERROR_NOT_FOUND) ||
                    e.getStatus().equals(Status.CLIENT_ERROR_GONE)) {
                throw new FileNotFoundException(e.getMessage());
            } else if (e.getStatus().equals(Status.CLIENT_ERROR_FORBIDDEN)) {
                throw new AccessDeniedException(e.getMessage());
            } else {
                throw new IOException(e.getMessage(), e);
            }
        } finally {
            resource.release();
        }
    }

    @Override
    public Format getSourceFormat() throws IOException {
        Format format = ResolverUtil.inferSourceFormat(identifier);
        if (format == Format.UNKNOWN) {
            format = getSourceFormatFromContentTypeHeader();
        }
        getStreamSource().newInputStream(); // throws IOException if not found etc.
        return format;
    }

    public Reference getUrl() throws IOException {
        final Configuration config = Application.getConfiguration();

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
        ClientResource resource = newClientResource(url);
        resource.setNext(client);
        try {
            resource.head();
            contentType = resource.getResponse().getHeaders().
                    getFirstValue("Content-Type", true);
            if (contentType != null) {
                format = Format.getFormat(contentType);
            }
        } catch (ResourceException e) {
            // nothing we can do but log it
            if (contentType.length() > 0) {
                logger.warn("Failed to determine source format based on a " +
                        "Content-Type of {}", contentType);
            } else {
                logger.warn("Failed to determine source format (missing " +
                        "Content-Type at {})", url);
            }
        } finally {
            resource.release();
        }
        return format;
    }

    private Reference getUrlWithBasicStrategy() {
        final Configuration config = Application.getConfiguration();
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
        final String[] args = { identifier.toString() };
        final String method = "get_url";
        final Object result = engine.invoke(method, args);
        if (result == null) {
            throw new FileNotFoundException(method + " returned nil for " +
                    identifier);
        }
        return new Reference((String) result);
    }

}
