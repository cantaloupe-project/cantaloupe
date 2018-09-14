package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ImageWriterFactoryTest extends BaseTest {

    private ImageWriterFactory instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new ImageWriterFactory();
    }

    @Test
    public void testSupportedFormats() {
        Set<Format> outputFormats = EnumSet.of(
                Format.GIF, Format.JPG, Format.PNG, Format.TIF);
        assertEquals(outputFormats, ImageWriterFactory.supportedFormats());
    }

    @Test
    public void testNewImageWriter() {
        assertNotNull(instance.newImageWriter(Format.JPG));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewImageWriterWithUnsupportedFormat() {
        instance.newImageWriter(Format.UNKNOWN);
    }

}
