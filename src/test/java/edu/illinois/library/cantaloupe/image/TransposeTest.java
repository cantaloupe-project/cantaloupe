package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;

public class TransposeTest extends CantaloupeTestCase {

    private Transpose transpose;

    public void setUp() {
        this.transpose = new Transpose();
        assertEquals(Transpose.Axis.HORIZONTAL, this.transpose.getAxis());
    }

    public void testEquals() {
        Transpose t2 = new Transpose();
        t2.setAxis(Transpose.Axis.HORIZONTAL);
        assertTrue(transpose.equals(t2));
        t2.setAxis(Transpose.Axis.VERTICAL);
        assertFalse(transpose.equals(t2));
        t2.setAxis(null);
        assertFalse(transpose.equals(t2));
    }

    public void testIsNoOp() {
        transpose.setAxis(Transpose.Axis.HORIZONTAL);
        assertFalse(transpose.isNoOp());
        transpose.setAxis(Transpose.Axis.VERTICAL);
        assertFalse(transpose.isNoOp());
        transpose.setAxis(null);
        assertTrue(transpose.isNoOp());
    }

    public void testToString() {
        transpose.setAxis(Transpose.Axis.HORIZONTAL);
        assertEquals("h", transpose.toString());
        transpose = new Transpose();
        transpose.setAxis(Transpose.Axis.VERTICAL);
        assertEquals("v", transpose.toString());
    }

}
