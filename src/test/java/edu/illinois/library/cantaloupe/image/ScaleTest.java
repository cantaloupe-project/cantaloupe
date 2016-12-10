package edu.illinois.library.cantaloupe.image;

import com.mortennobel.imagescaling.ResampleFilters;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.util.Map;

import static org.junit.Assert.*;

public class ScaleTest {

    private Scale scale;

    @Before
    public void setUp() {
        this.scale = new Scale();
    }

    @Test
    public void testConstructor1() {
        assertEquals(Scale.Mode.FULL, scale.getMode());
        assertNull(scale.getPercent());
        assertNull(scale.getHeight());
        assertNull(scale.getWidth());
    }

    @Test
    public void testConstructor2() {
        this.scale = new Scale(0.3f);
        assertEquals(Scale.Mode.ASPECT_FIT_INSIDE, scale.getMode());
        assertEquals(0.3f, scale.getPercent(), 0.00001f);
    }

    @Test
    public void testConstructor3() {
        this.scale = new Scale(300, 200, Scale.Mode.ASPECT_FIT_HEIGHT);
        assertEquals(Scale.Mode.ASPECT_FIT_HEIGHT, scale.getMode());
        assertEquals(300, (long) scale.getWidth());
        assertEquals(200, (long) scale.getHeight());
    }

    @Test
    public void testFilterGetResampleFilter() {
        assertSame(ResampleFilters.getBellFilter(),
                Scale.Filter.BELL.getResampleFilter());
        assertSame(ResampleFilters.getBiCubicFilter(),
                Scale.Filter.BICUBIC.getResampleFilter());
        assertSame(ResampleFilters.getBoxFilter(),
                Scale.Filter.BOX.getResampleFilter());
        assertSame(ResampleFilters.getBSplineFilter(),
                Scale.Filter.BSPLINE.getResampleFilter());
        assertSame(ResampleFilters.getHermiteFilter(),
                Scale.Filter.HERMITE.getResampleFilter());
        assertSame(ResampleFilters.getLanczos3Filter(),
                Scale.Filter.LANCZOS3.getResampleFilter());
        assertSame(ResampleFilters.getMitchellFilter(),
                Scale.Filter.MITCHELL.getResampleFilter());
        assertSame(ResampleFilters.getTriangleFilter(),
                Scale.Filter.TRIANGLE.getResampleFilter());
    }

    @Test
    public void testGetResultingScale() {
        final Dimension fullSize = new Dimension(300, 200);
        final float fudge = 0.0000001f;

        scale.setMode(Scale.Mode.FULL);
        assertEquals(1f, scale.getResultingScale(fullSize), fudge);

        // ASPECT_FIT_WIDTH
        scale.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        scale.setWidth(200);
        scale.setHeight(100);
        assertEquals(0.6666667f, scale.getResultingScale(fullSize), fudge);

        // ASPECT_FIT_HEIGHT
        scale.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        scale.setWidth(200);
        scale.setHeight(100);
        assertEquals(0.5f, scale.getResultingScale(fullSize), fudge);

        // ASPECT_FIT_INSIDE
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setWidth(200);
        scale.setHeight(100);
        assertEquals(0.5f, scale.getResultingScale(fullSize), fudge);

        // NON_ASPECT_FILL
        scale.setMode(Scale.Mode.NON_ASPECT_FILL);
        scale.setWidth(200);
        scale.setHeight(100);
        assertNull(scale.getResultingScale(fullSize));

        // percent
        scale = new Scale();
        scale.setPercent(0.5f);
        assertEquals(0.5f, scale.getResultingScale(fullSize), fudge);
    }

