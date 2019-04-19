package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CropToSquareTest extends CropTest {

    private CropToSquare instance;

    @Override
    protected CropToSquare newInstance() {
        return new CropToSquare();
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = newInstance();
    }

    @Test
    void testEqualsWithEqualInstances() {
        CropToSquare crop1 = new CropToSquare();
        CropToSquare crop2 = new CropToSquare();
        assertEquals(crop1, crop2);
    }

    @Test
    void testEqualsWithUnequalInstances() {
        // All instances are equal.
    }

    @Test
    void testGetRectangle1() {
        final Dimension fullSize = new Dimension(300, 200);
        final CropToSquare crop = new CropToSquare();
        assertEquals(new Rectangle(50, 0, 200, 200),
                crop.getRectangle(fullSize));
    }

    @Test
    void testGetRectangle2() {
        final Dimension fullSize = new Dimension(300, 200);
        final CropToSquare crop = new CropToSquare();

        // scale constraint 1:1
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        assertEquals(new Rectangle(50, 0, 200, 200),
                crop.getRectangle(fullSize, scaleConstraint));

        // scale constraint 1:2
        scaleConstraint = new ScaleConstraint(1, 2);
        assertEquals(new Rectangle(50, 0, 200, 200),
                crop.getRectangle(fullSize, scaleConstraint));
    }

    @Test
    void testGetRectangle3() {
        final ReductionFactor rf = new ReductionFactor(2);
        final Dimension reducedSize = new Dimension(300, 200);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 2);
        CropToSquare crop = new CropToSquare();
        assertEquals(new Rectangle(50, 0, 200, 200),
                crop.getRectangle(reducedSize, rf, scaleConstraint));
    }

    @Test
    void testGetResultingSize() {
        final Dimension fullSize = new Dimension(300, 200);
        final CropToSquare crop = new CropToSquare();

        // scale constraint 1:1
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        assertEquals(new Dimension(200, 200),
                crop.getResultingSize(fullSize, scaleConstraint));

        // scale constraint 1:2
        scaleConstraint = new ScaleConstraint(1, 2);
        assertEquals(new Dimension(200, 200),
                crop.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void testHasEffect() {
        CropToSquare crop = new CropToSquare();
        assertTrue(crop.hasEffect());
    }

    @Test
    void testHasEffectWithArguments() {
        // very different width & height
        Dimension fullSize = new Dimension(600, 400);
        OperationList opList = new OperationList();
        assertTrue(instance.hasEffect(fullSize, opList));

        // little bit different width & height
        fullSize = new Dimension(600.4, 600.3);
        opList = new OperationList();
        assertFalse(instance.hasEffect(fullSize, opList));

        // exact same width & height
        fullSize = new Dimension(600.00001, 600.00001);
        opList = new OperationList();
        assertFalse(instance.hasEffect(fullSize, opList));
    }

    @Test
    void testHashCode() {
        assertEquals(instance.toString().hashCode(), instance.hashCode());
    }

    @Test
    void testToMap() {
        final CropToSquare crop = new CropToSquare();
        final Dimension fullSize = new Dimension(150, 100);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        Map<String,Object> map = crop.toMap(fullSize, scaleConstraint);
        assertEquals(crop.getClass().getSimpleName(), map.get("class"));
        assertEquals(25, map.get("x"));
        assertEquals(0, map.get("y"));
        assertEquals(100, map.get("width"));
        assertEquals(100, map.get("height"));
    }

    @Test
    void testToMapReturnsUnmodifiableMap() {
        Dimension fullSize = new Dimension(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map = instance.toMap(fullSize, scaleConstraint);
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("test", "test"));
    }

    @Test
    void testToString() {
        assertEquals("square", instance.toString());
    }

    @Test
    void testValidate() throws Exception {
        // All instances are valid.
        Dimension fullSize = new Dimension(1000, 1000);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        instance.validate(fullSize, scaleConstraint);
    }

}
