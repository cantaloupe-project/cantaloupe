package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import java.awt.Dimension;
import java.io.FileInputStream;
import java.nio.channels.ReadableByteChannel;

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
    public void testGetScale() {
        final double fudge = 0.0000001f;
        assertTrue(Math.abs(ProcessorUtil.getScale(new ReductionFactor(0))) - Math.abs(1.0f) < fudge);
        assertTrue(Math.abs(ProcessorUtil.getScale(new ReductionFactor(1))) - Math.abs(0.5f) < fudge);
        assertTrue(Math.abs(ProcessorUtil.getScale(new ReductionFactor(2))) - Math.abs(0.25f) < fudge);
        assertTrue(Math.abs(ProcessorUtil.getScale(new ReductionFactor(3))) - Math.abs(0.125f) < fudge);
        assertTrue(Math.abs(ProcessorUtil.getScale(new ReductionFactor(4))) - Math.abs(0.0625f) < fudge);
        assertTrue(Math.abs(ProcessorUtil.getScale(new ReductionFactor(5))) - Math.abs(0.03125f) < fudge);
    }

    @Test
    public void testGetSizeWithFile() throws Exception {
        Dimension expected = new Dimension(64, 56);
        Dimension actual = ProcessorUtil.getSize(
                TestUtil.getFixture("images/jpg-rgb-64x56x8-baseline.jpg"),
                SourceFormat.JPG);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetSizeWithInputStream() throws Exception {
        Dimension expected = new Dimension(64, 56);
        ReadableByteChannel readableChannel = new FileInputStream(
                TestUtil.getFixture("images/jpg-rgb-64x56x8-baseline.jpg")).getChannel();
        Dimension actual = ProcessorUtil.getSize(readableChannel,
                SourceFormat.JPG);
        assertEquals(expected, actual);
    }

}
