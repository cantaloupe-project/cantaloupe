package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import java.awt.Dimension;
import java.util.Map;

import static org.junit.Assert.*;

public class ColorTransformTest extends BaseTest {

    @Test
    public void testValues() {
        assertNotNull(ColorTransform.valueOf("BITONAL"));
        assertNotNull(ColorTransform.valueOf("GRAY"));
        assertEquals(2, ColorTransform.values().length);
    }

    @Test
    public void testGetEffectiveSize() {
        Dimension fullSize = new Dimension(200, 200);
        assertEquals(fullSize, ColorTransform.BITONAL.getResultingSize(fullSize));
        assertEquals(fullSize, ColorTransform.GRAY.getResultingSize(fullSize));
    }

    @Test
    public void testHasEffect() {
        assertTrue(ColorTransform.BITONAL.hasEffect());
        assertTrue(ColorTransform.GRAY.hasEffect());
    }

    @Test
    public void testHasEffectWithArguments() {
        Dimension fullSize = new Dimension(600, 400);
        OperationList opList = new OperationList(
                new Identifier("cats"),
                Format.JPG,
                new Crop(0, 0, 300, 200));
        assertTrue(ColorTransform.BITONAL.hasEffect(fullSize, opList));
        assertTrue(ColorTransform.GRAY.hasEffect(fullSize, opList));
    }

    @Test
    public void testToMap() {
        Map<String,Object> map = ColorTransform.BITONAL.toMap(new Dimension(0, 0));
        assertEquals(ColorTransform.class.getSimpleName(), map.get("class"));
        assertEquals("bitonal", map.get("type"));
    }

    @Test
    public void testToString() {
        assertEquals("bitonal", ColorTransform.BITONAL.toString());
        assertEquals("gray", ColorTransform.GRAY.toString());
    }

}
