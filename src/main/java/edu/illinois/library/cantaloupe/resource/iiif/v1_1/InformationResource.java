package edu.illinois.library.cantaloupe.resource.iiif.v1_1;

import java.awt.Dimension;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.illinois.library.cantaloupe.ImageServerApplication;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.StreamProcessor;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.FileResolver;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.resolver.StreamResolver;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Reference;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

/**
 * Handles IIIF Image API 1.1 information requests.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#image-info-request">Information
 * Requests</a>
 */
public class InformationResource extends AbstractResource {

    /**
     * Responds to information requests.
     *
     * @return StringRepresentation
     * @throws Exception
     */
    @Get("json")
    public StringRepresentation doGet() throws Exception {
        Map<String,Object> attrs = this.getRequest().getAttributes();
        String identifier = (String) attrs.get("identifier");
        Identifier internalId = new Identifier(identifier);
        // Get the resolver
        Resolver resolver = ResolverFactory.getResolver();
        // Determine the format of the source image
        SourceFormat sourceFormat = resolver.getSourceFormat(internalId);
        if (sourceFormat.equals(SourceFormat.UNKNOWN)) {
            throw new UnsupportedSourceFormatException();
        }
        // Obtain an instance of the processor assigned to that format in
        // the config file
        Processor proc = ProcessorFactory.getProcessor(sourceFormat);
        // Get an ImageInfo instance corresponding to the source image
        ComplianceLevel complianceLevel = ComplianceLevel.getLevel(
                proc.getSupportedFeatures(sourceFormat),
                proc.getSupportedIiif1_1Qualities(sourceFormat),
                proc.getAvailableOutputFormats(sourceFormat));
        ImageInfo imageInfo = getImageInfo(internalId,
                getSize(internalId, proc, resolver, sourceFormat),
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
        imageInfo.setId(getImageUri(identifier));
        imageInfo.setWidth(fullSize.width);
        imageInfo.setHeight(fullSize.height);
        imageInfo.setProfile(complianceLevel.getUri());
        // totally arbitrary
        imageInfo.setTileWidth(512);
        imageInfo.setTileHeight(512);

        // scale factors
        for (short i = 0; i < 5; i++) {
            imageInfo.getScaleFactors().add((int) Math.pow(2, i));
        }

        // formats
        for (OutputFormat format : outputFormats) {
            imageInfo.getFormats().add(format.getExtension());
        }

        // qualities
        for (Quality quality : qualities) {
            String qualityStr = quality.toString().toLowerCase();
            if (quality.equals(Filter.DEFAULT)) {
                qualityStr = "native";
            }
            imageInfo.getQualities().add(qualityStr);
        }

        return imageInfo;
    }

    private String getImageUri(Identifier identifier) {
        return getPublicRootRef() + ImageServerApplication.IIIF_PATH +
                "/" + Reference.encode(identifier.toString());
    }

    /**
     * Gets the size of the image corresponding to the given identifier, first
     * by checking the cache and then, if necessary, by reading it from the
     * image and caching the result.
     *
     * @param identifier
     * @param proc
     * @param resolver
     * @param sourceFormat
     * @return
     * @throws Exception
     */
    private Dimension getSize(Identifier identifier, Processor proc,
                              Resolver resolver, SourceFormat sourceFormat)
            throws Exception {
        Dimension size = null;
        Cache cache = CacheFactory.getInstance();
        if (cache != null) {
            size = cache.getDimension(identifier);
            if (size == null) {
                size = readSize(identifier, resolver, proc, sourceFormat);
                cache.putDimension(identifier, size);
            }
        }
        if (size == null) {
            size = readSize(identifier, resolver, proc, sourceFormat);
        }
        return size;
    }

    /**
     * Reads the size from the source image.
     *
     * @param identifier
     * @param resolver
     * @param proc
     * @param sourceFormat
     * @return
     * @throws Exception
     */
    private Dimension readSize(Identifier identifier, Resolver resolver,
                               Processor proc, SourceFormat sourceFormat)
            throws Exception {
        Dimension size = null;
        if (resolver instanceof FileResolver) {
            if (proc instanceof FileProcessor) {
                size = ((FileProcessor)proc).getSize(
                        ((FileResolver) resolver).getFile(identifier),
                        sourceFormat);
            } else if (proc instanceof StreamProcessor) {
                size = ((StreamProcessor)proc).getSize(
                        ((StreamResolver) resolver).getInputStream(identifier),
                        sourceFormat);
            }
        } else if (resolver instanceof StreamResolver) {
            if (!(proc instanceof StreamProcessor)) {
                // StreamResolvers don't support FileProcessors
            } else {
                size = ((StreamProcessor)proc).getSize(
                        ((StreamResolver) resolver).getInputStream(identifier),
                        sourceFormat);
            }
        }
        return size;
    }

}
