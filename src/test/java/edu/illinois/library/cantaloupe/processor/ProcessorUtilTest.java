package edu.illinois.library.cantaloupe.processor;

import junit.framework.TestCase;

public class ProcessorUtilTest extends TestCase {

    public void testGetReductionFactor() {
        assertEquals(0, ProcessorUtil.getReductionFactor(0.75, 5));
        assertEquals(1, ProcessorUtil.getReductionFactor(0.5, 5));
        assertEquals(1, ProcessorUtil.getReductionFactor(0.45, 5));
        assertEquals(2, ProcessorUtil.getReductionFactor(0.25, 5));
        assertEquals(2, ProcessorUtil.getReductionFactor(0.2, 5));
        assertEquals(1, ProcessorUtil.getReductionFactor(0.2, 1));
        assertEquals(2, ProcessorUtil.getReductionFactor(0.2, 0));
    }

    public void testGetScale() {
        final double fudge = 0.0000001f;
        assertTrue(Math.abs(ProcessorUtil.getScale(0)) - Math.abs(1.0f) < fudge);
        assertTrue(Math.abs(ProcessorUtil.getScale(1)) - Math.abs(0.5f) < fudge);
        assertTrue(Math.abs(ProcessorUtil.getScale(2)) - Math.abs(0.25f) < fudge);
        assertTrue(Math.abs(ProcessorUtil.getScale(3)) - Math.abs(0.125f) < fudge);
    }

}
