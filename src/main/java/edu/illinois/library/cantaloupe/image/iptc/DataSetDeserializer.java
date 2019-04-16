package edu.illinois.library.cantaloupe.image.iptc;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.Arrays;

public class DataSetDeserializer extends JsonDeserializer<DataSet> {

    @Override
    public DataSet deserialize(final JsonParser parser,
                               final DeserializationContext deserializationContext) throws IOException {
        final JsonNode rootNode = parser.getCodec().readTree(parser);
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