package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class EncodeTest extends BaseTest {

    private Encode instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Encode(Format.get("jpg"));
        assertEquals(8, instance.getMaxComponentSize());
    }

    @Test
    void getResultingSize() {
        Dimension size = new Dimension(500, 500);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        assertEquals(size, instance.getResultingSize(size, scaleConstraint));
    }

    @Test
    void testHasEffect() {
        assertTrue(instance.hasEffect());
    }

    @Test
    void testHasEffectWithArguments() {
        Dimension size = new Dimension(500, 500);
        OperationList opList = new OperationList();
        assertTrue(instance.hasEffect(size, opList));
    }

    @Test
    void setBackgroundColorWhenFrozenThrowsException() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setBackgroundColor(Color.RED));
    }

    @Test
    void setCompressionWhenFrozenThrowsException() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setCompression(Compression.LZW));
    }

    @Test
    void setFormatWhenFrozenThrowsException() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setFormat(Format.get("png")));
    }

    @Test
    void setInterlacingWhenFrozenThrowsException() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setInterlacing(false));
    }

    @Test
    void setMaxComponentSizeWithZeroArgument() {
        instance.setMaxComponentSize(0);
        assertEquals(Integer.MAX_VALUE, instance.getMaxComponentSize());
    }

    @Test
    void setMaxComponentSizeWhenFrozenThrowsException() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setMaxComponentSize(8));
    }

    @Test
    void setMetadataWhenFrozenThrowsException() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setMetadata(new Metadata()));
    }

    @Test
    void setMetadataWithValidArgument() {
        Metadata metadata = new Metadata();
        instance.setMetadata(metadata);
        assertEquals(metadata, instance.getMetadata());
    }

    @Test
    void setQualityWithZeroArgumentThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setQuality(0));
    }

    @Test
    void setQualityWithNegativeArgumentThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setQuality(-1));
    }

    @Test
    void setQualityWithArgumentAboveMaxThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setQuality(Encode.MAX_QUALITY + 1));
    }

    @Test
    void setQualityWhenFrozenThrowsException() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setQuality(50));
    }

    @Test
    void setQualityWithValidArgument() {
        instance.setQuality(50);
        assertEquals(50, instance.getQuality());
    }

    @Test
    void testToMap() {
        instance.setCompression(Compression.JPEG);
        instance.setInterlacing(true);
        instance.setQuality(50);
        instance.setBackgroundColor(Color.BLUE);
        instance.setMaxComponentSize(10);
        Metadata metadata = new Metadata();
        metadata.setXMP("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"></rdf:RDF>");
        instance.setMetadata(metadata);

        Dimension size = new Dimension(500, 500);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        final Map<String,Object> map = instance.toMap(size, scaleConstraint);
        assertEquals("Encode", map.get("class"));
        assertEquals("#0000FF", map.get("background_color"));
        assertEquals(Compression.JPEG.toString(), map.get("compression"));
        assertEquals(Format.get("jpg").getPreferredMediaType(), map.get("format"));
        assertTrue((boolean) map.get("interlace"));
        assertEquals(50, map.get("quality"));
        assertEquals(10, map.get("max_sample_size"));
        assertEquals("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"></rdf:RDF>",
                ((Map<String,Object>) map.get("metadata")).get("xmp_string"));
    }

    @Test
    void testToMapReturnsUnmodifiableMap() {
        Dimension fullSize = new Dimension(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String, Object> map = instance.toMap(fullSize, scaleConstraint);
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("test", "test"));
    }

    @Test
    void testToString() {
        instance.setCompression(Compression.JPEG);
        instance.setInterlacing(true);
        instance.setQuality(50);
        instance.setBackgroundColor(Color.BLUE);
        instance.setMaxComponentSize(10);
        Metadata metadata = new Metadata();
        metadata.setXMP("<rdf:RDF></rdf:RDF>");
        instance.setMetadata(metadata);
        assertTrue(instance.toString().matches("^jpg_JPEG_50_interlace_#0000FF_10_.*"));
    }

}
