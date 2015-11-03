package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;

public class RotationTest extends CantaloupeTestCase {

    private Rotation rotation;

    public void setUp() {
        this.rotation = new Rotation();
        assertEquals(0f, this.rotation.getDegrees());
        assertFalse(this.rotation.shouldMirror());
    }

    /* degrees */

    public void testSetDegrees() {
        Float degrees = new Float(50.0);
        this.rotation.setDegrees(degrees);
        assertEquals(degrees, this.rotation.getDegrees());
    }

    public void testSetLargeDegrees() {
        Float degrees = new Float(530.0);
        try {
            this.rotation.setDegrees(degrees);
        } catch (IllegalArgumentException e) {
            assertEquals("Degrees must be between 0 and 360", e.getMessage());
        }
    }

    public void testSetNegativeDegrees() {
        Float degrees = new Float(-50.0);
        try {
            this.rotation.setDegrees(degrees);
        } catch (IllegalArgumentException e) {
            assertEquals("Degrees must be between 0 and 360", e.getMessage());
        }
    }

    public void testToString() {
        Rotation r = new Rotation(50f);
        assertEquals("50", r.toString());

        r.setMirror(true);
        assertEquals("!50", r.toString());
    }

}
