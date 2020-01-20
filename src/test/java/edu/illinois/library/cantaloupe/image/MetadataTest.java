package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.image.exif.DataType;
import edu.illinois.library.cantaloupe.image.exif.Directory;
import edu.illinois.library.cantaloupe.image.exif.Tag;
import edu.illinois.library.cantaloupe.image.exif.TagSet;
import edu.illinois.library.cantaloupe.image.iptc.DataSet;
import edu.illinois.library.cantaloupe.processor.codec.ImageReader;
import edu.illinois.library.cantaloupe.processor.codec.ImageReaderFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.Rational;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MetadataTest extends BaseTest {

    private Metadata instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Metadata();
    }

    @Test
    void testEncapsulateXMP() {
        final String xmp = "<rdf:RDF></rdf:RDF>";
        String actual = Metadata.encapsulateXMP(xmp);
        assertTrue(actual.startsWith("<?xpacket"));
        assertTrue(actual.endsWith("<?xpacket end=\"r\"?>"));
    }

    @Test
    void testEqualsWithEqualInstances() {
        Directory exif = new Directory(TagSet.EXIF);
        List<DataSet> iptc = List.of(new DataSet(
                edu.illinois.library.cantaloupe.image.iptc.Tag.CITY,
                "Urbana".getBytes()));
        String xmp = "<rdf:RDF>cats</rdf:RDF>";

        Metadata m1 = new Metadata();
        m1.setEXIF(exif);
        m1.setIPTC(iptc);
        m1.setXMP(xmp);
        m1.setNativeMetadata("cats");

        Metadata m2 = new Metadata();
        m2.setEXIF(exif);
        m2.setIPTC(iptc);
        m2.setXMP(xmp);
        m2.setNativeMetadata("cats");

        assertEquals(m1, m2);
    }

    @Test
    void testEqualsWithDifferentEXIF() {
        Directory exif1 = new Directory(TagSet.EXIF);
        exif1.put(Tag.LENS_MODEL, DataType.ASCII, "cats");
        Metadata m1 = new Metadata();
        m1.setEXIF(exif1);

        Directory exif2 = new Directory(TagSet.EXIF);
        exif2.put(Tag.LENS_MAKE, DataType.ASCII, "cats");
        Metadata m2 = new Metadata();
        m2.setEXIF(exif2);

        assertNotEquals(m1, m2);
    }

    @Test
    void testEqualsWithDifferentIPTC() {
        List<DataSet> iptc1 = List.of(new DataSet(
                edu.illinois.library.cantaloupe.image.iptc.Tag.CITY,
                "Urbana".getBytes()));
        Metadata m1 = new Metadata();
        m1.setIPTC(iptc1);

        List<DataSet> iptc2 = List.of(new DataSet(
                edu.illinois.library.cantaloupe.image.iptc.Tag.CITY,
                "Champaign".getBytes()));
        Metadata m2 = new Metadata();
        m2.setIPTC(iptc2);

        assertNotEquals(m1, m2);
    }

    @Test
    void testEqualsWithDifferentNativeMetadata() {
        Metadata m1 = new Metadata();
        m1.setNativeMetadata("cats");

        Metadata m2 = new Metadata();
        m2.setNativeMetadata("dogs");

        assertNotEquals(m1, m2);
    }

    @Test
    void testEqualsWithDifferentXMP() {
        Metadata m1 = new Metadata();
        m1.setXMP("<rdf:RDF>cats</rdf:RDF>");

        Metadata m2 = new Metadata();
        m2.setXMP("<rdf:RDF>dogs</rdf:RDF>");

        assertNotEquals(m1, m2);
    }

    @Test
    void testGetEXIFWithPresentEXIFData() throws Exception {
        Path fixture = TestUtil.getImage("jpg-exif.jpg");
        ImageReader reader = new ImageReaderFactory()
                .newImageReader(Format.JPG, fixture);
        try {
            Metadata metadata = reader.getMetadata(0);
            assertTrue(metadata.getEXIF().isPresent());
        } finally {
            reader.dispose();
        }
    }

    @Test
    void testGetEXIFWithNoEXIFData() throws Exception {
        Path fixture = TestUtil.getImage("jpg");
        ImageReader reader = new ImageReaderFactory()
                .newImageReader(Format.JPG, fixture);
        try {
            Metadata metadata = reader.getMetadata(0);
            assertFalse(metadata.getEXIF().isPresent());
        } finally {
            reader.dispose();
        }
    }

    @Test
    void testGetIPTCWithPresentIPTCData() throws Exception {
        Path fixture = TestUtil.getImage("jpg-iptc.jpg");
        ImageReader reader = new ImageReaderFactory()
                .newImageReader(Format.JPG, fixture);
        try {
            Metadata metadata = reader.getMetadata(0);
            assertTrue(metadata.getIPTC().isPresent());
        } finally {
            reader.dispose();
        }
    }

    @Test
    void testGetIPTCWithNoIPTCData() throws Exception {
        Path fixture = TestUtil.getImage("jpg");
        ImageReader reader = new ImageReaderFactory()
                .newImageReader(Format.JPG, fixture);
        try {
            Metadata metadata = reader.getMetadata(0);
            assertFalse(metadata.getIPTC().isPresent());
        } finally {
            reader.dispose();
        }
    }

    @Test
    void testGetNativeMetadataWithPresentData() throws Exception {
        Path fixture = TestUtil.getImage("png-nativemetadata.png");
        ImageReader reader = new ImageReaderFactory()
                .newImageReader(Format.PNG, fixture);
        try {
            Metadata metadata = reader.getMetadata(0);
            assertTrue(metadata.getNativeMetadata().isPresent());
        } finally {
            reader.dispose();
        }
    }

    @Test
    void testGetNativeMetadataWithNoData() throws Exception {
        Path fixture = TestUtil.getImage("png-rgb-1x1x8.png");
        ImageReader reader = new ImageReaderFactory()
                .newImageReader(Format.PNG, fixture);
        try {
            Metadata metadata = reader.getMetadata(0);
            assertFalse(metadata.getNativeMetadata().isPresent());
        } finally {
            reader.dispose();
        }
    }

    @Test
    void testGetOrientationWithNoOrientation() {
        assertEquals(Orientation.ROTATE_0, instance.getOrientation());
    }

    @Test
    void testGetOrientationWithOnlyEXIFOrientation() throws Exception {
        Path fixture = TestUtil.getImage("jpg-exif-orientation-270.jpg");
        ImageReader reader = new ImageReaderFactory()
                .newImageReader(Format.JPG, fixture);
        try {
            Metadata metadata = reader.getMetadata(0);
            assertEquals(Orientation.ROTATE_270, metadata.getOrientation());
        } finally {
            reader.dispose();
        }
    }

    @Test
    void testGetOrientationWithOnlyXMPOrientation() throws Exception {
        Path fixture = TestUtil.getImage("jpg-xmp-orientation-90.jpg");
        ImageReader reader = new ImageReaderFactory()
                .newImageReader(Format.JPG, fixture);
        try {
            Metadata metadata = reader.getMetadata(0);
            assertEquals(Orientation.ROTATE_90, metadata.getOrientation());
        } finally {
            reader.dispose();
        }
    }

    @Test
    void testGetOrientationWithMalformedXMP() {
        instance.setXMP("����\u0000\u0010JFIF\u0000\u0001\u0001\u0001\u0000H\u0000H\u0000\u0000��\u0000C\u0000\b\u0006\u0006\u0007\u0006\u0005\b\u0007\u0007\u0007");
        assertEquals(Orientation.ROTATE_0, instance.getOrientation());
    }

    @Test
    void testGetXMPModelWithPresentXMPData() throws Exception {
        Path fixture = TestUtil.getImage("jpg-xmp.jpg");
        ImageReader reader = new ImageReaderFactory()
                .newImageReader(Format.JPG, fixture);
        try {
            Metadata metadata = reader.getMetadata(0);
            Model model = metadata.getXMPModel().get();
            assertEquals(12, model.size());
        } finally {
            reader.dispose();
        }
    }

    @Test
    void testGetXMPModelWithNoXMPData() throws Exception {
        Path fixture = TestUtil.getImage("jpg");
        ImageReader reader = new ImageReaderFactory()
                .newImageReader(Format.JPG, fixture);
        try {
            Metadata metadata = reader.getMetadata(0);
            assertFalse(metadata.getXMPModel().isPresent());
        } finally {
            reader.dispose();
        }
    }

    @Test
    void testHashCodeWithEqualInstances() {
        Directory exif = new Directory(TagSet.EXIF);
        String xmp = "<rdf:RDF>cats</rdf:RDF>";

        Metadata m1 = new Metadata();
        m1.setEXIF(exif);
        m1.setXMP(xmp);
        m1.setNativeMetadata("cats");

        Metadata m2 = new Metadata();
        m2.setEXIF(exif);
        m2.setXMP(xmp);
        m2.setNativeMetadata("cats");

        assertEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    void testHashCodeWithDifferentEXIF() {
        Directory exif1 = new Directory(TagSet.EXIF);
        exif1.put(Tag.LENS_MODEL, DataType.ASCII, "cats");
        Metadata m1 = new Metadata();
        m1.setEXIF(exif1);

        Directory exif2 = new Directory(TagSet.EXIF);
        exif1.put(Tag.LENS_MAKE, DataType.ASCII, "cats");
        Metadata m2 = new Metadata();
        m2.setEXIF(exif2);

        assertNotEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    void testHashCodeWithDifferentIPTC() {
        List<DataSet> iptc1 = List.of(new DataSet(
                edu.illinois.library.cantaloupe.image.iptc.Tag.CITY,
                "Urbana".getBytes()));
        Metadata m1 = new Metadata();
        m1.setIPTC(iptc1);

        List<DataSet> iptc2 = List.of(new DataSet(
                edu.illinois.library.cantaloupe.image.iptc.Tag.CITY,
                "Champaign".getBytes()));
        Metadata m2 = new Metadata();
        m2.setIPTC(iptc2);

        assertNotEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    void testHashCodeWithDifferentNativeMetadata() {
        Metadata m1 = new Metadata();
        m1.setNativeMetadata("cats");

        Metadata m2 = new Metadata();
        m2.setNativeMetadata("dogs");

        assertNotEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    void testHashCodeWithDifferentXMP() {
        Metadata m1 = new Metadata();
        m1.setXMP("<rdf:RDF>cats</rdf:RDF>");

        Metadata m2 = new Metadata();
        m2.setXMP("<rdf:RDF>dogs</rdf:RDF>");

        assertNotEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    void testSetEXIFWithNullArgument() {
        instance.setEXIF(null);
        assertFalse(instance.getEXIF().isPresent());
    }

    @Test
    void testSetIPTCWithNullArgument() {
        instance.setIPTC(null);
        assertFalse(instance.getIPTC().isPresent());
    }

    @Test
    void testSetXMPWithNullByteArrayArgument() {
        instance.setXMP((byte[]) null);
        assertFalse(instance.getXMP().isPresent());
    }

    @Test
    void testSetXMPWithNullStringArgument() {
        instance.setXMP((String) null);
        assertFalse(instance.getXMP().isPresent());
    }

    @Test
    void testSetXMPTrimsData() {
        instance.setXMP("<??><rdf:RDF></rdf:RDF> <??>");
        String xmp = instance.getXMP().orElseThrow();
        assertTrue(xmp.startsWith("<rdf:RDF"));
        assertTrue(xmp.endsWith("</rdf:RDF>"));
    }

    @Test
    void testToMap() {
        // assemble the expected map structure
        final Map<String,Object> expectedMap = new HashMap<>(2);

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
        baselineFields.put(Tag.EXIF_IFD_POINTER.getFieldName(), exifMap);

        expectedMap.put("exif", baselineMap);
        expectedMap.put("iptc", List.of(new DataSet(
                edu.illinois.library.cantaloupe.image.iptc.Tag.CITY,
                "Urbana".getBytes()).toMap()));
        expectedMap.put("xmp_string", "<rdf:RDF></rdf:RDF>");
        expectedMap.put("native", Map.of("key1", "value1", "key2", "value2"));

        // assemble the Metadata
        // EXIF
        final Directory exifIFD = new Directory(TagSet.EXIF);
        exifIFD.put(Tag.EXPOSURE_TIME, DataType.RATIONAL, new Rational(1, 160));

        final Directory rootIFD = new Directory(TagSet.BASELINE_TIFF);
        rootIFD.put(Tag.IMAGE_WIDTH, DataType.SHORT, 64);
        rootIFD.put(Tag.IMAGE_LENGTH, DataType.SHORT, 56);
        rootIFD.put(Tag.EXIF_IFD_POINTER, exifIFD);
        instance.setEXIF(rootIFD);
        // IPTC
        List<DataSet> iptc = List.of(new DataSet(
                edu.illinois.library.cantaloupe.image.iptc.Tag.CITY,
                "Urbana".getBytes()));
        instance.setIPTC(iptc);
        // XMP
        instance.setXMP("<rdf:RDF></rdf:RDF>");
        // native
        instance.setNativeMetadata(Map.of("key1", "value1", "key2", "value2"));

        final Map<String,Object> actualMap = new HashMap<>(instance.toMap());
        // remove the model key for comparison
        assertTrue(actualMap.containsKey("xmp_model"));
        actualMap.remove("xmp_model");

        // compare
        assertEquals(expectedMap, actualMap);
    }

}
