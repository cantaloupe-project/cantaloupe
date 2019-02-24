package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.processor.resample.ResampleFilters;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class ScaleTest extends BaseTest {

    public static class FilterTest extends BaseTest {

        @Test
        public void testToResampleFilter() {
            assertSame(ResampleFilters.getBellFilter(),
                    Scale.Filter.BELL.toResampleFilter());
            assertSame(ResampleFilters.getBiCubicFilter(),
                    Scale.Filter.BICUBIC.toResampleFilter());
            assertSame(ResampleFilters.getBoxFilter(),
                    Scale.Filter.BOX.toResampleFilter());
            assertSame(ResampleFilters.getBSplineFilter(),
                    Scale.Filter.BSPLINE.toResampleFilter());
            assertSame(ResampleFilters.getHermiteFilter(),
                    Scale.Filter.HERMITE.toResampleFilter());
            assertSame(ResampleFilters.getLanczos3Filter(),
                    Scale.Filter.LANCZOS3.toResampleFilter());
            assertSame(ResampleFilters.getMitchellFilter(),
                    Scale.Filter.MITCHELL.toResampleFilter());
            assertSame(ResampleFilters.getTriangleFilter(),
                    Scale.Filter.TRIANGLE.toResampleFilter());
        }

    }

    private static final double DELTA = 0.00000001;

    private Scale instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.instance = new Scale();
    }

    @Test
    public void noOpConstructor() {
        assertEquals(Scale.Mode.FULL, instance.getMode());
        assertNull(instance.getPercent());
        assertNull(instance.getHeight());
        assertNull(instance.getWidth());
    }

    @Test
    public void percentageConstructor() {
        this.instance = new Scale(0.3);
        assertEquals(Scale.Mode.ASPECT_FIT_INSIDE, instance.getMode());
        assertEquals(0.3, instance.getPercent(), DELTA);
    }

    @Test
    public void pixelConstructor() {
        this.instance = new Scale(300, 200, Scale.Mode.ASPECT_FIT_HEIGHT);
        assertEquals(Scale.Mode.ASPECT_FIT_HEIGHT, instance.getMode());
        assertEquals(300, (long) instance.getWidth());
        assertEquals(200, (long) instance.getHeight());
    }

    @Test
    public void equalsWithEqualInstances() {
        Scale scale1 = new Scale();
        Scale scale2 = new Scale();
        assertEquals(scale1, scale2);

        scale1 = new Scale(0.4);
        scale2 = new Scale(0.4);
        assertEquals(scale1, scale2);

        scale1 = new Scale(40, 40, Scale.Mode.ASPECT_FIT_HEIGHT);
        scale2 = new Scale(40, 40, Scale.Mode.ASPECT_FIT_HEIGHT);
        assertEquals(scale1, scale2);
    }

    @Test
    public void equalsWithUnequalInstances() {
        Scale scale1 = new Scale(0.4);
        Scale scale2 = new Scale(0.6);
        assertNotEquals(scale1, scale2);

        scale1 = new Scale(40, 42, Scale.Mode.ASPECT_FIT_HEIGHT);
        scale2 = new Scale(40, 40, Scale.Mode.ASPECT_FIT_HEIGHT);
        assertNotEquals(scale1, scale2);
    }

    @Test
    public void getDifferentialScalesWithFull() {
        final Dimension fullSize = new Dimension(300, 200);
        final ReductionFactor rf = new ReductionFactor(2);
        final ScaleConstraint sc = new ScaleConstraint(1, 4);

        instance.setMode(Scale.Mode.FULL);

        assertArrayEquals(new double[] { 1, 1 },
                instance.getDifferentialScales(fullSize, rf, sc), DELTA);
    }

    @Test
    public void getDifferentialScalesWithAspectFitWidth() {
        final Dimension fullSize = new Dimension(1000, 800);
        final ScaleConstraint sc = new ScaleConstraint(1, 4); // client sees 250x200
        final ReductionFactor rf = new ReductionFactor(2); // reader returns 250x200

        instance.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(200);
        assertArrayEquals(new double[] { 0.8, 0.8 },
                instance.getDifferentialScales(fullSize, rf, sc), DELTA);
    }

    @Test
    public void getDifferentialScalesWithAspectFitHeight() {
        final Dimension fullSize = new Dimension(1000, 800);
        final ScaleConstraint sc = new ScaleConstraint(1, 4); // client sees 250x200
        final ReductionFactor rf = new ReductionFactor(2); // reader returns 250x200

        instance.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        instance.setHeight(150);
        assertArrayEquals(new double[] { 0.75, 0.75 },
                instance.getDifferentialScales(fullSize, rf, sc), DELTA);
    }

    @Test
    public void getDifferentialScalesWithAspectFitInside() {
        final Dimension fullSize = new Dimension(1000, 800);
        final ScaleConstraint sc = new ScaleConstraint(1, 4); // client sees 250x200
        final ReductionFactor rf = new ReductionFactor(2); // reader returns 250x200

        instance.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(200);
        instance.setHeight(150);
        assertArrayEquals(new double[] { 0.75, 0.75 },
                instance.getDifferentialScales(fullSize, rf, sc), DELTA);
    }

    @Test
    public void getDifferentialScalesWithNonAspectFill() {
        final Dimension fullSize = new Dimension(1000, 800);
        final ScaleConstraint sc = new ScaleConstraint(1, 4); // client sees 250x200
        final ReductionFactor rf = new ReductionFactor(2); // reader returns 250x200

        instance.setMode(Scale.Mode.NON_ASPECT_FILL);
        instance.setWidth(200);
        instance.setHeight(150);
        assertArrayEquals(new double[] { 0.8, 0.75 },
                instance.getDifferentialScales(fullSize, rf, sc), DELTA);
    }

    @Test
    public void getDifferentialScalesWithPercent() {
        final Dimension fullSize = new Dimension(300, 200);
        ReductionFactor rf = new ReductionFactor(1);
        ScaleConstraint sc =  new ScaleConstraint(1, 4);

        instance = new Scale(0.5);
        assertArrayEquals(new double[] { 0.25, 0.25 },
                instance.getDifferentialScales(fullSize, rf, sc), DELTA);

        instance.setPercent(0.25);
        assertArrayEquals(new double[] { 0.125, 0.125 },
                instance.getDifferentialScales(fullSize, rf, sc), DELTA);

        instance.setPercent(1.5);
        rf = new ReductionFactor();
        assertArrayEquals(new double[] { 0.375, 0.375 },
                instance.getDifferentialScales(fullSize, rf, sc), DELTA);
    }

    @Test
    public void getReductionFactorWithFull() {
        Dimension size = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        assertEquals(0, instance.getReductionFactor(size, sc, 999).factor);
    }

    @Test
    public void getReductionFactorWithAspectFitWidth() {
        Dimension size = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 1);

        instance.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(145);
        instance.setHeight(145);
        assertEquals(1, instance.getReductionFactor(size, sc, 999).factor);
    }

    @Test
    public void getReductionFactorWithAspectFitHeight() {
        Dimension size = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 1);

        instance.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        instance.setWidth(145);
        instance.setHeight(145);
        assertEquals(1, instance.getReductionFactor(size, sc, 999).factor);
    }

    @Test
    public void getReductionFactorWithAspectFitInside() {
        Dimension size = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 1);

        instance.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(145);
        instance.setHeight(145);
        assertEquals(1, instance.getReductionFactor(size, sc, 999).factor);
    }

    @Test
    public void getReductionFactorWithNonAspectFill() {
        Dimension size = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 1);

        instance.setMode(Scale.Mode.NON_ASPECT_FILL);
        instance.setWidth(145);
        instance.setHeight(145);
        assertEquals(0, instance.getReductionFactor(size, sc, 999).factor);
    }

    @Test
    public void getReductionFactorWithPercent() {
        Dimension size = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        instance = new Scale();
        instance.setPercent(0.45);
        assertEquals(1, instance.getReductionFactor(size, sc, 999).factor);
        instance.setPercent(0.2);
        assertEquals(2, instance.getReductionFactor(size, sc, 999).factor);
        assertEquals(1, instance.getReductionFactor(size, sc, 1).factor);
    }

    @Test
    public void getResultingScalesWithFull() {
        final Dimension fullSize = new Dimension(300, 200);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3);
        instance.setMode(Scale.Mode.FULL);
        double scScale = scaleConstraint.getRational().doubleValue();
        assertArrayEquals(
                new double[] { scScale, scScale },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    public void getResultingScalesWithPercent() {
        final Dimension fullSize = new Dimension(300, 200);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3);
        instance = new Scale();
        instance.setPercent(0.5);
        assertArrayEquals(new double[] { 0.5 * 1 / 3.0, 0.5 * 1 / 3.0 },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    public void getResultingScalesWithAspectFitWidth() {
        final Dimension fullSize = new Dimension(900, 600);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3); // 300x200
        instance.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(200);
        instance.setHeight(100);
        assertArrayEquals(new double[] { 200 / 900.0, 200 / 900.0 },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    public void getResultingScalesWithAspectFitHeight() {
        final Dimension fullSize = new Dimension(900, 600);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3);
        instance.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        instance.setWidth(200);
        instance.setHeight(100);
        assertArrayEquals(new double[] { 100 / 600.0, 100 / 600.0 },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    public void getResultingScalesWithAspectFitInside() {
        final Dimension fullSize = new Dimension(900, 600);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3);
        instance.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(200);
        instance.setHeight(100);

        double expected = Math.min(
                instance.getWidth() / fullSize.width(),
                instance.getHeight() / fullSize.height());
        assertArrayEquals(new double[] { expected, expected },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    public void getResultingScalesWithNonAspectFill() {
        final Dimension fullSize = new Dimension(300, 200);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3);
        instance.setMode(Scale.Mode.NON_ASPECT_FILL);
        instance.setWidth(200);
        instance.setHeight(100);

        assertArrayEquals(new double[] { 200 / 300.0, 0.5 },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    public void getResultingSize1WithFull() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(Scale.Mode.FULL);
        assertEquals(fullSize,
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    public void getResultingSize1WithAspectFitWidth() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        // down
        instance.setWidth(400);
        instance.setHeight(200);
        Dimension actual = instance.getResultingSize(fullSize, scaleConstraint);
        assertEquals(400, actual.intWidth());
        assertEquals(267, actual.intHeight());
        // up
        instance.setWidth(1200);
        instance.setHeight(600);
        actual = instance.getResultingSize(fullSize, scaleConstraint);
        assertEquals(1200, actual.intWidth());
        assertEquals(800, actual.intHeight());
    }

    @Test
    public void getResultingSize1WithAspectFitHeight() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        // down
        instance.setWidth(400);
        instance.setHeight(200);
        assertEquals(new Dimension(300, 200),
                instance.getResultingSize(fullSize, scaleConstraint));
        // up
        instance.setWidth(1200);
        instance.setHeight(600);
        assertEquals(new Dimension(900, 600),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    public void getResultingSize1WithAspectFitInside() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        // down
        instance.setWidth(400);
        instance.setHeight(200);
        assertEquals(new Dimension(300, 200),
                instance.getResultingSize(fullSize, scaleConstraint));
        // up
        instance.setWidth(1200);
        instance.setHeight(600);
        assertEquals(new Dimension(900, 600),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    public void getResultingSize1WithNonAspectFill() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(Scale.Mode.NON_ASPECT_FILL);
        // down
        instance.setWidth(400);
        instance.setHeight(200);
        assertEquals(new Dimension(400, 200),
                instance.getResultingSize(fullSize, scaleConstraint));
        // up
        instance.setWidth(1200);
        instance.setHeight(600);
        assertEquals(new Dimension(1200, 600),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    public void getResultingSize1WithPercent() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance = new Scale();
        // down
        instance.setPercent(0.5);
        assertEquals(new Dimension(300, 200),
                instance.getResultingSize(fullSize, scaleConstraint));
        // up
        instance.setPercent(1.5);
        assertEquals(new Dimension(900, 600),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    public void getResultingSize2WithFull() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor();
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(Scale.Mode.FULL);
        assertEquals(new Dimension(300, 200),
                instance.getResultingSize(fullSize, rf, sc));
    }

    @Test
    public void getResultingSize2WithAspectFitWidth() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        // down
        instance.setWidth(400);
        instance.setHeight(200);
        Dimension actual = instance.getResultingSize(fullSize, rf, sc);
        assertEquals(400, actual.intWidth());
        assertEquals(267, actual.intHeight());
        // up
        instance.setWidth(1200);
        instance.setHeight(600);
        actual = instance.getResultingSize(fullSize, rf, sc);
        assertEquals(1200, actual.intWidth());
        assertEquals(800, actual.intHeight());
    }

    @Test
    public void getResultingSize2WithAspectFitHeight() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        // down
        instance.setWidth(400);
        instance.setHeight(200);
        assertEquals(new Dimension(300, 200),
                instance.getResultingSize(fullSize, rf, sc));
        // up
        instance.setWidth(1200);
        instance.setHeight(600);
        assertEquals(new Dimension(900, 600),
                instance.getResultingSize(fullSize, rf, sc));
    }

    @Test
    public void getResultingSize2WithAspectFitInside() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        // down
        instance.setWidth(400);
        instance.setHeight(200);
        assertEquals(new Dimension(300, 200),
                instance.getResultingSize(fullSize, rf, sc));
        // up
        instance.setWidth(1200);
        instance.setHeight(600);
        assertEquals(new Dimension(900, 600),
                instance.getResultingSize(fullSize, rf, sc));
    }

    @Test
    public void getResultingSize2WithNonAspectFill() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(Scale.Mode.NON_ASPECT_FILL);
        // down
        instance.setWidth(400);
        instance.setHeight(200);
        assertEquals(new Dimension(400, 200),
                instance.getResultingSize(fullSize, rf, sc));
        // up
        instance.setWidth(1200);
        instance.setHeight(600);
        assertEquals(new Dimension(1200, 600),
                instance.getResultingSize(fullSize, rf, sc));
    }

    @Test
    public void getResultingSize2WithPercent() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance = new Scale();
        // down
        instance.setPercent(0.5);
        assertEquals(new Dimension(300, 200),
                instance.getResultingSize(fullSize, rf, sc));
        // up
        instance.setPercent(1.5);
        assertEquals(new Dimension(900, 600),
                instance.getResultingSize(fullSize, rf, sc));
    }

    @Test
    public void hasEffect() {
        instance.setMode(Scale.Mode.FULL);
        assertFalse(instance.hasEffect());
        instance = new Scale(1);
        assertFalse(instance.hasEffect());
        instance = new Scale(0.5);
        assertTrue(instance.hasEffect());
        instance = new Scale(100, 100, Scale.Mode.ASPECT_FIT_INSIDE);
        assertTrue(instance.hasEffect());
        instance.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        assertTrue(instance.hasEffect());
        instance.setMode(Scale.Mode.NON_ASPECT_FILL);
        assertTrue(instance.hasEffect());
    }

    @Test
    public void hasEffectWithArguments() {
        final Dimension fullSize = new Dimension(600, 400);
        final OperationList opList =
                new OperationList(new CropByPixels(0, 0, 300, 200));

        instance.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        instance.setPercent(0.5);
        assertTrue(instance.hasEffect(fullSize, opList));

        instance.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(300);
        assertFalse(instance.hasEffect(fullSize, opList));

        instance = new Scale();
        instance.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        instance.setHeight(200);
        assertFalse(instance.hasEffect(fullSize, opList));

        instance = new Scale();
        instance.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(300);
        instance.setHeight(200);
        assertFalse(instance.hasEffect(fullSize, opList));

        instance = new Scale();
        instance.setMode(Scale.Mode.NON_ASPECT_FILL);
        instance.setWidth(300);
        instance.setHeight(200);
        assertFalse(instance.hasEffect(fullSize, opList));
    }

    @Test
    public void hasEffectWithScaleConstraint() {
        final Dimension fullSize = new Dimension(600, 400);
        final OperationList opList = new OperationList();
        opList.setScaleConstraint(new ScaleConstraint(1, 2));

        assertTrue(instance.hasEffect(fullSize, opList));
    }

    @Test
    public void isUpWithFull() {
        Dimension size = new Dimension(600, 400);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        instance = new Scale();
        instance.setMode(Scale.Mode.FULL);
        assertFalse(instance.isUp(size, scaleConstraint));
    }

    @Test
    public void isUpWithAspectFitWidth() {
        Dimension size = new Dimension(600, 400);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        instance = new Scale();
        instance.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(300); // down
        assertFalse(instance.isUp(size, scaleConstraint));
        instance.setWidth(600); // even
        assertFalse(instance.isUp(size, scaleConstraint));
        instance.setWidth(800); // up
        assertTrue(instance.isUp(size, scaleConstraint));
    }

    @Test
    public void isUpWithAspectFitHeight() {
        Dimension size = new Dimension(600, 400);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        instance = new Scale();
        instance.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        instance.setHeight(200); // down
        assertFalse(instance.isUp(size, scaleConstraint));
        instance.setHeight(400); // even
        assertFalse(instance.isUp(size, scaleConstraint));
        instance.setHeight(600); // up
        assertTrue(instance.isUp(size, scaleConstraint));
    }

    @Test
    public void isUpWithAspectFitInside() {
        Dimension size = new Dimension(600, 400);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        instance = new Scale();
        instance.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(300); // down
        instance.setHeight(200);
        assertFalse(instance.isUp(size, scaleConstraint));
        instance.setWidth(600); // even
        instance.setHeight(400);
        assertFalse(instance.isUp(size, scaleConstraint));
        instance.setWidth(800); // up
        instance.setHeight(600);
        assertTrue(instance.isUp(size, scaleConstraint));
    }

    @Test
    public void isUpWithNonAspectFill() {
        Dimension size = new Dimension(600, 400);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        instance = new Scale();
        instance.setMode(Scale.Mode.NON_ASPECT_FILL);
        instance.setWidth(300); // down
        instance.setHeight(200);
        assertFalse(instance.isUp(size, scaleConstraint));
        instance.setWidth(600); // even
        instance.setHeight(400);
        assertFalse(instance.isUp(size, scaleConstraint));
        instance.setWidth(800); // up
        instance.setHeight(600);
        assertTrue(instance.isUp(size, scaleConstraint));
        instance.setWidth(500);
        instance.setHeight(800);
        assertTrue(instance.isUp(size, scaleConstraint));
        instance.setWidth(900);
        instance.setHeight(300);
        assertTrue(instance.isUp(size, scaleConstraint));
    }

    @Test
    public void isUpWithPercent() {
        Dimension size = new Dimension(600, 400);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        instance = new Scale();
        instance.setPercent(0.5); // down
        assertFalse(instance.isUp(size, scaleConstraint));
        instance.setPercent(1.0); // even
        assertFalse(instance.isUp(size, scaleConstraint));
        instance.setPercent(1.2); // up
        assertTrue(instance.isUp(size, scaleConstraint));
    }

    @Test(expected = IllegalStateException.class)
    public void setFilterWhenFrozenThrowsException() {
        instance.freeze();
        instance.setFilter(Scale.Filter.LANCZOS3);
    }

    @Test
    public void setHeight() {
        Integer height = 50;
        instance.setHeight(height);
        assertEquals(height, this.instance.getHeight());
    }

    @Test
    public void setHeightWithNegativeHeight() {
        try {
            instance.setHeight(-1);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void setHeightWithZeroHeight() {
        try {
            instance.setHeight(0);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void setHeightWithNullHeight() {
        instance.setHeight(null);
        assertNull(instance.getHeight());
    }

    @Test(expected = IllegalStateException.class)
    public void setHeightWhenFrozenThrowsException() {
        instance.freeze();
        instance.setHeight(80);
    }

    @Test
    public void setPercent() {
        double percent = 0.5;
        instance.setPercent(percent);
        assertEquals(percent, instance.getPercent(), 0.000001f);
        assertEquals(Scale.Mode.ASPECT_FIT_INSIDE, instance.getMode());
    }

    @Test
    public void setPercentWithNegativePercent() {
        try {
            instance.setPercent(-0.5);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Percent must be greater than zero", e.getMessage());
        }
    }

    @Test
    public void setPercentWithZeroPercent() {
        try {
            instance.setPercent(0.0);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Percent must be greater than zero", e.getMessage());
        }
    }

    @Test
    public void setPercentWithNullPercent() {
        instance.setPercent(null);
        assertNull(instance.getPercent());
    }

    @Test(expected = IllegalStateException.class)
    public void setPercentWhenFrozenThrowsException() {
        instance.freeze();
        instance.setPercent(0.5);
    }

    @Test(expected = IllegalStateException.class)
    public void setModeWhenFrozenThrowsException() {
        instance.freeze();
        instance.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
    }

    @Test
    public void setWidth() {
        Integer width = 50;
        instance.setWidth(width);
        assertEquals(width, this.instance.getWidth());
    }

    @Test
    public void setWidthWithNegativeWidth() {
        try {
            instance.setWidth(-1);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void setWidthWithZeroWidth() {
        try {
            instance.setWidth(0);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void setWidthWithNullWidth() {
        this.instance.setWidth(null);
        assertNull(instance.getWidth());
    }

    @Test(expected = IllegalStateException.class)
    public void setWidthWhenFrozenThrowsException() {
        instance.freeze();
        instance.setWidth(50);
    }

    @Test
    public void toMap() {
        instance.setWidth(50);
        instance.setHeight(45);
        instance.setMode(Scale.Mode.ASPECT_FIT_INSIDE);

        Dimension fullSize = new Dimension(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Dimension resultingSize =
                instance.getResultingSize(fullSize, scaleConstraint);

        Map<String,Object> map = instance.toMap(fullSize, scaleConstraint);
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
        assertEquals(resultingSize.intWidth(), map.get("width"));
        assertEquals(resultingSize.intHeight(), map.get("height"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void toMapReturnsUnmodifiableMap() {
        Dimension fullSize = new Dimension(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map = instance.toMap(fullSize, scaleConstraint);
        map.put("test", "test");
    }

    @Test
    public void testToString() {
        Scale scale = new Scale();
        assertEquals("none", scale.toString());

        scale = new Scale(50, null, Scale.Mode.ASPECT_FIT_WIDTH);
        assertEquals("50,", scale.toString());

        scale = new Scale(null, 50, Scale.Mode.ASPECT_FIT_HEIGHT);
        assertEquals(",50", scale.toString());

        scale = new Scale(0.5f);
        assertEquals("50%", scale.toString());

        scale = new Scale(50, 40, Scale.Mode.NON_ASPECT_FILL);
        assertEquals("50,40", scale.toString());

        scale = new Scale(50, 40, Scale.Mode.ASPECT_FIT_INSIDE);
        assertEquals("!50,40", scale.toString());

        scale = new Scale(50, 40, Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setFilter(Scale.Filter.LANCZOS3);
        assertEquals("!50,40,lanczos3", scale.toString());
    }

}
