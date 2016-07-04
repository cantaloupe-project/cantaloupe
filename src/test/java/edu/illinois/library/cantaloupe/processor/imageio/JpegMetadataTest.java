package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.processor.Orientation;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static org.junit.Assert.*;

public class JpegMetadataTest {

    private JpegMetadata getInstance(String fixtureName)
            throws IOException {
        final Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("JPEG");
        final ImageReader reader = it.next();
        final File srcFile = TestUtil.getImage(fixtureName);
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile)) {
            reader.setInput(is);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            return new JpegMetadata(metadata,
                    metadata.getNativeMetadataFormatName());
        } finally {
            reader.dispose();
        }
    }

    @Test
    public void testGetExif() throws IOException {
        assertNotNull(getInstance("jpg-exif.jpg").getXmp());
    }

    @Test
    public void testGetIptc() throws IOException {
        assertNotNull(getInstance("jpg-iptc.jpg").getXmp());
    }

    @Test
    public void testGetOrientation() throws IOException {
        assertEquals(Orientation.ROTATE_90,
                getInstance("jpg-rotated.jpg").getOrientation());
    }

    @Test
    public void testGetXmp() throws IOException {
        assertNotNull(getInstance("jpg-xmp.jpg").getXmp());
    }

}
