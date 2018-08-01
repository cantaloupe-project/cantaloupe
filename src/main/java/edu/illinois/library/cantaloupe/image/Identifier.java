package edu.illinois.library.cantaloupe.image;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Immutable application-unique source image identifier.
 */
@JsonSerialize(using = Identifier.IdentifierSerializer.class)
@JsonDeserialize(using = Identifier.IdentifierDeserializer.class)
public class Identifier implements Comparable<Identifier> {

    /**
     * Deserializes a type/subtype string into an {@link Identifier}.
     */
    static class IdentifierDeserializer extends JsonDeserializer<Identifier> {
        @Override
        public Identifier deserialize(JsonParser jsonParser,
                                      DeserializationContext deserializationContext) throws IOException {
            return new Identifier(jsonParser.getValueAsString());
        }
    }

    /**
     * Serializes an {@link Identifier} as a string.
     */
    static class IdentifierSerializer extends JsonSerializer<Identifier> {
        @Override
        public void serialize(Identifier identifier,
                              JsonGenerator jsonGenerator,
                              SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeString(identifier.toString());
        }
    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(Identifier.class);

    private String value;

    /**
     * @param pathComponent URI path component.
     */
    public static Identifier fromURIPathComponent(String pathComponent) {
        // Decode entities.
        final String decodedComponent = Reference.decode(pathComponent);
        // Decode slash substitutes.
        final String deSlashedComponent =
                StringUtils.decodeSlashes(decodedComponent);

        LOGGER.debug("Raw path component: {} -> decoded: {} -> " +
                        "slashes substituted: {}",
                pathComponent, decodedComponent, deSlashedComponent);

        return new Identifier(deSlashedComponent);
    }

    /**
     * @param value Identifier value
     * @throws IllegalArgumentException If the given value is null.
     */
    public Identifier(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null.");
        }
        this.value = value;
    }

    @Override
    public int compareTo(Identifier identifier) {
        return toString().compareTo(identifier.toString());
    }

    /**
     * @param obj Instance to compare.
     * @return {@literal true} if {@literal obj} is a reference to the same
     *         instance; {@literal true} if it is a different instance with the
     *         same value; {@literal true} if it is a {@link String} instance
     *         with the same value; {@literal false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Identifier) {
            return toString().equals(obj.toString());
        } else if (obj instanceof String) {
            return toString().equals(obj);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode(){
        return toString().hashCode();
    }

    /**
     * @return Value of the instance.
     */
    @Override
    public String toString() {
        return value;
    }

}
