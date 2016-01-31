package edu.illinois.library.cantaloupe.image;

import org.junit.Test;

import java.awt.Dimension;
import java.util.Map;

import static org.junit.Assert.*;

public class FilterTest {

    @Test
    public void testValues() {
        assertNotNull(Filter.valueOf("BITONAL"));
        assertNotNull(Filter.valueOf("GRAY"));
        assertEquals(2, Filter.values().length);
    }

    @Test
    public void testGetEffectiveSize() {
        Dimension fullSize = new Dimension(200, 200);
        assertEquals(fullSize, Filter.BITONAL.getResultingSize(fullSize));
        assertEquals(fullSize, Filter.GRAY.getResultingSize(fullSize));
    }

    @Test
    public void testIsNoOp() {
        assertFalse(Filter.BITONAL.isNoOp());
        assertFalse(Filter.GRAY.isNoOp());
    }

    @Test
    public void testToMap() {
        Map<String,Object> map = Filter.BITONAL.toMap(new Dimension(0, 0));
        assertEquals("filter", map.get("operation"));
        assertEquals("bitonal", map.get("type"));
    }

    @Test
    public void testToString() {
        assertEquals("bitonal", Filter.BITONAL.toString());
        assertEquals("gray", Filter.GRAY.toString());
    }

}
