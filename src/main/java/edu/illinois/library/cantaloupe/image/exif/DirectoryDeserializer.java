package edu.illinois.library.cantaloupe.image.exif;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import edu.illinois.library.cantaloupe.util.Rational;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class DirectoryDeserializer extends JsonDeserializer<Directory> {

    @Override
    public Directory deserialize(final JsonParser parser,
                                 final DeserializationContext deserializationContext) throws IOException {
        final JsonNode rootNode = parser.getCodec().readTree(parser);
        return deserialize(rootNode, parser);
    }

    /**
     * Recursively deserializes a {@link Directory} and all of its sub-{@link
     * Directory}s.
     *
     * @param dirNode Directory node (maybe but not necessarily the root
     *                directory).
     * @param parser  Parser.
     */
    private Directory deserialize(final JsonNode dirNode,
                                  final JsonParser parser) throws IOException {
        Directory dir;

        // Find the parent tag.
        int parentTag = 0;
        Iterator<Map.Entry<String,JsonNode>> dirEntries = dirNode.fields();
        while (dirEntries.hasNext()) {
            Map.Entry<String, JsonNode> dirEntry = dirEntries.next();
            JsonNode rootValue = dirEntry.getValue();
            if ("parentTag".equals(dirEntry.getKey())) {
                parentTag = rootValue.intValue();
                break;
            }
        }

        final TagSet tagSet = (parentTag > 0) ?
                TagSet.forIFDPointerTag(parentTag) : TagSet.BASELINE_TIFF;
        if (tagSet == null) {
            throw new JsonParseException(parser,
                    "Unrecognized tag set: " + parentTag);
        }

        dir = new Directory(tagSet);
        dirEntries = dirNode.fields();
        while (dirEntries.hasNext()) {
            Map.Entry<String, JsonNode> rootEntry = dirEntries.next();
            if (!"fields".equals(rootEntry.getKey())) {
                continue;
            }

            JsonNode rootValue = rootEntry.getValue();
            Iterator<JsonNode> fieldsIter = rootValue.elements();
            while (fieldsIter.hasNext()) {
                final JsonNode field = fieldsIter.next();
                // We'll have to iterate over the keys twice; once to find
                // the data type, and then again to decode the value.
                Tag tag            = null;
                DataType dataType  = null;
                JsonNode jsonValue = null;
                Object value       = null;

                Iterator<Map.Entry<String, JsonNode>> keysIter = field.fields();
                while (keysIter.hasNext()) {
                    Map.Entry<String, JsonNode> keyEntry = keysIter.next();
                    switch (keyEntry.getKey()) {
                        case "tag":
                            tag = tagSet.getTag(keyEntry.getValue().intValue());
                            break;
                        case "dataType":
                            dataType = DataType.forValue(keyEntry.getValue().intValue());
                            break;
                    }
                }

                if (tag == null) {
                    throw new JsonParseException(parser,
                            "Field is missing tag");
                } else if (dataType == null) {
                    throw new JsonParseException(parser,
                            "Field is missing data type");
                }

                keysIter = field.fields();
                while (keysIter.hasNext()) {
                    Map.Entry<String, JsonNode> keyEntry = keysIter.next();
                    if ("value".equals(keyEntry.getKey())) {
                        jsonValue = keyEntry.getValue();
                        value = toJavaValue(dataType, jsonValue);
                        break;
                    }
                }

                if (jsonValue == null || value == null) {
                    throw new JsonParseException(parser,
                            "Field is missing value");
                }

                if (tag.isIFDPointer()) {
                    value = deserialize(jsonValue, parser);
                }

                dir.put(tag, dataType, value);
            }
        }
        return dir;
    }

    private Object toJavaValue(DataType dataType,
                               JsonNode valueNode) throws IOException {
        switch (dataType) {
            case BYTE:
                return valueNode.binaryValue();
            case ASCII:
                return valueNode.asText();
            case SHORT:
                return valueNode.intValue();
            case LONG:
                return valueNode.longValue();
            case RATIONAL:
                Iterator<JsonNode> it = valueNode.elements();
                return new Rational(it.next().longValue(),
                        it.next().longValue());
            case SBYTE:
                return valueNode.binaryValue();
            case UNDEFINED:
                return valueNode.binaryValue();
            case SSHORT:
                return valueNode.shortValue();
            case SLONG:
                return valueNode.longValue();
            case SRATIONAL:
                it = valueNode.elements();
                return new Rational(it.next().longValue(),
                        it.next().longValue());
            case FLOAT:
                return valueNode.floatValue();
            case DOUBLE:
                return valueNode.doubleValue();
            default:
                throw new IllegalArgumentException("Unknown data type");
        }
    }

}