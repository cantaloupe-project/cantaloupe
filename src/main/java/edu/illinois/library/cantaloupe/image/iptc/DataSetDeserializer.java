package edu.illinois.library.cantaloupe.image.iptc;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.JsonNode;

import java.util.Arrays;

public class DataSetDeserializer extends ValueDeserializer<DataSet> {

    @Override
    public DataSet deserialize(final JsonParser parser,
                               final DeserializationContext deserializationContext) {
        final JsonNode rootNode = deserializationContext.readTree(parser);
        final int record        = rootNode.get("record").intValue();
        final int tagNum        = rootNode.get("tag").intValue();
        final byte[] dataField  = rootNode.get("dataField").binaryValue();

        final Tag tag = Arrays.stream(Tag.values())
                .filter(t -> t.getRecord().getRecordNum() == record &&
                        t.getDataSetNum() == tagNum)
                .findFirst()
                .orElse(null);

        return new DataSet(tag, dataField);
    }

}
