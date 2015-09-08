package edu.illinois.library.cantaloupe.resource;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.illinois.library.cantaloupe.ImageServerApplication;
import edu.illinois.library.cantaloupe.image.ImageInfo;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
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
        this.addHeader("Link", "<http://iiif.io/api/image/2/context.json>; " +
                "rel=\"http://www.w3.org/ns/json-ld#context\"; type=\"application/ld+json\"");

        Map<String,Object> attrs = this.getRequest().getAttributes();
        String identifier = Reference.decode((String) attrs.get("identifier"));

        Resolver resolver = ResolverFactory.getResolver();
        File sourceFile = resolver.getFile(identifier);
        InputStream inputStream = resolver.getInputStream(identifier);

        SourceFormat sourceFormat = SourceFormat.getSourceFormat(identifier);
        Processor proc = ProcessorFactory.getProcessor(sourceFormat);
        ImageInfo imageInfo;
        if (sourceFile != null) {
            imageInfo = proc.getImageInfo(sourceFile, sourceFormat,
                    this.getImageUri(identifier));
        } else {
            imageInfo = proc.getImageInfo(inputStream, sourceFormat,
                    this.getImageUri(identifier));
        }

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writer().
                without(SerializationFeature.WRITE_NULL_MAP_VALUES).
                without(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS).
                writeValueAsString(imageInfo);
        // TODO: could use JacksonRepresentation; not sure whether it's worth bothering
        StringRepresentation rep = new StringRepresentation(json);

        // if the client has requested JSON-LD, set a content type of JSON-LD;
        // otherwise set it to JSON
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

    private String getImageUri(String identifier) {
        return this.getRootRef() + ImageServerApplication.BASE_IIIF_PATH +
                "/" + Reference.encode(identifier);
    }

}
