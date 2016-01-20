package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.image.Crop;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RegionTest {

    private static final float FUDGE = 0.0000001f;

    private Region region;

    @Before
    public void setUp() {
        region = new Region();
        region.setPercent(true);
        region.setX(20f);
        region.setY(20f);
        region.setWidth(20f);
        region.setHeight(20f);
    }

    /* fromUri(String) */

    /**
     * Tests fromUri(String) with a value of "full".
     */
    @Test
    public void testFromUriFull() {
        Region r = Region.fromUri("full");
        assertTrue(r.isFull());
        assertFalse(r.isPercent());
    }

    /**
     * Tests fromUri(String) with absolute pixel values.
     */
    @Test
    public void testFromUriAbsolute() {
        Region r = Region.fromUri("0,0,50,40");
        assertEquals(0f, r.getX(), FUDGE);
        assertEquals(0f, r.getY(), FUDGE);
        assertEquals(50f, r.getWidth(), FUDGE);
        assertEquals(40f, r.getHeight(), FUDGE);
        assertFalse(r.isPercent());
        assertFalse(r.isFull());
    }

    /**
     * Tests fromUri(String) with percentage values.
     */
    @Test
    public void testFromUriPercentage() {
        Region r = Region.fromUri("pct:0,0,50,40");
        assertEquals(0f, r.getX(), FUDGE);
        assertEquals(0f, r.getY(), FUDGE);
        assertEquals(50f, r.getWidth(), FUDGE);
        assertEquals(40f, r.getHeight(), FUDGE);
        assertTrue(r.isPercent());
        assertFalse(r.isFull());
    }

    @Test
    public void testFromUriWithIllegalValues() {
        try {
            Region.fromUri("pct:-2,3,50,50");
        } catch (IllegalArgumentException e) {
            assertEquals("X must be a positive float", e.getMessage());
        }
        try {
            Region.fromUri("pct:2,-3,50,50");
        } catch (IllegalArgumentException e) {
            assertEquals("Y must be a positive float", e.getMessage());
        }
        try {
            Region.fromUri("2,3,-50,50");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
        try {
            Region.fromUri("2,3,50,-50");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    /* equals */

    @Test
    public void testEqualsWithEqualRegion() {
        Region region2 = new Region();
        region2.setPercent(true);
        region2.setX(20f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(20f);
        assertTrue(region.equals(region2));
    }

    @Test
    public void testEqualsWithUnequalRegion1() {
        Region region2 = new Region();
        region2.setPercent(true);
        region2.setX(50f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(20f);
        assertFalse(region.equals(region2));
    }

    @Test
    public void testEqualsWithUnequalRegion2() {
        Region region2 = new Region();
        region2.setPercent(true);
        region2.setX(20f);
        region2.setY(50f);
        region2.setWidth(20f);
        region2.setHeight(20f);
        assertFalse(region.equals(region2));
    }

    @Test
    public void testEqualsWithUnequalRegion3() {
        Region region2 = new Region();
        region2.setPercent(true);
        region2.setX(20f);
        region2.setY(20f);
        region2.setWidth(50f);
        region2.setHeight(20f);
        assertFalse(region.equals(region2));
    }

    @Test
    public void testEqualsWithUnequalRegion4() {
        Region region2 = new Region();
        region2.setPercent(true);
        region2.setX(20f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(50f);
        assertFalse(region.equals(region2));
    }

    @Test
    public void testEqualsWithEqualCrop() {
        Crop crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setX(0.2f);
        crop.setY(0.2f);
        crop.setWidth(0.2f);
        crop.setHeight(0.2f);
        assertTrue(region.equals(crop));
    }

    @Test
    public void testEqualsWithUnequalCrop1() {
        Crop crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setX(0.5f);
        crop.setY(0.2f);
        crop.setWidth(0.2f);
        crop.setHeight(0.2f);
        assertFalse(region.equals(crop));
    }

    @Test
    public void testEqualsWithUnequalCrop2() {
        Crop crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setX(0.2f);
        crop.setY(0.5f);
        crop.setWidth(0.2f);
        crop.setHeight(0.2f);
        assertFalse(region.equals(crop));
    }

    @Test
    public void testEqualsWithUnequalCrop3() {
        Crop crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setX(0.2f);
        crop.setY(0.2f);
        crop.setWidth(0.5f);
        crop.setHeight(0.2f);
        assertFalse(region.equals(crop));
    }

    @Test
    public void testEqualsWithUnequalCrop4() {
        Crop crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setX(0.2f);
        crop.setY(0.2f);
        crop.setWidth(0.2f);
        crop.setHeight(0.5f);
        assertFalse(region.equals(crop));
    }

    /* height */

    @Test
    public void testSetHeight() {
        float height = 50f;
        this.region.setHeight(height);
        assertEquals(height, this.region.getHeight(), FUDGE);
    }

    @Test
    public void testSetNegativeHeight() {
        try {
            this.region.setHeight(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void testSetZeroHeight() {
        try {
            this.region.setHeight(0f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    /* width */

    @Test
    public void testSetWidth() {
        float width = 50f;
        this.region.setWidth(width);
        assertEquals(width, this.region.getWidth(), FUDGE);
    }

    @Test
    public void testSetNegativeWidth() {
        try {
            this.region.setWidth(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void testSetZeroWidth() {
        try {
            this.region.setWidth(0f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    /* x */

    @Test
    public void testSetX() {
        Float x = 50.0f;
        this.region.setX(x);
        assertEquals(x, this.region.getX());
    }

    @Test
    public void testSetNegativeX() {
        try {
            this.region.setX(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("X must be a positive float", e.getMessage());
        }
    }

    /* y */

    @Test
    public void testSetY() {
        float y = 50.0f;
        this.region.setY(y);
        assertEquals(y, this.region.getY(), FUDGE);
    }

    @Test
    public void testSetNegativeY() {
        try {
            this.region.setY(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Y must be a positive float", e.getMessage());
        }
    }

    @Test
    public void testToCrop() {
        region = new Region();
        region.setX(30f);
        region.setY(40f);
        region.setWidth(50f);
        region.setHeight(50f);
        region.setPercent(true);
        region.setFull(false);
        assertTrue(region.equals(region.toCrop()));
    }

    @Test
    public void testToString() {
        Region r = Region.fromUri("full");
        assertEquals("full", r.toString());

        r = Region.fromUri("0,0,50,40");
        assertEquals("0,0,50,40", r.toString());

        r = Region.fromUri("pct:0,0,50,40");
        assertEquals("pct:0,0,50,40", r.toString());
    }

}
