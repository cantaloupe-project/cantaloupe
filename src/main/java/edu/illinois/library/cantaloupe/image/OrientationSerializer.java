package edu.illinois.library.cantaloupe.image;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;

/**
 * Serializes an {@link Orientation} as an EXIF orientation integer.
 */
final class OrientationSerializer extends ValueSerializer<Orientation> {

    @Override
    public void serialize(Orientation orientation,
                          JsonGenerator generator,
                          SerializationContext serializationContext) {
        generator.writeNumber(orientation.getEXIFValue());
    }

}
