package edu.illinois.library.cantaloupe.image;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Serializes an {@link Info}.
 *
 * @since 5.0
 */
final class InfoSerializer extends JsonSerializer<Info> {

    static final String IDENTIFIER_KEY         = "identifier";
    static final String IMAGES_KEY             = "images";
    static final String INCLUDING_METADATA_KEY = "isIncludingMetadata";
    static final String MEDIA_TYPE_KEY         = "mediaType";
    static final String METADATA_KEY           = "metadata";
    static final String NUM_RESOLUTIONS_KEY    = "numResolutions";

    @Override
    public void serialize(Info info,
                          JsonGenerator generator,
                          SerializerProvider serializerProvider) throws IOException {
        generator.writeStartObject();
        // identifier
        if (info.getIdentifier() != null) {
            generator.writeStringField(IDENTIFIER_KEY, info.getIdentifier().toString());
        }
        // mediaType
        if (info.getMediaType() != null) {
            generator.writeStringField(MEDIA_TYPE_KEY, info.getMediaType().toString());
        }
        // numResolutions
        generator.writeNumberField(NUM_RESOLUTIONS_KEY, info.getNumResolutions());
        // images
        generator.writeArrayFieldStart(IMAGES_KEY);
        info.getImages().forEach(image -> {
            try {
                generator.writeObject(image);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        generator.writeEndArray();
        // metadata
        generator.writeObjectField(METADATA_KEY, info.getMetadata());
        generator.writeEndObject();
    }

}
