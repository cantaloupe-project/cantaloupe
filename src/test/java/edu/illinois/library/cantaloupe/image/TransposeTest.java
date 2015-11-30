package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;

public class TransposeTest extends CantaloupeTestCase {

    private Transpose transpose;

    public void setUp() {
        this.transpose = Transpose.HORIZONTAL;
    }

    public void testIsNoOp() {
        assertFalse(transpose.isNoOp());
    }

    public void testToString() {
        transpose = Transpose.HORIZONTAL;
        assertEquals("h", transpose.toString());
        transpose = Transpose.VERTICAL;
        assertEquals("v", transpose.toString());
    }

}
