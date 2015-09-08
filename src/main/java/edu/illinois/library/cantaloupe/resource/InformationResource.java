package edu.illinois.library.cantaloupe.resource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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
        StringRepresentation rep = new StringRepresentation(json);
        rep.setMediaType(new MediaType("application/json"));
        return rep;
    }

    private String getImageUri(String identifier) {
        return this.getRootRef() + ImageServerApplication.BASE_IIIF_PATH +
                "/" + Reference.encode(identifier);
    }

}
