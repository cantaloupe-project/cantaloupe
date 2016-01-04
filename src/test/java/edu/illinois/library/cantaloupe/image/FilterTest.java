package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;

import java.awt.Dimension;

public class FilterTest extends CantaloupeTestCase {

    public void testValues() {
        assertNotNull(Filter.valueOf("BITONAL"));
        assertNotNull(Filter.valueOf("GRAY"));
        assertEquals(2, Filter.values().length);
    }

    public void testGetEffectiveSize() {
        Dimension fullSize = new Dimension(200, 200);
        assertEquals(fullSize, Filter.BITONAL.getResultingSize(fullSize));
        assertEquals(fullSize, Filter.GRAY.getResultingSize(fullSize));
    }

    public void testIsNoOp() {
        assertFalse(Filter.BITONAL.isNoOp());
        assertFalse(Filter.GRAY.isNoOp());
    }

    public void testToString() {
        assertEquals("bitonal", Filter.BITONAL.toString());
        assertEquals("gray", Filter.GRAY.toString());
    }

}
