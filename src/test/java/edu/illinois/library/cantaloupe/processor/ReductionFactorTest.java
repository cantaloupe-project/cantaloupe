package edu.illinois.library.cantaloupe.processor;

import org.junit.Test;

import static org.junit.Assert.*;

public class ReductionFactorTest {

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
    public void testGetScale() {
        final double fudge = 0.0000001f;
        assertTrue(Math.abs(new ReductionFactor(0).getScale() - 1.0f) < fudge);
        assertTrue(Math.abs(new ReductionFactor(1).getScale() - 0.5f) < fudge);
        assertTrue(Math.abs(new ReductionFactor(2).getScale() - 0.25f) < fudge);
        assertTrue(Math.abs(new ReductionFactor(3).getScale() - 0.125f) < fudge);
        assertTrue(Math.abs(new ReductionFactor(4).getScale() - 0.0625f) < fudge);
        assertTrue(Math.abs(new ReductionFactor(5).getScale() - 0.03125f) < fudge);
    }

}
