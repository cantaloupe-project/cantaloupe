package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ScaleByPixelsTest extends ScaleTest {

    private ScaleByPixels instance;

    @Override
    ScaleByPixels newInstance() {
        return new ScaleByPixels(100, 50, ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = newInstance();
    }

    @Test
    void testNoOpConstructor() {
        instance = new ScaleByPixels();
        assertNull(instance.getWidth());
        assertNull(instance.getHeight());
        assertEquals(ScaleByPixels.Mode.FULL, instance.getMode());
    }

    @Test
    void testConstructor2() {
        instance = new ScaleByPixels(100, 50);
        assertEquals(100, instance.getWidth());
        assertEquals(50, instance.getHeight());
        assertNull(instance.getMode());
    }

    @Test
    void testConstructor3() {
        assertEquals(100, instance.getWidth());
        assertEquals(50, instance.getHeight());
        assertEquals(ScaleByPixels.Mode.ASPECT_FIT_INSIDE, instance.getMode());
    }

    @Test
    void testGetDifferentialScalesWithFull() {
        final Dimension fullSize = new Dimension(300, 200);
        final ReductionFactor rf = new ReductionFactor(2);
        final ScaleConstraint sc = new ScaleConstraint(1, 4);

        instance.setMode(ScaleByPixels.Mode.FULL);

        assertArrayEquals(new double[] { 1, 1 },
                instance.getDifferentialScales(fullSize, rf, sc), DELTA);
    }

    @Test
    void testGetDifferentialScalesWithAspectFitWidth() {
        final Dimension fullSize = new Dimension(1000, 800);
        final ScaleConstraint sc = new ScaleConstraint(1, 4); // client sees 250x200
        final ReductionFactor rf = new ReductionFactor(2); // reader returns 250x200

        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(200);
        assertArrayEquals(new double[] { 0.8, 0.8 },
                instance.getDifferentialScales(fullSize, rf, sc), DELTA);
    }

    @Test
    void testGetDifferentialScalesWithAspectFitHeight() {
        final Dimension fullSize = new Dimension(1000, 800);
        final ScaleConstraint sc = new ScaleConstraint(1, 4); // client sees 250x200
        final ReductionFactor rf = new ReductionFactor(2); // reader returns 250x200

        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_HEIGHT);
        instance.setHeight(150);
        assertArrayEquals(new double[] { 0.75, 0.75 },
                instance.getDifferentialScales(fullSize, rf, sc), DELTA);
    }

    @Test
    void testGetDifferentialScalesWithAspectFitInside() {
        final Dimension fullSize = new Dimension(1000, 800);
        final ScaleConstraint sc = new ScaleConstraint(1, 4); // client sees 250x200
        final ReductionFactor rf = new ReductionFactor(2); // reader returns 250x200

        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(200);
        instance.setHeight(150);
        assertArrayEquals(new double[] { 0.75, 0.75 },
                instance.getDifferentialScales(fullSize, rf, sc), DELTA);
    }

    @Test
    void testGetDifferentialScalesWithNonAspectFill() {
        final Dimension fullSize = new Dimension(1000, 800);
        final ScaleConstraint sc = new ScaleConstraint(1, 4); // client sees 250x200
        final ReductionFactor rf = new ReductionFactor(2); // reader returns 250x200

        instance.setMode(ScaleByPixels.Mode.NON_ASPECT_FILL);
        instance.setWidth(200);
        instance.setHeight(150);
        assertArrayEquals(new double[] { 0.8, 0.75 },
                instance.getDifferentialScales(fullSize, rf, sc), DELTA);
    }

    @Test
    void testGetReductionFactorWithFull() {
        Dimension size = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        instance.setMode(ScaleByPixels.Mode.FULL);
        assertEquals(0, instance.getReductionFactor(size, sc, 999).factor);
    }

    @Test
    void testGetReductionFactorWithAspectFitWidth() {
        Dimension size = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 1);

        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(145);
        instance.setHeight(145);
        assertEquals(1, instance.getReductionFactor(size, sc, 999).factor);
    }

    @Test
    void testGetReductionFactorWithAspectFitHeight() {
        Dimension size = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 1);

        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_HEIGHT);
        instance.setWidth(145);
        instance.setHeight(145);
        assertEquals(1, instance.getReductionFactor(size, sc, 999).factor);
    }

    @Test
    void testGetReductionFactorWithAspectFitInside() {
        Dimension size = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 1);

        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(145);
        instance.setHeight(145);
        assertEquals(1, instance.getReductionFactor(size, sc, 999).factor);
    }

    @Test
    void testGetReductionFactorWithNonAspectFill() {
        Dimension size = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 1);

        instance.setMode(ScaleByPixels.Mode.NON_ASPECT_FILL);
        instance.setWidth(145);
        instance.setHeight(145);
        assertEquals(0, instance.getReductionFactor(size, sc, 999).factor);
    }

    @Test
    void testGetResultingScalesWithFull() {
        final Dimension fullSize = new Dimension(300, 200);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3);
        instance.setMode(ScaleByPixels.Mode.FULL);
        double scScale = scaleConstraint.getRational().doubleValue();
        assertArrayEquals(
                new double[] { scScale, scScale },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    void testGetResultingScalesWithAspectFitWidth() {
        final Dimension fullSize = new Dimension(900, 600);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3); // 300x200
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(200);
        instance.setHeight(100);
        assertArrayEquals(new double[] { 200 / 900.0, 200 / 900.0 },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    void testGetResultingScalesWithAspectFitHeight() {
        final Dimension fullSize = new Dimension(900, 600);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3);
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_HEIGHT);
        instance.setWidth(200);
        instance.setHeight(100);
        assertArrayEquals(new double[] { 100 / 600.0, 100 / 600.0 },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    void testGetResultingScalesWithAspectFitInsideAndDownscaling() {
        final Dimension fullSize = new Dimension(900, 600);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3);
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(200);
        instance.setHeight(100);

        double expected = Math.min(
                instance.getWidth() / fullSize.width(),
                instance.getHeight() / fullSize.height());
        assertArrayEquals(new double[] { expected, expected },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    void testGetResultingScalesWithAspectFitInsideAndUnlimitedMaxScale() {
        final Dimension fullSize = new Dimension(900, 600);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3);
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(1800);
        instance.setHeight(1200);

        double expected = Math.min(
                instance.getWidth() / fullSize.width(),
                instance.getHeight() / fullSize.height());
        assertArrayEquals(new double[] { expected, expected },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    void testGetResultingScalesWithAspectFitInsideAndMaxScale1() {
        final Dimension fullSize = new Dimension(900, 600);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3);
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(1800);
        instance.setHeight(1200);
        instance.setMaxScale(1.0);

        assertArrayEquals(new double[] { 1.0, 1.0 },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    void testGetResultingScalesWithNonAspectFill() {
        final Dimension fullSize = new Dimension(300, 200);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3);
        instance.setMode(ScaleByPixels.Mode.NON_ASPECT_FILL);
        instance.setWidth(200);
        instance.setHeight(100);

        assertArrayEquals(new double[] { 200 / 300.0, 0.5 },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    void testGetResultingSize1WithFull() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(ScaleByPixels.Mode.FULL);
        assertEquals(fullSize,
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void testGetResultingSize1WithAspectFitWidthAndDownscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(400);
        instance.setHeight(200);

        Dimension actual = instance.getResultingSize(fullSize, scaleConstraint);
        assertEquals(400, actual.intWidth());
        assertEquals(267, actual.intHeight());
    }

    @Test
    void testGetResultingSize1WithAspectFitWidthAndUpscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(1200);
        instance.setHeight(600);

        Dimension actual = instance.getResultingSize(fullSize, scaleConstraint);
        assertEquals(1200, actual.intWidth());
        assertEquals(800, actual.intHeight());
    }

    @Test
    void testGetResultingSize1WithAspectFitHeightAndDownscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_HEIGHT);
        instance.setWidth(400);
        instance.setHeight(200);

        assertEquals(new Dimension(300, 200),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void testGetResultingSize1WithAspectFitHeightAndUpscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_HEIGHT);
        instance.setWidth(1200);
        instance.setHeight(600);

        assertEquals(new Dimension(900, 600),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void testGetResultingSize1WithAspectFitInsideAndDownscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(400);
        instance.setHeight(200);

        assertEquals(new Dimension(300, 200),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void testGetResultingSize1WithAspectFitInsideAndUnlimitedMaxScale() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(1200);
        instance.setHeight(600);

        assertEquals(new Dimension(900, 600),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void testGetResultingSize1WithAspectFitInsideAndMaxScale1() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        instance.setMaxScale(1.0);
        instance.setWidth(1200);
        instance.setHeight(600);

        assertEquals(new Dimension(600, 400),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void testGetResultingSize1WithNonAspectFillAndDownscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(ScaleByPixels.Mode.NON_ASPECT_FILL);
        instance.setWidth(400);
        instance.setHeight(200);

        assertEquals(new Dimension(400, 200),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void testGetResultingSize1WithNonAspectFillAndUpscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(ScaleByPixels.Mode.NON_ASPECT_FILL);
        instance.setWidth(1200);
        instance.setHeight(600);

        assertEquals(new Dimension(1200, 600),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void testGetResultingSize2WithFull() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor();
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(ScaleByPixels.Mode.FULL);
        assertEquals(new Dimension(300, 200),
                instance.getResultingSize(fullSize, rf, sc));
    }

    @Test
    void testGetResultingSize2WithAspectFitWidthAndDownscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(400);
        instance.setHeight(200);
        Dimension actual = instance.getResultingSize(fullSize, rf, sc);
        assertEquals(400, actual.intWidth());
        assertEquals(267, actual.intHeight());
    }

    @Test
    void testGetResultingSize2WithAspectFitWidthAndUpscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(1200);
        instance.setHeight(600);
        Dimension actual = instance.getResultingSize(fullSize, rf, sc);
        assertEquals(1200, actual.intWidth());
        assertEquals(800, actual.intHeight());
    }

    @Test
    void testGetResultingSize2WithAspectFitHeightAndDownscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_HEIGHT);
        instance.setWidth(400);
        instance.setHeight(200);

        assertEquals(new Dimension(300, 200),
                instance.getResultingSize(fullSize, rf, sc));
    }

    @Test
    void testGetResultingSize2WithAspectFitHeightAndUpscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_HEIGHT);
        instance.setWidth(1200);
        instance.setHeight(600);

        assertEquals(new Dimension(900, 600),
                instance.getResultingSize(fullSize, rf, sc));
    }

    @Test
    void testGetResultingSize2WithAspectFitInsideAndDownscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(400);
        instance.setHeight(200);

        assertEquals(new Dimension(300, 200),
                instance.getResultingSize(fullSize, rf, sc));
    }

    @Test
    void testGetResultingSize2WithAspectFitInsideAndUnlimitedMaxScale() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(1200);
        instance.setHeight(600);

        assertEquals(new Dimension(900, 600),
                instance.getResultingSize(fullSize, rf, sc));
    }

    @Test
    void testGetResultingSize2WithAspectFitInsideAndMaxScale1() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(1200);
        instance.setHeight(600);
        instance.setMaxScale(1.0);

        assertEquals(new Dimension(600, 400),
                instance.getResultingSize(fullSize, rf, sc));
    }

    @Test
    void testGetResultingSize2WithNonAspectFillAndDownscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(ScaleByPixels.Mode.NON_ASPECT_FILL);
        instance.setWidth(400);
        instance.setHeight(200);

        assertEquals(new Dimension(400, 200),
                instance.getResultingSize(fullSize, rf, sc));
    }

    @Test
    void testGetResultingSize2WithNonAspectFillAndUpscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint sc = new ScaleConstraint(1, 2);
        instance.setMode(ScaleByPixels.Mode.NON_ASPECT_FILL);
        instance.setWidth(1200);
        instance.setHeight(600);

        assertEquals(new Dimension(1200, 600),
                instance.getResultingSize(fullSize, rf, sc));
    }

    @Test
    void testHasEffect() {
        instance.setMode(ScaleByPixels.Mode.FULL);
        assertFalse(instance.hasEffect());
        instance = new ScaleByPixels(100, 100, ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        assertTrue(instance.hasEffect());
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_WIDTH);
        assertTrue(instance.hasEffect());
        instance.setMode(ScaleByPixels.Mode.NON_ASPECT_FILL);
        assertTrue(instance.hasEffect());
    }

    @Test
    void testHasEffectWithArguments() {
        final Dimension fullSize = new Dimension(600, 400);
        final OperationList opList =
                new OperationList(new CropByPixels(0, 0, 300, 200));

        instance = new ScaleByPixels(
                300, null, ScaleByPixels.Mode.ASPECT_FIT_WIDTH);
        assertFalse(instance.hasEffect(fullSize, opList));

        instance = new ScaleByPixels(
                null, 200, ScaleByPixels.Mode.ASPECT_FIT_HEIGHT);
        assertFalse(instance.hasEffect(fullSize, opList));

        instance = new ScaleByPixels(
                300, 200, ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        assertFalse(instance.hasEffect(fullSize, opList));

        instance = new ScaleByPixels(
                300, 200, ScaleByPixels.Mode.NON_ASPECT_FILL);
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
    void testIsUpWithFull() {
        Dimension size = new Dimension(600, 400);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        instance = new ScaleByPixels(50, 20, ScaleByPixels.Mode.FULL);
        assertFalse(instance.isUp(size, scaleConstraint));
    }

    @Test
    void testIsUpWithAspectFitWidth() {
        Dimension size = new Dimension(600, 400);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        instance = new ScaleByPixels(
                null, 300, ScaleByPixels.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(300); // down
        assertFalse(instance.isUp(size, scaleConstraint));
        instance.setWidth(600); // even
        assertFalse(instance.isUp(size, scaleConstraint));
        instance.setWidth(800); // up
        assertTrue(instance.isUp(size, scaleConstraint));
    }

    @Test
    void testIsUpWithAspectFitHeight() {
        Dimension size = new Dimension(600, 400);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        instance = new ScaleByPixels(
                null, 200, ScaleByPixels.Mode.ASPECT_FIT_HEIGHT);
        instance.setHeight(200); // down
        assertFalse(instance.isUp(size, scaleConstraint));
        instance.setHeight(400); // even
        assertFalse(instance.isUp(size, scaleConstraint));
        instance.setHeight(600); // up
        assertTrue(instance.isUp(size, scaleConstraint));
    }

    @Test
    void testIsUpWithAspectFitInside() {
        Dimension size = new Dimension(600, 400);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        instance = new ScaleByPixels(
                null, null, ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
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
    void testIsUpWithNonAspectFill() {
        Dimension size = new Dimension(600, 400);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        instance = new ScaleByPixels(
                null, null, ScaleByPixels.Mode.NON_ASPECT_FILL);
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
    void testSetHeight() {
        Integer height = 50;
        instance.setHeight(height);
        assertEquals(height, instance.getHeight());
    }

    @Test
    void testSetHeightWithNegativeHeight() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setHeight(-1));
    }

    @Test
    void testSetHeightWithZeroHeight() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setHeight(0));
    }

    @Test
    void testSetHeightWithNullHeight() {
        instance.setHeight(null);
        assertNull(instance.getHeight());
    }

    @Test
    void testSetHeightWhenFrozenThrowsException() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setHeight(80));
    }

    @Test
    void testSetModeWhenFrozenThrowsException() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_HEIGHT));
    }

    @Test
    void testSetWidth() {
        Integer width = 50;
        instance.setWidth(width);
        assertEquals(width, instance.getWidth());
    }

    @Test
    void testSetWidthWithNegativeWidth() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setWidth(-1));
    }

    @Test
    void testSetWidthWithZeroWidth() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setWidth(0));
    }

    @Test
    void testSetWidthWithNullWidth() {
        instance.setWidth(null);
        assertNull(instance.getWidth());
    }

    @Test
    void testSetWidthWhenFrozenThrowsException() {
        instance.freeze();
        assertThrows(IllegalStateException.class, () -> instance.setWidth(50));
    }

    @Test
    void testToMap() {
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
    void testToString() {
        Scale scale = new ScaleByPixels(
                null, null, ScaleByPixels.Mode.FULL);
        assertEquals("none", scale.toString());

        scale = new ScaleByPixels(
                50, null, ScaleByPixels.Mode.ASPECT_FIT_WIDTH);
        assertEquals("50,", scale.toString());

        scale = new ScaleByPixels(
                null, 50, ScaleByPixels.Mode.ASPECT_FIT_HEIGHT);
        assertEquals(",50", scale.toString());

        scale = new ScaleByPixels(
                50, 40, ScaleByPixels.Mode.NON_ASPECT_FILL);
        assertEquals("50,40", scale.toString());

        scale = new ScaleByPixels(
                50, 40, ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        assertEquals("!50,40", scale.toString());

        scale = new ScaleByPixels(
                50, 40, ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        scale.setFilter(Scale.Filter.LANCZOS3);
        assertEquals("!50,40,lanczos3", scale.toString());
    }

}
