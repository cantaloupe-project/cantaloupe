package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
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
    }

    /* getResultingSize(Dimension) */

    @Test
    public void testGetResultingSize() {
        Dimension size = new Dimension(500, 500);
        assertEquals(size, instance.getResultingSize(size));
    }

    /* hasEffect() */

    @Test
    public void testHasEffect() {
        assertTrue(instance.hasEffect());
    }

    /* hasEffect(Dimension, OperationList) */

    @Test
    public void testHasEffectWithArguments() {
        Dimension size = new Dimension(500, 500);
        OperationList opList = TestUtil.newOperationList();
        assertTrue(instance.hasEffect(size, opList));
    }

    /* setQuality() */

    @Test
    public void testSetQualityWithZeroArgumentThrowsException() {
        try {
            instance.setQuality(0);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void testSetQualityWithNegativeArgumentThrowsException() {
        try {
            instance.setQuality(-1);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void testSetQualityWithArgumentAboveMaxThrowsException() {
        try {
            instance.setQuality(Encode.MAX_QUALITY + 1);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void testSetQualityWithValidArgument() {
        instance.setQuality(50);
        assertEquals(50, instance.getQuality());
    }

    /* toMap() */

    @Test
    public void testToMap() {
        instance.setCompression(Compression.JPEG);
        instance.setInterlacing(true);
        instance.setQuality(50);
        instance.setBackgroundColor(Color.BLUE);

        final Map<String,Object> map = instance.toMap(new Dimension(500, 500));
        assertEquals("Encode", map.get("class"));
        assertEquals("#0000FF", map.get("background_color"));
        assertEquals(Compression.JPEG.toString(), map.get("compression"));
        assertEquals(Format.JPG.getPreferredMediaType(), map.get("format"));
        assertTrue((boolean) map.get("interlace"));
        assertEquals(50, map.get("quality"));
    }

    /* toString() */

    @Test
    public void testToString() {
        instance.setCompression(Compression.JPEG);
        instance.setInterlacing(true);
        instance.setQuality(50);
        instance.setBackgroundColor(Color.BLUE);
        assertEquals("jpg_JPEG_50_interlace_#0000FF", instance.toString());
    }

}
