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
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * IANA media (a.k.a. MIME) type. Instances are immutable.
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

    private String subtype, type;

    /**
     * Attempts to detect the media type(s) of the given magic bytes. The
     * detection is fast but imperfect.
     *
     * @param bytes Bytes to probe.
     * @return     Media types associated with the data in the given file, or
     *             an empty list if none were detected.
     */
    public static List<MediaType> detectMediaTypes(byte[] bytes)
            throws IOException {
        final List<MediaType> types = new ArrayList<>();

        // https://tika.apache.org/1.1/detection.html
        try (TikaInputStream is = TikaInputStream.get(bytes)) {
            AutoDetectParser parser = new AutoDetectParser();
            Detector detector = parser.getDetector();
            Metadata md = new Metadata();
            org.apache.tika.mime.MediaType mediaType = detector.detect(is, md);
            types.add(new MediaType(mediaType.toString()));
        }
        return types;
    }

    /**
     * Attempts to detect the media type(s) of the given file by reading its
     * magic bytes. The detection is fast but imperfect.
     *
     * @param path File to probe.
     * @return     Media types associated with the data in the given file, or
     *             an empty list if none were detected.
     */
    public static List<MediaType> detectMediaTypes(Path path)
            throws IOException {
        final List<MediaType> types = new ArrayList<>();

        // https://tika.apache.org/1.1/detection.html
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
     * Attempts to detect the media type(s) of the data read from a stream.
     * The detection is fast but imperfect.
     *
     * @param inputStream Stream to read from. Must {@link
     *                    InputStream#markSupported() support marking}.
     * @return            Media types associated with the data in the given
     *                    stream, or an empty list if none were detected.
     */
    public static List<MediaType> detectMediaTypes(InputStream inputStream)
            throws IOException {
        final List<MediaType> types = new ArrayList<>();

        // https://tika.apache.org/1.1/detection.html
        AutoDetectParser parser = new AutoDetectParser();
        Detector detector = parser.getDetector();

        org.apache.tika.mime.MediaType mediaType = detector.detect(
                inputStream, new Metadata());
        types.add(new MediaType(mediaType.toString()));

        return types;
    }

    /**
     * @param contentType {@literal Content-Type} header value.
     * @return            Media type corresponding to the given header value.
     * @throws IllegalArgumentException if the format of the argument is
     *                    illegal.
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.1.5">RFC
     *      7231</a>
     */
    public static MediaType fromContentType(String contentType) {
        String[] parts = contentType.split(";");
        if (parts.length > 0) {
            return new MediaType(parts[0].trim());
        }
        throw new IllegalArgumentException("Unrecognized Content-Type");
    }

    /**
     * @param mediaType
     * @throws IllegalArgumentException if the given string is not a media
     *                                  type.
     */
    public MediaType(String mediaType) {
        if (mediaType == null || mediaType.isEmpty()) {
            throw new IllegalArgumentException("Null or empty argument");
        }
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
     * @return    {@literal true} if the given object is the same media type;
     *            {@literal false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof MediaType) {
            return Objects.equals(obj.toString(), toString());
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        String[] parts = { type, subtype };
        return Arrays.hashCode(parts);
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
        return type + "/" + subtype;
    }

}
