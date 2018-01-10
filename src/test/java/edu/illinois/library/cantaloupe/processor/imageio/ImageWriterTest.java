package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ImageWriterTest extends BaseTest {

    private ImageWriter instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new ImageWriter(TestUtil.newOperationList());
    }

    @Test
    public void testSupportedFormats() {
        Set<Format> outputFormats = EnumSet.of(
                Format.GIF, Format.JPG, Format.PNG, Format.TIF);
        assertEquals(outputFormats, ImageWriter.supportedFormats());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor1WithUnsupportedFormat() {
        OperationList ops = new OperationList(new Identifier("cats"), Format.UNKNOWN);
        new ImageWriter(ops);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor2WithUnsupportedFormat() {
        OperationList ops = new OperationList(new Identifier("cats"), Format.UNKNOWN);
        new ImageWriter(ops, new NullMetadata(null, ""));
    }

    @Test
    public void testGetIIOWriter() {
        assertNotNull(instance.getIIOWriter());
    }

    @Test
    public void testWrite() {
        // TODO: write this
    }

}
