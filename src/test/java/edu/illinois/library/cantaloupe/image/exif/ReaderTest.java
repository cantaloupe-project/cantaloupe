package edu.illinois.library.cantaloupe.image.exif;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.Rational;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.InputStream;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class ReaderTest extends BaseTest {

    private Reader instance;

    private static Directory getExpectedIntelIFD0() {
        final Directory exifIFD = new Directory(TagSet.EXIF);
        exifIFD.put(Tag.EXPOSURE_TIME, DataType.RATIONAL, new Rational(16777216, 671088640));
        exifIFD.put(Tag.F_NUMBER, DataType.RATIONAL, new Rational(184549376, 83886080));
        exifIFD.put(Tag.EXPOSURE_PROGRAM, DataType.SHORT, 2);
        exifIFD.put(Tag.PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, 40);
        exifIFD.put(Tag.EXIF_VERSION, DataType.UNDEFINED, new byte[] { 0x31, 0x32, 0x32, 0x30});
        exifIFD.put(Tag.DATE_TIME_ORIGINAL, DataType.ASCII, "2015:12:31 12:42:48");
        exifIFD.put(Tag.DATE_TIME_DIGITIZED, DataType.ASCII, "2015:12:31 12:42:48");
        exifIFD.put(Tag.COMPONENTS_CONFIGURATION, DataType.UNDEFINED, new byte[] { 0x03, 0x02, 0x01 });
        exifIFD.put(Tag.SHUTTER_SPEED, DataType.SRATIONAL, new Rational(1028849664, -1542520832));
        exifIFD.put(Tag.APERTURE, DataType.RATIONAL, new Rational(-1977679872, -586350592));
        exifIFD.put(Tag.BRIGHTNESS, DataType.SRATIONAL, new Rational(940703744, -1258029056));
        exifIFD.put(Tag.EXPOSURE_BIAS, DataType.SRATIONAL, new Rational(0, 16777216));
        exifIFD.put(Tag.METERING_MODE, DataType.SHORT, 5);
        exifIFD.put(Tag.FLASH, DataType.SHORT, 16);
        exifIFD.put(Tag.FOCAL_LENGTH, DataType.RATIONAL, new Rational(352321536, 83886080));
        exifIFD.put(Tag.SUBJECT_AREA, DataType.SHORT, 95);
        exifIFD.put(Tag.MAKER_NOTE, DataType.UNDEFINED, new byte[] {
                0x41, 0x70, 0x70, 0x6C, 0x65, 0x20, 0x69, 0x4F, 0x53, 0x00,
                0x00, 0x01, 0x4D, 0x4D, 0x00, 0x0A, 0x00, 0x01, 0x00, 0x09,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x04, 0x00, 0x03,
                0x00, 0x07, 0x00, 0x00, 0x00, 0x68, 0x00, 0x00, 0x00, (byte) 0x8C,
                0x00, 0x04, 0x00, 0x09, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00,
                0x00, 0x01, 0x00, 0x05, 0x00, 0x09, 0x00, 0x00, 0x00, 0x01,
                0x00, 0x00, 0x00, (byte) 0x80, 0x00, 0x06, 0x00, 0x09, 0x00,
                0x00, 0x00, 0x01, 0x00, 0x00, 0x00, (byte) 0x87, 0x00, 0x07,
                0x00, 0x09, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x00, 0x08, 0x00, 0x0A, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00,
                0x00, (byte) 0xF4, 0x00, 0x0A, 0x00, 0x09, 0x00, 0x00, 0x00,
                0x01, 0x00, 0x00, 0x00, 0x02, 0x00, 0x0E, 0x00, 0x09, 0x00,
                0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x14, 0x00,
                0x09, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x03, 0x00,
                0x00, 0x00, 0x00, 0x62, 0x70, 0x6C, 0x69, 0x73, 0x74, 0x30,
                0x30, (byte) 0xD4, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                0x08, 0x55, 0x66, 0x6C, 0x61, 0x67, 0x73, 0x55, 0x76, 0x61,
                0x6C, 0x75, 0x65, 0x55, 0x65, 0x70, 0x6F, 0x63, 0x68, 0x59,
                0x74, 0x69, 0x6D, 0x65, 0x73, 0x63, 0x61, 0x6C, 0x65, 0x10,
                0x01, 0x13, 0x00, 0x03, 0x2A, 0x07, 0x20, 0x58, 0x39,
                (byte) 0xC4, 0x10, 0x00, 0x12, 0x3B, (byte) 0x9A, (byte) 0xCA,
                0x00, 0x08, 0x11, 0x17, 0x1D, 0x23, 0x2D, 0x2F, 0x38, 0x3A,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x09, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x3F, (byte) 0xFF, (byte) 0xFF, (byte) 0xEC, 0x2C, 0x00,
                0x00, 0x14, (byte) 0x87, (byte) 0xFF, (byte) 0xFF, (byte) 0xF9,
                (byte) 0xBC, 0x00, 0x00, (byte) 0xBF, 0x6D, 0x00, 0x00, 0x07,
                0x0E, 0x00, 0x00, 0x1A, (byte) 0xFB });
        exifIFD.put(Tag.SUB_SEC_TIME_ORIGINAL, DataType.ASCII, "56");
        exifIFD.put(Tag.SUB_SEC_TIME_DIGITIZED, DataType.ASCII, "56");
        exifIFD.put(Tag.FLASHPIX_VERSION, DataType.UNDEFINED, new byte[] { 0x30, 0x30, 0x31, 0x30});
        exifIFD.put(Tag.COLOR_SPACE, DataType.SHORT, 1);
        exifIFD.put(Tag.PIXEL_X_DIMENSION, DataType.SHORT, 64);
        exifIFD.put(Tag.PIXEL_Y_DIMENSION, DataType.SHORT, 56);
        exifIFD.put(Tag.SENSING_METHOD, DataType.SHORT, 2);
        exifIFD.put(Tag.SCENE_TYPE, DataType.UNDEFINED, new byte[] { 0x01 });
        exifIFD.put(Tag.CUSTOM_RENDERED, DataType.SHORT, 2);
        exifIFD.put(Tag.EXPOSURE_MODE, DataType.SHORT, 0);
        exifIFD.put(Tag.WHITE_BALANCE, DataType.SHORT, 0);
        exifIFD.put(Tag.FOCAL_LENGTH_IN_35MM_FILM, DataType.SHORT, 29);
        exifIFD.put(Tag.SCENE_CAPTURE_TYPE, DataType.SHORT, 0);
        exifIFD.put(Tag.LENS_SPECIFICATION, DataType.RATIONAL, new Rational(1392508928, 335544320));
        exifIFD.put(Tag.LENS_MAKE, DataType.ASCII, "Apple");
        exifIFD.put(Tag.LENS_MODEL, DataType.ASCII, "iPhone 5s back camera 4.15mm f/2.2");

        final Directory ifd0 = new Directory(TagSet.BASELINE_TIFF);
        ifd0.put(Tag.MAKE, DataType.ASCII, "Apple");
        ifd0.put(Tag.MODEL, DataType.ASCII, "iPhone 5s");
        ifd0.put(Tag.ORIENTATION, DataType.SHORT, 1);
        ifd0.put(Tag.X_RESOLUTION, DataType.RATIONAL, new Rational(1207959552, 16777216));
        ifd0.put(Tag.Y_RESOLUTION, DataType.RATIONAL, new Rational(1207959552, 16777216));
        ifd0.put(Tag.RESOLUTION_UNIT, DataType.SHORT, 2);
        ifd0.put(Tag.SOFTWARE, DataType.ASCII, "Photos 1.5");
        ifd0.put(Tag.DATE_TIME, DataType.ASCII, "2015:12:31 12:42:48");
        ifd0.put(Tag.Y_CB_CR_POSITIONING, DataType.SHORT, 1);
        ifd0.put(Tag.EXIF_IFD_POINTER, exifIFD);

        return ifd0;
    }

    private static Directory getExpectedMotorolaIFD0() {
        final Directory exifIFD = new Directory(TagSet.EXIF);
        exifIFD.put(Tag.EXPOSURE_TIME, DataType.RATIONAL, new Rational(1, 40));
        exifIFD.put(Tag.F_NUMBER, DataType.RATIONAL, new Rational(11, 5));
        exifIFD.put(Tag.EXPOSURE_PROGRAM, DataType.SHORT, 2);
        exifIFD.put(Tag.PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, 40);
        exifIFD.put(Tag.EXIF_VERSION, DataType.UNDEFINED, new byte[] { 0x30, 0x32, 0x32, 0x31 });
        exifIFD.put(Tag.DATE_TIME_ORIGINAL, DataType.ASCII, "2015:12:31 12:42:48");
        exifIFD.put(Tag.DATE_TIME_DIGITIZED, DataType.ASCII, "2015:12:31 12:42:48");
        exifIFD.put(Tag.COMPONENTS_CONFIGURATION, DataType.UNDEFINED, new byte[] { 0x01, 0x02, 0x03, 0x00 });
        exifIFD.put(Tag.SHUTTER_SPEED, DataType.SRATIONAL, new Rational(2294, 431));
        exifIFD.put(Tag.APERTURE, DataType.RATIONAL, new Rational(7801, 3429));
        exifIFD.put(Tag.BRIGHTNESS, DataType.SRATIONAL, new Rational(4664, 1205));
        exifIFD.put(Tag.EXPOSURE_BIAS, DataType.SRATIONAL, new Rational(0, 1));
        exifIFD.put(Tag.METERING_MODE, DataType.SHORT, 5);
        exifIFD.put(Tag.FLASH, DataType.SHORT, 16);
        exifIFD.put(Tag.FOCAL_LENGTH, DataType.RATIONAL, new Rational(83, 20));
        exifIFD.put(Tag.SUBJECT_AREA, DataType.SHORT, 6);
        exifIFD.put(Tag.MAKER_NOTE, DataType.UNDEFINED, new byte[] {
                0x41, 0x70, 0x70, 0x6C, 0x65, 0x20, 0x69, 0x4F, 0x53, 0x00,
                0x00, 0x01, 0x4D, 0x4D, 0x00, 0x0A, 0x00, 0x01, 0x00, 0x09,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x04, 0x00, 0x03,
                0x00, 0x07, 0x00, 0x00, 0x00, 0x68, 0x00, 0x00, 0x00, (byte) 0x8C,
                0x00, 0x04, 0x00, 0x09, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00,
                0x00, 0x01, 0x00, 0x05, 0x00, 0x09, 0x00, 0x00, 0x00, 0x01,
                0x00, 0x00, 0x00, (byte) 0x80, 0x00, 0x06, 0x00, 0x09, 0x00,
                0x00, 0x00, 0x01, 0x00, 0x00, 0x00, (byte) 0x87, 0x00, 0x07,
                0x00, 0x09, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x00, 0x08, 0x00, 0x0A, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00,
                0x00, (byte) 0xF4, 0x00, 0x0A, 0x00, 0x09, 0x00, 0x00, 0x00,
                0x01, 0x00, 0x00, 0x00, 0x02, 0x00, 0x0E, 0x00, 0x09, 0x00,
                0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x14, 0x00,
                0x09, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x03, 0x00,
                0x00, 0x00, 0x00, 0x62, 0x70, 0x6C, 0x69, 0x73, 0x74, 0x30,
                0x30, (byte) 0xD4, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                0x08, 0x55, 0x66, 0x6C, 0x61, 0x67, 0x73, 0x55, 0x76, 0x61,
                0x6C, 0x75, 0x65, 0x55, 0x65, 0x70, 0x6F, 0x63, 0x68, 0x59,
                0x74, 0x69, 0x6D, 0x65, 0x73, 0x63, 0x61, 0x6C, 0x65, 0x10,
                0x01, 0x13, 0x00, 0x03, 0x2A, 0x07, 0x20, 0x58, 0x39,
                (byte) 0xC4, 0x10, 0x00, 0x12, 0x3B, (byte) 0x9A, (byte) 0xCA,
                0x00, 0x08, 0x11, 0x17, 0x1D, 0x23, 0x2D, 0x2F, 0x38, 0x3A,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x09, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x3F, (byte) 0xFF, (byte) 0xFF, (byte) 0xEC, 0x2C, 0x00,
                0x00, 0x14, (byte) 0x87, (byte) 0xFF, (byte) 0xFF, (byte) 0xF9,
                (byte) 0xBC, 0x00, 0x00, (byte) 0xBF, 0x6D, 0x00, 0x00, 0x07,
                0x0E, 0x00, 0x00, 0x1A, (byte) 0xFB });
        exifIFD.put(Tag.SUB_SEC_TIME_ORIGINAL, DataType.ASCII, "865");
        exifIFD.put(Tag.SUB_SEC_TIME_DIGITIZED, DataType.ASCII, "865");
        exifIFD.put(Tag.FLASHPIX_VERSION, DataType.UNDEFINED, new byte[] { 0x30, 0x31, 0x30, 0x30});
        exifIFD.put(Tag.COLOR_SPACE, DataType.SHORT, 1);
        exifIFD.put(Tag.PIXEL_X_DIMENSION, DataType.SHORT, 64);
        exifIFD.put(Tag.PIXEL_Y_DIMENSION, DataType.SHORT, 56);
        exifIFD.put(Tag.SENSING_METHOD, DataType.SHORT, 2);
        exifIFD.put(Tag.SCENE_TYPE, DataType.UNDEFINED, new byte[] { 0x01, 0x00, 0x00, 0x00 });
        exifIFD.put(Tag.CUSTOM_RENDERED, DataType.SHORT, 2);
        exifIFD.put(Tag.EXPOSURE_MODE, DataType.SHORT, 0);
        exifIFD.put(Tag.WHITE_BALANCE, DataType.SHORT, 0);
        exifIFD.put(Tag.FOCAL_LENGTH_IN_35MM_FILM, DataType.SHORT, 29);
        exifIFD.put(Tag.SCENE_CAPTURE_TYPE, DataType.SHORT, 0);
        exifIFD.put(Tag.LENS_SPECIFICATION, DataType.RATIONAL, new Rational(83, 20));
        exifIFD.put(Tag.LENS_MAKE, DataType.ASCII, "Apple");
        exifIFD.put(Tag.LENS_MODEL, DataType.ASCII, "iPhone 5s back camera 4.15mm f/2.2");

        final Directory ifd0 = new Directory(TagSet.BASELINE_TIFF);
        ifd0.put(Tag.MAKE, DataType.ASCII, "Apple");
        ifd0.put(Tag.MODEL, DataType.ASCII, "iPhone 5s");
        ifd0.put(Tag.ORIENTATION, DataType.SHORT, 1);
        ifd0.put(Tag.X_RESOLUTION, DataType.RATIONAL, new Rational(72, 1));
        ifd0.put(Tag.Y_RESOLUTION, DataType.RATIONAL, new Rational(72, 1));
        ifd0.put(Tag.RESOLUTION_UNIT, DataType.SHORT, 2);
        ifd0.put(Tag.SOFTWARE, DataType.ASCII, "Photos 1.5");
        ifd0.put(Tag.DATE_TIME, DataType.ASCII, "2015:12:31 12:42:48");
        ifd0.put(Tag.EXIF_IFD_POINTER, exifIFD);

        return ifd0;
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new Reader();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        instance.close();
    }

    @Test
    public void testReadWithLittleEndian() throws Exception {
        // N.B.: convenient way of swapping byte order:
        // exiftool -all= -tagsfromfile bigendian.jpg -all:all -unsafe -exifbyteorder=little-endian bigendian.jpg
        byte[] exif = Files.readAllBytes(TestUtil.getImage("exif-intel.bin"));
        instance.setSource(exif);
        Directory expected = getExpectedIntelIFD0();
        Directory actual = instance.read();

        assertEquals(expected, actual);
    }

    @Test
    public void testReadWithBigEndian() throws Exception {
        // N.B.: convenient way of swapping byte order:
        // exiftool -all= -tagsfromfile littleendian.jpg -all:all -unsafe -exifbyteorder=big-endian littleendian.jpg
        byte[] exif = Files.readAllBytes(TestUtil.getImage("exif-motorola.bin"));
        instance.setSource(exif);
        Directory expected = getExpectedMotorolaIFD0();
        Directory actual = instance.read();

        assertEquals(expected, actual);
    }

    @Test(expected = IllegalStateException.class)
    public void testSetSourceCalledConsecutively1() {
        instance.setSource(new byte[] {});
        instance.setSource(new byte[] {});
    }

    @Test(expected = IllegalStateException.class)
    public void testSetSourceCalledConsecutively2() {
        instance.setSource(new MemoryCacheImageInputStream(InputStream.nullInputStream()));
        instance.setSource(new MemoryCacheImageInputStream(InputStream.nullInputStream()));
    }

    @Test(expected = IllegalStateException.class)
    public void testSetSourceCalledConsecutively3() {
        instance.setSource(new byte[] {});
        instance.setSource(new MemoryCacheImageInputStream(InputStream.nullInputStream()));
    }

    @Test(expected = IllegalStateException.class)
    public void testSetSourceCalledConsecutively4() {
        instance.setSource(new MemoryCacheImageInputStream(InputStream.nullInputStream()));
        instance.setSource(new byte[] {});
    }

}