    @Test
    public void testGetResultingSize() {
        final Dimension fullSize = new Dimension(600, 400);

        scale.setMode(Scale.Mode.FULL);
        assertEquals(fullSize, scale.getResultingSize(fullSize));

        // ASPECT_FIT_WIDTH
        scale.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        // down
        scale.setWidth(400);
        scale.setHeight(200);
        assertEquals(new Dimension(400, 266), scale.getResultingSize(fullSize));
        // up
        scale.setWidth(1200);
        scale.setHeight(600);
        assertEquals(new Dimension(1200, 800), scale.getResultingSize(fullSize));

        // ASPECT_FIT_HEIGHT
        scale.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        // down
        scale.setWidth(400);
        scale.setHeight(200);
        assertEquals(new Dimension(300, 200), scale.getResultingSize(fullSize));
        // up
        scale.setWidth(1200);
        scale.setHeight(600);
        assertEquals(new Dimension(900, 600), scale.getResultingSize(fullSize));

        // ASPECT_FIT_INSIDE
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        // down
        scale.setWidth(400);
        scale.setHeight(200);
        assertEquals(new Dimension(300, 200), scale.getResultingSize(fullSize));
        // up
        scale.setWidth(1200);
        scale.setHeight(600);
        assertEquals(new Dimension(900, 600), scale.getResultingSize(fullSize));

        // NON_ASPECT_FILL
        scale.setMode(Scale.Mode.NON_ASPECT_FILL);
        // down
        scale.setWidth(400);
        scale.setHeight(200);
        assertEquals(new Dimension(400, 200), scale.getResultingSize(fullSize));
        // up
        scale.setWidth(1200);
        scale.setHeight(600);
        assertEquals(new Dimension(1200, 600), scale.getResultingSize(fullSize));

        // percent
        scale = new Scale();
        // down
        scale.setPercent(0.5f);
        assertEquals(new Dimension(300, 200), scale.getResultingSize(fullSize));
        // up
        scale.setPercent(1.5f);
        assertEquals(new Dimension(900, 600), scale.getResultingSize(fullSize));
    }

    @Test
    public void testIsNoOp() {
        scale.setMode(Scale.Mode.FULL);
        assertTrue(scale.isNoOp());
        scale = new Scale(1f);
        assertTrue(scale.isNoOp());
        scale = new Scale(0.5f);
        assertFalse(scale.isNoOp());
        scale = new Scale(100, 100, Scale.Mode.ASPECT_FIT_INSIDE);
        assertFalse(scale.isNoOp());
        scale.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        assertFalse(scale.isNoOp());
        scale.setMode(Scale.Mode.NON_ASPECT_FILL);
        assertFalse(scale.isNoOp());
    }

    @Test
    public void testIsNoOpWithArguments() {
        Dimension fullSize = new Dimension(600, 400);
        OperationList opList = new OperationList();
        opList.add(new Crop(0, 0, 300, 200));

        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setPercent(0.5f);
        assertFalse(scale.isNoOp(fullSize, opList));

        scale.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        scale.setWidth(300);
        assertTrue(scale.isNoOp(fullSize, opList));

        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        scale.setHeight(200);
        assertTrue(scale.isNoOp(fullSize, opList));

        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setWidth(300);
        scale.setHeight(200);
        assertTrue(scale.isNoOp(fullSize, opList));

        scale = new Scale();
        scale.setMode(Scale.Mode.NON_ASPECT_FILL);
        scale.setWidth(300);
        scale.setHeight(200);
        assertTrue(scale.isNoOp(fullSize, opList));
    }

    @Test
    public void testSetHeight() {
        Integer height = 50;
        this.scale.setHeight(height);
        assertEquals(height, this.scale.getHeight());
    }

    @Test
    public void testSetNegativeHeight() {
        try {
            this.scale.setHeight(-1);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void testSetZeroHeight() {
        try {
            this.scale.setHeight(0);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void testSetNullHeight() {
        this.scale.setHeight(null);
        assertNull(scale.getHeight());
    }

    @Test
    public void testSetPercent() {
        float percent = 0.5f;
        this.scale.setPercent(percent);
        assertEquals(percent, this.scale.getPercent(), 0.000001f);
    }

    @Test
    public void testSetNegativePercent() {
        try {
            this.scale.setPercent(-0.5f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Percent must be greater than zero", e.getMessage());
        }
    }

    @Test
    public void testSetZeroPercent() {
        try {
            this.scale.setPercent(0f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Percent must be greater than zero", e.getMessage());
        }
    }

    @Test
    public void testSetNullPercent() {
        this.scale.setPercent(null);
        assertNull(scale.getPercent());
    }

    @Test
    public void testSetWidth() {
        Integer width = 50;
        this.scale.setWidth(width);
        assertEquals(width, this.scale.getWidth());
    }

    @Test
    public void testSetNegativeWidth() {
        try {
            this.scale.setWidth(-1);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void testSetZeroWidth() {
        try {
            this.scale.setWidth(0);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void testSetNullWidth() {
        this.scale.setWidth(null);
        assertNull(scale.getWidth());
    }

    @Test
    public void testToMap() {
        scale.setWidth(50);
        scale.setHeight(45);
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);

        Dimension fullSize = new Dimension(100, 100);
        Dimension resultingSize = scale.getResultingSize(fullSize);

        Map<String,Object> map = scale.toMap(fullSize);
        assertEquals(scale.getClass().getSimpleName(), map.get("class"));
        assertEquals(resultingSize.width, map.get("width"));
        assertEquals(resultingSize.height, map.get("height"));
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
