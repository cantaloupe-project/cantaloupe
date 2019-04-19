package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CropByPixelsTest extends CropTest {

    private CropByPixels instance;

    @Override
    protected CropByPixels newInstance() {
        return new CropByPixels(0, 0, 1000, 1000);
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = newInstance();
    }

    @Test
    public void constructor() {
        instance = new CropByPixels(5, 10, 50, 80);
        assertEquals(5, instance.getX(), DELTA);
        assertEquals(10, instance.getY(), DELTA);
        assertEquals(50, instance.getWidth(), DELTA);
        assertEquals(80, instance.getHeight(), DELTA);
    }

    @Test
    public void equalsWithEqualInstances() {
        Crop crop1 = new CropByPixels(50, 50, 50, 50);
        Crop crop2 = new CropByPixels(50, 50, 50, 50);
        assertEquals(crop1, crop2);
    }

    @Test
    public void equalsWithUnequalX() {
        Crop crop1 = new CropByPixels(50, 50, 50, 50);
        Crop crop2 = new CropByPixels(51, 50, 50, 50);
        assertNotEquals(crop1, crop2);
    }

    @Test
    public void equalsWithUnequalY() {
        Crop crop1 = new CropByPixels(50, 50, 50, 50);
        Crop crop2 = new CropByPixels(50, 51, 50, 50);
        assertNotEquals(crop1, crop2);
    }

    @Test
    public void equalsWithUnequalWidth() {
        Crop crop1 = new CropByPixels(50, 50, 50, 50);
        Crop crop2 = new CropByPixels(50, 50, 51, 50);
        assertNotEquals(crop1, crop2);
    }

    @Test
    public void equalsWithUnequalHeight() {
        Crop crop1 = new CropByPixels(50, 50, 50, 50);
        Crop crop2 = new CropByPixels(50, 50, 50, 51);
        assertNotEquals(crop1, crop2);
    }

    @Test
    public void getRectangle1() {
        final Dimension fullSize = new Dimension(300, 200);
        Crop crop                = new CropByPixels(20, 20, 50, 50);
        assertEquals(new Rectangle(20, 20, 50, 50),
                crop.getRectangle(fullSize));
    }

    @Test
    public void getRectangle1DoesNotExceedFullSizeBounds() {
        final Dimension fullSize = new Dimension(300, 200);
        Crop crop                = new CropByPixels(200, 150, 100, 100);
        assertEquals(new Rectangle(200, 150, 100, 50),
                crop.getRectangle(fullSize));
    }

    @Test
    public void getRectangle2() {
        final Dimension fullSize = new Dimension(300, 200);
        Crop crop = new CropByPixels(20, 20, 50, 50);

        // scale constraint 1:1
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        assertEquals(new Rectangle(20, 20, 50, 50),
                crop.getRectangle(fullSize, sc));

        // scale constraint 1:2
        sc = new ScaleConstraint(1, 2);
        assertEquals(new Rectangle(40, 40, 100, 100),
                crop.getRectangle(fullSize, sc));
    }

    @Test
    public void getRectangle2DoesNotExceedFullSizeBounds() {
        final Dimension fullSize = new Dimension(1000, 800);

        // scale constraint 1:1
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        Crop crop          = new CropByPixels(400, 400, 700, 500);
        assertEquals(new Rectangle(400, 400, 600, 400),
                crop.getRectangle(fullSize, sc));

        // scale constraint 1:2
        sc   = new ScaleConstraint(1, 2);
        crop = new CropByPixels(200, 200, 350, 250);
        assertEquals(new Rectangle(400, 400, 600, 400),
                crop.getRectangle(fullSize, sc));
    }

    @Test
    public void getRectangle3WithLargerReductionThanConstraint() {
        final Dimension reducedSize = new Dimension(500, 500);
        final ReductionFactor rf    = new ReductionFactor(2);    // full: 2000x2000
        final ScaleConstraint sc    = new ScaleConstraint(1, 2); // 1000x1000
        final Crop crop             = new CropByPixels(100, 100, 200, 200);
        assertEquals(new Rectangle(50, 50, 100, 100),
                crop.getRectangle(reducedSize, rf, sc));
    }

    @Test
    public void getRectangle3WithSmallerReductionThanConstraint() {
        final Dimension reducedSize = new Dimension(500, 500);
        final ReductionFactor rf    = new ReductionFactor(1);    // full: 1000x1000
        final ScaleConstraint sc    = new ScaleConstraint(1, 4); // 250x250
        final Crop crop             = new CropByPixels(100, 100, 200, 200);
        assertEquals(new Rectangle(200, 200, 300, 300),
                crop.getRectangle(reducedSize, rf, sc));
    }

    @Test
    public void getRectangle3With90Orientation() {
        final Dimension reducedSize = new Dimension(500, 500);
        final ReductionFactor rf    = new ReductionFactor(2);    // full: 2000x2000
        final ScaleConstraint sc    = new ScaleConstraint(1, 2); // 1000x1000
        final Crop crop             = new CropByPixels(100, 100, 200, 200);
        crop.setOrientation(Orientation.ROTATE_90);

        assertEquals(new Rectangle(50, 350, 100, 100),
                crop.getRectangle(reducedSize, rf, sc));
    }

    @Test
    public void getRectangle3With180Orientation() {
        final Dimension reducedSize = new Dimension(500, 500);
        final ReductionFactor rf    = new ReductionFactor(2);    // full: 2000x2000
        final ScaleConstraint sc    = new ScaleConstraint(1, 2); // 1000x1000
        final Crop crop             = new CropByPixels(100, 100, 200, 200);
        crop.setOrientation(Orientation.ROTATE_180);

        assertEquals(new Rectangle(350, 350, 100, 100),
                crop.getRectangle(reducedSize, rf, sc));
    }

    @Test
    public void getRectangle3With270Orientation() {
        final Dimension reducedSize = new Dimension(500, 500);
        final ReductionFactor rf    = new ReductionFactor(2);    // full: 2000x2000
        final ScaleConstraint sc    = new ScaleConstraint(1, 2); // 1000x1000
        final Crop crop             = new CropByPixels(100, 100, 200, 200);
        crop.setOrientation(Orientation.ROTATE_270);

        assertEquals(new Rectangle(350, 50, 100, 100),
                crop.getRectangle(reducedSize, rf, sc));
    }

    @Test
    public void getRectangle3DoesNotExceedFullSizeBounds() {
        final Dimension reducedSize = new Dimension(300, 200);
        final ReductionFactor rf    = new ReductionFactor(2);    // full: 1200x800
        final ScaleConstraint sc    = new ScaleConstraint(1, 4); // 300x200
        final Crop crop             = new CropByPixels(200, 150, 100, 100);
        assertEquals(new Rectangle(200, 150, 100, 50),
                crop.getRectangle(reducedSize, rf, sc));
    }

    @Test
    public void getResultingSize() {
        final Dimension fullSize = new Dimension(200, 200);
        final Crop crop = new CropByPixels(20, 20, 50, 50);

        // scale constraint 1:1
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        assertEquals(new Dimension(50, 50),
                crop.getResultingSize(fullSize, sc));

        // scale constraint 1:2
        sc = new ScaleConstraint(1, 2);
        assertEquals(new Dimension(100, 100),
                crop.getResultingSize(fullSize, sc));
    }

    @Test
    public void hasEffect() {
        // new instance
        CropByPixels crop = new CropByPixels(0, 0, 1000, 1000);
        assertTrue(crop.hasEffect());

        crop.setWidth(50);
        crop.setHeight(50);
        assertTrue(crop.hasEffect());
    }

    @Test
    public void hasEffectWithArgumentsWithFullArea() {
        Dimension fullSize   = new Dimension(600, 400);
        OperationList opList = new OperationList();

        instance.setWidth(600);
        instance.setHeight(400);
        assertFalse(instance.hasEffect(fullSize, opList));
    }

    @Test
    public void hasEffectWithArgumentsWithGreaterThanFullArea() {
        Dimension fullSize   = new Dimension(600, 400);
        OperationList opList = new OperationList();

        instance.setWidth(800);
        instance.setHeight(600);
        assertFalse(instance.hasEffect(fullSize, opList));
    }

    @Test
    public void hasEffectWithArgumentsWithNonzeroOrigin() {
        Dimension fullSize   = new Dimension(600, 400);
        OperationList opList = new OperationList();

        instance.setX(5);
        instance.setY(5);
        instance.setWidth(595);
        instance.setHeight(395);
        assertTrue(instance.hasEffect(fullSize, opList));

        instance.setWidth(600);
        instance.setHeight(400);
        assertTrue(instance.hasEffect(fullSize, opList));
    }

    @Test
    public void testHashCode() {
        assertEquals(instance.toString().hashCode(), instance.hashCode());
    }

    @Test
    public void setHeight() {
        int height = 50;
        instance.setHeight(height);
        assertEquals(height, instance.getHeight());
    }

    @Test
    public void setHeightWithZeroHeight() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setHeight(0));
    }

    @Test
    public void setHeightWithNegativeHeight() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setHeight(-50));
    }

    @Test
    public void setHeightThrowsExceptionWhenInstanceIsFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setHeight(30));
    }

    @Test
    public void setWidth() {
        int width = 50;
        instance.setWidth(width);
        assertEquals(width, instance.getWidth());
    }

    @Test
    public void setWidthWithZeroWidth() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setWidth(0));
    }

    @Test
    public void setWidthWithNegativeWidth() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setWidth(-13));
    }

    @Test
    public void setWidthThrowsExceptionWhenInstanceIsFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class, () -> instance.setWidth(0));
    }

    @Test
    public void setX() {
        int x = 50;
        instance.setX(x);
        assertEquals(x, instance.getX());
    }

    @Test
    public void setXWithNegativeX() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setX(-50));
    }

    @Test
    public void setXThrowsExceptionWhenInstanceIsFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class, () -> instance.setX(30));
    }

    @Test
    public void setY() {
        int y = 50;
        instance.setY(y);
        assertEquals(y, instance.getY());
    }

    @Test
    public void setYWithNegativeY() {
        assertThrows(IllegalArgumentException.class, () -> instance.setY(-10));
    }

    @Test
    public void setYThrowsExceptionWhenInstanceIsFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class, () -> instance.setY(30));
    }

    @Test
    public void testToMap() {
        final Crop crop          = new CropByPixels(25, 25, 50, 50);
        final Dimension fullSize = new Dimension(100, 100);
        final ScaleConstraint sc = new ScaleConstraint(1, 1);

        Map<String,Object> map = crop.toMap(fullSize, sc);
        assertEquals(crop.getClass().getSimpleName(), map.get("class"));
        assertEquals(25, map.get("x"));
        assertEquals(25, map.get("y"));
        assertEquals(50, map.get("width"));
        assertEquals(50, map.get("height"));
    }

    @Test
    public void toMapReturnsUnmodifiableMap() {
        Dimension fullSize = new Dimension(100, 100);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        Map<String,Object> map = instance.toMap(fullSize, sc);
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("test", "test"));
    }

    @Test
    public void testToString() {
        instance.setX(10);
        instance.setY(10);
        instance.setWidth(50);
        instance.setHeight(40);
        assertEquals("10,10,50,40", instance.toString());
    }

    @Test
    public void validateWithValidInstance() throws Exception {
        Dimension fullSize = new Dimension(1000, 1000);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        instance.setWidth(100);
        instance.setHeight(100);
        instance.validate(fullSize, sc);
    }

    @Test
    public void validateWithOutOfBoundsX() {
        Dimension fullSize = new Dimension(1000, 1000);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        Crop crop          = new CropByPixels(1001, 0, 5, 5);
        assertThrows(ValidationException.class,
                () -> crop.validate(fullSize, sc));
    }

    @Test
    public void validateWithOutOfBoundsY() {
        Dimension fullSize = new Dimension(1000, 1000);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        Crop crop          = new CropByPixels(0, 1001, 5, 5);
        assertThrows(ValidationException.class,
                () -> crop.validate(fullSize, sc));
    }

    @Test
    public void validateWithZeroDimensionX() {
        Dimension fullSize = new Dimension(1000, 1000);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        Crop crop          = new CropByPixels(1000, 0, 100, 100);
        assertThrows(ValidationException.class,
                () -> crop.validate(fullSize, sc));
    }

    @Test
    public void validateWithZeroDimensions() {
        Dimension fullSize = new Dimension(1000, 1000);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        Crop crop          = new CropByPixels(0, 1000, 100, 100);
        assertThrows(ValidationException.class,
                () -> crop.validate(fullSize, sc));
    }

}
