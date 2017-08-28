package edu.illinois.library.cantaloupe.resource.iiif.v2;

import java.io.FileNotFoundException;
import java.util.List;

import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.resource.JSONRepresentation;
import edu.illinois.library.cantaloupe.processor.ProcessorConnector;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import org.restlet.Request;
import org.restlet.data.Header;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Reference;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.util.Series;

/**
 * Handles IIIF Image API 2.x information requests.
 *
 * @see <a href="http://iiif.io/api/image/2.1/#information-request">Information
 * Requests</a>
 */
public class InformationResource extends IIIF2Resource {

    /**
     * Redirects /{identifier} to /{identifier}/info.json, respecting the
     * Servlet context root.
     */
    public static class RedirectingResource extends IIIF2Resource {
        @Get
        public Representation doGet() {
            final Request request = getRequest();
            final String identifier =
                    (String) request.getAttributes().get("identifier");
            final Reference newRef = new Reference(
                    getPublicRootRef(request.getRootRef(), request.getHeaders()) +
                            RestletApplication.IIIF_2_PATH + "/" + identifier +
                            "/info.json");
            redirectSeeOther(newRef);
            return new EmptyRepresentation();
        }
    }

    /**
     * Responds to information requests.
     *
     * @return {@link ImageInfo} instance serializedto JSON.
     */
    @Get
    public Representation doGet() throws Exception {
        final Identifier identifier = getIdentifier();
        final Resolver resolver = new ResolverFactory().newResolver(identifier);
        final MediaType mediaType = getNegotiatedMediaType();

        // Setup the resolver context.
        final RequestContext requestContext = new RequestContext();
        requestContext.setRequestURI(getReference().toString());
        requestContext.setRequestHeaders(getRequest().getHeaders().getValuesMap());
        requestContext.setClientIP(getCanonicalClientIpAddress());
        requestContext.setCookies(getRequest().getCookies().getValuesMap());
        resolver.setContext(requestContext);

        // Determine the format of the source image.
        Format format;
        try {
            format = resolver.getSourceFormat();
        } catch (FileNotFoundException e) { // this needs to be rethrown
            if (Configuration.getInstance().
                    getBoolean(Key.CACHE_SERVER_PURGE_MISSING, false)) {
                // if the image was not found, purge it from the cache
                final Cache cache = CacheFactory.getDerivativeCache();
                if (cache != null) {
                    cache.purge(identifier);
                }
            }
            throw e;
        }

        // Obtain an instance of the processor assigned to that format.
        final Processor processor = new ProcessorFactory().newProcessor(format);

        // Connect it to the resolver.
        new ProcessorConnector(resolver, processor, identifier).connect();

        final ImageInfo<String, Object> imageInfo =
                new ImageInfoFactory().newImageInfo(
                        identifier, getImageUri(identifier), processor,
                        getOrReadInfo(identifier, processor));

        commitCustomResponseHeaders();

        return new JSONRepresentation(imageInfo, mediaType);
    }

    /**
     * @param identifier
     * @return Full image URI corresponding to the given identifier, respecting
     *         the X-Forwarded-* and X-IIIF-ID reverse proxy headers.
     */
    private String getImageUri(Identifier identifier) {
        final Series<Header> headers = getRequest().getHeaders();
        final String identifierStr = headers.getFirstValue(
                "X-IIIF-ID", true, identifier.toString());
        return getPublicRootRef(getRequest().getRootRef(), headers) +
                RestletApplication.IIIF_2_PATH + "/" +
                Reference.encode(identifierStr);
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

}
