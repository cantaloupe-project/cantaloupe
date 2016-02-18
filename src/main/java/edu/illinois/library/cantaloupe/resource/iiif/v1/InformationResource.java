package edu.illinois.library.cantaloupe.resource.iiif.v1;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Reference;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 * Handles IIIF Image API 1.1 information requests.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#image-info-request">Information
 * Requests</a>
 */
public class InformationResource extends AbstractResource {

    @Override
    protected void doInit() throws ResourceException {
        if (!Application.getConfiguration().
                getBoolean("endpoint.iiif.1.enabled", true)) {
            throw new EndpointDisabledException();
        }
        super.doInit();
    }

    /**
     * Responds to information requests.
     *
     * @return StringRepresentation
     * @throws Exception
     */
    @Get("json")
    public StringRepresentation doGet() throws Exception {
        Map<String,Object> attrs = this.getRequest().getAttributes();
        Identifier identifier = new Identifier(
                Reference.decode((String) attrs.get("identifier")));
        identifier = decodeSlashes(identifier);
        // Get the resolver
        Resolver resolver = ResolverFactory.getResolver(identifier);
        Format format = Format.UNKNOWN;
        try {
            // Determine the format of the source image
            format = resolver.getSourceFormat(identifier);
        } catch (FileNotFoundException e) {
            if (Application.getConfiguration().
                    getBoolean(PURGE_MISSING_CONFIG_KEY, false)) {
                // if the image was not found, purge it from the cache
                final Cache cache = CacheFactory.getInstance();
                if (cache != null) {
                    cache.purge(identifier);
                }
            }
            throw e;
        }

        // Obtain an instance of the processor assigned to that format in
        // the config file
        Processor proc = ProcessorFactory.getProcessor(resolver, identifier,
                format);

        // Get an ImageInfo instance corresponding to the source image
        ImageInfo imageInfo = ImageInfoFactory.newImageInfo(
                getImageUri(identifier), proc);
        StringRepresentation rep = new StringRepresentation(imageInfo.toJson());

        this.addHeader("Link", String.format("<%s>;rel=\"profile\";",
                imageInfo.profile));

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
        return rep;
    }

    private String getImageUri(Identifier identifier) {
        return getPublicRootRef(getRequest()) + WebApplication.IIIF_1_PATH +
                "/" + Reference.encode(identifier.toString());
    }

}
