package edu.illinois.library.cantaloupe.image.iptc;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;

/**
 * Serializes a {@link DataSet} as JSON.
 */
public class DataSetSerializer extends ValueSerializer<DataSet> {

    @Override
    public void serialize(DataSet dataSet,
                          JsonGenerator generator,
                          SerializationContext serializationContext) {
        generator.writeStartObject();
        generator.writeName("record");
        generator.writeNumber(dataSet.getTag().getRecord().getRecordNum());
        generator.writeName("tag");
        generator.writeNumber(dataSet.getTag().getDataSetNum());
        generator.writeName("dataField");
        generator.writeBinary(dataSet.getDataField());
        generator.writeEndObject();
    }

}
