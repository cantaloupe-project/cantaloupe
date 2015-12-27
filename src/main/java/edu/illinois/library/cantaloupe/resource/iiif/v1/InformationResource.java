package edu.illinois.library.cantaloupe.resource.iiif.v1;

import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
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
        // Get the resolver
        Resolver resolver = ResolverFactory.getResolver(identifier);
        SourceFormat sourceFormat = SourceFormat.UNKNOWN;
        try {
            // Determine the format of the source image
            sourceFormat = resolver.getSourceFormat(identifier);
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

        if (sourceFormat.equals(SourceFormat.UNKNOWN)) {
            throw new UnsupportedSourceFormatException();
        }
        // Obtain an instance of the processor assigned to that format in
        // the config file
        Processor proc = ProcessorFactory.getProcessor(sourceFormat);

        checkProcessorResolverCompatibility(resolver, proc);

        // Get an ImageInfo instance corresponding to the source image
        ComplianceLevel complianceLevel = ComplianceLevel.getLevel(
                proc.getSupportedFeatures(sourceFormat),
                proc.getSupportedIiif1_1Qualities(sourceFormat),
                proc.getAvailableOutputFormats(sourceFormat));
        ImageInfo imageInfo = getImageInfo(identifier,
                getSize(identifier, proc, resolver, sourceFormat),
                complianceLevel,
                proc.getSupportedIiif1_1Qualities(sourceFormat),
                proc.getAvailableOutputFormats(sourceFormat));
        // Transform the ImageInfo into JSON
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writer().
                without(SerializationFeature.WRITE_NULL_MAP_VALUES).
                without(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS).
                writeValueAsString(imageInfo);
        // could use JacksonRepresentation here; not sure whether it's worth bothering
        StringRepresentation rep = new StringRepresentation(json);

        this.addHeader("Link", String.format("<%s>;rel=\"profile\";",
                complianceLevel.getUri()));

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

    private ImageInfo getImageInfo(Identifier identifier, Dimension fullSize,
                                   ComplianceLevel complianceLevel,
                                   Set<Quality> qualities,
                                   Set<OutputFormat> outputFormats) {
        ImageInfo imageInfo = new ImageInfo();
        imageInfo.id = getImageUri(identifier);
        imageInfo.width = fullSize.width;
        imageInfo.height = fullSize.height;
        imageInfo.profile = complianceLevel.getUri();
        // totally arbitrary
        imageInfo.tileWidth = 512;
        imageInfo.tileHeight = 512;

        // scale factors
        for (short i = 0; i < 5; i++) {
            imageInfo.scaleFactors.add((int) Math.pow(2, i));
        }

        // formats
        for (OutputFormat format : outputFormats) {
            imageInfo.formats.add(format.getExtension());
        }

        // qualities
        for (Quality quality : qualities) {
            String qualityStr = quality.toString().toLowerCase();
            if (quality.equals(Filter.NONE)) {
                qualityStr = "native";
            }
            imageInfo.qualities.add(qualityStr);
        }

        return imageInfo;
    }

    private String getImageUri(Identifier identifier) {
        return getPublicRootRef() + WebApplication.IIIF_1_PATH +
                "/" + Reference.encode(identifier.toString());
    }

}
