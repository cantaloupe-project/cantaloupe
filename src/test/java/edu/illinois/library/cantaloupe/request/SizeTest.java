package edu.illinois.library.cantaloupe.request;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.image.Scale;

public class SizeTest extends CantaloupeTestCase {

    private Scale size;

    public void setUp() {
        this.size = new Scale();
    }

    /* fromUri(String) */

    /**
     * Tests fromUri(String) with a value of "full".
     */
    public void testFromUriFull() {
        Scale s = Scale.fromUri("full");
        assertEquals(Scale.Mode.FULL, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with width scaling.
     */
    public void testFromUriWidthScaled() {
        Scale s = Scale.fromUri("50,");
        assertEquals(new Integer(50), s.getWidth());
        assertEquals(Scale.Mode.ASPECT_FIT_WIDTH, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with height scaling.
     */
    public void testFromUriHeightScaled() {
        Scale s = Scale.fromUri(",50");
        assertEquals(new Integer(50), s.getHeight());
        assertEquals(Scale.Mode.ASPECT_FIT_HEIGHT, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with percentage scaling.
     */
    public void testFromUriPercentageScaled() {
        Scale s = Scale.fromUri("pct:50");
        assertEquals(new Float(50), s.getPercent());
    }

    /**
     * Tests fromUri(String) with absolute width and height.
     */
    public void testFromUriAbsoluteScaled() {
        Scale s = Scale.fromUri("50,40");
        assertEquals(new Integer(50), s.getWidth());
        assertEquals(new Integer(40), s.getHeight());
        assertEquals(Scale.Mode.NON_ASPECT_FILL, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with scale-to-fit width and height.
     */
    public void testFromUriScaleToFit() {
        Scale s = Scale.fromUri("!50,40");
        assertEquals(new Integer(50), s.getWidth());
        assertEquals(new Integer(40), s.getHeight());
        assertEquals(Scale.Mode.ASPECT_FIT_INSIDE, s.getScaleMode());
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

    /* percent */

    public void testSetPercent() {
        Float percent = new Float(50);
        this.size.setPercent(percent);
        assertEquals(percent, this.size.getPercent());
    }

    public void testSetNegativePercent() {
        try {
            this.size.setPercent(new Float(-1.0));
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Percent must be positive", e.getMessage());
        }
    }

    public void testSetZeroPercent() {
        try {
            this.size.setPercent(new Float(0));
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Percent must be positive", e.getMessage());
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

    /* toString */

    public void testToString() {
        Scale s = Scale.fromUri("full");
        assertEquals("full", s.toString());

        s = Scale.fromUri("50,");
        assertEquals("50,", s.toString());

        s = Scale.fromUri(",50");
        assertEquals(",50", s.toString());

        s = Scale.fromUri("pct:50");
        assertEquals("pct:50", s.toString());

        s = Scale.fromUri("50,40");
        assertEquals("50,40", s.toString());

        s = Scale.fromUri("!50,40");
        assertEquals("!50,40", s.toString());
    }

}
