package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.CropByPercent;
import edu.illinois.library.cantaloupe.operation.CropByPixels;
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
        instance.setPercent(true);
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
    public void testFromUriWithFull() {
        Region r = Region.fromUri("full");
        assertTrue(r.isFull());
        assertFalse(r.isPercent());
    }

    /**
     * Tests fromUri(String) with absolute pixel values.
     */
    @Test
    public void testFromUriWithPixels() {
        Region r = Region.fromUri("0,0,50,40");
        assertEquals(0f, r.getX(), DELTA);
        assertEquals(0f, r.getY(), DELTA);
        assertEquals(50f, r.getWidth(), DELTA);
        assertEquals(40f, r.getHeight(), DELTA);
        assertFalse(r.isPercent());
        assertFalse(r.isFull());
    }

    /**
     * Tests fromUri(String) with percentage values.
     */
    @Test
    public void testFromUriWithPercent() {
        Region r = Region.fromUri("pct:0,0,50,40");
        assertEquals(0f, r.getX(), DELTA);
        assertEquals(0f, r.getY(), DELTA);
        assertEquals(50f, r.getWidth(), DELTA);
        assertEquals(40f, r.getHeight(), DELTA);
        assertTrue(r.isPercent());
        assertFalse(r.isFull());
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testFromUriWithIllegalX() {
        Region.fromUri("pct:-2,3,50,50");
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testFromUriWithIllegalY() {
        Region.fromUri("pct:2,-3,50,50");
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testFromUriWithIllegalWidth() {
        Region.fromUri("2,3,-50,50");
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testFromUriWithIllegalHeight() {
        Region.fromUri("2,3,50,-50");
    }

    /* equals() */

    @Test
    public void testEqualsWithFullRegions() {
        Region region1 = new Region();
        region1.setFull(true);
        Region region2 = new Region();
        region2.setFull(true);
        assertEquals(region1, region2);
    }

    @Test
    public void testEqualsWithEqualPixelRegions() {
        Region region1 = new Region();
        region1.setPercent(false);
        region1.setX(20f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setPercent(false);
        region2.setX(20f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(20f);

        assertEquals(region1, region2);
    }

    @Test
    public void testEqualsWithUnequalPixelRegionX() {
        Region region1 = new Region();
        region1.setPercent(false);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setPercent(false);
        region2.setX(51f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(20f);

        assertNotEquals(region1, region2);
    }

    @Test
    public void testEqualsWithUnequalPixelRegionY() {
        Region region1 = new Region();
        region1.setPercent(false);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setPercent(false);
        region2.setX(50f);
        region2.setY(21f);
        region2.setWidth(20f);
        region2.setHeight(20f);

        assertNotEquals(region1, region2);
    }

    @Test
    public void testEqualsWithUnequalPixelRegionWidth() {
        Region region1 = new Region();
        region1.setPercent(false);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setPercent(false);
        region2.setX(50f);
        region2.setY(20f);
        region2.setWidth(21f);
        region2.setHeight(20f);

        assertNotEquals(region1, region2);
    }

    @Test
    public void testEqualsWithUnequalPixelRegionHeight() {
        Region region1 = new Region();
        region1.setPercent(false);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setPercent(false);
        region2.setX(50f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(21f);

        assertNotEquals(region1, region2);
    }

    @Test
    public void testEqualsWithEqualPercentRegions() {
        Region region2 = new Region();
        region2.setPercent(true);
        region2.setX(20f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(20f);
        assertEquals(region2, instance);
    }

    @Test
    public void testEqualsWithUnequalPercentRegionX() {
        Region region1 = new Region();
        region1.setPercent(true);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setPercent(true);
        region2.setX(51f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(20f);

        assertNotEquals(region1, region2);
    }

    @Test
    public void testEqualsWithUnequalPercentRegionY() {
        Region region1 = new Region();
        region1.setPercent(true);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setPercent(true);
        region2.setX(50f);
        region2.setY(21f);
        region2.setWidth(20f);
        region2.setHeight(20f);

        assertNotEquals(region1, region2);
    }

    @Test
    public void testEqualsWithUnequalPercentRegionWidth() {
        Region region1 = new Region();
        region1.setPercent(true);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setPercent(true);
        region2.setX(50f);
        region2.setY(20f);
        region2.setWidth(21f);
        region2.setHeight(20f);

        assertNotEquals(region1, region2);
    }

    @Test
    public void testEqualsWithUnequalPercentRegionHeight() {
        Region region1 = new Region();
        region1.setPercent(true);
        region1.setX(50f);
        region1.setY(20f);
        region1.setWidth(20f);
        region1.setHeight(20f);

        Region region2 = new Region();
        region2.setPercent(true);
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
        assertEquals(height, instance.getHeight(), DELTA);
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
        instance.setWidth(width);
        assertEquals(width, instance.getWidth(), DELTA);
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
        float x = 50f;
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

    /* toCrop() */

    @Test
    public void testToCropWithFull() {
        instance = new Region();
        instance.setFull(true);
        Crop actual = instance.toCrop();
        Crop expected = new CropByPercent(0, 0, 1, 1);
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
        instance.setPercent(true);
        instance.setX(30f);
        instance.setY(40f);
        instance.setWidth(50f);
        instance.setHeight(60f);
        Crop actual = instance.toCrop();

        assertEquals(expected, actual);
    }

    /* toString() */

    @Test
    public void testToStringWithFull() {
        Region r = Region.fromUri("full");
        assertEquals("full", r.toString());
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

}
