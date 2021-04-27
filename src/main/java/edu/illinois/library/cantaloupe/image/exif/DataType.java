package edu.illinois.library.cantaloupe.image.exif;

import edu.illinois.library.cantaloupe.util.ArrayUtils;
import it.geosolutions.imageio.plugins.tiff.TIFFTag;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * TIFF field data type.
 */
public enum DataType {

    /**
     * 8-bit unsigned integer.
     */
    BYTE(1, 1),

    /**
     * 8-bit NULL-terminated string.
     */
    ASCII(2, 1),

    /**
     * 16-bit unsigned integer.
     */
    SHORT(3, 2),

    /**
     * 32-bit unsigned integer.
     */
    LONG(4, 4),

    /**
     * Two 32-bit unsigned integers.
     */
    RATIONAL(5, 8),

    /**
     * 8-bit signed integer.
     */
    SBYTE(6, 1),

    /**
     * 8-bit byte.
     */
    UNDEFINED(7, 1),

    /**
     * 16-bit signed integer.
     */
    SSHORT(8, 2),

    /**
     * 32-bit signed integer.
     */
    SLONG(9, 4),

    /**
     * Two 32-bit signed integers.
     */
    SRATIONAL(10, 8),

    /**
     * 4-byte single-precision IEEE floating-point value.
     */
    FLOAT(11, 4),

    /**
     * 8-byte double-precision IEEE floating-point value.
     */
    DOUBLE(12, 8);

    private final short value, numBytesPerComponent;

    /**
     * @param tiffTagType One of the {@link TIFFTag} constants.
     * @return            Equivalent instance.
     */
    static DataType forTIFFTagType(int tiffTagType) {
        switch (tiffTagType) {
            case TIFFTag.TIFF_BYTE:
                return BYTE;
            case TIFFTag.TIFF_ASCII:
                return ASCII;
            case TIFFTag.TIFF_SHORT:
                return SHORT;
            case TIFFTag.TIFF_LONG:
                return LONG;
            case TIFFTag.TIFF_RATIONAL:
                return RATIONAL;
            case TIFFTag.TIFF_SBYTE:
                return SBYTE;
            case TIFFTag.TIFF_SSHORT:
                return SSHORT;
            case TIFFTag.TIFF_SLONG:
                return SLONG;
            case TIFFTag.TIFF_SRATIONAL:
                return SRATIONAL;
            case TIFFTag.TIFF_FLOAT:
                return FLOAT;
            case TIFFTag.TIFF_DOUBLE:
                return DOUBLE;
            default:
                return UNDEFINED;
        }
    }

    /**
     * @param value TIFF data type {@link #getValue() value}.
     */
    static DataType forValue(int value) {
        return Arrays.stream(DataType.values())
                .filter(t -> t.value == value)
                .findFirst()
                .orElse(UNDEFINED);
    }

    /**
     * @param value                Data type value.
     * @param numBytesPerComponent Number of bytes per value component.
     */
    DataType(int value, int numBytesPerComponent) {
        this.value                = (short) value;
        this.numBytesPerComponent = (short) numBytesPerComponent;
    }

    /**
     * <p>Decodes the given byte array into one of the following types:</p>
     *
     * <dl>
     *     <dt>{@link #BYTE}</dt>
     *     <dd>{@link java.lang.Byte}</dd>
     *     <dt>{@link #ASCII}</dt>
     *     <dd>ASCII-encoded {@link java.lang.String}</dd>
     *     <dt>{@link #SHORT}</dt>
     *     <dd>{@link java.lang.Integer}</dd>
     *     <dt>{@link #LONG}</dt>
     *     <dd>{@link java.lang.Long}</dd>
     *     <dt>{@link #RATIONAL}</dt>
     *     <dd>{@link edu.illinois.library.cantaloupe.util.Rational}</dd>
     *     <dt>{@link #SBYTE}</dt>
     *     <dd>{@link java.lang.Byte}</dd>
     *     <dt>{@link #UNDEFINED}</dt>
     *     <dd>Input byte array</dd>
     *     <dt>{@link #SSHORT}</dt>
     *     <dd>{@link java.lang.Short}</dd>
     *     <dt>{@link #SLONG}</dt>
     *     <dd>{@link java.lang.Integer}</dd>
     *     <dt>{@link #SRATIONAL}</dt>
     *     <dd>{@link edu.illinois.library.cantaloupe.util.Rational}</dd>
     *     <dt>{@link #FLOAT}</dt>
     *     <dd>{@link java.lang.Float}</dd>
     *     <dt>{@link #DOUBLE}</dt>
     *     <dd>{@link java.lang.Double}</dd>
     * </dl>
     *
     * @param bytes     Field value.
     * @param byteOrder Byte order.
     * @return          Java equivalent value.
     */
    Object decode(byte[] bytes, ByteOrder byteOrder) {
        switch (this) {
            case ASCII:
                String str = new String(bytes, StandardCharsets.US_ASCII);
                return str.substring(0, str.length() - 1); // strip null terminator
            case BYTE:
            case SBYTE:
                return bytes[0];
            case SHORT:
            case SSHORT:
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                if (bytes.length == 2) {
                    return buffer.getShort();
                } else {
                    return (int) bytes[0];
                }
            case LONG:
            case SLONG:
                buffer = ByteBuffer.wrap(bytes);
                if (bytes.length >= 8) {
                    return buffer.getLong();
                } else if (bytes.length >= 4) {
                    return buffer.getInt();
                } else if (bytes.length == 2) {
                    return buffer.getShort();
                } else {
                    return (int) bytes[0];
                }
            case RATIONAL:
            case SRATIONAL:
                byte[] numBytes = Arrays.copyOfRange(bytes, 0, 4);
                byte[] denBytes = Arrays.copyOfRange(bytes, 4, 8);
                if (ByteOrder.LITTLE_ENDIAN.equals(byteOrder)) {
                    numBytes = ArrayUtils.reverse(numBytes);
                    denBytes = ArrayUtils.reverse(denBytes);
                }
                int numerator   = toInt(numBytes);
                int denominator = toInt(denBytes);
                return new Rational(numerator, denominator);
            case FLOAT:
                return ByteBuffer.wrap(bytes).getFloat();
            case DOUBLE:
                return ByteBuffer.wrap(bytes).getDouble();
            default:
                return bytes;
        }
    }

    short getNumBytesPerComponent() {
        return numBytesPerComponent;
    }

    short getValue() {
        return value;
    }

    private static int toInt(byte[] fourBytes) {
        return ((fourBytes[0] & 0xff) << 24) |
                ((fourBytes[1] & 0xff) << 16) |
                ((fourBytes[2] & 0xff) << 8) |
                (fourBytes[3] & 0xff);
    }

}
