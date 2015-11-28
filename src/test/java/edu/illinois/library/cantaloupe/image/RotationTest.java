package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;

public class RotationTest extends CantaloupeTestCase {

    private Rotation rotation;

    public void setUp() {
        this.rotation = new Rotation();
        assertEquals(0f, this.rotation.getDegrees());
    }

    public void testIsNoOp() {
        assertTrue(rotation.isNoOp());
        rotation.setDegrees(30);
        assertFalse(rotation.isNoOp());
    }

    public void testSetDegrees() {
        float degrees = 50f;
        this.rotation.setDegrees(degrees);
        assertEquals(degrees, this.rotation.getDegrees());
    }

    public void testSetLargeDegrees() {
        float degrees = 530f;
        try {
            this.rotation.setDegrees(degrees);
        } catch (IllegalArgumentException e) {
            assertEquals("Degrees must be between 0 and 360", e.getMessage());
        }
    }

    public void testSetNegativeDegrees() {
        float degrees = -50f;
        try {
            this.rotation.setDegrees(degrees);
        } catch (IllegalArgumentException e) {
            assertEquals("Degrees must be between 0 and 360", e.getMessage());
        }
    }

    public void testToString() {
        Rotation r = new Rotation(50f);
        assertEquals("50", r.toString());
        r = new Rotation(50.5f);
        assertEquals("50.5", r.toString());
        r = new Rotation(50.5000f);
        assertEquals("50.5", r.toString());
    }

}
