package edu.illinois.library.cantaloupe.image.exif;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.Rational;
import it.geosolutions.imageio.plugins.tiff.TIFFDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DirectoryTest extends BaseTest {

    private Directory instance;

    private static TIFFDirectory readTIFFDirectory(Path fixture) throws IOException {
        Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("TIFF");
        ImageReader reader = null;
        try {
            while (it.hasNext()) {
                reader = it.next();
                if (reader instanceof it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader) {
                    break;
                }
            }
            reader.setInput(ImageIO.createImageInputStream(fixture.toFile()));

            IIOMetadata iioMetadata = reader.getImageMetadata(0);
            return TIFFDirectory.createFromMetadata(iioMetadata);
        } finally {
            reader.dispose();
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Directory(TagSet.BASELINE_TIFF);
        instance.put(Tag.ARTIST, DataType.ASCII, "cats");
    }

    @Test
    void testJSONSerialization() throws Exception {
        final Directory exifIFD = new Directory(TagSet.EXIF);
        exifIFD.put(Tag.EXPOSURE_TIME, DataType.RATIONAL, new Rational(1, 160));
        exifIFD.put(Tag.PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, 50);
        exifIFD.put(Tag.EXIF_VERSION, DataType.UNDEFINED, new byte[] { 0x30, 0x32, 0x32, 0x30 });
        exifIFD.put(Tag.DATE_TIME_ORIGINAL, DataType.ASCII, "2002:07:12 16:54:59");
        exifIFD.put(Tag.SHUTTER_SPEED, DataType.SRATIONAL, new Rational(117, 16));
        exifIFD.put(Tag.PIXEL_X_DIMENSION, DataType.LONG, 64L);

        final Directory rootIFD = new Directory(TagSet.BASELINE_TIFF);
        rootIFD.put(Tag.IMAGE_WIDTH, DataType.SHORT, 64);
        rootIFD.put(Tag.IMAGE_LENGTH, DataType.SHORT, 56);
        rootIFD.put(Tag.MAKE, DataType.ASCII, "Canon");
        rootIFD.put(Tag.ORIENTATION, DataType.SHORT, 1);
        rootIFD.put(Tag.X_RESOLUTION, DataType.RATIONAL, new Rational(72, 1));
        rootIFD.put(Tag.PLANAR_CONFIGURATION, DataType.SHORT, 1);
        rootIFD.put(Tag.DATE_TIME, DataType.ASCII, "2002:07:12 16:54:59");
        rootIFD.put(Tag.EXIF_IFD_POINTER, exifIFD);

        final String expected = "{\n" +
                "  \"fields\" : [ {\n" +
                "    \"tag\" : 256,\n" +
                "    \"dataType\" : 3,\n" +
                "    \"value\" : 64\n" +
                "  }, {\n" +
                "    \"tag\" : 257,\n" +
                "    \"dataType\" : 3,\n" +
                "    \"value\" : 56\n" +
                "  }, {\n" +
                "    \"tag\" : 271,\n" +
                "    \"dataType\" : 2,\n" +
                "    \"value\" : \"Canon\"\n" +
                "  }, {\n" +
                "    \"tag\" : 274,\n" +
                "    \"dataType\" : 3,\n" +
                "    \"value\" : 1\n" +
                "  }, {\n" +
                "    \"tag\" : 282,\n" +
                "    \"dataType\" : 5,\n" +
                "    \"value\" : [ 72, 1 ]\n" +
                "  }, {\n" +
                "    \"tag\" : 284,\n" +
                "    \"dataType\" : 3,\n" +
                "    \"value\" : 1\n" +
                "  }, {\n" +
                "    \"tag\" : 306,\n" +
                "    \"dataType\" : 2,\n" +
                "    \"value\" : \"2002:07:12 16:54:59\"\n" +
                "  }, {\n" +
                "    \"tag\" : 34665,\n" +
                "    \"dataType\" : 4,\n" +
                "    \"value\" : {\n" +
                "      \"parentTag\" : 34665,\n" +
                "      \"fields\" : [ {\n" +
                "        \"tag\" : 33434,\n" +
                "        \"dataType\" : 5,\n" +
                "        \"value\" : [ 1, 160 ]\n" +
                "      }, {\n" +
                "        \"tag\" : 34855,\n" +
                "        \"dataType\" : 3,\n" +
                "        \"value\" : 50\n" +
                "      }, {\n" +
                "        \"tag\" : 36864,\n" +
                "        \"dataType\" : 7,\n" +
                "        \"value\" : \"MDIyMA==\"\n" +
                "      }, {\n" +
                "        \"tag\" : 36867,\n" +
                "        \"dataType\" : 2,\n" +
                "        \"value\" : \"2002:07:12 16:54:59\"\n" +
                "      }, {\n" +
                "        \"tag\" : 37377,\n" +
                "        \"dataType\" : 10,\n" +
                "        \"value\" : [ 117, 16 ]\n" +
                "      }, {\n" +
                "        \"tag\" : 40962,\n" +
                "        \"dataType\" : 4,\n" +
                "        \"value\" : 64\n" +
                "      } ]\n" +
                "    }\n" +
                "  } ]\n" +
                "}";

        String actual = new ObjectMapper().writer()
                .withDefaultPrettyPrinter()
                .writeValueAsString(rootIFD);
        assertEquals(expected, actual);
    }

    @Test
    void testJSONDeserialization() throws Exception {
        final String json = "{\n" +
                "  \"fields\" : [ {\n" +
                "    \"tag\" : 256,\n" +
                "    \"dataType\" : 3,\n" +
                "    \"value\" : 64\n" +
                "  }, {\n" +
                "    \"tag\" : 257,\n" +
                "    \"dataType\" : 3,\n" +
                "    \"value\" : 56\n" +
                "  }, {\n" +
                "    \"tag\" : 271,\n" +
                "    \"dataType\" : 2,\n" +
                "    \"value\" : \"Canon\"\n" +
                "  }, {\n" +
                "    \"tag\" : 274,\n" +
                "    \"dataType\" : 3,\n" +
                "    \"value\" : 1\n" +
                "  }, {\n" +
                "    \"tag\" : 282,\n" +
                "    \"dataType\" : 5,\n" +
                "    \"value\" : [ 72, 1 ]\n" +
                "  }, {\n" +
                "    \"tag\" : 284,\n" +
                "    \"dataType\" : 3,\n" +
                "    \"value\" : 1\n" +
                "  }, {\n" +
                "    \"tag\" : 306,\n" +
                "    \"dataType\" : 2,\n" +
                "    \"value\" : \"2002:07:12 16:54:59\"\n" +
                "  }, {\n" +
                "    \"tag\" : 34665,\n" +
                "    \"dataType\" : 4,\n" +
                "    \"value\" : {\n" +
                "      \"parentTag\" : 34665,\n" +
                "      \"fields\" : [ {\n" +
                "        \"tag\" : 33434,\n" +
                "        \"dataType\" : 5,\n" +
                "        \"value\" : [ 1, 160 ]\n" +
                "      }, {\n" +
                "        \"tag\" : 34855,\n" +
                "        \"dataType\" : 3,\n" +
                "        \"value\" : 50\n" +
                "      }, {\n" +
                "        \"tag\" : 36864,\n" +
                "        \"dataType\" : 7,\n" +
                "        \"value\" : \"MDIyMA==\"\n" +
                "      }, {\n" +
                "        \"tag\" : 36867,\n" +
                "        \"dataType\" : 2,\n" +
                "        \"value\" : \"2002:07:12 16:54:59\"\n" +
                "      }, {\n" +
                "        \"tag\" : 37377,\n" +
                "        \"dataType\" : 10,\n" +
                "        \"value\" : [ 117, 16 ]\n" +
                "      }, {\n" +
                "        \"tag\" : 40962,\n" +
                "        \"dataType\" : 4,\n" +
                "        \"value\" : 64\n" +
                "      } ]\n" +
                "    }\n" +
                "  } ]\n" +
                "}";

        final Directory exifIFD = new Directory(TagSet.EXIF);
        exifIFD.put(Tag.EXPOSURE_TIME, DataType.RATIONAL, new Rational(1, 160));
        exifIFD.put(Tag.PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, 50);
        exifIFD.put(Tag.EXIF_VERSION, DataType.UNDEFINED, new byte[] { 0x30, 0x32, 0x32, 0x30 });
        exifIFD.put(Tag.DATE_TIME_ORIGINAL, DataType.ASCII, "2002:07:12 16:54:59");
        exifIFD.put(Tag.SHUTTER_SPEED, DataType.SRATIONAL, new Rational(117, 16));
        exifIFD.put(Tag.PIXEL_X_DIMENSION, DataType.LONG, 64L);

        final Directory expectedIFD = new Directory(TagSet.BASELINE_TIFF);
        expectedIFD.put(Tag.IMAGE_WIDTH, DataType.SHORT, 64);
        expectedIFD.put(Tag.IMAGE_LENGTH, DataType.SHORT, 56);
        expectedIFD.put(Tag.MAKE, DataType.ASCII, "Canon");
        expectedIFD.put(Tag.ORIENTATION, DataType.SHORT, 1);
        expectedIFD.put(Tag.X_RESOLUTION, DataType.RATIONAL, new Rational(72, 1));
        expectedIFD.put(Tag.PLANAR_CONFIGURATION, DataType.SHORT, 1);
        expectedIFD.put(Tag.DATE_TIME, DataType.ASCII, "2002:07:12 16:54:59");
        expectedIFD.put(Tag.EXIF_IFD_POINTER, exifIFD);

        Directory actualIFD = new ObjectMapper().readValue(json, Directory.class);
        assertEquals(expectedIFD, actualIFD);
    }

    @Test
    void testFromTIFFDirectory() throws Exception {
        final Directory exifIFD = new Directory(TagSet.EXIF);
        exifIFD.put(Tag.EXPOSURE_TIME, DataType.RATIONAL, new Rational(1, 160));
        exifIFD.put(Tag.F_NUMBER, DataType.RATIONAL, new Rational(9, 1));
        exifIFD.put(Tag.PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, 50);
        exifIFD.put(Tag.EXIF_VERSION, DataType.UNDEFINED, new byte[] { 0x30, 0x32, 0x32, 0x30 });
        exifIFD.put(Tag.DATE_TIME_ORIGINAL, DataType.ASCII, "2002:07:12 16:54:59");
        exifIFD.put(Tag.DATE_TIME_DIGITIZED, DataType.ASCII, "2002:07:12 16:54:59");
        exifIFD.put(Tag.COMPONENTS_CONFIGURATION, DataType.UNDEFINED, new byte[] { 0x01, 0x02, 0x03, 0x00 });
        exifIFD.put(Tag.COMPRESSED_BITS_PER_PIXEL, DataType.RATIONAL, new Rational(5, 1));
        exifIFD.put(Tag.SHUTTER_SPEED, DataType.SRATIONAL, new Rational(117, 16));
        exifIFD.put(Tag.APERTURE, DataType.RATIONAL, new Rational(1623, 256));
        exifIFD.put(Tag.EXPOSURE_BIAS, DataType.SRATIONAL, new Rational(0, 1));
        exifIFD.put(Tag.MAX_APERTURE_VALUE, DataType.RATIONAL, new Rational(12742, 4289));
        exifIFD.put(Tag.METERING_MODE, DataType.SHORT, 5);
        exifIFD.put(Tag.FLASH, DataType.SHORT, 16);
        exifIFD.put(Tag.FOCAL_LENGTH, DataType.RATIONAL, new Rational(255, 32));
        exifIFD.put(Tag.FLASHPIX_VERSION, DataType.UNDEFINED, new byte[] { 0x30, 0x31, 0x30, 0x30 });
        exifIFD.put(Tag.COLOR_SPACE, DataType.SHORT, 1);
        exifIFD.put(Tag.PIXEL_X_DIMENSION, DataType.LONG, 64L);
        exifIFD.put(Tag.PIXEL_Y_DIMENSION, DataType.LONG, 56L);
        exifIFD.put(Tag.FOCAL_PLANE_X_RESOLUTION, DataType.RATIONAL, new Rational(784466, 101));
        exifIFD.put(Tag.FOCAL_PLANE_Y_RESOLUTION, DataType.RATIONAL, new Rational(240000, 31));
        exifIFD.put(Tag.FOCAL_PLANE_RESOLUTION_UNIT, DataType.SHORT, 2);
        exifIFD.put(Tag.SENSING_METHOD, DataType.SHORT, 2);
        exifIFD.put(Tag.FILE_SOURCE, DataType.UNDEFINED, new byte[] { 0x03 });
        exifIFD.put(Tag.CUSTOM_RENDERED, DataType.SHORT, 0);
        exifIFD.put(Tag.EXPOSURE_MODE, DataType.SHORT, 0);
        exifIFD.put(Tag.WHITE_BALANCE, DataType.SHORT, 0);
        exifIFD.put(Tag.DIGITAL_ZOOM_RATIO, DataType.RATIONAL, new Rational(1, 1));
        exifIFD.put(Tag.SCENE_CAPTURE_TYPE, DataType.SHORT, 0);

        final Directory gpsIFD = new Directory(TagSet.GPS);
        gpsIFD.put(Tag.GPS_LATITUDE_REF, DataType.ASCII, "N");
        gpsIFD.put(Tag.GPS_LATITUDE, DataType.RATIONAL, new Rational(44, 1));
        gpsIFD.put(Tag.GPS_LONGITUDE_REF, DataType.ASCII, "W");
        gpsIFD.put(Tag.GPS_LONGITUDE, DataType.RATIONAL, new Rational(87, 1));

        final Directory rootIFD = new Directory(TagSet.BASELINE_TIFF);
        rootIFD.put(Tag.IMAGE_WIDTH, DataType.SHORT, 64);
        rootIFD.put(Tag.IMAGE_LENGTH, DataType.SHORT, 56);
        rootIFD.put(Tag.BITS_PER_SAMPLE, DataType.SHORT, 8);
        rootIFD.put(Tag.COMPRESSION, DataType.SHORT, 1);
        rootIFD.put(Tag.PHOTOMETRIC_INTERPRETATION, DataType.SHORT, 2);
        rootIFD.put(Tag.MAKE, DataType.ASCII, "Canon");
        rootIFD.put(Tag.MODEL, DataType.ASCII, "Canon PowerShot S200");
        rootIFD.put(Tag.ORIENTATION, DataType.SHORT, 1);
        rootIFD.put(Tag.SAMPLES_PER_PIXEL, DataType.SHORT, 4);
        rootIFD.put(Tag.ROWS_PER_STRIP, DataType.SHORT, 56);
        rootIFD.put(Tag.X_RESOLUTION, DataType.RATIONAL, new Rational(72, 1));
        rootIFD.put(Tag.Y_RESOLUTION, DataType.RATIONAL, new Rational(72, 1));
        rootIFD.put(Tag.PLANAR_CONFIGURATION, DataType.SHORT, 1);
        rootIFD.put(Tag.RESOLUTION_UNIT, DataType.SHORT, 2);
        rootIFD.put(Tag.SOFTWARE, DataType.ASCII, "Photos 1.5");
        rootIFD.put(Tag.DATE_TIME, DataType.ASCII, "2002:07:12 16:54:59");
        rootIFD.put(Tag.EXIF_IFD_POINTER, exifIFD);
        rootIFD.put(Tag.GPS_IFD_POINTER, gpsIFD);

        final Path fixture = TestUtil.getImage("tif-exif.tif");
        final TIFFDirectory tiffDir = readTIFFDirectory(fixture);

        Directory actual = Directory.fromTIFFDirectory(tiffDir);
        assertEquals(rootIFD, actual);
    }

    @Test
    void testEqualsWithEqualInstances() {
        final Directory subIFD1 = new Directory(TagSet.EXIF);
        subIFD1.put(Tag.EXPOSURE_TIME, DataType.RATIONAL, new Rational(1, 160));
        subIFD1.put(Tag.PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, 50);
        subIFD1.put(Tag.PIXEL_X_DIMENSION, DataType.LONG, 64L);
        subIFD1.put(Tag.EXIF_VERSION, DataType.UNDEFINED, new byte[] { 0x30, 0x32, 0x32, 0x30 });
        subIFD1.put(Tag.DATE_TIME_ORIGINAL, DataType.ASCII, "2002:07:12 16:54:59");
        final Directory subIFD2 = new Directory(TagSet.GPS);
        subIFD2.put(Tag.GPS_LATITUDE_REF, DataType.ASCII, "N");
        subIFD2.put(Tag.GPS_LATITUDE, DataType.RATIONAL, new Rational(44, 1));
        final Directory rootIFD1 = new Directory(TagSet.BASELINE_TIFF);
        rootIFD1.put(Tag.IMAGE_LENGTH, DataType.SHORT, 56);
        rootIFD1.put(Tag.MAKE, DataType.ASCII, "Canon");
        rootIFD1.put(Tag.EXIF_IFD_POINTER, subIFD1);
        rootIFD1.put(Tag.GPS_IFD_POINTER, subIFD2);

        final Directory subIFD3 = new Directory(TagSet.EXIF);
        subIFD3.put(Tag.EXPOSURE_TIME, DataType.RATIONAL, new Rational(1, 160));
        subIFD3.put(Tag.PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, 50);
        subIFD3.put(Tag.PIXEL_X_DIMENSION, DataType.LONG, 64L);
        subIFD3.put(Tag.EXIF_VERSION, DataType.UNDEFINED, new byte[] { 0x30, 0x32, 0x32, 0x30 });
        subIFD3.put(Tag.DATE_TIME_ORIGINAL, DataType.ASCII, "2002:07:12 16:54:59");
        final Directory subIFD4 = new Directory(TagSet.GPS);
        subIFD4.put(Tag.GPS_LATITUDE_REF, DataType.ASCII, "N");
        subIFD4.put(Tag.GPS_LATITUDE, DataType.RATIONAL, new Rational(44, 1));
        final Directory rootIFD2 = new Directory(TagSet.BASELINE_TIFF);
        rootIFD2.put(Tag.IMAGE_LENGTH, DataType.SHORT, 56);
        rootIFD2.put(Tag.MAKE, DataType.ASCII, "Canon");
        rootIFD2.put(Tag.EXIF_IFD_POINTER, subIFD3);
        rootIFD2.put(Tag.GPS_IFD_POINTER, subIFD4);

        assertEquals(rootIFD1, rootIFD2);
    }

    @Test
    void testEqualsWithUnequalInstances() {
        final Directory subIFD1 = new Directory(TagSet.EXIF);
        subIFD1.put(Tag.EXPOSURE_TIME, DataType.RATIONAL, new Rational(1, 160));
        subIFD1.put(Tag.PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, 50);
        subIFD1.put(Tag.EXIF_VERSION, DataType.UNDEFINED, new byte[] { 0x30, 0x32, 0x32, 0x30 });
        subIFD1.put(Tag.DATE_TIME_ORIGINAL, DataType.ASCII, "2002:07:12 16:54:59");
        final Directory rootIFD1 = new Directory(TagSet.BASELINE_TIFF);
        rootIFD1.put(Tag.IMAGE_LENGTH, DataType.SHORT, 56);
        rootIFD1.put(Tag.MAKE, DataType.ASCII, "Canon");
        rootIFD1.put(Tag.EXIF_IFD_POINTER, subIFD1);

        final Directory subIFD2 = new Directory(TagSet.EXIF);
        subIFD2.put(Tag.EXPOSURE_TIME, DataType.RATIONAL, new Rational(1, 160));
        // DIFFERENT!!
        subIFD2.put(Tag.PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, 49);
        subIFD2.put(Tag.EXIF_VERSION, DataType.UNDEFINED, new byte[] { 0x30, 0x32, 0x32, 0x30 });
        subIFD2.put(Tag.DATE_TIME_ORIGINAL, DataType.ASCII, "2002:07:12 16:54:59");
        final Directory rootIFD2 = new Directory(TagSet.BASELINE_TIFF);
        rootIFD2.put(Tag.IMAGE_LENGTH, DataType.SHORT, 56);
        rootIFD2.put(Tag.MAKE, DataType.ASCII, "Canon");
        rootIFD2.put(Tag.EXIF_IFD_POINTER, subIFD2);

        assertNotEquals(rootIFD1, rootIFD2);
    }

    @Test
    void testGetValueWithMatchingValue() {
        final Directory ifd = new Directory(TagSet.BASELINE_TIFF);
        ifd.put(Tag.MAKE, DataType.ASCII, "Cats");
        assertEquals("Cats", ifd.getValue(Tag.MAKE));
    }

    @Test
    void testGetValueWithoutMatchingValue() {
        final Directory ifd = new Directory(TagSet.BASELINE_TIFF);
        assertNull(ifd.getValue(Tag.MAKE));
    }

    @Test
    void testHashCodeWithEqualInstances() {
        final Directory subIFD1 = new Directory(TagSet.EXIF);
        subIFD1.put(Tag.EXPOSURE_TIME, DataType.RATIONAL, new Rational(1, 160));
        subIFD1.put(Tag.PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, 50);
        subIFD1.put(Tag.PIXEL_X_DIMENSION, DataType.LONG, 64L);
        subIFD1.put(Tag.EXIF_VERSION, DataType.UNDEFINED, new byte[] { 0x30, 0x32, 0x32, 0x30 });
        subIFD1.put(Tag.DATE_TIME_ORIGINAL, DataType.ASCII, "2002:07:12 16:54:59");
        final Directory subIFD2 = new Directory(TagSet.GPS);
        subIFD2.put(Tag.GPS_LATITUDE_REF, DataType.ASCII, "N");
        subIFD2.put(Tag.GPS_LATITUDE, DataType.RATIONAL, new Rational(44, 1));
        final Directory rootIFD1 = new Directory(TagSet.BASELINE_TIFF);
        rootIFD1.put(Tag.IMAGE_LENGTH, DataType.SHORT, 56);
        rootIFD1.put(Tag.MAKE, DataType.ASCII, "Canon");
        rootIFD1.put(Tag.EXIF_IFD_POINTER, subIFD1);
        rootIFD1.put(Tag.GPS_IFD_POINTER, subIFD2);

        final Directory subIFD3 = new Directory(TagSet.EXIF);
        subIFD3.put(Tag.EXPOSURE_TIME, DataType.RATIONAL, new Rational(1, 160));
        subIFD3.put(Tag.PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, 50);
        subIFD3.put(Tag.PIXEL_X_DIMENSION, DataType.LONG, 64L);
        subIFD3.put(Tag.EXIF_VERSION, DataType.UNDEFINED, new byte[] { 0x30, 0x32, 0x32, 0x30 });
        subIFD3.put(Tag.DATE_TIME_ORIGINAL, DataType.ASCII, "2002:07:12 16:54:59");
        final Directory subIFD4 = new Directory(TagSet.GPS);
        subIFD4.put(Tag.GPS_LATITUDE_REF, DataType.ASCII, "N");
        subIFD4.put(Tag.GPS_LATITUDE, DataType.RATIONAL, new Rational(44, 1));
        final Directory rootIFD2 = new Directory(TagSet.BASELINE_TIFF);
        rootIFD2.put(Tag.IMAGE_LENGTH, DataType.SHORT, 56);
        rootIFD2.put(Tag.MAKE, DataType.ASCII, "Canon");
        rootIFD2.put(Tag.EXIF_IFD_POINTER, subIFD3);
        rootIFD2.put(Tag.GPS_IFD_POINTER, subIFD4);

        assertEquals(rootIFD1.hashCode(), rootIFD2.hashCode());
    }

    @Test
    void testHashCodeWithUnequalInstances() {
        final Directory subIFD1 = new Directory(TagSet.EXIF);
        subIFD1.put(Tag.EXPOSURE_TIME, DataType.RATIONAL, new Rational(1, 160));
        subIFD1.put(Tag.PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, 50);
        subIFD1.put(Tag.EXIF_VERSION, DataType.UNDEFINED, new byte[] { 0x30, 0x32, 0x32, 0x30 });
        subIFD1.put(Tag.DATE_TIME_ORIGINAL, DataType.ASCII, "2002:07:12 16:54:59");
        final Directory rootIFD1 = new Directory(TagSet.BASELINE_TIFF);
        rootIFD1.put(Tag.IMAGE_LENGTH, DataType.SHORT, 56);
        rootIFD1.put(Tag.MAKE, DataType.ASCII, "Canon");
        rootIFD1.put(Tag.EXIF_IFD_POINTER, subIFD1);

        final Directory subIFD2 = new Directory(TagSet.EXIF);
        subIFD2.put(Tag.EXPOSURE_TIME, DataType.RATIONAL, new Rational(1, 160));
        // DIFFERENT!!
        subIFD2.put(Tag.PHOTOGRAPHIC_SENSITIVITY, DataType.SHORT, 49);
        subIFD2.put(Tag.EXIF_VERSION, DataType.UNDEFINED, new byte[] { 0x30, 0x32, 0x32, 0x30 });
        subIFD2.put(Tag.DATE_TIME_ORIGINAL, DataType.ASCII, "2002:07:12 16:54:59");
        final Directory rootIFD2 = new Directory(TagSet.BASELINE_TIFF);
        rootIFD2.put(Tag.IMAGE_LENGTH, DataType.SHORT, 56);
        rootIFD2.put(Tag.MAKE, DataType.ASCII, "Canon");
        rootIFD2.put(Tag.EXIF_IFD_POINTER, subIFD2);

        assertNotEquals(rootIFD1.hashCode(), rootIFD2.hashCode());
    }

    @Test
    void testPut1WithIllegalTag() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.put(new Field(Tag.F_NUMBER, DataType.ASCII), "cats"));
    }

    @Test
    void testPut2WithIllegalTag() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.put(Tag.F_NUMBER, DataType.ASCII, "cats"));
    }

    @Test
    void testSize() {
        final Directory exifIFD = new Directory(TagSet.EXIF);
        exifIFD.put(Tag.EXPOSURE_TIME, DataType.RATIONAL, new Rational(1, 160));

        final Directory gpsIFD = new Directory(TagSet.GPS);
        gpsIFD.put(Tag.GPS_LATITUDE_REF, DataType.ASCII, "N");
        gpsIFD.put(Tag.GPS_LATITUDE, DataType.RATIONAL, new Rational(44, 1));

        final Directory rootIFD = new Directory(TagSet.BASELINE_TIFF);
        rootIFD.put(Tag.IMAGE_WIDTH, DataType.SHORT, 64);
        rootIFD.put(Tag.IMAGE_LENGTH, DataType.SHORT, 56);
        rootIFD.put(Tag.EXIF_IFD_POINTER, exifIFD);
        rootIFD.put(Tag.GPS_IFD_POINTER, gpsIFD);

        assertEquals(4, rootIFD.size());
    }

    @Test
    void testToMap() {
        // assemble a reference Directory structure
        final Directory exifIFD = new Directory(TagSet.EXIF);
        exifIFD.put(Tag.EXPOSURE_TIME, DataType.RATIONAL, new Rational(1, 160));

        final Directory rootIFD = new Directory(TagSet.BASELINE_TIFF);
        rootIFD.put(Tag.IMAGE_WIDTH, DataType.SHORT, 64);
        rootIFD.put(Tag.IMAGE_LENGTH, DataType.SHORT, 56);
        rootIFD.put(Tag.EXIF_IFD_POINTER, exifIFD);

        // assemble the expected map structure
        final Map<String,Object> baselineMap = new LinkedHashMap<>(2);
        baselineMap.put("tagSet", TagSet.BASELINE_TIFF.getName());
        Map<String,Object> baselineFields = new LinkedHashMap<>();
        baselineMap.put("fields", baselineFields);
        baselineFields.put(Tag.IMAGE_WIDTH.getFieldName(), 64);
        baselineFields.put(Tag.IMAGE_LENGTH.getFieldName(), 56);

        final Map<String,Object> exifMap = new LinkedHashMap<>(2);
        exifMap.put("tagSet", TagSet.EXIF.getName());
        Map<String,Object> exifFields = new LinkedHashMap<>();
        exifMap.put("fields", exifFields);
        exifFields.put(Tag.EXPOSURE_TIME.getFieldName(), new Rational(1, 160).toMap());
        baselineFields.put("EXIFIFD", exifMap);

        // compare
        assertEquals(baselineMap, rootIFD.toMap());
    }

}
