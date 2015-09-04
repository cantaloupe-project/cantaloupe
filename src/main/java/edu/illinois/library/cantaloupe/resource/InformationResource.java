package edu.illinois.library.cantaloupe.resource;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

public class InformationResource extends AbstractResource {

    @Get("json")
    public Representation doGet() throws Exception {
        this.addHeader("Link",
                "<http://iiif.io/api/image/2/level1.json>;rel=\"profile\"");
        this.addHeader("Link", "<http://iiif.io/api/image/2/context.json>; " +
                "rel=\"http://www.w3.org/ns/json-ld#context\"; type=\"application/ld+json\"");

        Map<String,Object> attrs = this.getRequest().getAttributes();
        String identifier = (String) attrs.get("identifier");

        Resolver resolver = ResolverFactory.getResolver();
        InputStream inputStream = resolver.resolve(identifier);
        if (inputStream == null) {
            throw new FileNotFoundException("Resource not found");
        }

        SourceFormat sourceFormat = resolver.getExpectedSourceFormat(identifier);
        Processor proc = ProcessorFactory.getProcessor(sourceFormat);
        ImageInfo imageInfo = proc.getImageInfo(inputStream,
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

    private String getImageUri(String identifier) {
        try {
            return this.getRootRef() +
                    ((ImageServerApplication)this.getApplication()).BASE_IIIF_PATH +
                    "/" + java.net.URLEncoder.encode(identifier, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

}
