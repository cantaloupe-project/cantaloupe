package edu.illinois.library.cantaloupe.resource;

import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.illinois.library.cantaloupe.Feature;
import edu.illinois.library.cantaloupe.ImageServerApplication;
import edu.illinois.library.cantaloupe.image.ImageInfo;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.ProcessorFeature;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Quality;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Reference;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

/**
 * Handles IIIF information requests.
 *
 * @see <a href="http://iiif.io/api/image/2.0/#information-request">Information
 * Requests</a>
 */
public class InformationResource extends AbstractResource {

    @Get("json")
    public Representation doGet() throws Exception {
        // 1. Assemble the URI parameters into a Parameters object
        Map<String,Object> attrs = this.getRequest().getAttributes();
        String identifier = Reference.decode((String) attrs.get("identifier"));
        // 2. Obtain a reference to the source image as a File and/or an
        // InputStream
        Resolver resolver = ResolverFactory.getResolver();
        File sourceFile = resolver.getFile(identifier);
        InputStream inputStream = resolver.getInputStream(identifier);
        // 3. Determine the format of the source image
        SourceFormat sourceFormat = resolver.getSourceFormat(identifier);
        // 4. Obtain an instance of the processor assigned to that format in
        // the config file
        Processor proc = ProcessorFactory.getProcessor(sourceFormat);
        // 5. Get an ImageInfo instance corresponding to the source image
        ImageInfo imageInfo;
        if (sourceFile != null) {
            imageInfo = getImageInfo(identifier,
                    proc.getSize(sourceFile, sourceFormat),
                    proc.getSupportedQualities(sourceFormat),
                    proc.getSupportedFeatures(sourceFormat),
                    proc.getAvailableOutputFormats(sourceFormat));
        } else {
            imageInfo = getImageInfo(identifier,
                    proc.getSize(inputStream, sourceFormat),
                    proc.getSupportedQualities(sourceFormat),
                    proc.getSupportedFeatures(sourceFormat),
                    proc.getAvailableOutputFormats(sourceFormat));
        }
        // 6. Transform the ImageInfo into JSON
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

    private ImageInfo getImageInfo(String identifier, Dimension size,
                                   Set<Quality> qualities,
                                   Set<ProcessorFeature> features,
                                   Set<OutputFormat> outputFormats) {
        ImageInfo imageInfo = new ImageInfo();
        imageInfo.setId(getImageUri(identifier));
        imageInfo.setWidth(size.width);
        imageInfo.setHeight(size.height);

        imageInfo.getProfile().add("http://iiif.io/api/image/2/level2.json"); // TODO: automatically determine this

        // formats
        Map<String, Set<String>> formatMap = new HashMap<String, Set<String>>();
        Set<String> formatStrings = new HashSet<String>();
        for (OutputFormat format : outputFormats) {
            formatStrings.add(format.getExtension());
        }
        formatMap.put("formats", formatStrings);
        imageInfo.getProfile().add(formatMap);

        // qualities
        Map<String, Set<String>> qualityMap = new HashMap<String, Set<String>>();
        Set<String> qualityStrings = new HashSet<String>();
        for (Quality quality : qualities) {
            qualityStrings.add(quality.toString().toLowerCase());
        }
        qualityMap.put("qualities", qualityStrings);
        imageInfo.getProfile().add(qualityMap);

        // supports
        Map<String, Set<String>> featureMap = new HashMap<String, Set<String>>();
        Set<String> featureStrings = new HashSet<String>();
        for (Feature feature : features) {
            featureStrings.add(feature.getName());
        }
        featureMap.put("features", featureStrings);
        imageInfo.getProfile().add(featureMap);

        return imageInfo;
    }

    private String getImageUri(String identifier) {
        return this.getRootRef() + ImageServerApplication.BASE_IIIF_PATH +
                "/" + Reference.encode(identifier);
    }

}
