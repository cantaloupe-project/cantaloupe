package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;

import java.awt.Dimension;

public class TransposeTest extends CantaloupeTestCase {

    private Transpose transpose;

    public void setUp() {
        this.transpose = Transpose.HORIZONTAL;
    }

    public void testGetEffectiveSize() {
        Dimension fullSize = new Dimension(200, 200);
        assertEquals(fullSize, Transpose.VERTICAL.getResultingSize(fullSize));
        assertEquals(fullSize, Transpose.HORIZONTAL.getResultingSize(fullSize));
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
