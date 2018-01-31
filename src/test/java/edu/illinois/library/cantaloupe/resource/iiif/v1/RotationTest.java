package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RotationTest extends BaseTest {

    private static final float DELTA = 0.0000001f;

    private Rotation instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        this.instance = new Rotation();
        assertEquals(0f, this.instance.getDegrees(), DELTA);
    }

    /* fromUri(String) */

    /**
     * Tests fromUri(String) with a legal value.
     */
    @Test
    public void testFromUri() {
        Rotation r = Rotation.fromUri("35");
        assertEquals(35f, r.getDegrees(), DELTA);

        r = Rotation.fromUri("35.5");
        assertEquals(35.5f, r.getDegrees(), DELTA);
    }

    /**
     * Tests fromUri(String) with a large value.
     */
    @Test
    public void testFromUriWithLargeValue() {
        try {
            Rotation.fromUri("720");
        } catch (IllegalClientArgumentException e) {
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
        } catch (IllegalClientArgumentException e) {
            assertEquals("Degrees must be between 0 and 360", e.getMessage());
        }
    }

    @Test
    public void testCompareTo() {
        Rotation r2 = new Rotation();
        assertEquals(0, this.instance.compareTo(r2));
        r2.setDegrees(15);
        assertEquals(-1, this.instance.compareTo(r2));
        r2.setDegrees(0);
        this.instance.setDegrees(30);
        assertEquals(1, this.instance.compareTo(r2));
    }

    @Test
    public void testEquals() {
        Rotation r2 = new Rotation();
        assertTrue(r2.equals(this.instance));
        r2.setDegrees(15);
        assertFalse(r2.equals(this.instance));
    }

    /* setDegrees() */

    @Test
    public void testSetDegrees() {
        float degrees = 50.0f;
        this.instance.setDegrees(degrees);
        assertEquals(degrees, this.instance.getDegrees(), DELTA);
    }

    @Test
    public void testSetLargeDegrees() {
        float degrees = 530.0f;
        try {
            this.instance.setDegrees(degrees);
        } catch (IllegalClientArgumentException e) {
            assertEquals("Degrees must be between 0 and 360", e.getMessage());
        }
    }

    @Test
    public void testSetNegativeDegrees() {
        float degrees = -50.0f;
        try {
            this.instance.setDegrees(degrees);
        } catch (IllegalClientArgumentException e) {
            assertEquals("Degrees must be between 0 and 360", e.getMessage());
        }
    }

    @Test
    public void testToRotate() {
        assertTrue(this.instance.equals(this.instance.toRotate()));
    }

    @Test
    public void testToString() {
        Rotation r = Rotation.fromUri("50");
        assertEquals("50", r.toString());

        r = Rotation.fromUri("50.50");
        assertEquals("50.5", r.toString());
    }

}
