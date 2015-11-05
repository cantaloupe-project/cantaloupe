package edu.illinois.library.cantaloupe.resource.iiif.v1_1;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;

public class RotationTest extends CantaloupeTestCase {

    private Rotation rotation;

    public void setUp() {
        this.rotation = new Rotation();
        assertEquals(0f, this.rotation.getDegrees());
    }

    /* fromUri(String) */

    /**
     * Tests fromUri(String) with a legal value.
     */
    public void testFromUri() {
        Rotation r = Rotation.fromUri("35");
        assertEquals(35f, r.getDegrees());

        r = Rotation.fromUri("35.5");
        assertEquals(35.5f, r.getDegrees());
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

    public void testCompareTo() {
        Rotation r2 = new Rotation();
        assertEquals(0, this.rotation.compareTo(r2));
        r2.setDegrees(15);
        assertEquals(-1, this.rotation.compareTo(r2));
        r2.setDegrees(0);
        this.rotation.setDegrees(30);
        assertEquals(1, this.rotation.compareTo(r2));
    }

    public void testEquals() {
        Rotation r2 = new Rotation();
        assertTrue(r2.equals(this.rotation));
        r2.setDegrees(15);
        assertFalse(r2.equals(this.rotation));
    }

    /* setDegrees() */

    public void testSetDegrees() {
        float degrees = 50.0f;
        this.rotation.setDegrees(degrees);
        assertEquals(degrees, this.rotation.getDegrees());
    }

    public void testSetLargeDegrees() {
        float degrees = 530.0f;
        try {
            this.rotation.setDegrees(degrees);
        } catch (IllegalArgumentException e) {
            assertEquals("Degrees must be between 0 and 360", e.getMessage());
        }
    }

    public void testSetNegativeDegrees() {
        float degrees = -50.0f;
        try {
            this.rotation.setDegrees(degrees);
        } catch (IllegalArgumentException e) {
            assertEquals("Degrees must be between 0 and 360", e.getMessage());
        }
    }

    public void testToRotation() {
        edu.illinois.library.cantaloupe.image.Rotation actual =
                new edu.illinois.library.cantaloupe.image.Rotation(this.rotation.getDegrees());
        assertTrue(this.rotation.equals(this.rotation.toRotation()));
    }

    public void testToString() {
        Rotation r = Rotation.fromUri("50");
        assertEquals("50", r.toString());

        r = Rotation.fromUri("50.50");
        assertEquals("50.5", r.toString());
    }

}
