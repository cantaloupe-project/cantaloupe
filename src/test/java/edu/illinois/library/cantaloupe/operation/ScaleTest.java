package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.processor.resample.ResampleFilters;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ScaleTest extends BaseTest {

    @Nested
    public class FilterTest extends BaseTest {

        @Test
        void testToResampleFilter() {
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

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        this.instance = new Scale();
    }

    @Test
    void noOpConstructor() {
        assertEquals(Scale.Mode.FULL, instance.getMode());
        assertNull(instance.getPercent());
        assertNull(instance.getHeight());
        assertNull(instance.getWidth());
    }

    @Test
    void percentageConstructor() {
        this.instance = new Scale(0.3);
        assertEquals(Scale.Mode.ASPECT_FIT_INSIDE, instance.getMode());
        assertEquals(0.3, instance.getPercent(), DELTA);
    }

    @Test
    void pixelConstructor() {
        this.instance = new Scale(300, 200, Scale.Mode.ASPECT_FIT_HEIGHT);
        assertEquals(Scale.Mode.ASPECT_FIT_HEIGHT, instance.getMode());
        assertEquals(300, (long) instance.getWidth());
        assertEquals(200, (long) instance.getHeight());
    }

    @Test
    void equalsWithEqualInstances() {
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
    void equalsWithUnequalInstances() {
        Scale scale1 = new Scale(0.4);
        Scale scale2 = new Scale(0.6);
        assertNotEquals(scale1, scale2);

        scale1 = new Scale(40, 42, Scale.Mode.ASPECT_FIT_HEIGHT);
        scale2 = new Scale(40, 40, Scale.Mode.ASPECT_FIT_HEIGHT);
        assertNotEquals(scale1, scale2);
    }

    @Test
    void getDifferentialScalesWithFull() {
        final Dimension fullSize = new Dimension(300, 200);
        final ReductionFactor rf = new ReductionFactor(2);
        final ScaleConstraint sc = new ScaleConstraint(1, 4);

        instance.setMode(Scale.Mode.FULL);

        assertArrayEquals(new double[] { 1, 1 },
                instance.getDifferentialScales(fullSize, rf, sc), DELTA);
    }

    @Test
    void getDifferentialScalesWithAspectFitWidth() {
        final Dimension fullSize = new Dimension(1000, 800);
        final ScaleConstraint sc = new ScaleConstraint(1, 4); // client sees 250x200
        final ReductionFactor rf = new ReductionFactor(2); // reader returns 250x200

        instance.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(200);
        assertArrayEquals(new double[] { 0.8, 0.8 },
                instance.getDifferentialScales(fullSize, rf, sc), DELTA);
    }

    @Test
    void getDifferentialScalesWithAspectFitHeight() {
        final Dimension fullSize = new Dimension(1000, 800);
        final ScaleConstraint sc = new ScaleConstraint(1, 4); // client sees 250x200
        final ReductionFactor rf = new ReductionFactor(2); // reader returns 250x200

        instance.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        instance.setHeight(150);
        assertArrayEquals(new double[] { 0.75, 0.75 },
                instance.getDifferentialScales(fullSize, rf, sc), DELTA);
    }

    @Test
    void getDifferentialScalesWithAspectFitInside() {
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
    void getDifferentialScalesWithNonAspectFill() {
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
    void getDifferentialScalesWithPercent() {
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
    void getReductionFactorWithFull() {
        Dimension size = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        assertEquals(0, instance.getReductionFactor(size, sc, 999).factor);
    }

    @Test
    void getReductionFactorWithAspectFitWidth() {
        Dimension size = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 1);

        instance.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(145);
        instance.setHeight(145);
        assertEquals(1, instance.getReductionFactor(size, sc, 999).factor);
    }

    @Test
    void getReductionFactorWithAspectFitHeight() {
        Dimension size = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 1);

        instance.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        instance.setWidth(145);
        instance.setHeight(145);
        assertEquals(1, instance.getReductionFactor(size, sc, 999).factor);
    }

    @Test
    void getReductionFactorWithAspectFitInside() {
        Dimension size = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 1);

        instance.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(145);
        instance.setHeight(145);
        assertEquals(1, instance.getReductionFactor(size, sc, 999).factor);
    }

    @Test
    void getReductionFactorWithNonAspectFill() {
        Dimension size = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 1);

        instance.setMode(Scale.Mode.NON_ASPECT_FILL);
        instance.setWidth(145);
        instance.setHeight(145);
        assertEquals(0, instance.getReductionFactor(size, sc, 999).factor);
    }

    @Test
    void getReductionFactorWithPercent() {
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
    void getResultingScalesWithFull() {
        final Dimension fullSize = new Dimension(300, 200);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3);
        instance.setMode(Scale.Mode.FULL);
        double scScale = scaleConstraint.getRational().doubleValue();
        assertArrayEquals(
                new double[] { scScale, scScale },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    void getResultingScalesWithPercent() {
        final Dimension fullSize = new Dimension(300, 200);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3);
        instance = new Scale();
        instance.setPercent(0.5);
        assertArrayEquals(new double[] { 0.5 * 1 / 3.0, 0.5 * 1 / 3.0 },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    void getResultingScalesWithAspectFitWidth() {
        final Dimension fullSize = new Dimension(900, 600);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3); // 300x200
        instance.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(200);
        instance.setHeight(100);
        assertArrayEquals(new double[] { 200 / 900.0, 200 / 900.0 },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    void getResultingScalesWithAspectFitHeight() {
        final Dimension fullSize = new Dimension(900, 600);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3);
        instance.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        instance.setWidth(200);
        instance.setHeight(100);
        assertArrayEquals(new double[] { 100 / 600.0, 100 / 600.0 },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    void getResultingScalesWithAspectFitInsideAndDownscaling() {
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
    void getResultingScalesWithAspectFitInsideAndUnlimitedMaxScale() {
        final Dimension fullSize = new Dimension(900, 600);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3);
        instance.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(1800);
        instance.setHeight(1200);

        double expected = Math.min(
                instance.getWidth() / fullSize.width(),
                instance.getHeight() / fullSize.height());
        assertArrayEquals(new double[] { expected, expected },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    void getResultingScalesWithAspectFitInsideAndMaxScale1() {
        final Dimension fullSize = new Dimension(900, 600);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3);
        instance.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(1800);
        instance.setHeight(1200);
        instance.setMaxScale(1.0);

        assertArrayEquals(new double[] { 1.0, 1.0 },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    void getResultingScalesWithNonAspectFill() {
        final Dimension fullSize = new Dimension(300, 200);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3);
        instance.setMode(Scale.Mode.NON_ASPECT_FILL);
        instance.setWidth(200);
        instance.setHeight(100);

        assertArrayEquals(new double[] { 200 / 300.0, 0.5 },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    void getResultingSize1WithFull() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(Scale.Mode.FULL);
        assertEquals(fullSize,
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void getResultingSize1WithAspectFitWidthAndDownscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(400);
        instance.setHeight(200);

        Dimension actual = instance.getResultingSize(fullSize, scaleConstraint);
        assertEquals(400, actual.intWidth());
        assertEquals(267, actual.intHeight());
    }

    @Test
    void getResultingSize1WithAspectFitWidthAndUpscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(1200);
        instance.setHeight(600);

        Dimension actual = instance.getResultingSize(fullSize, scaleConstraint);
        assertEquals(1200, actual.intWidth());
        assertEquals(800, actual.intHeight());
    }

    @Test
    void getResultingSize1WithAspectFitHeightAndDownscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        instance.setWidth(400);
        instance.setHeight(200);

        assertEquals(new Dimension(300, 200),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void getResultingSize1WithAspectFitHeightAndUpscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        instance.setWidth(1200);
        instance.setHeight(600);

        assertEquals(new Dimension(900, 600),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void getResultingSize1WithAspectFitInsideAndDownscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(400);
        instance.setHeight(200);

        assertEquals(new Dimension(300, 200),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void getResultingSize1WithAspectFitInsideAndUnlimitedMaxScale() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(1200);
        instance.setHeight(600);

        assertEquals(new Dimension(900, 600),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void getResultingSize1WithAspectFitInsideAndMaxScale1() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        instance.setMaxScale(1.0);
        instance.setWidth(1200);
        instance.setHeight(600);

        assertEquals(new Dimension(600, 400),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void getResultingSize1WithNonAspectFillAndDownscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(Scale.Mode.NON_ASPECT_FILL);
        instance.setWidth(400);
        instance.setHeight(200);

        assertEquals(new Dimension(400, 200),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void getResultingSize1WithNonAspectFillAndUpscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(Scale.Mode.NON_ASPECT_FILL);
        instance.setWidth(1200);
        instance.setHeight(600);

        assertEquals(new Dimension(1200, 600),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void getResultingSize1WithPercentAndDownscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance = new Scale();
        instance.setPercent(0.5);
        assertEquals(new Dimension(300, 200),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void getResultingSize1WithPercentAndUpscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance = new Scale();
        instance.setPercent(1.5);

        assertEquals(new Dimension(900, 600),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void getResultingSize2WithFull() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor();
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(Scale.Mode.FULL);
        assertEquals(new Dimension(300, 200),
                instance.getResultingSize(fullSize, rf, sc));
    }

    @Test
    void getResultingSize2WithAspectFitWidthAndDownscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(400);
        instance.setHeight(200);
        Dimension actual = instance.getResultingSize(fullSize, rf, sc);
        assertEquals(400, actual.intWidth());
        assertEquals(267, actual.intHeight());
    }

    @Test
    void getResultingSize2WithAspectFitWidthAndUpscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(1200);
        instance.setHeight(600);
        Dimension actual = instance.getResultingSize(fullSize, rf, sc);
        assertEquals(1200, actual.intWidth());
        assertEquals(800, actual.intHeight());
    }

    @Test
    void getResultingSize2WithAspectFitHeightAndDownscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        instance.setWidth(400);
        instance.setHeight(200);

        assertEquals(new Dimension(300, 200),
                instance.getResultingSize(fullSize, rf, sc));
    }

    @Test
    void getResultingSize2WithAspectFitHeightAndUpscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        instance.setWidth(1200);
        instance.setHeight(600);

        assertEquals(new Dimension(900, 600),
                instance.getResultingSize(fullSize, rf, sc));
    }

    @Test
    void getResultingSize2WithAspectFitInsideAndDownscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(400);
        instance.setHeight(200);

        assertEquals(new Dimension(300, 200),
                instance.getResultingSize(fullSize, rf, sc));
    }

    @Test
    void getResultingSize2WithAspectFitInsideAndUnlimitedMaxScale() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(1200);
        instance.setHeight(600);

        assertEquals(new Dimension(900, 600),
                instance.getResultingSize(fullSize, rf, sc));
    }

    @Test
    void getResultingSize2WithAspectFitInsideAndMaxScale1() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(1200);
        instance.setHeight(600);
        instance.setMaxScale(1.0);

        assertEquals(new Dimension(600, 400),
                instance.getResultingSize(fullSize, rf, sc));
    }

    @Test
    void getResultingSize2WithNonAspectFillAndDownscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(Scale.Mode.NON_ASPECT_FILL);
        instance.setWidth(400);
        instance.setHeight(200);

        assertEquals(new Dimension(400, 200),
                instance.getResultingSize(fullSize, rf, sc));
    }

    @Test
    void getResultingSize2WithNonAspectFillAndUpscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(Scale.Mode.NON_ASPECT_FILL);
        instance.setWidth(1200);
        instance.setHeight(600);

        assertEquals(new Dimension(1200, 600),
                instance.getResultingSize(fullSize, rf, sc));
    }

    @Test
    void getResultingSize2WithPercent() {
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
    void hasEffect() {
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
    void hasEffectWithArguments() {
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
    void hasEffectWithScaleConstraint() {
        final Dimension fullSize = new Dimension(600, 400);
        final OperationList opList = new OperationList();
        opList.setScaleConstraint(new ScaleConstraint(1, 2));

        assertTrue(instance.hasEffect(fullSize, opList));
    }

    @Test
    void isUpWithFull() {
        Dimension size = new Dimension(600, 400);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        instance = new Scale();
        instance.setMode(Scale.Mode.FULL);
        assertFalse(instance.isUp(size, scaleConstraint));
    }

    @Test
    void isUpWithAspectFitWidth() {
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
    void isUpWithAspectFitHeight() {
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
    void isUpWithAspectFitInside() {
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
    void isUpWithNonAspectFill() {
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
    void isUpWithPercent() {
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

    @Test
    void setFilterWhenFrozenThrowsException() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setFilter(Scale.Filter.LANCZOS3));
    }

    @Test
    void setHeight() {
        Integer height = 50;
        instance.setHeight(height);
        assertEquals(height, instance.getHeight());
    }

    @Test
    void setHeightWithNegativeHeight() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setHeight(-1));
    }

    @Test
    void setHeightWithZeroHeight() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setHeight(0));
    }

    @Test
    void setHeightWithNullHeight() {
        instance.setHeight(null);
        assertNull(instance.getHeight());
    }

    @Test
    void setHeightWhenFrozenThrowsException() {
        instance.freeze();
        assertThrows(IllegalStateException.class, () -> instance.setHeight(80));
    }

    @Test
    void setPercent() {
        double percent = 0.5;
        instance.setPercent(percent);
        assertEquals(percent, instance.getPercent(), 0.000001f);
        assertEquals(Scale.Mode.ASPECT_FIT_INSIDE, instance.getMode());
    }

    @Test
    void setPercentWithNegativePercent() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setPercent(-0.5));
    }

    @Test
    void setPercentWithZeroPercent() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setPercent(0.0));
    }

    @Test
    void setPercentWithNullPercent() {
        instance.setPercent(null);
        assertNull(instance.getPercent());
    }

    @Test
    void setPercentWhenFrozenThrowsException() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setPercent(0.5));
    }

    @Test
    void setModeWhenFrozenThrowsException() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setMode(Scale.Mode.ASPECT_FIT_HEIGHT));
    }

    @Test
    void setWidth() {
        Integer width = 50;
        instance.setWidth(width);
        assertEquals(width, instance.getWidth());
    }

    @Test
    void setWidthWithNegativeWidth() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setWidth(-1));
    }

    @Test
    void setWidthWithZeroWidth() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setWidth(0));
    }

    @Test
    void setWidthWithNullWidth() {
        instance.setWidth(null);
        assertNull(instance.getWidth());
    }

    @Test
    void setWidthWhenFrozenThrowsException() {
        instance.freeze();
        assertThrows(IllegalStateException.class, () -> instance.setWidth(50));
    }

    @Test
    void toMap() {
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

    @Test
    void toMapReturnsUnmodifiableMap() {
        Dimension fullSize = new Dimension(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map = instance.toMap(fullSize, scaleConstraint);
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("test", "test"));
    }

    @Test
    void testToString() {
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
