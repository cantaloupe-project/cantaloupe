package edu.illinois.library.cantaloupe.image.exif;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import edu.illinois.library.cantaloupe.util.Rational;

import java.io.IOException;
import java.io.UncheckedIOException;

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
public class DirectorySerializer extends JsonSerializer<Directory> {

    @Override
    public void serialize(Directory directory,
                          JsonGenerator generator,
                          SerializerProvider serializerProvider) throws IOException {
        generator.writeStartObject();

        final int parentTag = directory.getTagSet().getIFDPointerTag();
        if (parentTag > 0) {
            generator.writeFieldName("parentTag");
            generator.writeNumber(parentTag);
        }
        generator.writeFieldName("fields");
        generator.writeStartArray();

        directory.getFields().forEach((field, value) -> {
            try {
                generator.writeStartObject();
                generator.writeFieldName("tag");
                generator.writeNumber(field.getTag().getID());
                generator.writeFieldName("dataType");
                generator.writeNumber(field.getDataType().getValue());
                generator.writeFieldName("value");

                if (value instanceof Directory) {
                    serialize((Directory) value, generator, serializerProvider);
                } else if (value instanceof Rational) {
                    final long[] arr = new long[] {
                            ((Rational) value).getNumerator(),
                            ((Rational) value).getDenominator()
                    };
                    generator.writeArray(arr, 0, arr.length);
                } else if (value instanceof byte[]) {
                    generator.writeBinary((byte[]) value);
                } else {
                    generator.writeObject(value);
                }
                generator.writeEndObject();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        generator.writeEndArray();
        generator.writeEndObject();
    }

}