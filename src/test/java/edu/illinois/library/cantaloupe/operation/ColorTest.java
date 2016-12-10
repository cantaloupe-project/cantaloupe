package edu.illinois.library.cantaloupe.operation;

import org.junit.Test;

import java.awt.Dimension;
import java.util.Map;

import static org.junit.Assert.*;

public class ColorTest {

    @Test
    public void testValues() {
        assertNotNull(Color.valueOf("BITONAL"));
        assertNotNull(Color.valueOf("GRAY"));
        assertEquals(2, Color.values().length);
    }

    @Test
    public void testGetEffectiveSize() {
        Dimension fullSize = new Dimension(200, 200);
        assertEquals(fullSize, Color.BITONAL.getResultingSize(fullSize));
        assertEquals(fullSize, Color.GRAY.getResultingSize(fullSize));
    }

    @Test
    public void testIsNoOp() {
        assertFalse(Color.BITONAL.isNoOp());
        assertFalse(Color.GRAY.isNoOp());
    }

    @Test
    public void testIsNoOpWithArguments() {
        Dimension fullSize = new Dimension(600, 400);
        OperationList opList = new OperationList();
        opList.add(new Crop(0, 0, 300, 200));
        assertFalse(Color.BITONAL.isNoOp(fullSize, opList));
        assertFalse(Color.GRAY.isNoOp(fullSize, opList));
    }

    @Test
    public void testToMap() {
        Map<String,Object> map = Color.BITONAL.toMap(new Dimension(0, 0));
        assertEquals(Color.class.getSimpleName(), map.get("class"));
        assertEquals("bitonal", map.get("type"));
    }

    @Test
    public void testToString() {
        assertEquals("bitonal", Color.BITONAL.toString());
        assertEquals("gray", Color.GRAY.toString());
    }

}
