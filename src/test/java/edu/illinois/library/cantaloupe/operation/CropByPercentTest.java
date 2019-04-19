package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CropByPercentTest extends CropTest {

    private CropByPercent instance;

    @Override
    protected CropByPercent newInstance() {
        return new CropByPercent();
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = newInstance();
    }

    @Test
    public void constructor1() {
        instance = new CropByPercent();
        assertEquals(0, instance.getX(), DELTA);
        assertEquals(0, instance.getY(), DELTA);
        assertEquals(1, instance.getWidth(), DELTA);
        assertEquals(1, instance.getHeight(), DELTA);
    }

    @Test
    public void constructor2() {
        instance = new CropByPercent(0.02, 0.05, 0.5, 0.8);
        assertEquals(0.02, instance.getX(), DELTA);
        assertEquals(0.05, instance.getY(), DELTA);
        assertEquals(0.5, instance.getWidth(), DELTA);
        assertEquals(0.8, instance.getHeight(), DELTA);
    }

    @Test
    public void equalsWithEqualInstances() {
        Crop crop1 = new CropByPercent(0.2, 0.2, 0.5, 0.5);
        Crop crop2 = new CropByPercent(0.2, 0.2, 0.5, 0.5);
        assertEquals(crop1, crop2);
    }

    @Test
    public void equalsWithUnequalX() {
        Crop crop1 = new CropByPercent(0.2, 0.2, 0.5, 0.5);
        Crop crop2 = new CropByPercent(0.3, 0.2, 0.5, 0.5);
        assertNotEquals(crop1, crop2);
    }

    @Test
    public void equalsWithUnequalY() {
        Crop crop1 = new CropByPercent(0.2, 0.2, 0.5, 0.5);
        Crop crop2 = new CropByPercent(0.2, 0.3, 0.5, 0.5);
        assertNotEquals(crop1, crop2);
    }

    @Test
    public void equalsWithUnequalWidth() {
        Crop crop1 = new CropByPercent(0.2, 0.2, 0.5, 0.5);
        Crop crop2 = new CropByPercent(0.2, 0.2, 0.7, 0.5);
        assertNotEquals(crop1, crop2);
    }

    @Test
    public void equalsWithUnequalHeight() {
        Crop crop1 = new CropByPercent(0.2, 0.2, 0.5, 0.5);
        Crop crop2 = new CropByPercent(0.2, 0.2, 0.5, 0.7);
        assertNotEquals(crop1, crop2);
    }

    @Test
    public void getRectangle1() {
        final Dimension fullSize = new Dimension(1000, 500);
        Crop crop                = new CropByPercent(0.2, 0.2, 0.5, 0.5);
        assertEquals(new Rectangle(200, 100, 500, 250),
                crop.getRectangle(fullSize));
    }

    @Test
    public void getRectangle1DoesNotExceedFullSizeBounds() {
        final Dimension fullSize = new Dimension(1000, 500);
        Crop crop                = new CropByPercent(0.8, 0.8, 0.5, 0.5);
        assertEquals(new Rectangle(800, 400, 200, 100),
                crop.getRectangle(fullSize));
    }

    @Test
    public void getRectangle2() {
        final Dimension fullSize = new Dimension(1000, 500);
        final Crop crop          = new CropByPercent(0.2, 0.2, 0.5, 0.5);

        // scale constraint 1:1
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        assertEquals(new Rectangle(200, 100, 500, 250),
                crop.getRectangle(fullSize, sc));

        // scale constraint 1:2
        sc = new ScaleConstraint(1, 2);
        assertEquals(new Rectangle(200, 100, 500, 250),
                crop.getRectangle(fullSize, sc));
    }

    @Test
    public void getRectangle2DoesNotExceedFullSizeBounds() {
        final Dimension fullSize = new Dimension(1000, 500);
        final Crop crop = new CropByPercent(0.8, 0.8, 0.5, 0.5);

        // scale constraint 1:1
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        assertEquals(new Rectangle(800, 400, 200, 100),
                crop.getRectangle(fullSize, sc));

        // scale constraint 1:2
        sc = new ScaleConstraint(1, 2);
        assertEquals(new Rectangle(800, 400, 200, 100),
                crop.getRectangle(fullSize, sc));
    }

    @Test
    public void getRectangle3WithLargerReductionThanConstraint() {
        final Dimension reducedSize = new Dimension(300, 200);
        final ReductionFactor rf    = new ReductionFactor(2);
        final ScaleConstraint sc    = new ScaleConstraint(1, 2);
        final Crop crop             = new CropByPercent(0.2, 0.2, 0.5, 0.5);

        assertEquals(new Rectangle(60, 40, 150, 100),
                crop.getRectangle(reducedSize, rf, sc));
    }

    @Test
    public void getRectangle3WithSmallerReductionThanConstraint() {
        final Dimension reducedSize = new Dimension(300, 200);
        final ReductionFactor rf    = new ReductionFactor(1);
        final ScaleConstraint sc    = new ScaleConstraint(1, 4);
        final Crop crop             = new CropByPercent(0.2, 0.2, 0.5, 0.5);

        assertEquals(new Rectangle(60, 40, 150, 100),
                crop.getRectangle(reducedSize, rf, sc));
    }

    @Test
    public void getRectangle3With90Orientation() {
        final ReductionFactor rf    = new ReductionFactor(2);
        final Dimension reducedSize = new Dimension(300, 200);
        final ScaleConstraint sc    = new ScaleConstraint(1, 2);
        final Crop crop             = new CropByPercent(0.2, 0.2, 0.5, 0.5);
        crop.setOrientation(Orientation.ROTATE_90);

        assertEquals(new Rectangle(60, 60, 150, 100),
                crop.getRectangle(reducedSize, rf, sc));
    }

    @Test
    public void getRectangle3With180Orientation() {
        final ReductionFactor rf    = new ReductionFactor(2);
        final Dimension reducedSize = new Dimension(300, 200);
        final ScaleConstraint sc    = new ScaleConstraint(1, 2);
        final Crop crop             = new CropByPercent(0.2, 0.2, 0.5, 0.5);
        crop.setOrientation(Orientation.ROTATE_180);

        assertEquals(new Rectangle(90, 60, 150, 100),
                crop.getRectangle(reducedSize, rf, sc));
    }

    @Test
    public void getRectangle3With270Orientation() {
        final ReductionFactor rf    = new ReductionFactor(2);
        final Dimension reducedSize = new Dimension(300, 200);
        final ScaleConstraint sc    = new ScaleConstraint(1, 2);
        final Crop crop             = new CropByPercent(0.2, 0.2, 0.5, 0.5);
        crop.setOrientation(Orientation.ROTATE_270);

        assertEquals(new Rectangle(90, 40, 150, 100),
                crop.getRectangle(reducedSize, rf, sc));
    }

    @Test
    public void getRectangle3DoesNotExceedFullSizeBounds() {
        final ReductionFactor rf    = new ReductionFactor(2);
        final Dimension reducedSize = new Dimension(1000, 500);
        final ScaleConstraint sc    = new ScaleConstraint(1, 4);
        final Crop crop             = new CropByPercent(0.6, 0.6, 0.8, 0.8);
        assertEquals(new Rectangle(600, 300, 400, 200),
                crop.getRectangle(reducedSize, rf, sc));
    }

    @Test
    public void getResultingSize() {
        final Crop crop          = new CropByPercent(0, 0, 0.5, 0.5);
        final Dimension fullSize = new Dimension(1000, 1000);

        // scale constraint 1:1
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        assertEquals(new Dimension(500, 500),
                crop.getResultingSize(fullSize, sc));

        // scale constraint 1:2
        sc = new ScaleConstraint(1, 2);
        assertEquals(new Dimension(500, 500),
                crop.getResultingSize(fullSize, sc));
    }

    @Test
    public void hasEffect() {
        // new instance
        CropByPercent crop = new CropByPercent();
        assertFalse(crop.hasEffect());

        // 0% origin, 100% dimensions
        crop = new CropByPercent();
        crop.setWidth(1);
        crop.setHeight(1);
        assertFalse(crop.hasEffect());

        // 0% origin, <100% dimensions
        crop = new CropByPercent();
        crop.setWidth(0.8);
        crop.setHeight(0.8);
        assertTrue(crop.hasEffect());

        // >0% origin, 100% dimensions
        crop = new CropByPercent();
        crop.setX(0.1);
        crop.setY(0.1);
        assertTrue(crop.hasEffect());

        // >0% origin, <100% dimensions
        crop = new CropByPercent();
        crop.setX(0.1);
        crop.setY(0.1);
        crop.setWidth(0.9);
        crop.setHeight(0.9);
        assertTrue(crop.hasEffect());
    }

    @Test
    public void hasEffectWithArguments() {
        Dimension fullSize = new Dimension(600, 400);
        OperationList opList = new OperationList();

        // new instance
        CropByPercent crop = new CropByPercent();
        assertFalse(crop.hasEffect(fullSize, opList));

        // 0% origin, 100% dimensions
        crop = new CropByPercent();
        crop.setWidth(1);
        crop.setHeight(1);
        assertFalse(crop.hasEffect(fullSize, opList));

        // 0% origin, <100% dimensions
        crop = new CropByPercent();
        crop.setWidth(0.8);
        crop.setHeight(0.8);
        assertTrue(crop.hasEffect(fullSize, opList));

        // >0% origin, 100% dimensions
        crop = new CropByPercent();
        crop.setX(0.1);
        crop.setY(0.1);
        assertTrue(crop.hasEffect(fullSize, opList));

        // >0% origin, <100% dimensions
        crop = new CropByPercent();
        crop.setX(0.1);
        crop.setY(0.1);
        crop.setWidth(0.9);
        crop.setHeight(0.9);
        assertTrue(crop.hasEffect(fullSize, opList));
    }

    @Test
    public void testHashCode() {
        assertEquals(instance.toString().hashCode(), instance.hashCode());
    }

    @Test
    public void setHeight() {
        double height = 0.5;
        instance.setHeight(height);
        assertEquals(height, instance.getHeight(), DELTA);
    }

    @Test
    public void setHeightWithNegativeHeight() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setHeight(-0.5));
    }

    @Test
    public void setHeightWithZeroHeight() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setHeight(0));
    }

    @Test
    public void setHeightWithGreaterThan100PercentHeight() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setHeight(1.2));
    }

    @Test
    public void setHeightThrowsExceptionWhenInstanceIsFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setHeight(0.3));
    }

    @Test
    public void setWidth() {
        double width = 0.5;
        instance.setWidth(width);
        assertEquals(width, this.instance.getWidth(), DELTA);
    }

    @Test
    public void setWidthWithNegativeWidth() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setWidth(-0.5));
    }

    @Test
    public void setWidthWithZeroWidth() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setWidth(0));
    }

    @Test
    public void setWidthWithGreaterThan100PercentWidth() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setWidth(1.2));
    }

    @Test
    public void setWidthThrowsExceptionWhenInstanceIsFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setWidth(0.5));
    }

    @Test
    public void setX() {
        double x = 0.5;
        instance.setX(x);
        assertEquals(x, instance.getX(), DELTA);
    }

    @Test
    public void setXWithNegativeX() {
        assertThrows(IllegalArgumentException.class, () -> instance.setX(-0.5));
    }

    @Test
    public void setXWithGreaterThan100PercentX() {
        assertThrows(IllegalArgumentException.class, () -> instance.setX(1.2));
    }

    @Test
    public void setXThrowsExceptionWhenInstanceIsFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class, () -> instance.setX(0.5));
    }

    @Test
    public void setY() {
        double y = 0.5;
        instance.setY(y);
        assertEquals(y, instance.getY(), DELTA);
    }

    @Test
    public void setYWithNegativeY() {
        assertThrows(IllegalArgumentException.class, () -> instance.setY(-0.5));
    }

    @Test
    public void setYWithGreaterThan100PercentY() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setY(1.2));
    }

    @Test
    public void setYThrowsExceptionWhenInstanceIsFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class, () -> instance.setY(0.5));
    }

    @Test
    public void testToMap() {
        final Crop crop          = new CropByPercent(0.1, 0.2, 0.4, 0.5);
        final Dimension fullSize = new Dimension(100, 100);
        final ScaleConstraint sc = new ScaleConstraint(1, 1);

        Map<String,Object> map = crop.toMap(fullSize, sc);
        assertEquals(crop.getClass().getSimpleName(), map.get("class"));
        assertEquals(10, map.get("x"));
        assertEquals(20, map.get("y"));
        assertEquals(40, map.get("width"));
        assertEquals(50, map.get("height"));
    }

    @Test
    public void toMapReturnsUnmodifiableMap() {
        Dimension fullSize     = new Dimension(100, 100);
        ScaleConstraint sc     = new ScaleConstraint(1, 1);
        Map<String,Object> map = instance.toMap(fullSize, sc);
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("test", "test"));
    }

    @Test
    public void testToString() {
        instance.setX(0.1);
        instance.setY(0.2);
        instance.setWidth(0.5);
        instance.setHeight(0.4);
        assertEquals("0.1,0.2,0.5,0.4", instance.toString());
    }

    @Test
    public void validateWithValidInstance() throws Exception {
        Dimension fullSize = new Dimension(1000, 1000);
        ScaleConstraint sc = new ScaleConstraint(1, 1);
        instance.setWidth(0.1);
        instance.setHeight(0.1);
        instance.validate(fullSize, sc);
    }

}
