package edu.illinois.library.cantaloupe.image;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Serializes an {@link Orientation} as an EXIF orientation integer.
 */
final class OrientationSerializer extends JsonSerializer<Orientation> {

    @Override
    public void serialize(Orientation orientation,
                          JsonGenerator generator,
                          SerializerProvider serializerProvider) throws IOException {
        generator.writeNumber(orientation.getEXIFValue());
    }

}
