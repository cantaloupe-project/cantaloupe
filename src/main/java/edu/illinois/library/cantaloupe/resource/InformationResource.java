package edu.illinois.library.cantaloupe.resource;

import java.io.FileNotFoundException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.illinois.library.cantaloupe.image.ImageInfo;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import org.restlet.data.MediaType;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

public class InformationResource extends AbstractResource {

    @Get("json")
    public Representation doGet() throws FileNotFoundException,
            JsonProcessingException {
        this.addHeader("Link",
                "<http://iiif.io/api/image/2/level1.json>;rel=\"profile\"");
        this.addHeader("Link", "<http://iiif.io/api/image/2/context.json>; " +
                "rel=\"http://www.w3.org/ns/json-ld#context\"; type=\"application/ld+json\"");

        Map<String,Object> attrs = this.getRequest().getAttributes();
        String identifier = (String) attrs.get("identifier");
        Processor proc = ProcessorFactory.getProcessor();
        if (proc != null && !proc.resourceExists(identifier)) {
            throw new FileNotFoundException("Resource not found");
        }
        ImageInfo imageInfo = proc.getImageInfo(identifier,
                this.getImageUri(identifier));
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writer().
                without(SerializationFeature.WRITE_NULL_MAP_VALUES).
                without(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS).
                writeValueAsString(imageInfo);
        StringRepresentation rep = new StringRepresentation(json);
        rep.setMediaType(new MediaType("application/json"));
        return rep;
    }

}
