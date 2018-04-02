package edu.illinois.library.cantaloupe.resource.iiif.v2;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.List;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.resource.JSONRepresentation;
import edu.illinois.library.cantaloupe.processor.ProcessorConnector;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Reference;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

/**
 * Handles IIIF Image API 2.x information requests.
 *
 * @see <a href="http://iiif.io/api/image/2.1/#information-request">Information
 * Requests</a>
 */
public class InformationResource extends IIIF2Resource {

    /**
     * Redirects <code>/{identifier}</code> to
     * <code>/{identifier}/info.json</code>, respecting the Servlet context
     * root and {@link #PUBLIC_IDENTIFIER_HEADER} header.
     */
    public static class RedirectingResource extends IIIF2Resource {
        @Get
        public Representation doGet() {
            final Reference newRef = new Reference(
                    getPublicRootReference() +
                            RestletApplication.IIIF_2_PATH + "/" +
                            getPublicIdentifier() +
                            "/info.json");
            redirectSeeOther(newRef);
            return new EmptyRepresentation();
        }
    }

    /**
     * Responds to information requests.
     *
     * @return {@link ImageInfo} instance serialized as JSON.
     */
    @Get
    public Representation doGet() throws Exception {
        final Configuration config = Configuration.getInstance();
        final Identifier identifier = getIdentifier();
        final CacheFacade cacheFacade = new CacheFacade();

        // If we don't need to resolve first, and are using a cache, and the
        // cache contains an info matching the request, skip all the setup and
        // just return the cached info.
        if (!config.getBoolean(Key.CACHE_SERVER_RESOLVE_FIRST, false)) {
            try {
                Info info = cacheFacade.getInfo(identifier);
                if (info != null) {
                    // The source format will be null or UNKNOWN if the info was
                    // serialized in version < 3.4.
                    final Format format = info.getSourceFormat();
                    if (format != null && !Format.UNKNOWN.equals(format)) {
                        final Processor processor = new ProcessorFactory().
                                newProcessor(format);
                        commitCustomResponseHeaders();
                        return newRepresentation(identifier, info, processor);
                    }
                }
            } catch (IOException e) {
                // Don't rethrow -- it's still possible to service the request.
                getLogger().severe(e.getMessage());
            }
        }

        final Resolver resolver = new ResolverFactory().
                newResolver(identifier, getRequestContext());

        try {
            resolver.checkAccess();
        } catch (NoSuchFileException e) { // this needs to be rethrown!
            if (config.getBoolean(Key.CACHE_SERVER_PURGE_MISSING, false)) {
                // If the image was not found, purge it from the cache.
                cacheFacade.purgeAsync(identifier);
            }
            throw e;
        }

        // Determine the format of the source image.
        Format format = resolver.getSourceFormat();

        // Obtain an instance of the processor assigned to that format.
        try (Processor processor = new ProcessorFactory().newProcessor(format)) {
            // Connect it to the resolver.
            new ProcessorConnector().connect(resolver, processor, identifier);

            final Info info = getOrReadInfo(identifier, processor);

            commitCustomResponseHeaders();
            return newRepresentation(identifier, info, processor);
        }
    }

    /**
     * @return Full image URI corresponding to the given identifier, respecting
     *         the X-Forwarded-* and {@link #PUBLIC_IDENTIFIER_HEADER} reverse
     *         proxy headers.
     */
    private String getImageURI() {
        return getPublicRootReference() + RestletApplication.IIIF_2_PATH + "/" +
                Reference.encode(getPublicIdentifier());
    }

    private MediaType getNegotiatedMediaType() {
        MediaType mediaType;
        // If the client has requested JSON-LD, set the content type to
        // that; otherwise set it to JSON.
        List<Preference<MediaType>> preferences = getRequest().getClientInfo().
                getAcceptedMediaTypes();
        if (preferences.get(0) != null && preferences.get(0).toString().
                startsWith("application/ld+json")) {
            mediaType = new MediaType("application/ld+json");
        } else {
            mediaType = new MediaType("application/json");
        }
        return mediaType;
    }

    private Representation newRepresentation(Identifier identifier,
                                             Info info,
                                             Processor processor) {
        final ImageInfo<String, Object> imageInfo =
                new ImageInfoFactory().newImageInfo(
                        identifier, getImageURI(), processor, info);
        final MediaType mediaType = getNegotiatedMediaType();
        return new JSONRepresentation(imageInfo, mediaType);
    }

}
