package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;

public class QualityTest extends CantaloupeTestCase {

    public void testValues() {
        assertNotNull(Quality.valueOf("BITONAL"));
        assertNotNull(Quality.valueOf("COLOR"));
        assertNotNull(Quality.valueOf("DEFAULT"));
        assertNotNull(Quality.valueOf("GRAY"));
    }

    public void testIsNoOp() {
        assertFalse(Quality.BITONAL.isNoOp());
        assertTrue(Quality.COLOR.isNoOp());
        assertTrue(Quality.DEFAULT.isNoOp());
        assertFalse(Quality.GRAY.isNoOp());
    }

    public void testToString() {
        assertEquals("bitonal", Quality.BITONAL.toString());
        assertEquals("color", Quality.COLOR.toString());
        assertEquals("default", Quality.DEFAULT.toString());
        assertEquals("gray", Quality.GRAY.toString());
    }

}
