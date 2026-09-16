package edu.illinois.library.cantaloupe.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

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
        Map<DateTimeFeature,Boolean> features = new HashMap<>();
        features.put(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        write(outputStream, features);
    }

    public void write(OutputStream outputStream,
                      Map<DateTimeFeature,Boolean> serializationFeatures) throws IOException {
        var mapperBuilder = JsonMapper.builder();
        serializationFeatures.forEach(mapperBuilder::configure);

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
        mapperBuilder.withConfigOverride(Object.class, override ->
                override.setInclude(JsonInclude.Value.construct(
                        JsonInclude.Include.NON_EMPTY, null)));
        ObjectMapper mapper = mapperBuilder.build();
        mapper.writer().writeValue(outputStream, toWrite);
    }

}
