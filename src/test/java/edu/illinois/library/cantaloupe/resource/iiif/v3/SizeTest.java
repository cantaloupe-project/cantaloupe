package edu.illinois.library.cantaloupe.resource.iiif.v3;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.operation.ScaleByPercent;
import edu.illinois.library.cantaloupe.operation.ScaleByPixels;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SizeTest extends BaseTest {

    private static final double DELTA = 0.0000001;

    private Size instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        this.instance = new Size();
    }

    /* fromURI(String) */

    @Test
    void testFromURIWithMax() {
        Size s = Size.fromURI("max");
        assertEquals(Size.Type.MAX, s.getType());
        assertFalse(s.isUpscalingAllowed());
    }

    @Test
    void testFromURIWithMaxAllowingUpscaling() {
        Size s = Size.fromURI("^max");
        assertEquals(Size.Type.MAX, s.getType());
        assertTrue(s.isUpscalingAllowed());
    }

    @Test
    void testFromURIWithScaleToWidth() {
        Size s = Size.fromURI("50,");
        assertEquals(Integer.valueOf(50), s.getWidth());
        assertEquals(Size.Type.ASPECT_FIT_WIDTH, s.getType());
        assertFalse(s.isUpscalingAllowed());
    }

    @Test
    void testFromURIWithScaleToWidthAllowingUpscaling() {
        Size s = Size.fromURI("^50,");
        assertEquals(Integer.valueOf(50), s.getWidth());
        assertEquals(Size.Type.ASPECT_FIT_WIDTH, s.getType());
        assertTrue(s.isUpscalingAllowed());
    }

    @Test
    void testFromURIWithScaleToHeight() {
        Size s = Size.fromURI(",50");
        assertEquals(Integer.valueOf(50), s.getHeight());
        assertEquals(Size.Type.ASPECT_FIT_HEIGHT, s.getType());
        assertFalse(s.isUpscalingAllowed());
    }

    @Test
    void testFromURIWithScaleToHeightAllowingUpscaling() {
        Size s = Size.fromURI("^,50");
        assertEquals(Integer.valueOf(50), s.getHeight());
        assertEquals(Size.Type.ASPECT_FIT_HEIGHT, s.getType());
        assertTrue(s.isUpscalingAllowed());
    }

    @Test
    void testFromURIWithScaleToPercentage() {
        Size s = Size.fromURI("pct:50.5");
        assertEquals(50.5f, s.getPercent());
        assertFalse(s.isUpscalingAllowed());
    }

    @Test
    void testFromURIWithScaleToPercentageAllowingUpscaling() {
        Size s = Size.fromURI("^pct:50.5");
        assertEquals(50.5f, s.getPercent());
        assertTrue(s.isUpscalingAllowed());
    }

    @Test
    void testFromURIWithAbsoluteScale() {
        Size s = Size.fromURI("50,40");
        assertEquals(Integer.valueOf(50), s.getWidth());
        assertEquals(Integer.valueOf(40), s.getHeight());
        assertEquals(Size.Type.NON_ASPECT_FILL, s.getType());
        assertFalse(s.isUpscalingAllowed());
    }

    @Test
    void testFromURIWithAbsoluteScaleAllowingUpscaling() {
        Size s = Size.fromURI("^50,40");
        assertEquals(Integer.valueOf(50), s.getWidth());
        assertEquals(Integer.valueOf(40), s.getHeight());
        assertEquals(Size.Type.NON_ASPECT_FILL, s.getType());
        assertTrue(s.isUpscalingAllowed());
    }

    @Test
    void testFromURIWithScaleToFit() {
        Size s = Size.fromURI("!50,40");
        assertEquals(Integer.valueOf(50), s.getWidth());
        assertEquals(Integer.valueOf(40), s.getHeight());
        assertEquals(Size.Type.ASPECT_FIT_INSIDE, s.getType());
        assertFalse(s.isUpscalingAllowed());
    }

    @Test
    void testFromURIWithScaleToFitAllowingUpscaling() {
        Size s = Size.fromURI("^!50,40");
        assertEquals(Integer.valueOf(50), s.getWidth());
        assertEquals(Integer.valueOf(40), s.getHeight());
        assertEquals(Size.Type.ASPECT_FIT_INSIDE, s.getType());
        assertTrue(s.isUpscalingAllowed());
    }

    @Test
    void testFromURIWithPercentEncodedArgument() {
        Size s = Size.fromURI("%5Emax");
        assertEquals(Size.Type.MAX, s.getType());
        assertTrue(s.isUpscalingAllowed());
    }

    @Test
    void testFromURIWithIllegalArgument1() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromURI("cats"));
    }

    @Test
    void testFromURIWithIllegalArgument2() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromURI("pct:cats"));
    }

    @Test
    void testFromURIWithIllegalArgument3() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromURI("pct:50,30"));
    }

    @Test
    void testFromURIWithIllegalArgument4() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromURI("120,cats"));
    }

    @Test
    void testFromURIWithIllegalArgument5() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromURI("cats,120"));
    }

    @Test
    void testFromURIWithIllegalArgument6() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromURI("!cats,120"));
    }

    @Test
    void testFromURIWithIllegalArgument7() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Size.fromURI("!120,"));
    }

    /* equals() */

    @Test
    void testEquals() {
        instance.setType(Size.Type.ASPECT_FIT_INSIDE);
        instance.setWidth(300);
        instance.setHeight(200);
        Size size2 = new Size();
        size2.setType(Size.Type.ASPECT_FIT_INSIDE);
        size2.setWidth(300);
        size2.setHeight(200);
        assertEquals(instance, size2);

        size2.setType(Size.Type.ASPECT_FIT_WIDTH);
        assertNotEquals(instance, size2);

        size2.setType(Size.Type.ASPECT_FIT_INSIDE);
        size2.setWidth(299);
        assertNotEquals(instance, size2);

        size2.setWidth(300);
        size2.setHeight(199);
        assertNotEquals(instance, size2);

        size2.setHeight(200);
        size2.setType(null);
        assertNotEquals(instance, size2);

        size2.setType(Size.Type.ASPECT_FIT_INSIDE);
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
        assertEquals(new ScaleByPercent(0.5), instance.toScale(1));
    }

    @Test
    void testToScaleWithMaxAndUpscalingAllowed() {
        instance.setType(Size.Type.MAX);
        instance.setUpscalingAllowed(true);

        ScaleByPercent actual = (ScaleByPercent) instance.toScale(1);
        assertEquals(1, actual.getPercent(), DELTA);

        actual = (ScaleByPercent) instance.toScale(2);
        assertEquals(2, actual.getPercent(), DELTA);
    }

    @Test
    void testToScaleWithMaxAndUpscalingNotAllowed() {
        instance.setType(Size.Type.MAX);
        instance.setUpscalingAllowed(false);

        ScaleByPercent actual = (ScaleByPercent) instance.toScale(1);
        assertEquals(1, actual.getPercent(), DELTA);

        actual = (ScaleByPercent) instance.toScale(2);
        assertEquals(1, actual.getPercent(), DELTA);
    }

    @Test
    void testToScaleWithAspectFitWidth() {
        instance.setType(Size.Type.ASPECT_FIT_WIDTH);
        instance.setWidth(300);
        assertEquals(
                new ScaleByPixels(300, null, ScaleByPixels.Mode.ASPECT_FIT_WIDTH),
                instance.toScale(1));
    }

    @Test
    void testToScaleWithAspectFitHeight() {
        instance.setType(Size.Type.ASPECT_FIT_HEIGHT);
        instance.setHeight(300);
        assertEquals(
                new ScaleByPixels(null, 300, ScaleByPixels.Mode.ASPECT_FIT_HEIGHT),
                instance.toScale(1));
    }

    @Test
    void testToScaleWithAspectFitInside() {
        instance.setType(Size.Type.ASPECT_FIT_INSIDE);
        instance.setWidth(300);
        instance.setHeight(200);
        assertEquals(
                new ScaleByPixels(300, 200, ScaleByPixels.Mode.ASPECT_FIT_INSIDE),
                instance.toScale(1));
    }

    @Test
    void testToScaleWithNonAspectFill() {
        instance.setType(Size.Type.NON_ASPECT_FILL);
        instance.setWidth(300);
        instance.setHeight(200);
        assertEquals(
                new ScaleByPixels(300, 200, ScaleByPixels.Mode.NON_ASPECT_FILL),
                instance.toScale(1));
    }

    /* toString */

    @Test
    void testToString() {
        Size s = Size.fromURI("max");
        assertEquals("max", s.toString());

        s = Size.fromURI("50,");
        assertEquals("50,", s.toString());

        s = Size.fromURI(",50");
        assertEquals(",50", s.toString());

        s = Size.fromURI("pct:50");
        assertEquals("pct:50", s.toString());

        s = Size.fromURI("50,40");
        assertEquals("50,40", s.toString());

        s = Size.fromURI("!50,40");
        assertEquals("!50,40", s.toString());
    }

    @Test
    void testToCanonicalString() {
        final Dimension fullSize = new Dimension(1000, 800);

        Size s = Size.fromURI("max");
        assertEquals("max", s.toCanonicalString(fullSize));

        s = Size.fromURI("^max");
        assertEquals("^max", s.toCanonicalString(fullSize));

        s = Size.fromURI("50,");
        assertEquals("50,40", s.toCanonicalString(fullSize));

        s = Size.fromURI(",50");
        assertEquals("63,50", s.toCanonicalString(fullSize));

        s = Size.fromURI("pct:50");
        assertEquals("500,400", s.toCanonicalString(fullSize));

        s = Size.fromURI("50,40");
        assertEquals("50,40", s.toCanonicalString(fullSize));

        s = Size.fromURI("^50,40");
        assertEquals("50,40", s.toCanonicalString(fullSize));

        s = Size.fromURI("!50,40");
        assertEquals("50,40", s.toCanonicalString(fullSize));

        s = Size.fromURI("^!50,40");
        assertEquals("50,40", s.toCanonicalString(fullSize));
    }

}
