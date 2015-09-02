package edu.illinois.library.image;

import junit.framework.TestCase;

public class RegionTest extends TestCase {

    private Region region;

    public void setUp() {
        this.region = new Region();
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
        assertEquals(new Float(0), r.getX());
        assertEquals(new Float(0), r.getY());
        assertEquals(new Integer(50), r.getWidth());
        assertEquals(new Integer(40), r.getHeight());
        assertFalse(r.isPercent());
        assertFalse(r.isFull());
    }

    /**
     * Tests fromUri(String) with percentage values.
     */
    public void testFromUriPercentage() {
        Region r = Region.fromUri("pct:0,0,50,40");
        assertEquals(new Float(0), r.getX());
        assertEquals(new Float(0), r.getY());
        assertEquals(new Integer(50), r.getWidth());
        assertEquals(new Integer(40), r.getHeight());
        assertTrue(r.isPercent());
        assertFalse(r.isFull());
    }

    public void testFromUriWithIllegalValues() {
        // TODO: write this
    }

    /* height */

    public void testSetHeight() {
        Integer height = 50;
        this.region.setHeight(height);
        assertEquals(height, this.region.getHeight());
    }

    public void testSetNegativeHeight() {
        try {
            this.region.setHeight(-1);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    public void testSetZeroHeight() {
        try {
            this.region.setHeight(0);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    /* width */

    public void testSetWidth() {
        Integer width = 50;
        this.region.setWidth(width);
        assertEquals(width, this.region.getWidth());
    }

    public void testSetNegativeWidth() {
        try {
            this.region.setWidth(-1);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    public void testSetZeroWidth() {
        try {
            this.region.setWidth(0);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    /* x */

    public void testSetX() {
        Float x = new Float(50.0);
        this.region.setX(x);
        assertEquals(x, this.region.getX());
    }

    public void testSetNegativeX() {
        try {
            this.region.setX(new Float(-1));
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("X must be a positive float", e.getMessage());
        }
    }

    /* y */

    public void testSetY() {
        Float y = new Float(50.0);
        this.region.setY(y);
        assertEquals(y, this.region.getY());
    }

    public void testSetNegativeY() {
        try {
            this.region.setY(new Float(-1));
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Y must be a positive float", e.getMessage());
        }
    }

}
