package edu.illinois.library.cantaloupe.image;

import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ScaleTest {

    private Scale scale;

    @Before
    public void setUp() {
        this.scale = new Scale();
    }

    @Test
    public void testGetEffectiveSize() {
        final Dimension fullSize = new Dimension(300, 200);
        scale.setMode(Scale.Mode.FULL);
        assertEquals(fullSize, scale.getResultingSize(fullSize));
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setWidth(200);
        scale.setHeight(100);
        assertEquals(new Dimension(150, 100), scale.getResultingSize(fullSize));
        scale.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        scale.setWidth(200);
        scale.setHeight(100);
        assertEquals(new Dimension(200, 133), scale.getResultingSize(fullSize));
        scale.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        scale.setWidth(200);
        scale.setHeight(100);
        assertEquals(new Dimension(150, 100), scale.getResultingSize(fullSize));
        scale.setMode(Scale.Mode.NON_ASPECT_FILL);
        scale.setWidth(200);
        scale.setHeight(100);
        assertEquals(new Dimension(200, 100), scale.getResultingSize(fullSize));
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setPercent(0.5f);
        assertEquals(new Dimension(150, 100), scale.getResultingSize(fullSize));
    }

    @Test
    public void testIsNoOp() {
        scale.setMode(Scale.Mode.FULL);
        assertTrue(scale.isNoOp());
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        scale.setPercent(1f);
        assertTrue(scale.isNoOp());
        scale = new Scale();
        scale.setPercent(0.5f);
        assertFalse(scale.isNoOp());
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setWidth(100);
        scale.setHeight(100);
        assertFalse(scale.isNoOp());
        scale.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        assertFalse(scale.isNoOp());
        scale.setMode(Scale.Mode.NON_ASPECT_FILL);
        assertFalse(scale.isNoOp());
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
            assertEquals("Percent must be between 0-1", e.getMessage());
        }
    }

    @Test
    public void testSetZeroPercent() {
        try {
            this.scale.setPercent(0f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Percent must be between 0-1", e.getMessage());
        }
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
    public void testToMap() {
        scale.setWidth(50);
        scale.setHeight(45);
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);

        Dimension fullSize = new Dimension(100, 100);
        Dimension resultingSize = scale.getResultingSize(fullSize);

        Map<String,Object> map = scale.toMap(fullSize);
        assertEquals(resultingSize.width, map.get("width"));
        assertEquals(resultingSize.height, map.get("height"));
    }

    @Test
    public void testToString() {
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        assertEquals("none", scale.toString());

        scale = new Scale();
        scale.setWidth(50);
        scale.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        assertEquals("50,", scale.toString());

        scale = new Scale();
        scale.setHeight(50);
        scale.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        assertEquals(",50", scale.toString());

        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setPercent(0.5f);
        assertEquals("50%", scale.toString());

        scale = new Scale();
        scale.setWidth(50);
        scale.setHeight(40);
        scale.setMode(Scale.Mode.NON_ASPECT_FILL);
        assertEquals("50,40", scale.toString());

        scale = new Scale();
        scale.setWidth(50);
        scale.setHeight(40);
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        assertEquals("!50,40", scale.toString());
    }

}
