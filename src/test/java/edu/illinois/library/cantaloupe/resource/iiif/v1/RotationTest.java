package edu.illinois.library.cantaloupe.resource.iiif.v1;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RotationTest {

    private static final float FUDGE = 0.0000001f;

    private Rotation rotation;

    @Before
    public void setUp() {
        this.rotation = new Rotation();
        assertEquals(0f, this.rotation.getDegrees(), FUDGE);
    }

    /* fromUri(String) */

    /**
     * Tests fromUri(String) with a legal value.
     */
    @Test
    public void testFromUri() {
        Rotation r = Rotation.fromUri("35");
        assertEquals(35f, r.getDegrees());

        r = Rotation.fromUri("35.5");
        assertEquals(35.5f, r.getDegrees());
    }

    /**
     * Tests fromUri(String) with a large value.
     */
    @Test
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
    @Test
    public void testFromUriWithNegativeValue() {
        try {
            Rotation.fromUri("-35");
        } catch (IllegalArgumentException e) {
            assertEquals("Degrees must be between 0 and 360", e.getMessage());
        }
    }

    @Test
    public void testCompareTo() {
        Rotation r2 = new Rotation();
        assertEquals(0, this.rotation.compareTo(r2));
        r2.setDegrees(15);
        assertEquals(-1, this.rotation.compareTo(r2));
        r2.setDegrees(0);
        this.rotation.setDegrees(30);
        assertEquals(1, this.rotation.compareTo(r2));
    }

    @Test
    public void testEquals() {
        Rotation r2 = new Rotation();
        assertTrue(r2.equals(this.rotation));
        r2.setDegrees(15);
        assertFalse(r2.equals(this.rotation));
    }

    /* setDegrees() */

    @Test
    public void testSetDegrees() {
        float degrees = 50.0f;
        this.rotation.setDegrees(degrees);
        assertEquals(degrees, this.rotation.getDegrees());
    }

    @Test
    public void testSetLargeDegrees() {
        float degrees = 530.0f;
        try {
            this.rotation.setDegrees(degrees);
        } catch (IllegalArgumentException e) {
            assertEquals("Degrees must be between 0 and 360", e.getMessage());
        }
    }

    @Test
    public void testSetNegativeDegrees() {
        float degrees = -50.0f;
        try {
            this.rotation.setDegrees(degrees);
        } catch (IllegalArgumentException e) {
            assertEquals("Degrees must be between 0 and 360", e.getMessage());
        }
    }

    @Test
    public void testToRotate() {
        assertTrue(this.rotation.equals(this.rotation.toRotate()));
    }

    @Test
    public void testToString() {
        Rotation r = Rotation.fromUri("50");
        assertEquals("50", r.toString());

        r = Rotation.fromUri("50.50");
        assertEquals("50.5", r.toString());
    }

}
