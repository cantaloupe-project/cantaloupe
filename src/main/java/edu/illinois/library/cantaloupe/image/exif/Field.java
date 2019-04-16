package edu.illinois.library.cantaloupe.image.exif;

/**
 * TIFF field. This class uses the TIFF 6.0 definition of "field" which refers
 * to all 12 bytes preceding the field value.
 */
public final class Field implements Comparable<Field> {

    private Tag tag;
    private DataType dataFormat;

    Field(Tag tag, DataType dataType) {
        if (tag == null) {
            throw new NullPointerException(
                    Tag.class.getSimpleName() + " argument is null");
        } else if (dataType == null) {
            throw new NullPointerException(
                    DataType.class.getSimpleName() + " argument is null");
        }
        this.tag = tag;
        this.dataFormat = dataType;
    }

    @Override
    public int compareTo(Field other) {
        if (getTag().equals(other.getTag())) {
            return 0;
        }
        return (getTag().getID() < other.getTag().getID()) ? -1 : 1;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Field) {
            Field other = (Field) obj;
            return getTag().equals(other.getTag());
        }
        return super.equals(obj);
    }

    public DataType getDataType() {
        return dataFormat;
    }

    public Tag getTag() {
        return tag;
    }

    @Override
    public int hashCode() {
        return getTag().hashCode();
    }

    @Override
    public String toString() {
        return getTag().toString();
    }

}
