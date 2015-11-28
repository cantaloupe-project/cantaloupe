package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;

public class RotateTest extends CantaloupeTestCase {

    private Rotate rotate;

    public void setUp() {
        this.rotate = new Rotate();
        assertEquals(0f, this.rotate.getDegrees());
    }

    public void testIsNoOp() {
        assertTrue(rotate.isNoOp());
        rotate.setDegrees(30);
        assertFalse(rotate.isNoOp());
    }

    public void testSetDegrees() {
        float degrees = 50f;
        this.rotate.setDegrees(degrees);
        assertEquals(degrees, this.rotate.getDegrees());
    }

    public void testSetLargeDegrees() {
        float degrees = 530f;
        try {
            this.rotate.setDegrees(degrees);
        } catch (IllegalArgumentException e) {
            assertEquals("Degrees must be between 0 and 360", e.getMessage());
        }
    }

    public void testSetNegativeDegrees() {
        float degrees = -50f;
        try {
            this.rotate.setDegrees(degrees);
        } catch (IllegalArgumentException e) {
            assertEquals("Degrees must be between 0 and 360", e.getMessage());
        }
    }

    public void testToString() {
        Rotate r = new Rotate(50f);
        assertEquals("50", r.toString());
        r = new Rotate(50.5f);
        assertEquals("50.5", r.toString());
        r = new Rotate(50.5000f);
        assertEquals("50.5", r.toString());
    }

}
