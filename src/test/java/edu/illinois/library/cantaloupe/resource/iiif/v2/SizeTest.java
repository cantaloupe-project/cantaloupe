package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.operation.ScaleByPercent;
import edu.illinois.library.cantaloupe.operation.ScaleByPixels;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SizeTest extends BaseTest {

    private static final float DELTA = 0.0000001f;

    private Size instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        this.instance = new Size();
    }

    /* fromUri(String) */

    /**
     * Tests fromUri(String) with a value of "max".
     */
    @Test
    void testFromUriMax() {
        Size s = Size.fromUri("max");
        assertEquals(Size.ScaleMode.MAX, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with a value of "full".
     */
    @Test
    void testFromUriFull() {
        Size s = Size.fromUri("full");
        assertEquals(Size.ScaleMode.FULL, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with width scaling.
     */
    @Test
    void testFromUriWidthScaled() {
        Size s = Size.fromUri("50,");
        assertEquals(Integer.valueOf(50), s.getWidth());
        assertEquals(Size.ScaleMode.ASPECT_FIT_WIDTH, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with height scaling.
     */
    @Test
    void testFromUriHeightScaled() {
        Size s = Size.fromUri(",50");
        assertEquals(Integer.valueOf(50), s.getHeight());
        assertEquals(Size.ScaleMode.ASPECT_FIT_HEIGHT, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with percentage scaling.
     */
    @Test
    void testFromUriPercentageScaled() {
        Size s = Size.fromUri("pct:50");
        assertEquals(Float.valueOf(50), s.getPercent());
    }

    /**
     * Tests fromUri(String) with absolute width and height.
     */
    @Test
    void testFromUriAbsoluteScaled() {
        Size s = Size.fromUri("50,40");
        assertEquals(Integer.valueOf(50), s.getWidth());
        assertEquals(Integer.valueOf(40), s.getHeight());
        assertEquals(Size.ScaleMode.NON_ASPECT_FILL, s.getScaleMode());
    }

    /**
     * Tests fromUri(String) with scale-to-fit width and height.
     */
    @Test
    void testFromUriScaleToFit() {
        Size s = Size.fromUri("!50,40");
        assertEquals(Integer.valueOf(50), s.getWidth());
        assertEquals(Integer.valueOf(40), s.getHeight());
        assertEquals(Size.ScaleMode.ASPECT_FIT_INSIDE, s.getScaleMode());
    }

    @Test
    void testFromUriWithInvalidArgument1() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromUri("cats"));
    }

    @Test
    void testFromUriWithInvalidArgument2() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromUri("pct:cats"));
    }

    @Test
    void testFromUriWithInvalidArgument3() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromUri("pct:50,30"));
    }

    @Test
    void testFromUriWithInvalidArgument4() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromUri("120,cats"));
    }

    @Test
    void testFromUriWithInvalidArgument5() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromUri("cats,120"));
    }

    @Test
    void testFromUriWithInvalidArgument6() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromUri("!cats,120"));
    }

    @Test
    void testFromUriWithInvalidArgument7() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromUri("!120,"));
    }

    /* equals() */

    @Test
    void testEquals() {
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
    void testSetHeight() {
        Integer height = 50;
        this.instance.setHeight(height);
        assertEquals(height, this.instance.getHeight());
    }

    @Test
    void testSetZeroHeight() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setHeight(0),
                "Height must be a positive integer");
    }

    @Test
    void testSetNegativeHeight() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setHeight(-1),
                "Height must be a positive integer");
    }

    /* setPercent() */

    @Test
    void testSetPercent() {
        float percent = 50f;
        instance.setPercent(percent);
        assertEquals(percent, this.instance.getPercent(), DELTA);
    }

    @Test
    void testSetZeroPercent() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setPercent(0f),
                "Percent must be positive");
    }

    @Test
    void testSetNegativePercent() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setPercent(-1f),
                "Percent must be positive");
    }

    /* setWidth() */

    @Test
    void testSetWidth() {
        Integer width = 50;
        this.instance.setWidth(width);
        assertEquals(width, this.instance.getWidth());
    }

    @Test
    void testSetZeroWidth() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setWidth(0),
                "Width must be positive");
    }

    @Test
    void testSetNegativeWidth() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setWidth(-1),
                "Width must be positive");
    }

    /* toScale() */

    @Test
    void testToScaleWithPercent() {
        instance.setPercent(50f);
        assertEquals(new ScaleByPercent(0.5), instance.toScale());
    }

    @Test
    void testToScaleWithFull() {
        instance.setScaleMode(Size.ScaleMode.FULL);
        assertEquals(new ScaleByPercent(), instance.toScale());
    }

    @Test
    void testToScaleWithMax() {
        instance.setScaleMode(Size.ScaleMode.MAX);
        assertEquals(new ScaleByPercent(), instance.toScale());
    }

    @Test
    void testToScaleWithAspectFitWidth() {
        instance.setScaleMode(Size.ScaleMode.ASPECT_FIT_WIDTH);
        instance.setWidth(300);
        assertEquals(
                new ScaleByPixels(300, null, ScaleByPixels.Mode.ASPECT_FIT_WIDTH),
                instance.toScale());
    }

    @Test
    void testToScaleWithAspectFitHeight() {
        instance.setScaleMode(Size.ScaleMode.ASPECT_FIT_HEIGHT);
        instance.setHeight(300);
        assertEquals(
                new ScaleByPixels(null, 300, ScaleByPixels.Mode.ASPECT_FIT_HEIGHT),
                instance.toScale());
    }

    @Test
    void testToScaleWithAspectFitInside() {
        instance.setScaleMode(Size.ScaleMode.ASPECT_FIT_INSIDE);
        instance.setWidth(300);
        instance.setHeight(200);
        assertEquals(
                new ScaleByPixels(300, 200, ScaleByPixels.Mode.ASPECT_FIT_INSIDE),
                instance.toScale());
    }

    @Test
    void testToScaleWithNonAspectFill() {
        instance.setScaleMode(Size.ScaleMode.NON_ASPECT_FILL);
        instance.setWidth(300);
        instance.setHeight(200);
        assertEquals(
                new ScaleByPixels(300, 200, ScaleByPixels.Mode.NON_ASPECT_FILL),
                instance.toScale());
    }

    /* toString */

    @Test
    void testToString() {
        Size s = Size.fromUri("full");
        assertEquals("full", s.toString());

        s = Size.fromUri("max");
        assertEquals("max", s.toString());

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

    @Test
    void testToCanonicalString() {
        final Dimension fullSize = new Dimension(1000, 800);

        Size s = Size.fromUri("full");
        assertEquals("full", s.toCanonicalString(fullSize));

        s = Size.fromUri("max");
        assertEquals("max", s.toCanonicalString(fullSize));

        s = Size.fromUri("50,");
        assertEquals("50,", s.toCanonicalString(fullSize));

        s = Size.fromUri(",50");
        assertEquals("63,", s.toCanonicalString(fullSize));

        s = Size.fromUri("pct:50");
        assertEquals("500,", s.toCanonicalString(fullSize));

        s = Size.fromUri("50,40");
        assertEquals("50,40", s.toCanonicalString(fullSize));

        s = Size.fromUri("!50,40");
        assertEquals("50,", s.toCanonicalString(fullSize));
    }

}
