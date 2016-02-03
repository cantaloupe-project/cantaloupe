package edu.illinois.library.cantaloupe.processor;

import org.junit.Test;

import static org.junit.Assert.*;

public class ReductionFactorTest {

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
