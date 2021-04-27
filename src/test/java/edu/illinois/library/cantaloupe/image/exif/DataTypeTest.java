package edu.illinois.library.cantaloupe.image.exif;

import edu.illinois.library.cantaloupe.test.BaseTest;
import it.geosolutions.imageio.plugins.tiff.TIFFTag;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class DataTypeTest extends BaseTest {

    private static final float DELTA_F  = 0.00001f;
    private static final double DELTA_L = 0.0000000001;

    @Test
    void testForTIFFTagType() {
        assertEquals(DataType.BYTE, DataType.forTIFFTagType(TIFFTag.TIFF_BYTE));
        assertEquals(DataType.ASCII, DataType.forTIFFTagType(TIFFTag.TIFF_ASCII));
        assertEquals(DataType.SHORT, DataType.forTIFFTagType(TIFFTag.TIFF_SHORT));
        assertEquals(DataType.LONG, DataType.forTIFFTagType(TIFFTag.TIFF_LONG));
        assertEquals(DataType.RATIONAL, DataType.forTIFFTagType(TIFFTag.TIFF_RATIONAL));
        assertEquals(DataType.SBYTE, DataType.forTIFFTagType(TIFFTag.TIFF_SBYTE));
        assertEquals(DataType.SSHORT, DataType.forTIFFTagType(TIFFTag.TIFF_SSHORT));
        assertEquals(DataType.SLONG, DataType.forTIFFTagType(TIFFTag.TIFF_SLONG));
        assertEquals(DataType.SRATIONAL, DataType.forTIFFTagType(TIFFTag.TIFF_SRATIONAL));
        assertEquals(DataType.FLOAT, DataType.forTIFFTagType(TIFFTag.TIFF_FLOAT));
        assertEquals(DataType.DOUBLE, DataType.forTIFFTagType(TIFFTag.TIFF_DOUBLE));
    }

    @Test
    void testForValue() {
        assertEquals(DataType.BYTE, DataType.forValue(1));
        assertEquals(DataType.ASCII, DataType.forValue(2));
        assertEquals(DataType.SHORT, DataType.forValue(3));
        assertEquals(DataType.LONG, DataType.forValue(4));
        assertEquals(DataType.RATIONAL, DataType.forValue(5));
        assertEquals(DataType.SBYTE, DataType.forValue(6));
        assertEquals(DataType.UNDEFINED, DataType.forValue(7));
        assertEquals(DataType.SSHORT, DataType.forValue(8));
        assertEquals(DataType.SLONG, DataType.forValue(9));
        assertEquals(DataType.SRATIONAL, DataType.forValue(10));
        assertEquals(DataType.FLOAT, DataType.forValue(11));
        assertEquals(DataType.DOUBLE, DataType.forValue(12));
    }

    @Test
    void testDecodeWithByte() {
        byte[] bytes = new byte[] { 0x79 };
        assertEquals((byte) 0x79, DataType.BYTE.decode(bytes, ByteOrder.BIG_ENDIAN));
    }

    @Test
    void testDecodeWithASCII() {
        byte[] bytes = "cats\u0000".getBytes(StandardCharsets.US_ASCII);
        assertEquals("cats", DataType.ASCII.decode(bytes, ByteOrder.BIG_ENDIAN));
    }

    @Test
    void testDecodeWithShort() {
        byte[] bytes = new byte[] { 0x12, 0x79 };
        assertEquals((short) 4729, DataType.SHORT.decode(bytes, ByteOrder.BIG_ENDIAN));
    }

    @Test
    void testDecodeWithLong() {
        final byte[] eightBytes = new byte[] { 0x00, 0x00, 0x03, 0x04, 0x05, 0x08, 0x12, 0x33 };
        assertEquals(3315799167539L, DataType.LONG.decode(eightBytes, ByteOrder.BIG_ENDIAN));

        byte[] sevenBytes = Arrays.copyOfRange(eightBytes, 0, 7);
        assertEquals(772, DataType.LONG.decode(sevenBytes, ByteOrder.BIG_ENDIAN));

        byte[] fiveBytes = Arrays.copyOfRange(eightBytes, 3, 8);
        assertEquals(67438610, DataType.LONG.decode(fiveBytes, ByteOrder.BIG_ENDIAN));

        byte[] threeBytes = Arrays.copyOfRange(eightBytes, 5, 8);
        assertEquals(8, DataType.LONG.decode(threeBytes, ByteOrder.BIG_ENDIAN));

        byte[] twoBytes = Arrays.copyOfRange(eightBytes, 6, 8);
        assertEquals((short) 4659, DataType.LONG.decode(twoBytes, ByteOrder.BIG_ENDIAN));

        byte[] oneByte = Arrays.copyOfRange(eightBytes, 7, 8);
        assertEquals(51, DataType.LONG.decode(oneByte, ByteOrder.BIG_ENDIAN));
    }

    @Test
    void testDecodeWithRationalBigEndian() {
        byte[] bytes = new byte[] { 0x00, 0x00, 0x03, 0x04, 0x05, 0x08, 0x12, 0x33 };
        assertEquals(new Rational(772, 84415027),
                DataType.RATIONAL.decode(bytes, ByteOrder.BIG_ENDIAN));
    }

    @Test
    void testDecodeWithRationalLittleEndian() {
        byte[] bytes = new byte[] { 0x00, 0x00, 0x03, 0x04, 0x05, 0x08, 0x12, 0x33 };
        assertEquals(new Rational(67305472, 856819717),
                DataType.RATIONAL.decode(bytes, ByteOrder.LITTLE_ENDIAN));
    }

    @Test
    void testDecodeWithSignedByte() {
        byte[] bytes = new byte[] { 0x79 };
        assertEquals((byte) 0x79, DataType.SBYTE.decode(bytes, ByteOrder.BIG_ENDIAN));
    }

    @Test
    void testDecodeWithSignedShort() {
        byte[] bytes = new byte[] { 0x12, 0x79 };
        assertEquals((short) 4729, DataType.SSHORT.decode(bytes, ByteOrder.BIG_ENDIAN));
    }

    @Test
    void testDecodeWithSignedLong() {
        final long value = 395834590832L;
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putLong(value);
        assertEquals(value, DataType.LONG.decode(bytes, ByteOrder.BIG_ENDIAN));
    }

    @Test
    void testDecodeWithSignedRationalBigEndian() {
        byte[] bytes = new byte[] { 0x00, 0x00, 0x03, 0x04, 0x05, 0x08, 0x12, 0x33 };
        assertEquals(new Rational(772, 84415027),
                DataType.SRATIONAL.decode(bytes, ByteOrder.BIG_ENDIAN));
    }

    @Test
    void testDecodeWithSignedRationalLittleEndian() {
        byte[] bytes = new byte[] { 0x00, 0x00, 0x03, 0x04, 0x05, 0x08, 0x12, 0x33 };
        assertEquals(new Rational(67305472, 856819717),
                DataType.SRATIONAL.decode(bytes, ByteOrder.LITTLE_ENDIAN));
    }

    @Test
    void testDecodeWithFloat() {
        final float value = 342.234232f;
        byte[] bytes = new byte[4];
        ByteBuffer.wrap(bytes).putFloat(value);
        assertEquals(value, (float) DataType.FLOAT.decode(bytes, ByteOrder.BIG_ENDIAN), DELTA_F);
    }

    @Test
    void testDecodeWithDouble() {
        final double value = 342.234202;
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putDouble(value);
        assertEquals(value, (double) DataType.DOUBLE.decode(bytes, ByteOrder.BIG_ENDIAN), DELTA_L);
    }

}
