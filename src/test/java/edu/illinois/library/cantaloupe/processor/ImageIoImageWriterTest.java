package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.OutputFormat;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ImageIoImageWriterTest {

    ImageIoImageWriter instance;

    @Before
    public void setUp() {
        instance = new ImageIoImageWriter();
    }

    @Test
    public void testSupportedFormats() {
        // assemble a set of all ImageIO output formats
        final String[] writerMimeTypes = ImageIO.getWriterMIMETypes();
        final Set<OutputFormat> outputFormats = new HashSet<>();
        for (OutputFormat outputFormat : OutputFormat.values()) {
            for (String mimeType : writerMimeTypes) {
                if (outputFormat.getMediaType().equals(mimeType.toLowerCase())) {
                    outputFormats.add(outputFormat);
                }
            }
        }
        assertEquals(outputFormats, instance.supportedFormats());
    }

    @Test
    public void testWriteImage() {
        // TODO: write this
    }

}
