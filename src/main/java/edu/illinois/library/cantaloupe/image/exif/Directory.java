package edu.illinois.library.cantaloupe.image.exif;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import edu.illinois.library.cantaloupe.util.Rational;
import it.geosolutions.imageio.plugins.tiff.TIFFDirectory;
import it.geosolutions.imageio.plugins.tiff.TIFFField;
import it.geosolutions.imageio.plugins.tiff.TIFFTag;
import it.geosolutions.imageio.plugins.tiff.TIFFTagSet;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * TIFF image file directory (IFD).
 */
@JsonSerialize(using = DirectorySerializer.class)
@JsonDeserialize(using = DirectoryDeserializer.class)
public final class Directory {

    /**
     * N.B.: {@link TreeMap} preserves natural order.
     */
    private final Map<Field, Object> fields = new TreeMap<>();

    private TagSet tagSet;

    /**
     * Converts a {@link TIFFDirectory} structure into an instance.
     *
     * @return Instance equivalent to the argument, or {@literal null} if the
     *         argument's tag set is not {@link TagSet recognized}.
     */
    public static Directory fromTIFFDirectory(TIFFDirectory tiffDirectory) {
        Directory dir = null;
        final TIFFTagSet tiffTagSet = tiffDirectory.getTagSets()[0];
        final TagSet tagSet = TagSet.forTIFFTagSet(tiffTagSet.getClass());
        if (tagSet != null) {
            dir = new Directory(tagSet);
            for (TIFFField tiffField : tiffDirectory.getTIFFFields()) {
                final TIFFTag tiffTag = tiffField.getTag();
                final int tagNum      = tiffTag.getNumber();
                if (tiffTag.isIFDPointer()) {
                    final TIFFDirectory subIFD = (TIFFDirectory) tiffField.getData();
                    final Directory subDir     = fromTIFFDirectory(subIFD);
                    final Tag tag              = tagSet.getTag(tagNum);
                    dir.put(tag, subDir);
                } else {
                    final Tag tag = tagSet.getTag(tagNum);
                    if (tag != null) {
                        final DataType dataType =
                                DataType.forTIFFTagType(tiffField.getType());
                        if (tiffField.getData() != null) {
                            final Object value = decode(tiffField, dataType);
                            dir.put(tag, dataType, value);
                        }
                    }
                }
            }
        }
        return dir;
    }

    private static Object decode(TIFFField tiffField, DataType dataType) {
        switch (dataType) {
            case BYTE:
                return tiffField.getAsBytes()[0];
            case ASCII:
                return tiffField.getAsString(0);
            case SHORT:
                return tiffField.getAsInt(0);
            case LONG:
                return tiffField.getAsLong(0);
            case RATIONAL:
                long[] value = tiffField.getAsRational(0);
                return new Rational(value[0], value[1]);
            case SBYTE:
                return tiffField.getAsBytes()[0];
            case SSHORT:
                return tiffField.getAsInt(0);
            case SLONG:
                return tiffField.getAsLong(0);
            case SRATIONAL:
                int[] value2 = tiffField.getAsSRational(0);
                return new Rational(value2[0], value2[1]);
            case FLOAT:
                return tiffField.getAsFloat(0);
            case DOUBLE:
                return tiffField.getAsDouble(0);
            default:
                return tiffField.getAsBytes();
        }
    }

    /**
     * @param tagSet Tag set on which this instance will be based.
     */
    public Directory(TagSet tagSet) {
        this.tagSet = tagSet;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Directory) {
            // N.B.: fields.equals() would be easier, but that won't work with
            // arrays.
            final Directory other = (Directory) obj;
            // Check the tag set.
            if (!tagSet.equals(other.getTagSet())) {
                return false;
            }
            // Check keys.
            if (!fields.keySet().equals(other.fields.keySet())) {
                return false;
            }
            // Check values.
            for (Field field : fields.keySet()) {
                final Object value      = fields.get(field);
                final Object otherValue = other.fields.get(field);
                if (value instanceof byte[]) {
                    if (otherValue instanceof byte[]) {
                        if (!Arrays.equals((byte[]) value, (byte[]) otherValue)) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                } else {
                    if (!value.equals(otherValue)) {
                        return false;
                    }
                }
            }
            return true;
        }
        return super.equals(obj);
    }

