package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import java.awt.Dimension;
import java.util.Map;

import static org.junit.Assert.*;

public class ColorTransformTest extends BaseTest {

    @Test
    public void testValues() {
        assertEquals(2, ColorTransform.values().length);
        assertNotNull(ColorTransform.BITONAL);
        assertNotNull(ColorTransform.GRAY);
    }

    @Test
    public void testGetEffectiveSize() {
        Dimension fullSize = new Dimension(200, 200);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 2);
        assertSame(fullSize,
                ColorTransform.BITONAL.getResultingSize(fullSize, scaleConstraint));
        assertSame(fullSize,
                ColorTransform.GRAY.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    public void testHasEffect() {
        assertTrue(ColorTransform.BITONAL.hasEffect());
        assertTrue(ColorTransform.GRAY.hasEffect());
    }

    @Test
    public void testHasEffectWithArguments() {
        Dimension fullSize = new Dimension(600, 400);
        OperationList opList = new OperationList(new Crop(0, 0, 300, 200));

        assertTrue(ColorTransform.BITONAL.hasEffect(fullSize, opList));
        assertTrue(ColorTransform.GRAY.hasEffect(fullSize, opList));
    }

    @Test
    public void testToMap() {
        Dimension size = new Dimension(0, 0);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map = ColorTransform.BITONAL.toMap(size, scaleConstraint);

        assertEquals(ColorTransform.class.getSimpleName(), map.get("class"));
        assertEquals("bitonal", map.get("type"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void toMapReturnsUnmodifiableMap() {
        Dimension fullSize = new Dimension(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map = ColorTransform.GRAY.toMap(fullSize, scaleConstraint);
        map.put("test", "test");
    }

    @Test
    public void testToString() {
        assertEquals("bitonal", ColorTransform.BITONAL.toString());
        assertEquals("gray", ColorTransform.GRAY.toString());
    }

}
