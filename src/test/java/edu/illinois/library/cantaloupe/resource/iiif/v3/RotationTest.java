package edu.illinois.library.cantaloupe.resource.iiif.v3;

import edu.illinois.library.cantaloupe.operation.Transpose;
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

        this.instance = new Rotation();
        assertEquals(0f, instance.getDegrees(), DELTA);
        assertFalse(instance.shouldMirror());
    }

    /* fromURI(String) */

    /**
     * Tests fromURI(String) with a legal value.
     */
    @Test
    void testFromURI() {
        Rotation r = Rotation.fromURI("35");
        assertEquals(35f, r.getDegrees(), DELTA);
        assertFalse(r.shouldMirror());

        r = Rotation.fromURI("35.5");
        assertEquals(35.5f, r.getDegrees(), DELTA);
        assertFalse(r.shouldMirror());

        r = Rotation.fromURI("!35");
        assertEquals(35f, r.getDegrees(), DELTA);
        assertTrue(r.shouldMirror());
    }

    /**
     * Tests fromURI(String) with a large value.
     */
    @Test
    void testFromURIWithLargeValue() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Rotation.fromURI("720"),
                "Degrees must be between 0 and 360");
    }

    /**
     * Tests fromURI(String) with a negative value.
     */
    @Test
    void testFromURIWithNegativeValue() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Rotation.fromURI("-35"),
                "Degrees must be between 0 and 360");
    }

    @Test
    void testEquals() {
        Rotation r2 = new Rotation();
        assertEquals(r2, instance);
        r2.setDegrees(15);
        assertNotEquals(r2, instance);
        r2.setDegrees(0);
        r2.setMirror(true);
        assertNotEquals(r2, instance);
    }

    @Test
    void testIsZero() {
        instance.setDegrees(0);
        assertTrue(instance.isZero());
        instance.setDegrees(5);
        assertFalse(instance.isZero());
        instance.setDegrees(180);
        assertFalse(instance.isZero());
        instance.setDegrees(360);
        assertTrue(instance.isZero());
    }

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
        assertEquals(instance.getDegrees(),
                instance.toRotate().getDegrees(), DELTA);
    }

    @Test
    void testToTranspose() {
        instance.setMirror(false);
        assertNull(instance.toTranspose());
        instance.setMirror(true);
        assertEquals(Transpose.HORIZONTAL, instance.toTranspose());
    }

    @Test
    void testToString() {
        Rotation r = Rotation.fromURI("50");
        assertEquals("50", r.toString());

        r = Rotation.fromURI("!50");
        assertEquals("!50", r.toString());
    }

    @Test
    void testToCanonicalString() {
        Rotation r = Rotation.fromURI("50");
        assertEquals("50", r.toCanonicalString());

        r = Rotation.fromURI("!50");
        assertEquals("!50", r.toCanonicalString());

        r = Rotation.fromURI("50.50");
        assertEquals("50.5", r.toCanonicalString());

        r = Rotation.fromURI(".5");
        assertEquals("0.5", r.toCanonicalString());

        r = Rotation.fromURI("!.5");
        assertEquals("!0.5", r.toCanonicalString());
    }

}
