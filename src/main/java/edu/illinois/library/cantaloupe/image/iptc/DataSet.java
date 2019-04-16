package edu.illinois.library.cantaloupe.image.iptc;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

@JsonSerialize(using = DataSetSerializer.class)
@JsonDeserialize(using = DataSetDeserializer.class)
public final class DataSet {

    private Tag tag;
    private byte[] dataField;
    private Charset stringEncoding = StandardCharsets.US_ASCII;

    private transient int longValue = -1;
    private transient String stringValue;

    public DataSet(Tag tag, byte[] dataField) {
        this.tag = tag;
        this.dataField = dataField;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof DataSet) {
            DataSet other = (DataSet) obj;
            return Objects.equals(getTag(), other.getTag()) &&
                    Arrays.equals(getDataField(), other.getDataField());
        }
        return super.equals(obj);
    }

    public Tag getTag() {
        return tag;
    }

    /**
     * @return Raw data field bytes.
     */
    public byte[] getDataField() {
        return dataField;
    }

    /**
     * @return Data field bytes coerced to a {@literal long}.
     */
    public long getDataFieldAsLong() {
        if (longValue == -1) {
            longValue = 0;
            int shift = 8 * (dataField.length - 1);
            for (byte b : dataField) {
                longValue |= ((b & 0xff) << shift);
                shift -= 8;
            }
        }
        return longValue;
    }

    /**
     * @return Data field bytes coerced to a {@link String} in the encoding
     *         supplied to {@link #setStringEncoding(Charset)}, or else ASCII.
     */
    public String getDataFieldAsString() {
        if (stringValue == null) {
            switch (tag.getDataType()) {
                case UNSIGNED_INT_16:
                    stringValue = Long.toString(getDataFieldAsLong());
                    break;
                default:
                    stringValue = new String(dataField, stringEncoding);
                    break;
            }
        }
        return stringValue;
    }

    @Override
    public int hashCode() {
        int[] codes = { getTag().hashCode(), Arrays.hashCode(dataField) };
        return Arrays.hashCode(codes);
    }

    /**
     * Controls the encoding of strings returned from {@link
     * #getDataFieldAsString()}.
     */
    void setStringEncoding(Charset encoding) {
        this.stringEncoding = encoding;
    }

    /**
     * <p>Returns a map with the following structure:</p>
     *
     * {@code
     * {
     *     "TagName": Object
     * }}
     *
     * @return Map representation of the instance.
     */
    public Map<String,Object> toMap() {
        return Map.of(
                getTag().getName(),
                DataType.UNSIGNED_INT_16.equals(getTag().getDataType()) ?
                        getDataFieldAsLong() : getDataFieldAsString());
    }

    @Override
    public String toString() {
        return getTag() + ": " + getDataFieldAsString();
    }

}
