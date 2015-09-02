package edu.illinois.library.cantaloupe.image;

import junit.framework.TestCase;

public class SizeTest extends TestCase {

    private Size size;

    public void setUp() {
        this.size = new Size();
    }

    /* fromUri(String) */

    /**
     * Tests fromUri(String) with a value of "full".
     */
    public void testFromUriFull() {
        Size s = Size.fromUri("full");
        assertTrue(s.isFull());
    }

    /**
     * Tests fromUri(String) with width scaling.
     */
    public void testFromUriWidthScaled() {
        Size s = Size.fromUri("50,");
        assertEquals(new Integer(50), s.getWidth());
        assertFalse(s.isFull());
    }

    /**
     * Tests fromUri(String) with height scaling.
     */
    public void testFromUriHeightScaled() {
        Size s = Size.fromUri(",50");
        assertEquals(new Integer(50), s.getHeight());
        assertFalse(s.isFull());
    }

    /**
     * Tests fromUri(String) with percentage scaling.
     */
    public void testFromUriPercentageScaled() {
        Size s = Size.fromUri("pct:50");
        assertEquals(new Float(50), s.getScaleToPercent());
        assertFalse(s.isFull());
    }

    /**
     * Tests fromUri(String) with absolute width and height.
     */
    public void testFromUriAbsoluteScaled() {
        Size s = Size.fromUri("50,40");
        assertEquals(new Integer(50), s.getWidth());
        assertEquals(new Integer(40), s.getHeight());
        assertFalse(s.isScaleToFit());
    }

    /**
     * Tests fromUri(String) with scale-to-fit width and height.
     */
    public void testFromUriScaleToFit() {
        Size s = Size.fromUri("!50,40");
        assertEquals(new Integer(50), s.getWidth());
        assertEquals(new Integer(40), s.getHeight());
        assertTrue(s.isScaleToFit());
    }

    /* height */

    public void testSetHeight() {
        Integer height = 50;
        this.size.setHeight(height);
        assertEquals(height, this.size.getHeight());
    }

    public void testSetNegativeHeight() {
        try {
            this.size.setHeight(-1);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    public void testSetZeroHeight() {
        try {
            this.size.setHeight(0);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    /* width */

    public void testSetWidth() {
        Integer width = 50;
        this.size.setWidth(width);
        assertEquals(width, this.size.getWidth());
    }

    public void testSetNegativeWidth() {
        try {
            this.size.setWidth(-1);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    public void testSetZeroWidth() {
        try {
            this.size.setWidth(0);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

}
