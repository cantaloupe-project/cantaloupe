package edu.illinois.library.cantaloupe.resource.iiif.v2_0;

import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.resource.iiif.Feature;
import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Reference;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 * Handles IIIF information requests.
 *
 * @see <a href="http://iiif.io/api/image/2.0/#information-request">Information
 * Requests</a>
 */
public class InformationResource extends AbstractResource {

    private static final Set<ServiceFeature> SUPPORTED_SERVICE_FEATURES =
            new HashSet<>();

    static {
        SUPPORTED_SERVICE_FEATURES.add(ServiceFeature.SIZE_BY_WHITELISTED);
        SUPPORTED_SERVICE_FEATURES.add(ServiceFeature.BASE_URI_REDIRECT);
        SUPPORTED_SERVICE_FEATURES.add(ServiceFeature.CANONICAL_LINK_HEADER);
        SUPPORTED_SERVICE_FEATURES.add(ServiceFeature.CORS);
        SUPPORTED_SERVICE_FEATURES.add(ServiceFeature.JSON_LD_MEDIA_TYPE);
    }

    @Override
    protected void doInit() throws ResourceException {
        if (!Application.getConfiguration().
                getBoolean("endpoint.iiif.2.enabled", true)) {
            throw new EndpointDisabledException();
        }
        super.doInit();
    }

    /**
     * Responds to IIIF Information requests.
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
        // Determine the format of the source image
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
        ImageInfo imageInfo = getImageInfo(identifier,
                getSize(identifier, proc, resolver, sourceFormat),
                proc.getSupportedIiif2_0Qualities(sourceFormat),
                proc.getSupportedFeatures(sourceFormat),
                proc.getAvailableOutputFormats(sourceFormat));
        // Transform the ImageInfo into JSON
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writer().
                without(SerializationFeature.WRITE_NULL_MAP_VALUES).
                without(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS).
                writeValueAsString(imageInfo);
        // could use JacksonRepresentation here; not sure whether it's worth bothering
        StringRepresentation rep = new StringRepresentation(json);

        this.addHeader("Link", "<http://iiif.io/api/image/2/context.json>; " +
                "rel=\"http://www.w3.org/ns/json-ld#context\"; type=\"application/ld+json\"");

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

    private ImageInfo getImageInfo(Identifier identifier, Dimension fullSize,
                                   Set<Quality> qualities,
                                   Set<ProcessorFeature> processorFeatures,
                                   Set<OutputFormat> outputFormats) {
        ImageInfo imageInfo = new ImageInfo();
        imageInfo.id = getImageUri(identifier);
        imageInfo.width = fullSize.width;
        imageInfo.height = fullSize.height;

        final String complianceUri = ComplianceLevel.getLevel(
                SUPPORTED_SERVICE_FEATURES, processorFeatures, qualities,
                outputFormats).getUri();
        imageInfo.profile.add(complianceUri);

        // sizes
        final short maxReductionFactor = 4;
        final short minSize = 256;
        for (double i = 2; i <= Math.pow(2, maxReductionFactor); i *= 2) {
            final int width = (int) Math.round(fullSize.width / i);
            final int height = (int) Math.round(fullSize.height / i);
            if (width < minSize || height < minSize) {
                break;
            }
            ImageInfo.Size size = new ImageInfo.Size(width, height);
            imageInfo.sizes.add(0, size);
        }

        // formats
        Map<String, Set<String>> profileMap = new HashMap<>();
        Set<String> formatStrings = new HashSet<>();
        for (OutputFormat format : outputFormats) {
            formatStrings.add(format.getExtension());
        }
        profileMap.put("formats", formatStrings);
        imageInfo.profile.add(profileMap);

        // qualities
        Set<String> qualityStrings = new HashSet<>();
        for (Quality quality : qualities) {
            qualityStrings.add(quality.toString().toLowerCase());
        }
        profileMap.put("qualities", qualityStrings);

        // supports
        Set<String> featureStrings = new HashSet<>();
        for (Feature feature : processorFeatures) {
            featureStrings.add(feature.getName());
        }
        for (Feature feature : SUPPORTED_SERVICE_FEATURES) {
            featureStrings.add(feature.getName());
        }
        profileMap.put("supports", featureStrings);

        return imageInfo;
    }

    private String getImageUri(Identifier identifier) {
        return getPublicRootRef() + WebApplication.IIIF_2_0_PATH +
                "/" + Reference.encode(identifier.toString());
    }

}
