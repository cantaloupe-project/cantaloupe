package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.processor.resample.ResampleFilters;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
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

    private static final double DELTA = 0.00000001f;

    private Scale instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.instance = new Scale();
    }

    @Test
    public void constructor1() {
        assertEquals(Scale.Mode.FULL, instance.getMode());
        assertNull(instance.getPercent());
        assertNull(instance.getHeight());
        assertNull(instance.getWidth());
    }

    @Test
    public void constructor2() {
        this.instance = new Scale(0.3f);
        assertEquals(Scale.Mode.ASPECT_FIT_INSIDE, instance.getMode());
        assertEquals(0.3f, instance.getPercent(), 0.00001f);
    }

    @Test
    public void constructor3() {
        this.instance = new Scale(300, 200, Scale.Mode.ASPECT_FIT_HEIGHT);
        assertEquals(Scale.Mode.ASPECT_FIT_HEIGHT, instance.getMode());
        assertEquals(300, (long) instance.getWidth());
        assertEquals(200, (long) instance.getHeight());
    }

    @Test
    public void getDifferentialScaleWithFull() {
        final Dimension fullSize = new Dimension(300, 200);
        final ReductionFactor factor = new ReductionFactor(1);

        instance.setMode(Scale.Mode.FULL);
        assertEquals(1f, instance.getDifferentialScale(fullSize, factor), DELTA);
    }

    @Test
    public void getDifferentialScaleWithAspectFitWidth() {
        final Dimension fullSize = new Dimension(300, 200);
        final ReductionFactor factor = new ReductionFactor(1);

        instance.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(150);
        instance.setHeight(100);

        assertEquals(1f, instance.getDifferentialScale(fullSize, factor), DELTA);
        instance.setWidth(75);
        instance.setHeight(50);
        assertEquals(0.5f, instance.getDifferentialScale(fullSize, factor), DELTA);
    }

    @Test
    public void getDifferentialScaleWithAspectFitHeight() {
        final Dimension fullSize = new Dimension(300, 200);
        final ReductionFactor factor = new ReductionFactor(1);

        instance.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        instance.setWidth(150);
        instance.setHeight(100);
        assertEquals(1f, instance.getDifferentialScale(fullSize, factor), DELTA);

        instance.setWidth(75);
        instance.setHeight(50);
        assertEquals(0.5f, instance.getDifferentialScale(fullSize, factor), DELTA);
    }

    @Test
    public void getDifferentialScaleWithAspectFitInside() {
        final Dimension fullSize = new Dimension(300, 200);
        final ReductionFactor factor = new ReductionFactor(1);

        instance.setMode(Scale.Mode.FULL);
        assertEquals(1f, instance.getDifferentialScale(fullSize, factor), DELTA);

        instance.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(200);
        instance.setHeight(100);
        assertEquals(1f, instance.getDifferentialScale(fullSize, factor), DELTA);

        instance.setWidth(100);
        instance.setHeight(50);
        assertEquals(0.5f, instance.getDifferentialScale(fullSize, factor), DELTA);
    }

    @Test
    public void getDifferentialScaleWithNonAspectFill() {
        final Dimension fullSize = new Dimension(300, 200);
        final ReductionFactor factor = new ReductionFactor(1);

        instance.setMode(Scale.Mode.NON_ASPECT_FILL);
        instance.setWidth(200);
        instance.setHeight(100);
        assertNull(instance.getDifferentialScale(fullSize, factor));
    }

    @Test
    public void getDifferentialScaleWithPercent() {
        final Dimension fullSize = new Dimension(300, 200);
        ReductionFactor factor = new ReductionFactor(1);

        instance = new Scale();
        instance.setPercent(0.5);
        assertEquals(1f, instance.getDifferentialScale(fullSize, factor), DELTA);

        instance.setPercent(0.25);
        assertEquals(0.5f, instance.getDifferentialScale(fullSize, factor), DELTA);

        instance.setPercent(1.5);
        factor = new ReductionFactor(0);
        assertEquals(1.5f, instance.getDifferentialScale(fullSize, factor), DELTA);
    }

    @Test
    public void getReductionFactor() {
        Dimension size = new Dimension(300, 300);

        // FULL
        assertEquals(0, instance.getReductionFactor(size, 999).factor);

        // ASPECT_FIT_WIDTH
        instance.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(145);
        instance.setHeight(145);
        assertEquals(1, instance.getReductionFactor(size, 999).factor);

        // ASPECT_FIT_HEIGHT
        instance.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        instance.setWidth(145);
        instance.setHeight(145);
        assertEquals(1, instance.getReductionFactor(size, 999).factor);

        // ASPECT_FIT_INSIDE
        instance.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(145);
        instance.setHeight(145);
        assertEquals(1, instance.getReductionFactor(size, 999).factor);

        // NON_ASPECT_FILL
        instance.setMode(Scale.Mode.NON_ASPECT_FILL);
        instance.setWidth(145);
        instance.setHeight(145);
        assertEquals(0, instance.getReductionFactor(size, 999).factor);

        // percent
        instance = new Scale();
        instance.setPercent(0.45);
        assertEquals(1, instance.getReductionFactor(size, 999).factor);
        instance.setPercent(0.2);
        assertEquals(2, instance.getReductionFactor(size, 999).factor);
        assertEquals(1, instance.getReductionFactor(size, 1).factor);
    }

    @Test
    public void getResultingScale1() {
        final Dimension fullSize = new Dimension(300, 200);

        instance.setMode(Scale.Mode.FULL);
        assertEquals(1, instance.getResultingScale(fullSize), DELTA);

        // ASPECT_FIT_WIDTH
        instance.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(200);
        instance.setHeight(100);
        assertEquals(0.66666666, instance.getResultingScale(fullSize), DELTA);

        // ASPECT_FIT_HEIGHT
        instance.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        instance.setWidth(200);
        instance.setHeight(100);
        assertEquals(0.5, instance.getResultingScale(fullSize), DELTA);

        // ASPECT_FIT_INSIDE
        instance.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(200);
        instance.setHeight(100);
        assertEquals(0.5, instance.getResultingScale(fullSize), DELTA);

        // NON_ASPECT_FILL
        instance.setMode(Scale.Mode.NON_ASPECT_FILL);
        instance.setWidth(200);
        instance.setHeight(100);
        assertNull(instance.getResultingScale(fullSize));

        // percent
        instance = new Scale();
        instance.setPercent(0.5);
        assertEquals(0.5, instance.getResultingScale(fullSize), DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getResultingSizeWithNullArgument() {
        instance.getResultingSize(null);
    }

    @Test
    public void getResultingSize() {
        final Dimension fullSize = new Dimension(600, 400);

        instance.setMode(Scale.Mode.FULL);
        assertEquals(fullSize, instance.getResultingSize(fullSize));

        // ASPECT_FIT_WIDTH
        instance.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        // down
        instance.setWidth(400);
        instance.setHeight(200);
        assertEquals(new Dimension(400, 267), instance.getResultingSize(fullSize));
        // up
        instance.setWidth(1200);
        instance.setHeight(600);
        assertEquals(new Dimension(1200, 800), instance.getResultingSize(fullSize));

        // ASPECT_FIT_HEIGHT
        instance.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        // down
        instance.setWidth(400);
        instance.setHeight(200);
        assertEquals(new Dimension(300, 200), instance.getResultingSize(fullSize));
        // up
        instance.setWidth(1200);
        instance.setHeight(600);
        assertEquals(new Dimension(900, 600), instance.getResultingSize(fullSize));

        // ASPECT_FIT_INSIDE
        instance.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        // down
        instance.setWidth(400);
        instance.setHeight(200);
        assertEquals(new Dimension(300, 200), instance.getResultingSize(fullSize));
        // up
        instance.setWidth(1200);
        instance.setHeight(600);
        assertEquals(new Dimension(900, 600), instance.getResultingSize(fullSize));

        // NON_ASPECT_FILL
        instance.setMode(Scale.Mode.NON_ASPECT_FILL);
        // down
        instance.setWidth(400);
        instance.setHeight(200);
        assertEquals(new Dimension(400, 200), instance.getResultingSize(fullSize));
        // up
        instance.setWidth(1200);
        instance.setHeight(600);
        assertEquals(new Dimension(1200, 600), instance.getResultingSize(fullSize));

        // percent
        instance = new Scale();
        // down
        instance.setPercent(0.5);
        assertEquals(new Dimension(300, 200), instance.getResultingSize(fullSize));
        // up
        instance.setPercent(1.5);
        assertEquals(new Dimension(900, 600), instance.getResultingSize(fullSize));
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
        final OperationList opList = new OperationList(new Crop(0, 0, 300, 200));

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
    public void isUp() {
        Dimension size = new Dimension(600, 400);
        // Percent
        instance = new Scale();
        instance.setPercent(0.5); // down
        assertFalse(instance.isUp(size));
        instance.setPercent(1.0); // even
        assertFalse(instance.isUp(size));
        instance.setPercent(1.2); // up
        assertTrue(instance.isUp(size));

        // FULL
        instance = new Scale();
        instance.setMode(Scale.Mode.FULL);
        assertFalse(instance.isUp(size));

        // ASPECT_FIT_WIDTH
        instance = new Scale();
        instance.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        instance.setWidth(300); // down
        assertFalse(instance.isUp(size));
        instance.setWidth(600); // even
        assertFalse(instance.isUp(size));
        instance.setWidth(800); // up
        assertTrue(instance.isUp(size));

        // ASPECT_FIT_HEIGHT
        instance = new Scale();
        instance.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        instance.setHeight(200); // down
        assertFalse(instance.isUp(size));
        instance.setHeight(400); // even
        assertFalse(instance.isUp(size));
        instance.setHeight(600); // up
        assertTrue(instance.isUp(size));

        // ASPECT_FIT_INSIDE
        instance = new Scale();
        instance.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        instance.setWidth(300); // down
        instance.setHeight(200);
        assertFalse(instance.isUp(size));
        instance.setWidth(600); // even
        instance.setHeight(400);
        assertFalse(instance.isUp(size));
        instance.setWidth(800); // up
        instance.setHeight(600);
        assertTrue(instance.isUp(size));

        // NON_ASPECT_FILL
        instance = new Scale();
        instance.setMode(Scale.Mode.NON_ASPECT_FILL);
        instance.setWidth(300); // down
        instance.setHeight(200);
        assertFalse(instance.isUp(size));
        instance.setWidth(600); // even
        instance.setHeight(400);
        assertFalse(instance.isUp(size));
        instance.setWidth(800); // up
        instance.setHeight(600);
        assertTrue(instance.isUp(size));
        instance.setWidth(500);
        instance.setHeight(800);
        assertTrue(instance.isUp(size));
        instance.setWidth(900);
        instance.setHeight(300);
        assertTrue(instance.isUp(size));
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
        Dimension resultingSize = instance.getResultingSize(fullSize);

        Map<String,Object> map = instance.toMap(fullSize);
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
        assertEquals(resultingSize.width, map.get("width"));
        assertEquals(resultingSize.height, map.get("height"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void toMapReturnsUnmodifiableMap() {
        Dimension fullSize = new Dimension(100, 100);
        Map<String,Object> map = instance.toMap(fullSize);
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
