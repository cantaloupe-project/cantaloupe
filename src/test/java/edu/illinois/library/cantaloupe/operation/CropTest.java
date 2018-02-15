package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Map;

import static org.junit.Assert.*;

public class CropTest extends BaseTest {

    private static final float DELTA = 0.0000001f;

    private Crop instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        instance = new Crop();
        assertEquals(Crop.Unit.PIXELS, instance.getUnit());
        assertEquals(0f, instance.getX(), DELTA);
        assertEquals(0f, instance.getY(), DELTA);
        assertEquals(0f, instance.getWidth(), DELTA);
        assertEquals(0f, instance.getHeight(), DELTA);
    }

    @Test
    public void fromRectangle() {
        Rectangle rect = new Rectangle(25, 25, 75, 75);
        Crop crop = Crop.fromRectangle(rect);
        assertEquals(crop.getX(), rect.x, DELTA);
        assertEquals(crop.getY(), rect.y, DELTA);
        assertEquals(crop.getWidth(), rect.width, DELTA);
        assertEquals(crop.getHeight(), rect.height, DELTA);
    }

    @Test
    public void applyOrientationOf0() {
        final Dimension fullSize = new Dimension(500, 250);
        instance = new Crop(100, 50, 400, 200);
        instance.applyOrientation(Orientation.ROTATE_0, fullSize);
        assertEquals(100, instance.getX(), DELTA);
        assertEquals(50, instance.getY(), DELTA);
        assertEquals(400, instance.getWidth(), DELTA);
        assertEquals(200, instance.getHeight(), DELTA);
    }

    /**
     * The crop area rotates counter-clockwise over the image to a bottom-left
     * origin.
     */
    @Test
    public void applyOrientationOf90() {
        Dimension fullSize = new Dimension(500, 250);
        instance = new Crop(100, 50, 400, 200);
        instance.applyOrientation(Orientation.ROTATE_90, fullSize);
        assertEquals(50, instance.getX(), DELTA);
        assertEquals(0, instance.getY(), DELTA);
        assertEquals(200, instance.getWidth(), DELTA);
        assertEquals(150, instance.getHeight(), DELTA);

        fullSize = new Dimension(2000, 500);
        instance = new Crop(100, 100, 1900, 200);
        instance.applyOrientation(Orientation.ROTATE_90, fullSize);
        assertEquals(100, instance.getX(), DELTA);
        assertEquals(0, instance.getY(), DELTA);
        assertEquals(200, instance.getWidth(), DELTA);
        assertEquals(400, instance.getHeight(), DELTA);
    }

    @Test
    public void applyOrientationOf180() {
        Dimension fullSize = new Dimension(500, 250);
        instance = new Crop(100, 50, 400, 200);
        instance.applyOrientation(Orientation.ROTATE_180, fullSize);
        assertEquals(0, instance.getX(), DELTA);
        assertEquals(0, instance.getY(), DELTA);
        assertEquals(400, instance.getWidth(), DELTA);
        assertEquals(200, instance.getHeight(), DELTA);
    }

    /**
     * The crop area rotates clockwise over the image to a top-right origin.
     */
    @Test
    public void applyOrientationOf270() {
        Dimension fullSize = new Dimension(500, 250);
        instance = new Crop(100, 50, 400, 200);
        instance.applyOrientation(Orientation.ROTATE_270, fullSize);
        assertEquals(250, instance.getX(), DELTA);
        assertEquals(100, instance.getY(), DELTA);
        assertEquals(200, instance.getWidth(), DELTA);
        assertEquals(150, instance.getHeight(), DELTA);

        fullSize = new Dimension(2000, 500);
        instance = new Crop(100, 100, 1900, 200);
        instance.applyOrientation(Orientation.ROTATE_270, fullSize);
        assertEquals(1700, instance.getX(), DELTA);
        assertEquals(100, instance.getY(), DELTA);
        assertEquals(200, instance.getWidth(), DELTA);
        assertEquals(400, instance.getHeight(), DELTA);
    }

    @Test(expected = IllegalStateException.class)
    public void applyOrientationThrowsExceptionWhenFrozen() {
        instance.freeze();
        Dimension fullSize = new Dimension(500, 250);
        instance.applyOrientation(Orientation.ROTATE_90, fullSize);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getRectangleWithNullSize() {
        Crop crop = new Crop();
        crop.setFull(true);
        crop.getRectangle(null);
    }

    @Test
    public void getRectangleWithFull() {
        final Dimension fullSize = new Dimension(300, 200);
        Crop crop = new Crop();
        crop.setFull(true);
        assertEquals(new Rectangle(0, 0, 300, 200), crop.getRectangle(fullSize));
    }

    @Test
    public void getRectangleWithSquare() {
        final Dimension fullSize = new Dimension(300, 200);
        Crop crop = new Crop();
        crop.setShape(Crop.Shape.SQUARE);
        assertEquals(new Rectangle(50, 0, 200, 200), crop.getRectangle(fullSize));
    }

    @Test
    public void getRectangleWithPixels() {
        final Dimension fullSize = new Dimension(300, 200);
        Crop crop = new Crop(20, 20, 50, 50);
        assertEquals(new Rectangle(20, 20, 50, 50), crop.getRectangle(fullSize));
    }

    @Test
    public void getRectangleWithPercentage() {
        final Dimension fullSize = new Dimension(300, 200);
        Crop crop = new Crop(0.2f, 0.2f, 0.5f, 0.5f);
        crop.setUnit(Crop.Unit.PERCENT);
        assertEquals(new Rectangle(60, 40, 150, 100), crop.getRectangle(fullSize));
    }

    @Test
    public void getRectangleDoesNotExceedFullSizeBounds() {
        final Dimension fullSize = new Dimension(300, 200);
        Crop crop = new Crop(200f, 150f, 100f, 100f);
        assertEquals(new Rectangle(200, 150, 100, 50), crop.getRectangle(fullSize));
    }

    @Test
    public void getRectangleWithReductionFactorWithFull() {
        final Dimension imageSize = new Dimension(300, 200);
        final ReductionFactor rf = new ReductionFactor(2);
        Crop crop = new Crop();
        crop.setFull(true);
        assertEquals(new Rectangle(0, 0, 300, 200),
                crop.getRectangle(imageSize, rf));
    }

    @Test
    public void getRectangleWithReductionFactorWithSquare() {
        final Dimension fullSize = new Dimension(300, 200);
        final ReductionFactor rf = new ReductionFactor(2);
        Crop crop = new Crop();
        crop.setShape(Crop.Shape.SQUARE);
        assertEquals(new Rectangle(50, 0, 200, 200),
                crop.getRectangle(fullSize, rf));
    }

    @Test
    public void getRectangleWithReductionFactorWithPixels() {
        final Dimension fullSize = new Dimension(300, 200);
        final ReductionFactor rf = new ReductionFactor(2);
        Crop crop = new Crop(20, 20, 50, 50);
        assertEquals(new Rectangle(5, 5, 13, 13),
                crop.getRectangle(fullSize, rf));
    }

    @Test
    public void getRectangleWithReductionFactorWithPercentage() {
        final Dimension fullSize = new Dimension(300, 200);
        final ReductionFactor rf = new ReductionFactor(2);
        Crop crop = new Crop(0.2f, 0.2f, 0.5f, 0.5f);
        crop.setUnit(Crop.Unit.PERCENT);
        assertEquals(new Rectangle(15, 10, 38, 25),
                crop.getRectangle(fullSize, rf));
    }

    @Test
    public void getRectangleWithReductionFactorDoesNotExceedFullSizeBounds() {
        final Dimension fullSize = new Dimension(300, 200);
        final ReductionFactor rf = new ReductionFactor(2);
        Crop crop = new Crop(200f, 150f, 100f, 100f);
        assertEquals(new Rectangle(50, 38, 25, 25),
                crop.getRectangle(fullSize, rf));
    }

    @Test
    public void getResultingSize() {
        Dimension fullSize = new Dimension(200, 200);
        // full
        Crop crop = new Crop();
        crop.setFull(true);
        assertEquals(new Dimension(200, 200), crop.getResultingSize(fullSize));
        // pixels
        crop = new Crop(20f, 20f, 50f, 50f);
        assertEquals(new Dimension(50, 50), crop.getResultingSize(fullSize));
        // percentage
        crop = new Crop(0.2f, 0.2f, 0.5f, 0.5f);
        crop.setUnit(Crop.Unit.PERCENT);
        assertEquals(new Dimension(100, 100), crop.getResultingSize(fullSize));
    }

    @Test
    public void hasEffect() {
        // new instance
        Crop crop = new Crop();
        assertTrue(crop.hasEffect());
        // 100% crop
        crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setWidth(1f);
        crop.setHeight(1f);
        assertFalse(crop.hasEffect());
        // <100% crop
        crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setWidth(0.8f);
        crop.setHeight(0.8f);
        assertTrue(crop.hasEffect());
        // pixel crop
        crop = new Crop();
        crop.setWidth(50f);
        crop.setHeight(50f);
        assertTrue(crop.hasEffect());
    }

    @Test
    public void hasEffectWithArguments() {
        Dimension fullSize = new Dimension(600, 400);
        OperationList opList = new OperationList(new Identifier("cats"), Format.JPG);

        instance.setWidth(600);
        instance.setHeight(400);
        assertFalse(instance.hasEffect(fullSize, opList));

        instance = new Crop();
        instance.setShape(Crop.Shape.SQUARE);
        assertTrue(instance.hasEffect(fullSize, opList));
    }

    @Test
    public void setHeight() {
        float height = 50f;
        this.instance.setHeight(height);
        assertEquals(height, this.instance.getHeight(), DELTA);
    }

    @Test
    public void setHeightWithNegativeHeight() {
        try {
            this.instance.setHeight(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void setHeightWithZeroHeight() {
        try {
            instance.setHeight(0f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void setHeightWithGreaterThan100PercentHeight() {
        try {
            instance.setUnit(Crop.Unit.PERCENT);
            instance.setHeight(1.2f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Height percentage must be <= 1", e.getMessage());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void setHeightThrowsExceptionWhenInstanceIsFrozen() {
        instance.freeze();
        instance.setHeight(30f);
    }

    @Test(expected = IllegalStateException.class)
    public void setShapeThrowsExceptionWhenInstanceIsFrozen() {
        instance.freeze();
        instance.setShape(Crop.Shape.SQUARE);
    }

    @Test(expected = IllegalStateException.class)
    public void setUnitThrowsExceptionWhenInstanceIsFrozen() {
        instance.freeze();
        instance.setUnit(Crop.Unit.PIXELS);
    }

    @Test
    public void setWidth() {
        Float width = 50f;
        instance.setWidth(width);
        assertEquals(width, this.instance.getWidth(), DELTA);
    }

    @Test
    public void setWidthWithNegativeWidth() {
        try {
            instance.setWidth(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void setWidthWithZeroWidth() {
        try {
            instance.setWidth(0f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void setWidthWithGreaterThan100PercentWidth() {
        try {
            instance.setUnit(Crop.Unit.PERCENT);
            instance.setWidth(1.2f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width percentage must be <= 1", e.getMessage());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void setWidthThrowsExceptionWhenInstanceIsFrozen() {
        instance.freeze();
        instance.setWidth(30f);
    }

    @Test
    public void setX() {
        float x = 50f;
        instance.setX(x);
        assertEquals(x, this.instance.getX(), DELTA);
    }

    @Test
    public void setXWithNegativeX() {
        try {
            instance.setX(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("X must be a positive float", e.getMessage());
        }
    }

    @Test
    public void setXWithGreaterThan100PercentX() {
        try {
            instance.setUnit(Crop.Unit.PERCENT);
            instance.setX(1.2f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("X percentage must be <= 1", e.getMessage());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void setXThrowsExceptionWhenInstanceIsFrozen() {
        instance.freeze();
        instance.setX(30f);
    }

    @Test
    public void setY() {
        float y = 50f;
        instance.setY(y);
        assertEquals(y, this.instance.getY(), DELTA);
    }

    @Test
    public void setYWithNegativeY() {
        try {
            instance.setY(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Y must be a positive float", e.getMessage());
        }
    }

    @Test
    public void setYWithGreaterThan100PercentY() {
        try {
            instance.setUnit(Crop.Unit.PERCENT);
            instance.setY(1.2f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Y percentage must be <= 1", e.getMessage());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void setYThrowsExceptionWhenInstanceIsFrozen() {
        instance.freeze();
        instance.setY(30f);
    }

    @Test
    public void testToMap() {
        final Crop crop = new Crop(25, 25, 50, 50);
        crop.setUnit(Crop.Unit.PIXELS);
        crop.setFull(false);

        final Dimension fullSize = new Dimension(100, 100);

        Map<String,Object> map = crop.toMap(fullSize);
        assertEquals(crop.getClass().getSimpleName(), map.get("class"));
        assertEquals(25, map.get("x"));
        assertEquals(25, map.get("y"));
        assertEquals(50, map.get("width"));
        assertEquals(50, map.get("height"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void toMapReturnsUnmodifiableMap() {
        Dimension fullSize = new Dimension(100, 100);
        Map<String,Object> map = instance.toMap(fullSize);
        map.put("test", "test");
    }

    @Test
    public void testToString() {
        // full
        Crop crop = new Crop();
        crop.setFull(true);
        assertEquals("none", crop.toString());

        // square
        crop = new Crop();
        crop.setShape(Crop.Shape.SQUARE);
        assertEquals("square", crop.toString());

        // pixels
        crop = new Crop();
        crop.setWidth(50f);
        crop.setHeight(40f);
        assertEquals("0,0,50,40", crop.toString());

        // percent
        crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setWidth(0.5f);
        crop.setHeight(0.4f);
        assertEquals("0%,0%,50%,40%", crop.toString());
    }

    @Test
    public void validateWithValidInstance() {
        Dimension fullSize = new Dimension(1000, 1000);
        instance.setWidth(100);
        instance.setHeight(100);
        instance.validate(fullSize);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithOutOfBoundsCropX() {
        Dimension fullSize = new Dimension(1000, 1000);
        Crop crop = new Crop(1001, 0, 5, 5);
        crop.validate(fullSize);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithOutOfBoundsCropY() {
        Dimension fullSize = new Dimension(1000, 1000);
        Crop crop = new Crop(0, 1001, 5, 5);
        crop.validate(fullSize);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithZeroDimensionCropX() {
        Dimension fullSize = new Dimension(1000, 1000);
        Crop crop = new Crop(1000, 0, 100, 100);
        crop.validate(fullSize);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateWithZeroDimensionCrop() {
        Dimension fullSize = new Dimension(1000, 1000);
        Crop crop = new Crop(0, 1000, 100, 100);
        crop.validate(fullSize);
    }

}
