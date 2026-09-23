package edu.illinois.library.cantaloupe.image.exif;

import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.FloatNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.LongNode;
import tools.jackson.databind.node.StringNode;

import java.util.Iterator;
import java.util.Map;

public class DirectoryDeserializer extends ValueDeserializer<Directory> {

    @Override
    public Directory deserialize(final JsonParser parser,
                                 final DeserializationContext deserializationContext) {
        final JsonNode rootNode = deserializationContext.readTree(parser);
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
                                  final JsonParser parser) {
        Directory dir;

        // Find the parent tag.
        int parentTag = 0;
        for (Map.Entry<String, JsonNode> dirEntry : dirNode.properties()) {
            JsonNode rootValue = dirEntry.getValue();
            if ("parentTag".equals(dirEntry.getKey())) {
                parentTag = rootValue.intValue();
                break;
            }
        }

        final TagSet tagSet = (parentTag > 0) ?
                TagSet.forIFDPointerTag(parentTag) : TagSet.BASELINE_TIFF;
        if (tagSet == null) {
            throw new StreamReadException(parser,
                    "Unrecognized tag set: " + parentTag);
        }

        dir = new Directory(tagSet);
        for (Map.Entry<String, JsonNode> rootEntry : dirNode.properties()) {
            if (!"fields".equals(rootEntry.getKey())) {
                continue;
            }

            JsonNode rootValue = rootEntry.getValue();
            Iterator<JsonNode> fieldsIter = rootValue.iterator();
            while (fieldsIter.hasNext()) {
                final JsonNode field = fieldsIter.next();
                // We'll have to iterate over the keys twice; once to find
                // the data type, and then again to decode the value.
                Tag tag            = null;
                DataType dataType  = null;
                JsonNode jsonValue = null;
                Object value       = null;

                for (Map.Entry<String, JsonNode> keyEntry: field.properties()) {
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
                    throw new StreamReadException(parser,
                            "Field is missing tag");
                } else if (dataType == null) {
                    throw new StreamReadException(parser,
                            "Field is missing data type");
                }

                for (Map.Entry<String, JsonNode> keyEntry : field.properties()) {
                    if ("value".equals(keyEntry.getKey())) {
                        jsonValue = keyEntry.getValue();
                        break;
                    }
                }

                if (jsonValue == null) {
                    throw new StreamReadException(parser,
                            "Field is missing value");
                }

                if (tag.isIFDPointer()) {
                    value = deserialize(jsonValue, parser);
                } else {
                    value = toJavaValue(dataType, jsonValue);
                }
                if (value == null) {
                    throw new StreamReadException(parser,
                            "Field has an invalid value");
                }

                dir.put(tag, dataType, value);
            }
        }
        return dir;
    }

    private Object toJavaValue(DataType dataType,
                               JsonNode valueNode) {
        switch (dataType) {
            case BYTE:
                // IntNode has been seen in the wild; the other conditions may
                // or may not be needed here but they can't hurt.
                if (valueNode instanceof IntNode) {
                    return valueNode.longValue();
                } else if (valueNode instanceof LongNode) {
                    return valueNode.longValue();
                } else if (valueNode instanceof FloatNode) {
                    return valueNode.floatValue();
                } else if (valueNode instanceof DoubleNode) {
                    return valueNode.doubleValue();
                } else if (valueNode instanceof BooleanNode) {
                    return valueNode.booleanValue();
                } else if (valueNode instanceof StringNode) {
                    return valueNode.textValue();
                }
                return valueNode.binaryValue();
            case ASCII:
                return valueNode.asText();
            case SHORT:
                return valueNode.intValue();
            case LONG:
                return valueNode.longValue();
            case RATIONAL:
                Iterator<JsonNode> it = valueNode.iterator();
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
                it = valueNode.iterator();
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
