package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;

public class FilterTest extends CantaloupeTestCase {

    public void testValues() {
        assertNotNull(Filter.valueOf("BITONAL"));
        assertNotNull(Filter.valueOf("DEFAULT"));
        assertNotNull(Filter.valueOf("GRAY"));
        assertEquals(3, Filter.values().length);
    }

    public void testIsNoOp() {
        assertFalse(Filter.BITONAL.isNoOp());
        assertTrue(Filter.DEFAULT.isNoOp());
        assertFalse(Filter.GRAY.isNoOp());
    }

    public void testToString() {
        assertEquals("bitonal", Filter.BITONAL.toString());
        assertEquals("default", Filter.DEFAULT.toString());
        assertEquals("gray", Filter.GRAY.toString());
    }

}
