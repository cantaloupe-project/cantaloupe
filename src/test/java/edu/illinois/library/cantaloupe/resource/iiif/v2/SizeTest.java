package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SizeTest extends BaseTest {

    private static final float FUDGE = 0.0000001f;

    private Size size;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.size = new Size();
    }

    /* fromUri(String) */

    /**
     * Tests fromUri(String) with a value of "max".
     */
    @Test
    public void testFromUriMax() {
        Size s = Size.fromUri("max");
        assertEquals(Size.ScaleMode.MAX, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with a value of "full".
     */
    @Test
    public void testFromUriFull() {
        Size s = Size.fromUri("full");
        assertEquals(Size.ScaleMode.MAX, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with width scaling.
     */
    @Test
    public void testFromUriWidthScaled() {
        Size s = Size.fromUri("50,");
        assertEquals(new Integer(50), s.getWidth());
        assertEquals(Size.ScaleMode.ASPECT_FIT_WIDTH, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with height scaling.
     */
    @Test
    public void testFromUriHeightScaled() {
        Size s = Size.fromUri(",50");
        assertEquals(new Integer(50), s.getHeight());
        assertEquals(Size.ScaleMode.ASPECT_FIT_HEIGHT, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with percentage scaling.
     */
    @Test
    public void testFromUriPercentageScaled() {
        Size s = Size.fromUri("pct:50");
        assertEquals(new Float(50), s.getPercent());
    }

    /**
     * Tests fromUri(String) with absolute width and height.
     */
    @Test
    public void testFromUriAbsoluteScaled() {
        Size s = Size.fromUri("50,40");
        assertEquals(new Integer(50), s.getWidth());
        assertEquals(new Integer(40), s.getHeight());
        assertEquals(Size.ScaleMode.NON_ASPECT_FILL, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with scale-to-fit width and height.
     */
    @Test
    public void testFromUriScaleToFit() {
        Size s = Size.fromUri("!50,40");
        assertEquals(new Integer(50), s.getWidth());
        assertEquals(new Integer(40), s.getHeight());
        assertEquals(Size.ScaleMode.ASPECT_FIT_INSIDE, s.getScaleMode());
    }

    @Test
    public void testFromUriWithInvalidArgument() {
        try {
            Size.fromUri("cats");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
        try {
            Size.fromUri("pct:cats");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
        try {
            Size.fromUri("pct:50,30");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
        try {
            Size.fromUri("120,cats");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
        try {
            Size.fromUri("cats,120");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
        try {
            Size.fromUri("!cats,120");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
        try {
            Size.fromUri("!120,");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    /* equals() */

    @Test
    public void testEquals() {
        size.setScaleMode(Size.ScaleMode.ASPECT_FIT_INSIDE);
        size.setWidth(300);
        size.setHeight(200);
        Size size2 = new Size();
        size2.setScaleMode(Size.ScaleMode.ASPECT_FIT_INSIDE);
        size2.setWidth(300);
        size2.setHeight(200);
        assertEquals(size, size2);

        size2.setScaleMode(Size.ScaleMode.ASPECT_FIT_WIDTH);
        assertNotEquals(size, size2);

        size2.setScaleMode(Size.ScaleMode.ASPECT_FIT_INSIDE);
        size2.setWidth(299);
        assertNotEquals(size, size2);

        size2.setWidth(300);
        size2.setHeight(199);
        assertNotEquals(size, size2);

        size2.setHeight(200);
        size2.setScaleMode(null);
        assertNotEquals(size, size2);

        size2.setScaleMode(Size.ScaleMode.ASPECT_FIT_INSIDE);
        size2.setWidth(null);
        assertNotEquals(size, size2);

        size2.setWidth(300);
        size2.setHeight(null);
        assertNotEquals(size, size2);
    }

    /* setHeight() */

    @Test
    public void testSetHeight() {
        Integer height = 50;
        this.size.setHeight(height);
        assertEquals(height, this.size.getHeight());
    }

    @Test
    public void testSetNegativeHeight() {
        try {
            this.size.setHeight(-1);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void testSetZeroHeight() {
        try {
            this.size.setHeight(0);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    /* setPercent() */

    @Test
    public void testSetPercent() {
        float percent = 50f;
        this.size.setPercent(percent);
        assertEquals(percent, this.size.getPercent(), FUDGE);
    }

    @Test
    public void testSetNegativePercent() {
        try {
            this.size.setPercent(-1.0f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Percent must be positive", e.getMessage());
        }
    }

    @Test
    public void testSetZeroPercent() {
        try {
            this.size.setPercent(0f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Percent must be positive", e.getMessage());
        }
    }

    /* setWidth() */

    @Test
    public void testSetWidth() {
        Integer width = 50;
        this.size.setWidth(width);
        assertEquals(width, this.size.getWidth());
    }

    @Test
    public void testSetNegativeWidth() {
        try {
            this.size.setWidth(-1);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void testSetZeroWidth() {
        try {
            this.size.setWidth(0);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    /* toString */

    @Test
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