    /**
     * @return Immutable map.
     */
    public Map<Field, Object> getFields() {
        return Collections.unmodifiableMap(fields);
    }

    public TagSet getTagSet() {
        return tagSet;
    }

    public Object getValue(Tag tag) {
        return fields.keySet()
                .stream()
                .filter(f -> f.getTag().equals(tag))
                .map(fields::get)
                .findFirst()
                .orElse(null);
    }

    @Override
    public int hashCode() {
        final Map<Integer,Integer> codes = new HashMap<>();
        codes.put(tagSet.hashCode(), tagSet.hashCode());

        fields.forEach((key, value) -> {
            final int keyCode = key.hashCode();
            int valueCode;
            if (value instanceof byte[]) {
                valueCode = Arrays.hashCode((byte[]) value);
            } else {
                valueCode = value.hashCode();
            }
            codes.put(keyCode, valueCode);
        });
        return codes.hashCode();
    }

    /**
     * N.B.: Use {@link #put(Tag, Directory)} to add a sub-IFD.
     *
     * @param field Field.
     * @param value Normalized value, typically returned from {@link
     *              DataType#decode(byte[])}.
     * @throws IllegalArgumentException if the {@link Field} argument's {@link
     *         Field#getTag() tag} does not exist in the instance's {@link
     *         TagSet}.
     */
    public void put(Field field, Object value) {
        if (!field.getTag().getTagSet().equals(tagSet)) {
            throw new IllegalArgumentException(
                    field.getTag() + " does not exist in " + tagSet);
        }
        fields.put(field, value);
    }

    /**
     * N.B.: Use {@link #put(Tag, Directory)} to add a sub-IFD.
     *
     * @param tag    Tag.
     * @param format Data format.
     * @param value  Normalized value, typically returned from {@link
     *               DataType#decode(byte[])}.
     */
    public void put(Tag tag, DataType format, Object value) {
        put(new Field(tag, format), value);
    }

    /**
     * @param tag Tag.
     * @param dir Sub-IFD.
     */
    public void put(Tag tag, Directory dir) {
        put(new Field(tag, DataType.LONG), dir);
    }

    /**
     * @return Number of fields in the instance, including sub-IFD pointer
     *         fields but excluding sub-IFD fields.
     */
    public int size() {
        return fields.size();
    }

    /**
     * <p>Returns a map with the following structure:</p>
     *
     * {@code
     * {
     *     "tagSet": String,
     *     "fields": {
     *         "Field1Name": Object,
     *         "Field2Name": Object,
     *         "SubIFDPointerName": {
     *             "tagSet": String,
     *             "Fields": {
     *                 "Field1Name": Object,
     *                 "Field2Name": Object
     *             }
     *         }
     *     }
     * }}
     *
     * @return Map representation of the instance.
     */
    public Map<String,Object> toMap() {
        final Map<String,Object> map = new LinkedHashMap<>(2);
        map.put("tagSet", getTagSet().getName());

        final Map<String,Object> fields = new LinkedHashMap<>(getFields().size());
        getFields().forEach((field, value) -> {
            final String fieldName = field.getTag().getFieldName();
            if (value instanceof Directory) {
                fields.put(fieldName, ((Directory) value).toMap());
            } else if (value instanceof Rational) {
                fields.put(fieldName, ((Rational) value).toMap());
            } else {
                fields.put(fieldName, value);
            }
        });
        map.put("fields", fields);
        return Collections.unmodifiableMap(map);
    }

}
