package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import org.junit.Test;
import org.restlet.data.MediaType;

import javax.imageio.ImageIO;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

public class ImageIoImageReaderTest {

    @Test
    public void testSupportedFormats() {
        final HashSet<SourceFormat> formats = new HashSet<>();
        for (String mediaType : ImageIO.getReaderMIMETypes()) {
            final SourceFormat sourceFormat =
                    SourceFormat.getSourceFormat(new MediaType(mediaType));
            if (sourceFormat != null && !sourceFormat.equals(SourceFormat.UNKNOWN)) {
                formats.add(sourceFormat);
            }
        }
        assertEquals(formats, ImageIoImageReader.supportedFormats());
    }

    @Test
    public void testReadImageWithFile() {
        // this will be tested in ProcessorTest
    }

    @Test
    public void testReadImageWithInputStream() {
        // this will be tested in ProcessorTest
    }

}
