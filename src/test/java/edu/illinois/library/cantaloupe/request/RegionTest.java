package edu.illinois.library.cantaloupe.request;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.image.Region;

import java.awt.Dimension;
import java.awt.Rectangle;

public class RegionTest extends CantaloupeTestCase {

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
        assertEquals(new Float(50), r.getWidth());
        assertEquals(new Float(40), r.getHeight());
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
        assertEquals(new Float(50), r.getWidth());
        assertEquals(new Float(40), r.getHeight());
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

    public void testGetRectangle() {
        Dimension fullSize = new Dimension(200, 200);
        // full
        Region region = Region.fromUri("full");
        assertEquals(new Rectangle(0, 0, 200, 200), region.getRectangle(fullSize));
        // pixels
        region = Region.fromUri("20,20,50,50");
        assertEquals(new Rectangle(20, 20, 50, 50), region.getRectangle(fullSize));
        // percentage
        region = Region.fromUri("pct:20,20,50,50");
        assertEquals(new Rectangle(40, 40, 100, 100), region.getRectangle(fullSize));
    }

    /* height */

    public void testSetHeight() {
        Float height = (float) 50;
        this.region.setHeight(height);
        assertEquals(height, this.region.getHeight());
    }

    public void testSetNegativeHeight() {
        try {
            this.region.setHeight((float) -1);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    public void testSetZeroHeight() {
        try {
            this.region.setHeight((float) 0);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    /* width */

    public void testSetWidth() {
        Float width = (float) 50;
        this.region.setWidth(width);
        assertEquals(width, this.region.getWidth());
    }

    public void testSetNegativeWidth() {
        try {
            this.region.setWidth((float) -1);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    public void testSetZeroWidth() {
        try {
            this.region.setWidth((float) 0);
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

    /* toString */

    public void testToString() {
        Region r = Region.fromUri("full");
        assertEquals("full", r.toString());

        r = Region.fromUri("0,0,50,40");
        assertEquals("0,0,50,40", r.toString());

        r = Region.fromUri("pct:0,0,50,40");
        assertEquals("pct:0,0,50,40", r.toString());
    }

}
