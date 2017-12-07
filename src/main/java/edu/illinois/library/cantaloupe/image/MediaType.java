package edu.illinois.library.cantaloupe.image;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates an IANA media (a.k.a. MIME) type.
 */
@JsonSerialize(using = MediaType.MediaTypeSerializer.class)
@JsonDeserialize(using = MediaType.MediaTypeDeserializer.class)
public final class MediaType {

    /**
     * Deserializes a type/subtype string into a {@link MediaType}.
     */
    static class MediaTypeDeserializer extends JsonDeserializer<MediaType> {
        @Override
        public MediaType deserialize(JsonParser jsonParser,
                             DeserializationContext deserializationContext) throws IOException {
            return new MediaType(jsonParser.getValueAsString());
        }
    }

    /**
     * Serializes a {@link MediaType} as a type/subtype string.
     */
    static class MediaTypeSerializer extends JsonSerializer<MediaType> {
        @Override
        public void serialize(MediaType mediaType,
                              JsonGenerator jsonGenerator,
                              SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeString(mediaType.toString());
        }
    }

    public static final MediaType APPLICATION_JSON =
            new MediaType("application/json");
    public static final MediaType TEXT_PLAIN =
            new MediaType("text/plain");

    private String subtype;
    private String type;

    /**
     * @param path File to probe.
     * @return Media types associated with the given file.
     * @throws IOException
     */
    public static List<MediaType> detectMediaTypes(Path path)
            throws IOException {
        final List<MediaType> types = new ArrayList<>();
        try (TikaInputStream is = TikaInputStream.get(path)) {
            AutoDetectParser parser = new AutoDetectParser();
            Detector detector = parser.getDetector();
            Metadata md = new Metadata();
            md.add(Metadata.RESOURCE_NAME_KEY, path.toString());
            org.apache.tika.mime.MediaType mediaType = detector.detect(is, md);
            types.add(new MediaType(mediaType.toString()));
        }
        return types;
    }

    /**
     * @param mediaType
     * @throws IllegalArgumentException
     */
    public MediaType(String mediaType) {
        String[] parts = StringUtils.split(mediaType, "/");
        if (parts.length == 2) {
            type = parts[0];
            subtype = parts[1];
        } else {
            throw new IllegalArgumentException("Invalid media type: " + mediaType);
        }
    }

    /**
     * @param obj Object to compare against.
     * @return True if the string representation of the given object matches
     *         that of the instance.
     */
    @Override
    public boolean equals(Object obj) {
        return (obj != null && obj.toString() != null &&
                obj.toString().equals(toString()));
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * @return Format corresponding with the instance.
     */
    public Format toFormat() {
        for (Format enumValue : Format.values()) {
            for (MediaType type : enumValue.getMediaTypes()) {
                if (type.equals(this)) {
                    return enumValue;
                }
            }
        }
        return Format.UNKNOWN;
    }

    @Override
    public String toString() {
        return String.format("%s/%s", type, subtype);
    }

}
