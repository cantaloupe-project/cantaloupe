package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Format;
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

    @Test
    public void testGetIIOWriter() {
        assertNotNull(instance.getIIOWriter());
    }

    @Test
    public void testWrite() {
        // TODO: write this
    }

}
