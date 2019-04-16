package edu.illinois.library.cantaloupe.image.iptc;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Serializes a {@link DataSet} as JSON.
 */
public class DataSetSerializer extends JsonSerializer<DataSet> {

    @Override
    public void serialize(DataSet dataSet,
                          JsonGenerator generator,
                          SerializerProvider serializerProvider) throws IOException {
        generator.writeStartObject();
        generator.writeFieldName("record");
        generator.writeNumber(dataSet.getTag().getRecord().getRecordNum());
        generator.writeFieldName("tag");
        generator.writeNumber(dataSet.getTag().getDataSetNum());
        generator.writeFieldName("dataField");
        generator.writeBinary(dataSet.getDataField());
        generator.writeEndObject();
    }

}