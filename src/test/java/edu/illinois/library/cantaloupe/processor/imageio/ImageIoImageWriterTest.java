package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Format;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ImageIoImageWriterTest {

    ImageIoImageWriter instance;

    @Before
    public void setUp() {
        instance = new ImageIoImageWriter(null);
    }

    @Test
    public void testSupportedFormats() {
        Set<Format> outputFormats = new HashSet<>(Arrays.asList(
                Format.GIF, Format.JPG, Format.PNG, Format.TIF));
        assertEquals(outputFormats, ImageIoImageWriter.supportedFormats());
    }

    @Test
    public void testWriteImage() {
        // TODO: write this
    }

}
