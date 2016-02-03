package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.MediaType;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

public class ImageIoImageReaderTest {

    private ImageIoImageReader reader;

    @Before
    public void setUp() {
        reader = new ImageIoImageReader();
    }

    @Test
    public void testReadSizeWithFile() throws Exception {
        Dimension expected = new Dimension(64, 56);
        Dimension actual = reader.readSize(
                TestUtil.getImage("jpg-rgb-64x56x8-baseline.jpg"),
                SourceFormat.JPG);
        assertEquals(expected, actual);
    }

    @Test
    public void testReadSizeWithInputStream() throws Exception {
        Dimension expected = new Dimension(64, 56);
        StreamSource streamSource = new TestStreamSource(
                TestUtil.getImage("jpg-rgb-64x56x8-baseline.jpg"));
        Dimension actual = reader.readSize(streamSource, SourceFormat.JPG);
        assertEquals(expected, actual);
    }

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
