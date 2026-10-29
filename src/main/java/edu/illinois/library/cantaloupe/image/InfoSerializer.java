package edu.illinois.library.cantaloupe.image;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;
import edu.illinois.library.cantaloupe.Application;

import java.time.Instant;

/**
 * Serializes an {@link Info}.
 *
 * @since 5.0
 */
final class InfoSerializer extends ValueSerializer<Info> {

    static final String APPLICATION_VERSION_KEY     = "applicationVersion";
    static final String IDENTIFIER_KEY              = "identifier";
    static final String IMAGES_KEY                  = "images";
    static final String MEDIA_TYPE_KEY              = "mediaType";
    static final String METADATA_KEY                = "metadata";
    static final String NUM_RESOLUTIONS_KEY         = "numResolutions";
    static final String SERIALIZATION_TIMESTAMP_KEY = "serializationTimestamp";
    static final String SERIALIZATION_VERSION_KEY   = "serializationVersion";

    @Override
    public void serialize(Info info,
                          JsonGenerator generator,
                          SerializationContext serializationContext) {
        generator.writeStartObject();
        // application version
        generator.writeStringProperty(APPLICATION_VERSION_KEY,
                Application.getVersion());
        // serialization version
        generator.writeNumberProperty(SERIALIZATION_VERSION_KEY,
                Info.Serialization.CURRENT.getVersion());
        // serialization timestamp
        generator.writeStringProperty(SERIALIZATION_TIMESTAMP_KEY,
                Instant.now().toString());
        // identifier
        if (info.getIdentifier() != null) {
            generator.writeStringProperty(IDENTIFIER_KEY,
                    info.getIdentifier().toString());
        }
        // mediaType
        if (info.getMediaType() != null) {
            generator.writeStringProperty(MEDIA_TYPE_KEY,
                    info.getMediaType().toString());
        }
        // numResolutions
        generator.writeNumberProperty(NUM_RESOLUTIONS_KEY,
                info.getNumResolutions());
        // images
        generator.writeArrayPropertyStart(IMAGES_KEY);
        info.getImages().forEach(generator::writePOJO);
        generator.writeEndArray();
        // metadata
        generator.writePOJOProperty(METADATA_KEY, info.getMetadata());
        generator.writeEndObject();
    }

}
