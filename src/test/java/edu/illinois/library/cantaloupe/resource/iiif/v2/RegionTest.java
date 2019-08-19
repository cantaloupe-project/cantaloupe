package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.CropByPercent;
import edu.illinois.library.cantaloupe.operation.CropByPixels;
import edu.illinois.library.cantaloupe.operation.CropToSquare;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RegionTest extends BaseTest {

    private static final float DELTA = 0.0000001f;

    private Region instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        instance = new Region();
        instance.setType(Region.Type.PERCENT);
        instance.setX(20f);
        instance.setY(20f);
        instance.setWidth(20f);
        instance.setHeight(20f);
    }

    /* fromUri(String) */

    /**
     * Tests fromUri(String) with a value of "full".
     */
    @Test
    public void testFromUriFull() {
        Region r = Region.fromUri("full");
        assertEquals(Region.Type.FULL, r.getType());
    }

    /**
     * Tests fromUri(String) with a value of "square".
     */
    @Test
    public void testFromUriSquare() {
        Region r = Region.fromUri("square");
        assertEquals(Region.Type.SQUARE, r.getType());
    }

    /**
     * Tests fromUri(String) with absolute pixel values.
     */
    @Test
    public void testFromUriPixels() {
        Region r = Region.fromUri("0,0,50,40");
        assertEquals(0f, r.getX(), DELTA);
        assertEquals(0f, r.getY(), DELTA);
        assertEquals(50f, r.getWidth(), DELTA);
        assertEquals(40f, r.getHeight(), DELTA);
        assertEquals(Region.Type.PIXELS, r.getType());
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testFromUriPixelsWithIllegalX() {
        Region.fromUri("-2,3,50,50");
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testFromUriPixelsWithIllegalY() {
        Region.fromUri("2,-3,50,50");
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testFromUriPixelsWithIllegalWidth() {
        Region.fromUri("2,3,-50,50");
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testFromUriPixelsWithIllegalHeight() {
        Region.fromUri("2,3,50,-50");
    }

    /**
     * Tests fromUri(String) with percentage values.
     */
    @Test
    public void testFromUriPercent() {
        Region r = Region.fromUri("pct:0,0,50,40");
        assertEquals(0f, r.getX(), DELTA);
        assertEquals(0f, r.getY(), DELTA);
        assertEquals(50f, r.getWidth(), DELTA);
        assertEquals(40f, r.getHeight(), DELTA);
        assertEquals(Region.Type.PERCENT, r.getType());
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testFromUriPercentWithIllegalX() {
        Region.fromUri("pct:-2,3,50,50");
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testFromUriPercentWithIllegalY() {
        Region.fromUri("pct:2,-3,50,50");
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testFromUriPercentWithIllegalWidth() {
        Region.fromUri("pct:2,3,-50,50");
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testFromUriPercentWithIllegalHeight() {
        Region.fromUri("pct:2,3,50,-50");
    }

    /* equals() */

    @Test
    public void testEqualsWithFullRegions() {
        Region region1 = new Region();
        region1.setType(Region.Type.FULL);
        Region region2 = new Region();
        region2.setType(Region.Type.FULL);
        assertEquals(region1, region2);
    }

    @Test
    public void testEqualsWithSquareRegions() {
        Region region1 = new Region();
        region1.setType(Region.Type.SQUARE);
        Region region2 = new Region();
        region2.setType(Region.Type.SQUARE);
        assertEquals(region1, region2);
    }

    @Test
    public void testEqualsWithEqualPercentRegions() {
        Region region2 = new Region();
        region2.setType(Region.Type.PERCENT);
        region2.setX(20f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(20f);
        assertEquals(region2, instance);
    }

    @Test
    public void testEqualsWithUnequalPercentRegionX() {
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
    public void testEqualsWithUnequalPercentRegionY() {
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
    public void testEqualsWithUnequalPercentRegionWidth() {
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
    public void testEqualsWithUnequalPercentRegionHeight() {
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
    public void testEqualsWithEqualPixelRegions() {
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
    public void testEqualsWithUnequalPixelRegionX() {
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
    public void testEqualsWithUnequalPixelRegionY() {
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
    public void testEqualsWithUnequalPixelRegionWidth() {
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
    public void testEqualsWithUnequalPixelRegionHeight() {
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
    public void testSetHeight() {
        float height = 50f;
        instance.setHeight(height);
        assertEquals(height, this.instance.getHeight(), DELTA);
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testSetZeroHeight() {
        instance.setHeight(0f);
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testSetNegativeHeight() {
        instance.setHeight(-1f);
    }

    /* width */

    @Test
    public void testSetWidth() {
        float width = 50f;
        this.instance.setWidth(width);
        assertEquals(width, this.instance.getWidth(), DELTA);
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testSetZeroWidth() {
        instance.setWidth(0f);
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testSetNegativeWidth() {
        instance.setWidth(-1f);
    }

    /* x */

    @Test
    public void testSetX() {
        float x = 50.0f;
        instance.setX(x);
        assertEquals(x, instance.getX(), DELTA);
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testSetNegativeX() {
        instance.setX(-1f);
    }

    /* y */

    @Test
    public void testSetY() {
        float y = 50.0f;
        instance.setY(y);
        assertEquals(y, instance.getY(), DELTA);
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testSetNegativeY() {
        instance.setY(-1f);
    }

    @Test
    public void testToCropWithFull() {
        instance = new Region();
        instance.setType(Region.Type.FULL);
        Crop actual = instance.toCrop();
        Crop expected = new CropByPercent(0, 0, 1, 1);
        assertEquals(expected, actual);
    }

    @Test
    public void testToCropWithSquare() {
        Crop expected = new CropToSquare();

        instance = new Region();
        instance.setType(Region.Type.SQUARE);
        Crop actual = instance.toCrop();

        assertEquals(expected, actual);
    }

    @Test
    public void testToCropWithPixels() {
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
    public void testToCropWithPercent() {
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
    public void testToStringWithFull() {
        Region r = Region.fromUri("full");
        assertEquals("full", r.toString());
    }

    @Test
    public void testToStringWithSquare() {
        Region r = Region.fromUri("square");
        assertEquals("square", r.toString());
    }

    @Test
    public void testToStringWithPixels() {
        Region r = Region.fromUri("0,0,50,40");
        assertEquals("0,0,50,40", r.toString());
    }

    @Test
    public void testToStringWithPercent() {
        Region r = Region.fromUri("pct:0,0,50,40");
        assertEquals("pct:0,0,50,40", r.toString());
    }

    @Test
    public void testToCanonicalStringWithFull() {
        final Dimension fullSize = new Dimension(1000, 800);
        Region r = Region.fromUri("full");
        assertEquals("full", r.toCanonicalString(fullSize));
    }

    @Test
    public void testToCanonicalStringWithSquare() {
        final Dimension fullSize = new Dimension(1000, 800);
        Region r = Region.fromUri("square");
        assertEquals("square", r.toCanonicalString(fullSize));
    }

    @Test
    public void testToCanonicalStringWithPixels() {
        final Dimension fullSize = new Dimension(1000, 800);
        Region r = Region.fromUri("0,0,50,40");
        assertEquals("0,0,50,40", r.toCanonicalString(fullSize));
    }

    @Test
    public void testToCanonicalStringWithPercent() {
        final Dimension fullSize = new Dimension(1000, 800);
        Region r = Region.fromUri("pct:0,0,50,40");
        assertEquals("0,0,500,320", r.toCanonicalString(fullSize));
    }

}
