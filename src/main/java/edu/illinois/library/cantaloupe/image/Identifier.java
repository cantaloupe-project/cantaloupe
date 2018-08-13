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
import java.util.regex.Matcher;

/**
 * <p>Immutable application-unique source image identifier.</p>
 *
 * <h1>Input</h1>
 *
 * <p>When identifiers are supplied to the application via URIs, they must go
 * through some processing steps before they can be used (order is
 * important):</p>
 *
 * <ol>
 *     <li>URI decoding</li>
 *     <li>Strip off any {@link ScaleConstraint#IDENTIFIER_SUFFIX_PATTERN scale
 *     constraint suffix}</li>
 *     <li>{@link StringUtils#decodeSlashes(String) slash decoding}</li>
 * </ol>
 *
 * <p>({@link Identifier#fromURIPathComponent(String)} will handle all of
 * this.)</p>
 *
 * <h1>Output</h1>
 *
 * <p>The input steps must be reversed for output. Note that requests can
 * supply a {@link
 * edu.illinois.library.cantaloupe.resource.AbstractResource#PUBLIC_IDENTIFIER_HEADER}
 * to suggest that the identifier supplied in a URI is different from the one
 * the user agent is seeing and supplying to a reverse proxy.</p>
 *
 * <p>So, the steps for output are:</p>
 *
 * <ol>
 *     <li>Replace the URI identifier with the one from {@link
 *     edu.illinois.library.cantaloupe.resource.AbstractResource#PUBLIC_IDENTIFIER_HEADER},
 *     if present</li>
 *     <li>Encode slashes</li>
 *     <li>Append a {@link ScaleConstraint#IDENTIFIER_SUFFIX_PATTERN scale
 *     constraint suffix}, if necessary</li>
 *     <li>URI encoding</li>
 * </ol>
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
     * Translates the string in a raw URI path component to an {@link
     * Identifier}.
     *
     * @param pathComponent Raw URI path component.
     */
    public static Identifier fromURIPathComponent(String pathComponent) {
        // Decode entities.
        final String decodedComponent = Reference.decode(pathComponent);
        // Strip off any scale constraint suffix.
        final String deScaledComponent = stripScaleConstraint(decodedComponent);
        // Decode slash substitutes.
        final String deSlashedComponent =
                StringUtils.decodeSlashes(deScaledComponent);

        LOGGER.debug("Raw path component: {} -> " +
                        "decoded: {} -> scale constraint stripped: {} -> " +
                        "slashes substituted: {}",
                pathComponent, decodedComponent, deScaledComponent,
                deSlashedComponent);

        return new Identifier(deSlashedComponent);
    }

    /**
     * @param pathComponent Decoded URI path component.
     * @return The given path component with any {@link ScaleConstraint} suffix
     *         stripped off.
     */
    private static String stripScaleConstraint(String pathComponent) {
        final Matcher matcher =
                ScaleConstraint.IDENTIFIER_SUFFIX_PATTERN.matcher(pathComponent);
        if (matcher.find()) {
            String group = matcher.group(0);
            return pathComponent.substring(0, pathComponent.length() - group.length());
        }
        return pathComponent;
    }

    /**
     * @param value Identifier value
     * @throws IllegalArgumentException If the given value is {@literal null}.
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
    public int hashCode() {
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
