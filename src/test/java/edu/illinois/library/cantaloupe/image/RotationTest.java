package edu.illinois.library.cantaloupe.image;

import junit.framework.TestCase;

public class RotationTest extends TestCase {

    private Rotation rotation;

    public void setUp() {
        this.rotation = new Rotation();
    }

    /* fromUri(String) */

    /**
     * Tests fromUri(String) with a legal value.
     */
    public void testFromUri() {
        Rotation r = Rotation.fromUri("35");
        assertEquals(new Float(35), r.getDegrees());
        assertFalse(r.shouldMirror());

        r = Rotation.fromUri("!35");
        assertEquals(new Float(35), r.getDegrees());
        assertTrue(r.shouldMirror());
    }

    /**
     * Tests fromUri(String) with a large value.
     */
    public void testFromUriWithLargeValue() {
        try {
            Rotation.fromUri("720");
        } catch (IllegalArgumentException e) {
            assertEquals("Degrees must be between 0 and 360", e.getMessage());
        }
    }

    /**
     * Tests fromUri(String) with a negative value.
     */
    public void testFromUriWithNegativeValue() {
        try {
            Rotation.fromUri("-35");
        } catch (IllegalArgumentException e) {
            assertEquals("Degrees must be between 0 and 360", e.getMessage());
        }
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

}
