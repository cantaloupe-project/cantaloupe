package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RotationTest extends BaseTest {

    private static final float DELTA = 0.0000001f;

    private Rotation instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        instance = new Rotation();
        assertEquals(0f, instance.getDegrees(), DELTA);
    }

    /* fromUri(String) */

    /**
     * Tests fromUri(String) with a legal value.
     */
    @Test
    void testFromUri() {
        Rotation r = Rotation.fromUri("35");
        assertEquals(35f, r.getDegrees(), DELTA);

        r = Rotation.fromUri("35.5");
        assertEquals(35.5f, r.getDegrees(), DELTA);
    }

    /**
     * Tests fromUri(String) with a large value.
     */
    @Test
    void testFromUriWithLargeValue() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Rotation.fromUri("720"),
                "Degrees must be between 0 and 360");
    }

    /**
     * Tests fromUri(String) with a negative value.
     */
    @Test
    void testFromUriWithNegativeValue() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Rotation.fromUri("-35"),
                "Degrees must be between 0 and 360");
    }

    @Test
    void testCompareTo() {
        Rotation r2 = new Rotation();
        assertEquals(0, instance.compareTo(r2));
        r2.setDegrees(15);
        assertEquals(-1, instance.compareTo(r2));
        r2.setDegrees(0);
        instance.setDegrees(30);
        assertEquals(1, instance.compareTo(r2));
    }

    @Test
    void testEquals() {
        Rotation r2 = new Rotation();
        assertEquals(r2, instance);
        r2.setDegrees(15);
        assertNotEquals(r2, instance);
    }

    /* setDegrees() */

    @Test
    void testSetDegrees() {
        float degrees = 50.0f;
        instance.setDegrees(degrees);
        assertEquals(degrees, instance.getDegrees(), DELTA);
    }

    @Test
    void testSetLargeDegrees() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setDegrees(530),
                "Degrees must be between 0 and 360");
    }

    @Test
    void testSetNegativeDegrees() {
        assertThrows(IllegalClientArgumentException.class,
                () -> instance.setDegrees(-50),
                "Degrees must be between 0 and 360");
    }

    @Test
    void testToRotate() {
        assertEquals(instance, instance.toRotate());
    }

    @Test
    void testToString() {
        Rotation r = Rotation.fromUri("50");
        assertEquals("50", r.toString());

        r = Rotation.fromUri("50.50");
        assertEquals("50.5", r.toString());
    }

}
