package edu.illinois.library.cantaloupe.resource.iiif.v1;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.SerializationFeature;
import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.resource.SourceImageWrangler;
import org.restlet.Request;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Reference;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

/**
 * Handles IIIF Image API 1.x information requests.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#image-info-request">Information
 * Requests</a>
 */
public class InformationResource extends IIIF1Resource {

    /**
     * Redirects /{identifier} to /{identifier}/info.json, respecting the
     * Servlet context root and {@link #PUBLIC_IDENTIFIER_HEADER} header.
     */
    public static class RedirectingResource extends IIIF1Resource {
        @Get
        public Representation doGet() {
            final Reference newRef = new Reference(
                    getPublicRootReference() +
                            WebApplication.IIIF_1_PATH + "/" +
                            getPublicIdentifier() +
                            "/info.json");
            redirectSeeOther(newRef);
            return new EmptyRepresentation();
        }
    }

    /**
     * Responds to information requests.
     *
     * @return JacksonRepresentation that will write an {@link ImageInfo}
     *         instance to JSON.
     */
    @Get
    public Representation doGet() throws Exception {
        final Map<String,Object> attrs = getRequest().getAttributes();
        final String urlIdentifier = (String) attrs.get("identifier");
        final String decodedIdentifier = Reference.decode(urlIdentifier);
        final String reSlashedIdentifier = decodeSlashes(decodedIdentifier);
        final Identifier identifier = new Identifier(reSlashedIdentifier);

        // Get the resolver
        Resolver resolver = ResolverFactory.getResolver(identifier);
        Format format = Format.UNKNOWN;
        try {
            // Determine the format of the source image
            format = resolver.getSourceFormat();
        } catch (FileNotFoundException e) {
            if (ConfigurationFactory.getInstance().
                    getBoolean(Cache.PURGE_MISSING_CONFIG_KEY, false)) {
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
        final Processor processor = ProcessorFactory.getProcessor(format);

        new SourceImageWrangler(resolver, processor, identifier).wrangle();

        // Get an Info instance corresponding to the source image
        ImageInfo imageInfo = ImageInfoFactory.newImageInfo(
                getImageUri(), processor, getOrReadInfo(identifier, processor));

        getResponse().getHeaders().add("Link",
                String.format("<%s>;rel=\"profile\";", imageInfo.profile));

        JacksonRepresentation rep = new JacksonRepresentation<>(imageInfo);

        // If the client has requested JSON-LD, set the content type to
        // that; otherwise set it to JSON
        List<Preference<MediaType>> preferences = this.getRequest().
                getClientInfo().getAcceptedMediaTypes();
        if (preferences.get(0) != null && preferences.get(0).toString().
                startsWith("application/ld+json")) {
            rep.setMediaType(new MediaType("application/ld+json"));
        } else {
            rep.setMediaType(new MediaType("application/json"));
        }

        rep.getObjectWriter().
                without(SerializationFeature.WRITE_NULL_MAP_VALUES).
                without(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
        rep.setCharacterSet(CharacterSet.UTF_8);

        // Add client cache header(s) if configured to do so. We do this later
        // rather than sooner to prevent them from being sent along with an
        // error response.
        getResponseCacheDirectives().addAll(getCacheDirectives());

        return rep;
    }

    /**
     * @return Full image URI corresponding to the given identifier, respecting
     *         the X-Forwarded-* and {@link #PUBLIC_IDENTIFIER_HEADER}
     *         reverse proxy headers.
     */
    private String getImageUri() {
        return getPublicRootReference() + WebApplication.IIIF_1_PATH + "/" +
                Reference.encode(getPublicIdentifier());
    }

}
