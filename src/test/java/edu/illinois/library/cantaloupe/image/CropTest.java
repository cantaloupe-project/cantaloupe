package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;

import java.awt.Dimension;
import java.awt.Rectangle;

public class CropTest extends CantaloupeTestCase {

    private Crop crop;

    public void setUp() {
        this.crop = new Crop();
    }

    public void testGetRectangle() {
        Dimension fullSize = new Dimension(200, 200);
        // full
        Crop region = new Crop();
        region.setFull(true);
        assertEquals(new Rectangle(0, 0, 200, 200), region.getRectangle(fullSize));
        // pixels
        region = new Crop();
        region.setX(20f);
        region.setY(20f);
        region.setWidth(50f);
        region.setHeight(50f);
        assertEquals(new Rectangle(20, 20, 50, 50), region.getRectangle(fullSize));
        // percentage
        region = new Crop();
        region.setPercent(true);
        region.setX(0.2f);
        region.setY(0.2f);
        region.setWidth(0.5f);
        region.setHeight(0.5f);
        assertEquals(new Rectangle(40, 40, 100, 100), region.getRectangle(fullSize));
    }

    public void testIsNull() {
        // new instance
        Crop crop = new Crop();
        assertFalse(crop.isNoOp());
        // 100% crop
        crop = new Crop();
        crop.setPercent(true);
        crop.setWidth(1f);
        crop.setHeight(1f);
        assertTrue(crop.isNoOp());
        // <100% crop
        crop = new Crop();
        crop.setPercent(true);
        crop.setWidth(0.8f);
        crop.setHeight(0.8f);
        assertFalse(crop.isNoOp());
        // pixel crop
        crop = new Crop();
        crop.setWidth(50f);
        crop.setHeight(50f);
        assertFalse(crop.isNoOp());
    }

    public void testSetHeight() {
        float height = 50f;
        this.crop.setHeight(height);
        assertEquals(height, this.crop.getHeight());
    }

    public void testSetNegativeHeight() {
        try {
            this.crop.setHeight(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    public void testSetZeroHeight() {
        try {
            this.crop.setHeight(0f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    public void testSetMoreThan100PercentHeight() {
        try {
            this.crop.setPercent(true);
            this.crop.setHeight(1.2f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height percentage must be <= 1", e.getMessage());
        }
    }

    public void testSetWidth() {
        Float width = (float) 50;
        this.crop.setWidth(width);
        assertEquals(width, this.crop.getWidth());
    }

    public void testSetNegativeWidth() {
        try {
            this.crop.setWidth(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    public void testSetZeroWidth() {
        try {
            this.crop.setWidth(0f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    public void testSetMoreThan100PercentWidth() {
        try {
            this.crop.setPercent(true);
            this.crop.setWidth(1.2f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width percentage must be <= 1", e.getMessage());
        }
    }

    public void testSetX() {
        float x = 50f;
        this.crop.setX(x);
        assertEquals(x, this.crop.getX());
    }

    public void testSetNegativeX() {
        try {
            this.crop.setX(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("X must be a positive float", e.getMessage());
        }
    }

    public void testSetMoreThan100PercentX() {
        try {
            this.crop.setPercent(true);
            this.crop.setX(1.2f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("X percentage must be <= 1", e.getMessage());
        }
    }

    public void testSetY() {
        float y = 50f;
        this.crop.setY(y);
        assertEquals(y, this.crop.getY());
    }

    public void testSetNegativeY() {
        try {
            this.crop.setY(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Y must be a positive float", e.getMessage());
        }
    }

    public void testSetMoreThan100PercentY() {
        try {
            this.crop.setPercent(true);
            this.crop.setY(1.2f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Y percentage must be <= 1", e.getMessage());
        }
    }

    public void testToString() {
        Crop crop = new Crop();
        crop.setFull(true);
        assertEquals("full", crop.toString());

        crop = new Crop();
        crop.setWidth(50f);
        crop.setHeight(40f);
        assertEquals("0,0,50,40", crop.toString());

        crop = new Crop();
        crop.setPercent(true);
        crop.setWidth(0.5f);
        crop.setHeight(0.4f);
        assertEquals("pct:0,0,0.5,0.4", crop.toString());
    }

}
