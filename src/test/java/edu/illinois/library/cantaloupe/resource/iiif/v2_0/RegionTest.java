package edu.illinois.library.cantaloupe.resource.iiif.v2_0;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.image.Crop;

public class RegionTest extends CantaloupeTestCase {

    private Region region;

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
    public void testFromUriFull() {
        Region r = Region.fromUri("full");
        assertTrue(r.isFull());
        assertFalse(r.isPercent());
    }

    /**
     * Tests fromUri(String) with absolute pixel values.
     */
    public void testFromUriAbsolute() {
        Region r = Region.fromUri("0,0,50,40");
        assertEquals(0f, r.getX());
        assertEquals(0f, r.getY());
        assertEquals(50f, r.getWidth());
        assertEquals(40f, r.getHeight());
        assertFalse(r.isPercent());
        assertFalse(r.isFull());
    }

    /**
     * Tests fromUri(String) with percentage values.
     */
    public void testFromUriPercentage() {
        Region r = Region.fromUri("pct:0,0,50,40");
        assertEquals(0f, r.getX());
        assertEquals(0f, r.getY());
        assertEquals(50f, r.getWidth());
        assertEquals(40f, r.getHeight());
        assertTrue(r.isPercent());
        assertFalse(r.isFull());
    }

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

    public void testEqualsWithEqualRegion() {
        Region region2 = new Region();
        region2.setPercent(true);
        region2.setX(20f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(20f);
        assertTrue(region.equals(region2));
    }

    public void testEqualsWithUnequalRegion1() {
        Region region2 = new Region();
        region2.setPercent(true);
        region2.setX(50f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(20f);
        assertFalse(region.equals(region2));
    }

    public void testEqualsWithUnequalRegion2() {
        Region region2 = new Region();
        region2.setPercent(true);
        region2.setX(20f);
        region2.setY(50f);
        region2.setWidth(20f);
        region2.setHeight(20f);
        assertFalse(region.equals(region2));
    }

    public void testEqualsWithUnequalRegion3() {
        Region region2 = new Region();
        region2.setPercent(true);
        region2.setX(20f);
        region2.setY(20f);
        region2.setWidth(50f);
        region2.setHeight(20f);
        assertFalse(region.equals(region2));
    }

    public void testEqualsWithUnequalRegion4() {
        Region region2 = new Region();
        region2.setPercent(true);
        region2.setX(20f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(50f);
        assertFalse(region.equals(region2));
    }

    public void testEqualsWithEqualCrop() {
        Crop crop = new Crop();
        crop.setPercent(true);
        crop.setX(0.2f);
        crop.setY(0.2f);
        crop.setWidth(0.2f);
        crop.setHeight(0.2f);
        assertTrue(region.equals(crop));
    }

    public void testEqualsWithUnequalCrop1() {
        Crop crop = new Crop();
        crop.setPercent(true);
        crop.setX(0.5f);
        crop.setY(0.2f);
        crop.setWidth(0.2f);
        crop.setHeight(0.2f);
        assertFalse(region.equals(crop));
    }

    public void testEqualsWithUnequalCrop2() {
        Crop crop = new Crop();
        crop.setPercent(true);
        crop.setX(0.2f);
        crop.setY(0.5f);
        crop.setWidth(0.2f);
        crop.setHeight(0.2f);
        assertFalse(region.equals(crop));
    }

    public void testEqualsWithUnequalCrop3() {
        Crop crop = new Crop();
        crop.setPercent(true);
        crop.setX(0.2f);
        crop.setY(0.2f);
        crop.setWidth(0.5f);
        crop.setHeight(0.2f);
        assertFalse(region.equals(crop));
    }

    public void testEqualsWithUnequalCrop4() {
        Crop crop = new Crop();
        crop.setPercent(true);
        crop.setX(0.2f);
        crop.setY(0.2f);
        crop.setWidth(0.2f);
        crop.setHeight(0.5f);
        assertFalse(region.equals(crop));
    }

    /* height */

    public void testSetHeight() {
        float height = 50f;
        this.region.setHeight(height);
        assertEquals(height, this.region.getHeight());
    }

    public void testSetNegativeHeight() {
        try {
            this.region.setHeight(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    public void testSetZeroHeight() {
        try {
            this.region.setHeight(0f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    /* width */

    public void testSetWidth() {
        float width = 50f;
        this.region.setWidth(width);
        assertEquals(width, this.region.getWidth());
    }

    public void testSetNegativeWidth() {
        try {
            this.region.setWidth(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    public void testSetZeroWidth() {
        try {
            this.region.setWidth(0f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    /* x */

    public void testSetX() {
        Float x = 50.0f;
        this.region.setX(x);
        assertEquals(x, this.region.getX());
    }

    public void testSetNegativeX() {
        try {
            this.region.setX(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("X must be a positive float", e.getMessage());
        }
    }

    /* y */

    public void testSetY() {
        float y = 50.0f;
        this.region.setY(y);
        assertEquals(y, this.region.getY());
    }

    public void testSetNegativeY() {
        try {
            this.region.setY(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Y must be a positive float", e.getMessage());
        }
    }

    public void testToCrop() {
        region = new Region();
        region.setX(30f);
        region.setY(40f);
        region.setWidth(50f);
        region.setHeight(50f);
        region.setPercent(true);
        region.setFull(false);
        Crop crop = region.toCrop();
        assertEquals(region.getX(), crop.getX());
        assertEquals(region.getY(), crop.getY());
        assertEquals(region.getWidth(), crop.getWidth());
        assertEquals(region.getHeight(), crop.getHeight());
        assertEquals(region.isPercent(), crop.isPercent());
        assertEquals(region.isFull(), crop.isFull());
    }

    public void testToString() {
        Region r = Region.fromUri("full");
        assertEquals("full", r.toString());

        r = Region.fromUri("0,0,50,40");
        assertEquals("0,0,50,40", r.toString());

        r = Region.fromUri("pct:0,0,50,40");
        assertEquals("pct:0,0,50,40", r.toString());
    }

}
