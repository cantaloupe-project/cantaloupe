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
    }

}
