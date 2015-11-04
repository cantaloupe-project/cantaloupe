package edu.illinois.library.cantaloupe.resource.iiif.v2_0;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;

public class SizeTest extends CantaloupeTestCase {

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
        assertEquals(Size.ScaleMode.FULL, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with width scaling.
     */
    public void testFromUriWidthScaled() {
        Size s = Size.fromUri("50,");
        assertEquals(new Integer(50), s.getWidth());
        assertEquals(Size.ScaleMode.ASPECT_FIT_WIDTH, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with height scaling.
     */
    public void testFromUriHeightScaled() {
        Size s = Size.fromUri(",50");
        assertEquals(new Integer(50), s.getHeight());
        assertEquals(Size.ScaleMode.ASPECT_FIT_HEIGHT, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with percentage scaling.
     */
    public void testFromUriPercentageScaled() {
        Size s = Size.fromUri("pct:50");
        assertEquals(new Float(50), s.getPercent());
    }

    /**
     * Tests fromUri(String) with absolute width and height.
     */
    public void testFromUriAbsoluteScaled() {
        Size s = Size.fromUri("50,40");
        assertEquals(new Integer(50), s.getWidth());
        assertEquals(new Integer(40), s.getHeight());
        assertEquals(Size.ScaleMode.NON_ASPECT_FILL, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with scale-to-fit width and height.
     */
    public void testFromUriScaleToFit() {
        Size s = Size.fromUri("!50,40");
        assertEquals(new Integer(50), s.getWidth());
        assertEquals(new Integer(40), s.getHeight());
        assertEquals(Size.ScaleMode.ASPECT_FIT_INSIDE, s.getScaleMode());
    }

    /* setHeight() */

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

    /* setPercent() */

    public void testSetPercent() {
        float percent = 50f;
        this.size.setPercent(percent);
        assertEquals(percent, this.size.getPercent());
    }

    public void testSetNegativePercent() {
        try {
            this.size.setPercent(-1.0f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Percent must be positive", e.getMessage());
        }
    }

    public void testSetZeroPercent() {
        try {
            this.size.setPercent(0f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Percent must be positive", e.getMessage());
        }
    }

    /* setWidth() */

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

    /* toString */

    public void testToString() {
        Size s = Size.fromUri("full");
        assertEquals("full", s.toString());

        s = Size.fromUri("50,");
        assertEquals("50,", s.toString());

        s = Size.fromUri(",50");
        assertEquals(",50", s.toString());

        s = Size.fromUri("pct:50");
        assertEquals("pct:50", s.toString());

        s = Size.fromUri("50,40");
        assertEquals("50,40", s.toString());

        s = Size.fromUri("!50,40");
        assertEquals("!50,40", s.toString());
    }

}
