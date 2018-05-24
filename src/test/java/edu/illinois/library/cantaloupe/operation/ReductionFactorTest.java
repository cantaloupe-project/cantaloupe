package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReductionFactorTest extends BaseTest {

    private static final double DELTA = 0.0000001;

    @Test
    public void testForScale1() {
        assertEquals(new ReductionFactor(0), ReductionFactor.forScale(0.75));
        assertEquals(new ReductionFactor(1), ReductionFactor.forScale(0.5));
        assertEquals(new ReductionFactor(1), ReductionFactor.forScale(0.45));
        assertEquals(new ReductionFactor(2), ReductionFactor.forScale(0.25));
        assertEquals(new ReductionFactor(2), ReductionFactor.forScale(0.2));
        assertEquals(new ReductionFactor(2), ReductionFactor.forScale(0.1251));
        assertEquals(new ReductionFactor(3), ReductionFactor.forScale(0.125001));
        assertEquals(new ReductionFactor(3), ReductionFactor.forScale(0.125));
        assertEquals(new ReductionFactor(3), ReductionFactor.forScale(0.1249999));
        assertEquals(new ReductionFactor(4), ReductionFactor.forScale(0.0625));
        assertEquals(new ReductionFactor(5), ReductionFactor.forScale(0.03125));
    }

    @Test
    public void testForScale2() {
        assertEquals(new ReductionFactor(2), ReductionFactor.forScale(0.25001, 0.001));
        assertEquals(new ReductionFactor(2), ReductionFactor.forScale(0.25, 0.001));
        assertEquals(new ReductionFactor(2), ReductionFactor.forScale(0.24999, 0.001));

        assertEquals(new ReductionFactor(1), ReductionFactor.forScale(0.2501, 0.00001));
        assertEquals(new ReductionFactor(2), ReductionFactor.forScale(0.25, 0.00001));
        assertEquals(new ReductionFactor(2), ReductionFactor.forScale(0.2499, 0.00001));
    }

    @Test
    public void testConstructor() {
        ReductionFactor rf = new ReductionFactor(3);
        assertEquals(3, rf.factor);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNegativeArgument() {
        new ReductionFactor(-3);
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
        assertTrue(Math.abs(new ReductionFactor(0).getScale() - 1.0) < DELTA);
        assertTrue(Math.abs(new ReductionFactor(1).getScale() - 0.5) < DELTA);
        assertTrue(Math.abs(new ReductionFactor(2).getScale() - 0.25) < DELTA);
        assertTrue(Math.abs(new ReductionFactor(3).getScale() - 0.125) < DELTA);
        assertTrue(Math.abs(new ReductionFactor(4).getScale() - 0.0625) < DELTA);
        assertTrue(Math.abs(new ReductionFactor(5).getScale() - 0.03125) < DELTA);
    }

    @Test
    public void testToString() {
        assertEquals("1", new ReductionFactor(1).toString());
    }

}
