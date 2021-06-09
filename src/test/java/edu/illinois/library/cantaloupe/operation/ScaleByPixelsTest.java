package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.MetaIdentifier;
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
    void noOpConstructor() {
        instance = new ScaleByPixels();
        assertNull(instance.getWidth());
        assertNull(instance.getHeight());
        assertNull(instance.getMode());
    }

    @Test
    void constructor2() {
        assertEquals(100, instance.getWidth());
        assertEquals(50, instance.getHeight());
        assertEquals(ScaleByPixels.Mode.ASPECT_FIT_INSIDE, instance.getMode());
    }

    @Test
    void constructor3() {
        assertEquals(100, instance.getWidth());
        assertEquals(50, instance.getHeight());
        assertEquals(ScaleByPixels.Mode.ASPECT_FIT_INSIDE, instance.getMode());
    }

    /* getDifferentialScales() */

    @Test
    void getDifferentialScalesWithAspectFitWidth() {
        final Dimension fullSize = new Dimension(1000, 800);
        final ScaleConstraint sc = new ScaleConstraint(1, 4); // client sees 250x200
        final ReductionFactor rf = new ReductionFactor(2); // reader returns 250x200

        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(200);
        assertArrayEquals(new double[] { 0.8, 0.8 },
                instance.getDifferentialScales(fullSize, rf, sc), DELTA);
    }

    @Test
    void getDifferentialScalesWithAspectFitHeight() {
        final Dimension fullSize = new Dimension(1000, 800);
        final ScaleConstraint sc = new ScaleConstraint(1, 4); // client sees 250x200
        final ReductionFactor rf = new ReductionFactor(2); // reader returns 250x200

        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_HEIGHT);
        instance.setHeight(150);
        assertArrayEquals(new double[] { 0.75, 0.75 },
                instance.getDifferentialScales(fullSize, rf, sc), DELTA);
    }

    @Test
    void getDifferentialScalesWithAspectFitInside() {
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
    void getDifferentialScalesWithNonAspectFill() {
        final Dimension fullSize = new Dimension(1000, 800);
        final ScaleConstraint sc = new ScaleConstraint(1, 4); // client sees 250x200
        final ReductionFactor rf = new ReductionFactor(2); // reader returns 250x200

        instance.setMode(ScaleByPixels.Mode.NON_ASPECT_FILL);
        instance.setWidth(200);
        instance.setHeight(150);
        assertArrayEquals(new double[] { 0.8, 0.75 },
                instance.getDifferentialScales(fullSize, rf, sc), DELTA);
    }

    /* getReductionFactor() */

    @Test
    void getReductionFactorWithAspectFitWidth() {
        Dimension size = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 1);

        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(145);
        instance.setHeight(145);
        assertEquals(1, instance.getReductionFactor(size, sc, 999).factor);
    }

    @Test
    void getReductionFactorWithAspectFitWidthAndScaleConstraint() {
        Dimension size = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 2);

        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(145);
        instance.setHeight(145);
        assertEquals(1, instance.getReductionFactor(size, sc, 999).factor);
    }

    @Test
    void getReductionFactorWithAspectFitHeight() {
        Dimension size = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 1);

        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_HEIGHT);
        instance.setWidth(145);
        instance.setHeight(145);
        assertEquals(1, instance.getReductionFactor(size, sc, 999).factor);
    }

    @Test
    void getReductionFactorWithAspectFitHeightAndScaleConstraint() {
        Dimension size = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 2);

        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_HEIGHT);
        instance.setWidth(145);
        instance.setHeight(145);
        assertEquals(1, instance.getReductionFactor(size, sc, 999).factor);
    }

    @Test
    void getReductionFactorWithAspectFitInside() {
        Dimension size = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 1);

        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(145);
        instance.setHeight(145);
        assertEquals(1, instance.getReductionFactor(size, sc, 999).factor);
    }

    @Test
    void getReductionFactorWithAspectFitInsideAndScaleConstraint() {
        Dimension size = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 2);

        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(145);
        instance.setHeight(145);
        assertEquals(1, instance.getReductionFactor(size, sc, 999).factor);
    }

    @Test
    void getReductionFactorWithNonAspectFill() {
        Dimension size = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 1);

        instance.setMode(ScaleByPixels.Mode.NON_ASPECT_FILL);
        instance.setWidth(145);
        instance.setHeight(145);
        assertEquals(0, instance.getReductionFactor(size, sc, 999).factor);
    }

    @Test
    void getReductionFactorWithNonAspectFillAndScaleConstraint() {
        Dimension size = new Dimension(300, 300);
        ScaleConstraint sc = new ScaleConstraint(1, 2);

        instance.setMode(ScaleByPixels.Mode.NON_ASPECT_FILL);
        instance.setWidth(145);
        instance.setHeight(145);
        assertEquals(0, instance.getReductionFactor(size, sc, 999).factor);
    }

    /* getResultingScales() */

    @Test
    void getResultingScalesWithAspectFitWidth() {
        final Dimension fullSize = new Dimension(900, 600);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3); // 300x200
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(200);
        instance.setHeight(100);
        assertArrayEquals(new double[] { 200 / 900.0, 200 / 900.0 },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    void getResultingScalesWithAspectFitHeight() {
        final Dimension fullSize = new Dimension(900, 600);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3);
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_HEIGHT);
        instance.setWidth(200);
        instance.setHeight(100);
        assertArrayEquals(new double[] { 100 / 600.0, 100 / 600.0 },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    void getResultingScalesWithAspectFitInsideAndDownscaling() {
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
    void getResultingScalesWithAspectFitInside() {
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
    void getResultingScalesWithNonAspectFill() {
        final Dimension fullSize = new Dimension(300, 200);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 3);
        instance.setMode(ScaleByPixels.Mode.NON_ASPECT_FILL);
        instance.setWidth(200);
        instance.setHeight(100);

        assertArrayEquals(new double[] { 200 / 300.0, 0.5 },
                instance.getResultingScales(fullSize, scaleConstraint), DELTA);
    }

    @Test
    void getResultingSize1WithAspectFitWidthAndDownscaling() {
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
    void getResultingSize1WithAspectFitWidthAndUpscaling() {
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
    void getResultingSize1WithAspectFitHeightAndDownscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_HEIGHT);
        instance.setWidth(400);
        instance.setHeight(200);

        assertEquals(new Dimension(300, 200),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void getResultingSize1WithAspectFitHeightAndUpscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_HEIGHT);
        instance.setWidth(1200);
        instance.setHeight(600);

        assertEquals(new Dimension(900, 600),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void getResultingSize1WithAspectFitInsideAndDownscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(400);
        instance.setHeight(200);

        assertEquals(new Dimension(300, 200),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void getResultingSize1WithAspectFitInside() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(1200);
        instance.setHeight(600);

        assertEquals(new Dimension(900, 600),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void getResultingSize1WithNonAspectFillAndDownscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(ScaleByPixels.Mode.NON_ASPECT_FILL);
        instance.setWidth(400);
        instance.setHeight(200);

        assertEquals(new Dimension(400, 200),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void getResultingSize1WithNonAspectFillAndUpscaling() {
        final Dimension fullSize = new Dimension(600, 400);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.setMode(ScaleByPixels.Mode.NON_ASPECT_FILL);
        instance.setWidth(1200);
        instance.setHeight(600);

        assertEquals(new Dimension(1200, 600),
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void getResultingSize2WithAspectFitWidthAndDownscaling() {
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
    void getResultingSize2WithAspectFitWidthAndUpscaling() {
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
    void getResultingSize2WithAspectFitHeightAndDownscaling() {
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
    void getResultingSize2WithAspectFitHeightAndUpscaling() {
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
    void getResultingSize2WithAspectFitInsideAndDownscaling() {
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
    void getResultingSize2WithAspectFitInside() {
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
    void getResultingSize2WithNonAspectFillAndDownscaling() {
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
    void getResultingSize2WithNonAspectFillAndUpscaling() {
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
    void hasEffect() {
        instance = new ScaleByPixels(100, 100, ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        assertTrue(instance.hasEffect());
        instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_WIDTH);
        assertTrue(instance.hasEffect());
        instance.setMode(ScaleByPixels.Mode.NON_ASPECT_FILL);
        assertTrue(instance.hasEffect());
    }

    @Test
    void hasEffectWithArguments() {
        final Dimension fullSize = new Dimension(600, 400);
        final OperationList opList = OperationList.builder()
                .withOperations(new CropByPixels(0, 0, 300, 200))
                .build();

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
        opList.setMetaIdentifier(MetaIdentifier.builder()
                .withIdentifier("cats")
                .withScaleConstraint(1, 2)
                .build());
        assertTrue(instance.hasEffect(fullSize, opList));
    }

    @Test
    void isHeightUpWithAspectFitWidth() {
        Dimension size = new Dimension(600, 400);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        instance = new ScaleByPixels(
                null, 300, ScaleByPixels.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(300); // down
        assertFalse(instance.isHeightUp(size, scaleConstraint));
        instance.setWidth(600); // even
        assertFalse(instance.isHeightUp(size, scaleConstraint));
        instance.setWidth(800); // up
        assertTrue(instance.isHeightUp(size, scaleConstraint));
    }

    @Test
    void isHeightUpWithAspectFitHeight() {
        Dimension size = new Dimension(600, 400);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        instance = new ScaleByPixels(
                null, 200, ScaleByPixels.Mode.ASPECT_FIT_HEIGHT);
        instance.setHeight(200); // down
        assertFalse(instance.isHeightUp(size, scaleConstraint));
        instance.setHeight(400); // even
        assertFalse(instance.isHeightUp(size, scaleConstraint));
        instance.setHeight(600); // up
        assertTrue(instance.isHeightUp(size, scaleConstraint));
    }

    @Test
    void isHeightUpWithAspectFitInside() {
        Dimension size = new Dimension(600, 400);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        instance = new ScaleByPixels(
                null, null, ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(300); // down
        instance.setHeight(200);
        assertFalse(instance.isHeightUp(size, scaleConstraint));
        instance.setWidth(600); // even
        instance.setHeight(400);
        assertFalse(instance.isHeightUp(size, scaleConstraint));
        instance.setWidth(800); // up
        instance.setHeight(600);
        assertTrue(instance.isHeightUp(size, scaleConstraint));
    }

    @Test
    void isHeightUpWithNonAspectFill() {
        Dimension size = new Dimension(600, 400);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        instance = new ScaleByPixels(
                null, null, ScaleByPixels.Mode.NON_ASPECT_FILL);
        instance.setWidth(300); // down
        instance.setHeight(200);
        assertFalse(instance.isHeightUp(size, scaleConstraint));
        instance.setWidth(600); // even
        instance.setHeight(400);
        assertFalse(instance.isHeightUp(size, scaleConstraint));
        instance.setWidth(800); // up
        instance.setHeight(600);
        assertTrue(instance.isHeightUp(size, scaleConstraint));
        instance.setWidth(500);
        instance.setHeight(800);
        assertTrue(instance.isHeightUp(size, scaleConstraint));
        instance.setWidth(900);
        instance.setHeight(300);
        assertFalse(instance.isHeightUp(size, scaleConstraint));
    }

    @Test
    void isUpWithAspectFitWidth() {
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
    void isUpWithAspectFitHeight() {
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
    void isUpWithAspectFitInside() {
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
    void isUpWithNonAspectFill() {
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
    void isWidthUpWithAspectFitWidth() {
        Dimension size = new Dimension(600, 400);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        instance = new ScaleByPixels(
                null, 300, ScaleByPixels.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(300); // down
        assertFalse(instance.isWidthUp(size, scaleConstraint));
        instance.setWidth(600); // even
        assertFalse(instance.isWidthUp(size, scaleConstraint));
        instance.setWidth(800); // up
        assertTrue(instance.isWidthUp(size, scaleConstraint));
    }

    @Test
    void isWidthUpWithAspectFitHeight() {
        Dimension size = new Dimension(600, 400);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        instance = new ScaleByPixels(
                null, 200, ScaleByPixels.Mode.ASPECT_FIT_HEIGHT);
        instance.setHeight(200); // down
        assertFalse(instance.isWidthUp(size, scaleConstraint));
        instance.setHeight(400); // even
        assertFalse(instance.isWidthUp(size, scaleConstraint));
        instance.setHeight(600); // up
        assertTrue(instance.isWidthUp(size, scaleConstraint));
    }

    @Test
    void isWidthUpWithAspectFitInside() {
        Dimension size = new Dimension(600, 400);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        instance = new ScaleByPixels(
                null, null, ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(300); // down
        instance.setHeight(200);
        assertFalse(instance.isWidthUp(size, scaleConstraint));
        instance.setWidth(600); // even
        instance.setHeight(400);
        assertFalse(instance.isWidthUp(size, scaleConstraint));
        instance.setWidth(800); // up
        instance.setHeight(600);
        assertTrue(instance.isWidthUp(size, scaleConstraint));
    }

    @Test
    void isWidthUpWithNonAspectFill() {
        Dimension size = new Dimension(600, 400);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        instance = new ScaleByPixels(
                null, null, ScaleByPixels.Mode.NON_ASPECT_FILL);
        instance.setWidth(300); // down
        instance.setHeight(200);
        assertFalse(instance.isWidthUp(size, scaleConstraint));
        instance.setWidth(600); // even
        instance.setHeight(400);
        assertFalse(instance.isWidthUp(size, scaleConstraint));
        instance.setWidth(800); // up
        instance.setHeight(600);
        assertTrue(instance.isWidthUp(size, scaleConstraint));
        instance.setWidth(500);
        instance.setHeight(800);
        assertFalse(instance.isWidthUp(size, scaleConstraint));
        instance.setWidth(900);
        instance.setHeight(300);
        assertTrue(instance.isWidthUp(size, scaleConstraint));
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
        assertThrows(IllegalStateException.class,
                () -> instance.setHeight(80));
    }

    @Test
    void setModeWhenFrozenThrowsException() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setMode(ScaleByPixels.Mode.ASPECT_FIT_HEIGHT));
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
