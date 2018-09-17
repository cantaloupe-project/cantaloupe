package edu.illinois.library.cantaloupe.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Representation for serializing objects to JSON strings.
 */
public class JacksonRepresentation implements Representation {

    private Object toWrite;

    public JacksonRepresentation(Object toWrite) {
        this.toWrite = toWrite;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        // Serialize dates as ISO-8601 strings rather than timestamps.
        Map<SerializationFeature,Boolean> features = new HashMap<>();
        features.put(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        write(outputStream, features);
    }

    public void write(OutputStream outputStream,
                      Map<SerializationFeature,Boolean> serializationFeatures) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        // Make ObjectMapper aware of JDK8 date/time objects
        // See: https://github.com/FasterXML/jackson-modules-java8
        mapper.registerModule(new JavaTimeModule());

        serializationFeatures.forEach(mapper::configure);

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
