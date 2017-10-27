package edu.illinois.library.cantaloupe.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;

import java.io.IOException;
import java.io.OutputStream;

public class JSONRepresentation extends OutputRepresentation {

    private Object toWrite;

    public JSONRepresentation(Object toWrite) {
        super(MediaType.APPLICATION_JSON);
        setCharacterSet(CharacterSet.UTF_8);
        this.toWrite = toWrite;
    }

    public JSONRepresentation(Object toWrite, MediaType mediaType) {
        super(mediaType);
        setCharacterSet(CharacterSet.UTF_8);
        this.toWrite = toWrite;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        // Make ObjectMapper aware of JDK8 date/time objects
        // See: https://github.com/FasterXML/jackson-modules-java8
        mapper.registerModule(new JavaTimeModule());
        // And tell it to serialize dates as ISO-8601 strings rather than
        // timestamps.
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.
                WRITE_DATES_AS_TIMESTAMPS, false);
        // Add a config override to omit keys with empty or null values.
        //
        // (It would be better not to do this, and to instead use @JsonInclude
        // annotations on the classes being serialized, which are currently
        // e.i.l.c.resource.iiif.v1.ImageInfo
        // and e.i.l.c.resource.iiif.v2.ImageInfo, but that won't work the way
        // they are currently written.)
        //
        // The IIIF Image API 2.1 spec (sec. 5.3) says,
        // "If any of formats, qualities, or supports have no additional values
        // beyond those specified in the referenced compliance level, then
        // the property should be omitted from the response rather than being
        // present with an empty list."
        mapper.configOverride(Object.class).setInclude(
                JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, null));
        mapper.writer().writeValue(outputStream, toWrite);
    }

}
