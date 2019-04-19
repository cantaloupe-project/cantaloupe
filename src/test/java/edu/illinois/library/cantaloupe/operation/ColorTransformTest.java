package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ColorTransformTest extends BaseTest {

    @Test
    void testValues() {
        assertEquals(2, ColorTransform.values().length);
        assertNotNull(ColorTransform.BITONAL);
        assertNotNull(ColorTransform.GRAY);
    }

    @Test
    void testGetEffectiveSize() {
        Dimension fullSize = new Dimension(200, 200);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 2);
        assertSame(fullSize,
                ColorTransform.BITONAL.getResultingSize(fullSize, scaleConstraint));
        assertSame(fullSize,
                ColorTransform.GRAY.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void testHasEffect() {
        assertTrue(ColorTransform.BITONAL.hasEffect());
        assertTrue(ColorTransform.GRAY.hasEffect());
    }

    @Test
    void testHasEffectWithArguments() {
        Dimension fullSize = new Dimension(600, 400);
        OperationList opList = new OperationList(new CropByPixels(0, 0, 300, 200));

        assertTrue(ColorTransform.BITONAL.hasEffect(fullSize, opList));
        assertTrue(ColorTransform.GRAY.hasEffect(fullSize, opList));
    }

    @Test
    void testToMap() {
        Dimension size = new Dimension(0, 0);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map = ColorTransform.BITONAL.toMap(size, scaleConstraint);

        assertEquals(ColorTransform.class.getSimpleName(), map.get("class"));
        assertEquals("bitonal", map.get("type"));
    }

    @Test
    public void toMapReturnsUnmodifiableMap() {
        Dimension fullSize = new Dimension(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map = ColorTransform.GRAY.toMap(fullSize, scaleConstraint);
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("test", "test"));
    }

    @Test
    void testToString() {
        assertEquals("bitonal", ColorTransform.BITONAL.toString());
        assertEquals("gray", ColorTransform.GRAY.toString());
    }

}
