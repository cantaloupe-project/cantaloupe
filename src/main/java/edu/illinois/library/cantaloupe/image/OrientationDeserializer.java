package edu.illinois.library.cantaloupe.image;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Deserializes an EXIF orientation integer into a {@link Orientation}.
 */
final class OrientationDeserializer extends JsonDeserializer<Orientation> {

    @Override
    public Orientation deserialize(JsonParser parser,
                                   DeserializationContext deserializationContext) throws IOException {
        return Orientation.forEXIFOrientation(parser.getValueAsInt());
    }

}
