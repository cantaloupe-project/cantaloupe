package edu.illinois.library.cantaloupe.image;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Deserializes an EXIF orientation integer into a {@link Orientation}.
 */
final class OrientationDeserializer extends ValueDeserializer<Orientation> {

    @Override
    public Orientation deserialize(JsonParser parser,
                                   DeserializationContext deserializationContext) {
        return Orientation.forEXIFOrientation(parser.getValueAsInt());
    }

}
