package edu.illinois.library.cantaloupe.image.exif;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;

/**
 * <p>Serializes a {@link Directory} as JSON.</p>
 *
 * <p>Notes:</p>
 *
 * <ul>
 *     <li>{@link Rational}s are serialized as two-element arrays.</li>
 *     <li>Bytes and byte arrays are serialized as Base64-encoded strings.</li>
 * </ul>
 */
public class DirectorySerializer extends ValueSerializer<Directory> {

    @Override
    public void serialize(Directory directory,
                          JsonGenerator generator,
                          SerializationContext serializationContext) {
        generator.writeStartObject();

        final int parentTag = directory.getTagSet().getIFDPointerTag();
        if (parentTag > 0) {
            generator.writeName("parentTag");
            generator.writeNumber(parentTag);
        }
        generator.writeName("fields");
        generator.writeStartArray();

        directory.getFields().forEach((field, value) -> {
            generator.writeStartObject();
            generator.writeName("tag");
            generator.writeNumber(field.getTag().getID());
            generator.writeName("dataType");
            generator.writeNumber(field.getDataType().getValue());
            generator.writeName("value");

            if (value instanceof Directory) {
                serialize((Directory) value, generator, serializationContext);
            } else if (value instanceof Rational) {
                final long[] arr = new long[] {
                        ((Rational) value).getNumerator(),
                        ((Rational) value).getDenominator()
                };
                generator.writeArray(arr, 0, arr.length);
            } else if (value instanceof byte[]) {
                generator.writeBinary((byte[]) value);
            } else {
                generator.writePOJO(value);
            }
            generator.writeEndObject();
        });
        generator.writeEndArray();
        generator.writeEndObject();
    }

}
