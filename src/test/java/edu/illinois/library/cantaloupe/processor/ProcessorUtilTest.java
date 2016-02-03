package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import java.awt.Dimension;

import static org.junit.Assert.*;

public class ProcessorUtilTest {

    @Test
    public void testGetReductionFactor() {
        assertEquals(new ReductionFactor(0), ProcessorUtil.getReductionFactor(0.75f, 5));
        assertEquals(new ReductionFactor(1), ProcessorUtil.getReductionFactor(0.5f, 5));
        assertEquals(new ReductionFactor(1), ProcessorUtil.getReductionFactor(0.45f, 5));
        assertEquals(new ReductionFactor(2), ProcessorUtil.getReductionFactor(0.25f, 5));
        assertEquals(new ReductionFactor(2), ProcessorUtil.getReductionFactor(0.2f, 5));
        assertEquals(new ReductionFactor(3), ProcessorUtil.getReductionFactor(0.125f, 5));
        assertEquals(new ReductionFactor(4), ProcessorUtil.getReductionFactor(0.0625f, 5));
        assertEquals(new ReductionFactor(5), ProcessorUtil.getReductionFactor(0.03125f, 5));
        // max
        assertEquals(new ReductionFactor(1), ProcessorUtil.getReductionFactor(0.2f, 1));
    }

    @Test
    public void testGetSizeWithFile() throws Exception {
        Dimension expected = new Dimension(64, 56);
        Dimension actual = ProcessorUtil.getSize(
                TestUtil.getImage("jpg-rgb-64x56x8-baseline.jpg"),
                SourceFormat.JPG);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetSizeWithInputStream() throws Exception {
        Dimension expected = new Dimension(64, 56);
        StreamSource streamSource = new TestStreamSource(
                TestUtil.getImage("jpg-rgb-64x56x8-baseline.jpg"));
        Dimension actual = ProcessorUtil.getSize(streamSource,
                SourceFormat.JPG);
        assertEquals(expected, actual);
    }

}
