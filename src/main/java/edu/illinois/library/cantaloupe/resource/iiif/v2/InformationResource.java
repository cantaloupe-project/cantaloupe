package edu.illinois.library.cantaloupe.resource.iiif.v2;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.DerivativeFileCache;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.resource.JSONRepresentation;
import edu.illinois.library.cantaloupe.resource.SourceImageWrangler;
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
     * Redirects /{identifier} to /{identifier}/info.json, respecting the
     * Servlet context root.
     */
    public static class RedirectingResource extends IIIF2Resource {
        @Get
        public Representation doGet() {
            final String identifier = (String) this.getRequest().
                    getAttributes().get("identifier");
            final Reference newRef = new Reference(
                    getPublicRootRef(getRequest()) +
                            WebApplication.IIIF_2_PATH + "/" + identifier +
                            "/info.json");
            redirectSeeOther(newRef);
            return new EmptyRepresentation();
        }
    }

    /**
     * Responds to information requests.
     *
     * @return {@link ImageInfo} instance serializedto JSON.
     * @throws Exception
     */
    @Get
    public Representation doGet() throws Exception {
        Map<String,Object> attrs = this.getRequest().getAttributes();
        Identifier identifier = new Identifier(
                Reference.decode((String) attrs.get("identifier")));
        identifier = decodeSlashes(identifier);

        // Get the resolver
        Resolver resolver = ResolverFactory.getResolver(identifier);
        // Determine the format of the source image
        Format format = Format.UNKNOWN;
        try {
            // Determine the format of the source image
            format = resolver.getSourceFormat();
        } catch (FileNotFoundException e) {
            if (ConfigurationFactory.getInstance().
                    getBoolean(Key.CACHE_SERVER_PURGE_MISSING, false)) {
                // if the image was not found, purge it from the cache
                final Cache cache = CacheFactory.getDerivativeCache();
                if (cache != null) {
                    cache.purge(identifier);
                }
            }
            throw e;
        }

        // Obtain an instance of the processor assigned to that format in
        // the config file
        Processor processor = ProcessorFactory.getProcessor(format);

        new SourceImageWrangler(resolver, processor, identifier).wrangle();

        // If the cache is enabled and is file-based, add an X-Sendfile header.
        final Cache cache = CacheFactory.getDerivativeCache();
        if (cache instanceof DerivativeFileCache) {
            DerivativeFileCache fileCache = (DerivativeFileCache) cache;
            if (fileCache.infoExists(identifier)) {
                final String relativePathname =
                        ((DerivativeFileCache) cache).getRelativePathname(identifier);
                addXSendfileHeader(relativePathname);
            }
        }

        // Get an Info instance corresponding to the source image
        ImageInfo imageInfo = ImageInfoFactory.newImageInfo(
                identifier, getImageUri(identifier), processor,
                getOrReadInfo(identifier, processor));

        // Add client cache header(s) if configured to do so. We do this later
        // rather than sooner to prevent them from being sent along with an
        // error response.
        getResponseCacheDirectives().addAll(getCacheDirectives());

        JSONRepresentation rep = new JSONRepresentation(imageInfo);

        // 7. If the client has requested JSON-LD, set the content type to
        // that; otherwise set it to JSON
        List<Preference<MediaType>> preferences = this.getRequest().
                getClientInfo().getAcceptedMediaTypes();
        if (preferences.get(0) != null && preferences.get(0).toString().
                startsWith("application/ld+json")) {
            rep.setMediaType(new MediaType("application/ld+json"));
        } else {
            rep.setMediaType(new MediaType("application/json"));
        }

        return rep;
    }

    /**
     * @param identifier
     * @return Full image URI corresponding to the given identifier, respecting
     *         the X-Forwarded-* and X-IIIF-ID reverse proxy headers.
     */
    private String getImageUri(Identifier identifier) {
        final String identifierStr = getRequest().getHeaders().
                getFirstValue("X-IIIF-ID", true, identifier.toString());
        return getPublicRootRef(getRequest()) + WebApplication.IIIF_2_PATH +
                "/" + Reference.encode(identifierStr);
    }

}
