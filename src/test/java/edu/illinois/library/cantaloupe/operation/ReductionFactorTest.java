package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReductionFactorTest extends BaseTest {

    private static final float DELTA = 0.0000001f;

    @Test
    public void testForScale() {
        assertEquals(new ReductionFactor(0), ReductionFactor.forScale(0.75f, 5));
        assertEquals(new ReductionFactor(1), ReductionFactor.forScale(0.5f, 5));
        assertEquals(new ReductionFactor(1), ReductionFactor.forScale(0.45f, 5));
        assertEquals(new ReductionFactor(2), ReductionFactor.forScale(0.25f, 5));
        assertEquals(new ReductionFactor(2), ReductionFactor.forScale(0.2f, 5));
        assertEquals(new ReductionFactor(3), ReductionFactor.forScale(0.125f, 5));
        assertEquals(new ReductionFactor(4), ReductionFactor.forScale(0.0625f, 5));
        assertEquals(new ReductionFactor(5), ReductionFactor.forScale(0.03125f, 5));
        // max
        assertEquals(new ReductionFactor(1), ReductionFactor.forScale(0.2f, 1));

        // negative
        assertEquals(new ReductionFactor(0), ReductionFactor.forScale(1.5f, 5));
        assertEquals(new ReductionFactor(-1), ReductionFactor.forScale(2f, 5));
        assertEquals(new ReductionFactor(-1), ReductionFactor.forScale(2.25f, 5));
        assertEquals(new ReductionFactor(-1), ReductionFactor.forScale(3.25f, 5));
        assertEquals(new ReductionFactor(-2), ReductionFactor.forScale(4.f, 5));
        assertEquals(new ReductionFactor(-2), ReductionFactor.forScale(7f, 5));
        assertEquals(new ReductionFactor(-3), ReductionFactor.forScale(8f, 5));
    }

    @Test
    public void testEqualsWithSameInstance() {
        ReductionFactor rf = new ReductionFactor();
        assertEquals(rf, rf);
    }

    @Test
    public void testEqualsWithEqualInstances() {
        ReductionFactor rf1 = new ReductionFactor(2);
        ReductionFactor rf2 = new ReductionFactor(2);
        assertEquals(rf1, rf2);
    }

    @Test
    public void testEqualsWithUnequalInstances() {
        ReductionFactor rf1 = new ReductionFactor(2);
        ReductionFactor rf2 = new ReductionFactor(3);
        assertNotEquals(rf1, rf2);
    }

    @Test
    public void testGetScale() {
        assertTrue(Math.abs(new ReductionFactor(0).getScale() - 1.0f) < DELTA);
        assertTrue(Math.abs(new ReductionFactor(1).getScale() - 0.5f) < DELTA);
        assertTrue(Math.abs(new ReductionFactor(2).getScale() - 0.25f) < DELTA);
        assertTrue(Math.abs(new ReductionFactor(3).getScale() - 0.125f) < DELTA);
        assertTrue(Math.abs(new ReductionFactor(4).getScale() - 0.0625f) < DELTA);
        assertTrue(Math.abs(new ReductionFactor(5).getScale() - 0.03125f) < DELTA);
    }

    @Test
    public void testToString() {
        assertEquals("1", new ReductionFactor(1).toString());
    }

}
