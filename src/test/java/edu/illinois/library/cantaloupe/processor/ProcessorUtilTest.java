package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import java.awt.Dimension;

import static org.junit.Assert.*;

public class ProcessorUtilTest {

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
