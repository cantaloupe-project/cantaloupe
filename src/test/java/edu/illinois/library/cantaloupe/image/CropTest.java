package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;

import java.awt.Dimension;
import java.awt.Rectangle;

public class CropTest extends CantaloupeTestCase {

    private Crop region;

    public void setUp() {
        this.region = new Crop();
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
        region.setX(20f);
        region.setY(20f);
        region.setWidth(50f);
        region.setHeight(50f);
        region.setPercent(true);
        assertEquals(new Rectangle(40, 40, 100, 100), region.getRectangle(fullSize));
    }

    /* height */

    public void testSetHeight() {
        float height = 50f;
        this.region.setHeight(height);
        assertEquals(height, this.region.getHeight());
    }

    public void testSetNegativeHeight() {
        try {
            this.region.setHeight(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    public void testSetZeroHeight() {
        try {
            this.region.setHeight(0f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    /* width */

    public void testSetWidth() {
        Float width = (float) 50;
        this.region.setWidth(width);
        assertEquals(width, this.region.getWidth());
    }

    public void testSetNegativeWidth() {
        try {
            this.region.setWidth(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    public void testSetZeroWidth() {
        try {
            this.region.setWidth(0f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    /* x */

    public void testSetX() {
        float x = 50f;
        this.region.setX(x);
        assertEquals(x, this.region.getX());
    }

    public void testSetNegativeX() {
        try {
            this.region.setX(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("X must be a positive float", e.getMessage());
        }
    }

    /* y */

    public void testSetY() {
        float y = 50f;
        this.region.setY(y);
        assertEquals(y, this.region.getY());
    }

    public void testSetNegativeY() {
        try {
            this.region.setY(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Y must be a positive float", e.getMessage());
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
        crop.setWidth(50f);
        crop.setHeight(40f);
        assertEquals("pct:0,0,50,40", crop.toString());
    }

}
