package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SizeTest extends BaseTest {

    private static final float DELTA = 0.0000001f;

    private Size instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.instance = new Size();
    }

    /* fromUri(String) */

    /**
     * Tests fromUri(String) with a value of "full".
     */
    @Test
    public void testFromUriFull() {
        Size s = Size.fromUri("full");
        assertEquals(Size.ScaleMode.FULL, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with width scaling.
     */
    @Test
    public void testFromUriWidthScaled() {
        Size s = Size.fromUri("50,");
        assertEquals(Integer.valueOf(50), s.getWidth());
        assertEquals(Size.ScaleMode.ASPECT_FIT_WIDTH, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with height scaling.
     */
    @Test
    public void testFromUriHeightScaled() {
        Size s = Size.fromUri(",50");
        assertEquals(Integer.valueOf(50), s.getHeight());
        assertEquals(Size.ScaleMode.ASPECT_FIT_HEIGHT, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with percentage scaling.
     */
    @Test
    public void testFromUriPercentageScaled() {
        Size s = Size.fromUri("pct:50");
        assertEquals(Float.valueOf(50), s.getPercent());
    }

    /**
     * Tests fromUri(String) with absolute width and height.
     */
    @Test
    public void testFromUriAbsoluteScaled() {
        Size s = Size.fromUri("50,40");
        assertEquals(Integer.valueOf(50), s.getWidth());
        assertEquals(Integer.valueOf(40), s.getHeight());
        assertEquals(Size.ScaleMode.NON_ASPECT_FILL, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with scale-to-fit width and height.
     */
    @Test
    public void testFromUriScaleToFit() {
        Size s = Size.fromUri("!50,40");
        assertEquals(Integer.valueOf(50), s.getWidth());
        assertEquals(Integer.valueOf(40), s.getHeight());
        assertEquals(Size.ScaleMode.ASPECT_FIT_INSIDE, s.getScaleMode());
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testFromUriWithInvalidArgument1() {
        Size.fromUri("cats");
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testFromUriWithInvalidArgument2() {
        Size.fromUri("pct:cats");
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testFromUriWithInvalidArgument3() {
        Size.fromUri("pct:50,30");
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testFromUriWithInvalidArgument4() {
        Size.fromUri("120,cats");
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testFromUriWithInvalidArgument5() {
        Size.fromUri("cats,120");
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testFromUriWithInvalidArgument6() {
        Size.fromUri("!cats,120");
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testFromUriWithInvalidArgument7() {
        Size.fromUri("!120,");
    }

    /* equals() */

    @Test
    public void testEquals() {
        instance.setScaleMode(Size.ScaleMode.ASPECT_FIT_INSIDE);
        instance.setWidth(300);
        instance.setHeight(200);
        Size size2 = new Size();
        size2.setScaleMode(Size.ScaleMode.ASPECT_FIT_INSIDE);
        size2.setWidth(300);
        size2.setHeight(200);
        assertEquals(instance, size2);

        size2.setScaleMode(Size.ScaleMode.ASPECT_FIT_WIDTH);
        assertNotEquals(instance, size2);

        size2.setScaleMode(Size.ScaleMode.ASPECT_FIT_INSIDE);
        size2.setWidth(299);
        assertNotEquals(instance, size2);

        size2.setWidth(300);
        size2.setHeight(199);
        assertNotEquals(instance, size2);

        size2.setHeight(200);
        size2.setScaleMode(null);
        assertNotEquals(instance, size2);

        size2.setScaleMode(Size.ScaleMode.ASPECT_FIT_INSIDE);
        size2.setWidth(null);
        assertNotEquals(instance, size2);

        size2.setWidth(300);
        size2.setHeight(null);
        assertNotEquals(instance, size2);
    }

    /* setHeight() */

    @Test
    public void testSetHeight() {
        Integer height = 50;
        this.instance.setHeight(height);
        assertEquals(height, this.instance.getHeight());
    }

    @Test
    public void testSetNegativeHeight() {
        try {
            this.instance.setHeight(-1);
            fail("Expected exception");
        } catch (IllegalClientArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void testSetZeroHeight() {
        try {
            this.instance.setHeight(0);
            fail("Expected exception");
        } catch (IllegalClientArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    /* setPercent() */

    @Test
    public void testSetPercent() {
        float percent = 50f;
        this.instance.setPercent(percent);
        assertEquals(percent, this.instance.getPercent(), DELTA);
    }

    @Test
    public void testSetNegativePercent() {
        try {
            this.instance.setPercent(-1.0f);
            fail("Expected exception");
        } catch (IllegalClientArgumentException e) {
            assertEquals("Percent must be positive", e.getMessage());
        }
    }

    @Test
    public void testSetZeroPercent() {
        try {
            this.instance.setPercent(0f);
            fail("Expected exception");
        } catch (IllegalClientArgumentException e) {
            assertEquals("Percent must be positive", e.getMessage());
        }
    }

    /* setWidth() */

    @Test
    public void testSetWidth() {
        Integer width = 50;
        this.instance.setWidth(width);
        assertEquals(width, this.instance.getWidth());
    }

    @Test
    public void testSetNegativeWidth() {
        try {
            this.instance.setWidth(-1);
            fail("Expected exception");
        } catch (IllegalClientArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void testSetZeroWidth() {
        try {
            this.instance.setWidth(0);
            fail("Expected exception");
        } catch (IllegalClientArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    /* toString() */

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
