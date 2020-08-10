package edu.illinois.library.cantaloupe.resource.iiif.v3;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.CropByPercent;
import edu.illinois.library.cantaloupe.operation.CropByPixels;
import edu.illinois.library.cantaloupe.operation.CropToSquare;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RegionTest extends BaseTest {

    private static final double DELTA = 0.0000001;

    private Region instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        instance = new Region();
        instance.setType(Region.Type.PERCENT);
        instance.setX(20);
        instance.setY(20);
        instance.setWidth(20);
        instance.setHeight(20);
    }

    /* fromURI(String) */

    @Test
    void testFromURIFull() {
        Region r = Region.fromURI("full");
        assertEquals(Region.Type.FULL, r.getType());
    }

    @Test
    void testFromURISquare() {
        Region r = Region.fromURI("square");
        assertEquals(Region.Type.SQUARE, r.getType());
    }

    @Test
    void testFromURIPixels() {
        Region r = Region.fromURI("0,0,50,40");
        assertEquals(0f, r.getX(), DELTA);
        assertEquals(0f, r.getY(), DELTA);
        assertEquals(50f, r.getWidth(), DELTA);
        assertEquals(40f, r.getHeight(), DELTA);
        assertEquals(Region.Type.PIXELS, r.getType());
    }

    @Test
    void testFromURIPixelsWithIllegalX() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Region.fromURI("-2,3,50,50"));
    }

    @Test
    void testFromURIPixelsWithIllegalY() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Region.fromURI("2,-3,50,50"));
    }

    @Test
    void testFromURIPixelsWithIllegalWidth() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Region.fromURI("2,3,-50,50"));
    }

    @Test
    void testFromURIPixelsWithIllegalHeight() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Region.fromURI("2,3,50,-50"));
    }

    @Test
    void testFromURIPercent() {
        Region r = Region.fromURI("pct:0,0,50,40");
        assertEquals(0f, r.getX(), DELTA);
        assertEquals(0f, r.getY(), DELTA);
        assertEquals(50f, r.getWidth(), DELTA);
        assertEquals(40f, r.getHeight(), DELTA);
        assertEquals(Region.Type.PERCENT, r.getType());
    }

    @Test
    void testFromURIPercentWithIllegalX() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Region.fromURI("pct:-2,3,50,50"));
    }

    @Test
    void testFromURIPercentWithIllegalY() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Region.fromURI("pct:2,-3,50,50"));
    }

    @Test
    void testFromURIPercentWithIllegalWidth() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Region.fromURI("pct:2,3,-50,50"));
    }

    @Test
    void testFromURIPercentWithIllegalHeight() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Region.fromURI("pct:2,3,50,-50"));
    }

    /* equals() */

    @Test
    void testEqualsWithFullRegions() {
        Region region1 = new Region();
        region1.setType(Region.Type.FULL);
        Region region2 = new Region();
        region2.setType(Region.Type.FULL);
        assertEquals(region1, region2);
    }

    @Test
    void testEqualsWithSquareRegions() {
        Region region1 = new Region();
        region1.setType(Region.Type.SQUARE);
        Region region2 = new Region();
        region2.setType(Region.Type.SQUARE);
        assertEquals(region1, region2);
    }

    @Test
    void testEqualsWithEqualPercentRegions() {
        Region region2 = new Region();
        region2.setType(Region.Type.PERCENT);
        region2.setX(20f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(20f);
        assertEquals(region2, instance);
    }

    @Test
    void testEqualsWithUnequalPercentRegionX() {
        Region region1 = new Region();
        region1.setType(Region.Type.PERCENT);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setType(Region.Type.PERCENT);
        region2.setX(51f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(20f);

        assertNotEquals(region1, region2);
    }

    @Test
    void testEqualsWithUnequalPercentRegionY() {
        Region region1 = new Region();
        region1.setType(Region.Type.PERCENT);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setType(Region.Type.PERCENT);
        region2.setX(50f);
        region2.setY(21f);
        region2.setWidth(20f);
        region2.setHeight(20f);

        assertNotEquals(region1, region2);
    }

    @Test
    void testEqualsWithUnequalPercentRegionWidth() {
        Region region1 = new Region();
        region1.setType(Region.Type.PERCENT);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setType(Region.Type.PERCENT);
        region2.setX(50f);
        region2.setY(20f);
        region2.setWidth(21f);
        region2.setHeight(20f);

        assertNotEquals(region1, region2);
    }

    @Test
    void testEqualsWithUnequalPercentRegionHeight() {
        Region region1 = new Region();
        region1.setType(Region.Type.PERCENT);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setType(Region.Type.PERCENT);
        region2.setX(50f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(21f);

        assertNotEquals(region1, region2);
    }

    @Test
    void testEqualsWithEqualPixelRegions() {
        Region region1 = new Region();
        region1.setType(Region.Type.PIXELS);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setType(Region.Type.PIXELS);
        region2.setX(50f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(20f);

        assertEquals(region1, region2);
    }

    @Test
    void testEqualsWithUnequalPixelRegionX() {
        Region region1 = new Region();
        region1.setType(Region.Type.PIXELS);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setType(Region.Type.PIXELS);
        region2.setX(51f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(20f);

        assertNotEquals(region1, region2);
    }

    @Test
    void testEqualsWithUnequalPixelRegionY() {
        Region region1 = new Region();
        region1.setType(Region.Type.PIXELS);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setType(Region.Type.PIXELS);
        region2.setX(50f);
        region2.setY(21f);
        region2.setWidth(20f);
        region2.setHeight(20f);

        assertNotEquals(region1, region2);
    }

    @Test
    void testEqualsWithUnequalPixelRegionWidth() {
        Region region1 = new Region();
        region1.setType(Region.Type.PIXELS);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setType(Region.Type.PIXELS);
        region2.setX(50f);
        region2.setY(20f);
        region2.setWidth(21f);
        region2.setHeight(20f);

        assertNotEquals(region1, region2);
    }

    @Test
    void testEqualsWithUnequalPixelRegionHeight() {
        Region region1 = new Region();
        region1.setType(Region.Type.PIXELS);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setType(Region.Type.PIXELS);
        region2.setX(50f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(21f);

        assertNotEquals(region1, region2);
    }

    /* height */

    @Test
    void testSetHeight() {
        double height = 50;
        instance.setHeight(height);
        assertEquals(height, this.instance.getHeight(), DELTA);
    }

    @Test
    void testSetZeroHeight() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setHeight(0f));
    }

    @Test
    void testSetNegativeHeight() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setHeight(-1f));
    }

    /* width */

    @Test
    void testSetWidth() {
        double width = 50;
        instance.setWidth(width);
        assertEquals(width, this.instance.getWidth(), DELTA);
    }

    @Test
    void testSetZeroWidth() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setWidth(0f));
    }

    @Test
    void testSetNegativeWidth() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setWidth(-1f));
    }

    /* x */

    @Test
    void testSetX() {
        double x = 50;
        instance.setX(x);
        assertEquals(x, instance.getX(), DELTA);
    }

    @Test
    void testSetNegativeX() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setX(-1f));
    }

    /* y */

    @Test
    void testSetY() {
        double y = 50;
        instance.setY(y);
        assertEquals(y, instance.getY(), DELTA);
    }

    @Test
    void testSetNegativeY() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setY(-1f));
    }

    @Test
    void testToCropWithFull() {
        instance = new Region();
        instance.setType(Region.Type.FULL);
        Crop actual = instance.toCrop();
        Crop expected = new CropByPercent(0, 0, 1, 1);
        assertEquals(expected, actual);
    }

    @Test
    void testToCropWithSquare() {
        Crop expected = new CropToSquare();

        instance = new Region();
        instance.setType(Region.Type.SQUARE);
        Crop actual = instance.toCrop();

        assertEquals(expected, actual);
    }

    @Test
    void testToCropWithPixels() {
        Crop expected = new CropByPixels(10, 20, 200, 100);

        instance = new Region();
        instance.setX(10f);
        instance.setY(20f);
        instance.setWidth(200f);
        instance.setHeight(100f);
        Crop actual = instance.toCrop();

        assertEquals(expected, actual);
    }

    @Test
    void testToCropWithPercent() {
        Crop expected = new CropByPercent(0.3, 0.4, 0.5, 0.6);

        instance = new Region();
        instance.setType(Region.Type.PERCENT);
        instance.setX(30f);
        instance.setY(40f);
        instance.setWidth(50f);
        instance.setHeight(60f);
        Crop actual = instance.toCrop();

        assertEquals(expected, actual);
    }

    @Test
    void testToStringWithFull() {
        Region r = Region.fromURI("full");
        assertEquals("full", r.toString());
    }

    @Test
    void testToStringWithSquare() {
        Region r = Region.fromURI("square");
        assertEquals("square", r.toString());
    }

    @Test
    void testToStringWithPixels() {
        Region r = Region.fromURI("0,0,50,40");
        assertEquals("0,0,50,40", r.toString());
    }

    @Test
    void testToStringWithPercent() {
        Region r = Region.fromURI("pct:0,0,50,40");
        assertEquals("pct:0,0,50,40", r.toString());
    }

    @Test
    void testToCanonicalStringWithFull() {
        final Dimension fullSize = new Dimension(1000, 800);
        Region r = Region.fromURI("full");
        assertEquals("full", r.toCanonicalString(fullSize));
    }

    @Test
    void testToCanonicalStringWithSquare() {
        final Dimension fullSize = new Dimension(1000, 800);
        Region r = Region.fromURI("square");
        assertEquals("square", r.toCanonicalString(fullSize));
    }

    @Test
    void testToCanonicalStringWithPixels() {
        final Dimension fullSize = new Dimension(1000, 800);
        Region r = Region.fromURI("0,0,50,40");
        assertEquals("0,0,50,40", r.toCanonicalString(fullSize));
    }

    @Test
    void testToCanonicalStringWithPercent() {
        final Dimension fullSize = new Dimension(1000, 800);
        Region r = Region.fromURI("pct:0,0,50,40");
        assertEquals("0,0,500,320", r.toCanonicalString(fullSize));
    }

}
