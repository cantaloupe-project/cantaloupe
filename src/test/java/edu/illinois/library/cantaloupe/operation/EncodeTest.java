package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.util.Map;

import static org.junit.Assert.*;

public class EncodeTest extends BaseTest {

    private Encode instance;

    @Before
    public void setUp() {
        instance = new Encode(Format.JPG);
        assertEquals(8, instance.getMaxComponentSize());
    }

    @Test
    public void getResultingSize() {
        Dimension size = new Dimension(500, 500);
        assertEquals(size, instance.getResultingSize(size));
    }

    @Test
    public void hasEffect() {
        assertTrue(instance.hasEffect());
    }

    @Test
    public void hasEffectWithArguments() {
        Dimension size = new Dimension(500, 500);
        OperationList opList = new OperationList();
        assertTrue(instance.hasEffect(size, opList));
    }

    @Test(expected = IllegalStateException.class)
    public void setBackgroundColorWhenFrozenThrowsException() {
        instance.freeze();
        instance.setBackgroundColor(Color.RED);
    }

    @Test(expected = IllegalStateException.class)
    public void setCompressionWhenFrozenThrowsException() {
        instance.freeze();
        instance.setCompression(Compression.LZW);
    }

    @Test(expected = IllegalStateException.class)
    public void setFormatWhenFrozenThrowsException() {
        instance.freeze();
        instance.setFormat(Format.PNG);
    }

    @Test(expected = IllegalStateException.class)
    public void setInterlacingWhenFrozenThrowsException() {
        instance.freeze();
        instance.setInterlacing(false);
    }

    @Test
    public void setMaxComponentSizeWithZeroArgument() {
        instance.setMaxComponentSize(0);
        assertEquals(Integer.MAX_VALUE, instance.getMaxComponentSize());
    }

    @Test(expected = IllegalStateException.class)
    public void setMaxComponentSizeWhenFrozenThrowsException() {
        instance.freeze();
        instance.setMaxComponentSize(8);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setQualityWithZeroArgumentThrowsException() {
        instance.setQuality(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setQualityWithNegativeArgumentThrowsException() {
        instance.setQuality(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setQualityWithArgumentAboveMaxThrowsException() {
        instance.setQuality(Encode.MAX_QUALITY + 1);
    }

    @Test(expected = IllegalStateException.class)
    public void setQualityWhenFrozenThrowsException() {
        instance.freeze();
        instance.setQuality(50);
    }

    @Test
    public void setQualityWithValidArgument() {
        instance.setQuality(50);
        assertEquals(50, instance.getQuality());
    }

    @Test
    public void toMap() {
        instance.setCompression(Compression.JPEG);
        instance.setInterlacing(true);
        instance.setQuality(50);
        instance.setBackgroundColor(Color.BLUE);
        instance.setMaxComponentSize(10);

        final Map<String,Object> map = instance.toMap(new Dimension(500, 500));
        assertEquals("Encode", map.get("class"));
        assertEquals("#0000FF", map.get("background_color"));
        assertEquals(Compression.JPEG.toString(), map.get("compression"));
        assertEquals(Format.JPG.getPreferredMediaType(), map.get("format"));
        assertTrue((boolean) map.get("interlace"));
        assertEquals(50, map.get("quality"));
        assertEquals(10, map.get("max_sample_size"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void toMapReturnsUnmodifiableMap() {
        Dimension fullSize = new Dimension(100, 100);
        Map<String, Object> map = instance.toMap(fullSize);
        map.put("test", "test");
    }

    @Test
    public void testToString() {
        instance.setCompression(Compression.JPEG);
        instance.setInterlacing(true);
        instance.setQuality(50);
        instance.setBackgroundColor(Color.BLUE);
        instance.setMaxComponentSize(10);
        assertEquals("jpg_JPEG_50_interlace_#0000FF_10", instance.toString());
    }

}
