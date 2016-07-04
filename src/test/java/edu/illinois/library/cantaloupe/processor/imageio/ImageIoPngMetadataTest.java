package edu.illinois.library.cantaloupe.processor.imageio;

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

public class ImageIoPngMetadataTest {

    private ImageIoPngMetadata getInstance(String fixtureName)
            throws IOException {
        final Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("PNG");
        final ImageReader reader = it.next();
        final File srcFile = TestUtil.getImage(fixtureName);
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile)) {
            reader.setInput(is);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            return new ImageIoPngMetadata(metadata,
                    metadata.getNativeMetadataFormatName());
        }
    }

    @Test
    public void testGetOrientation() throws IOException {
        assertEquals(ImageIoMetadata.Orientation.ROTATE_90,
                getInstance("png-rotated.png").getOrientation());
    }

    @Test
    public void testGetXmp() throws IOException {
        assertNotNull(getInstance("png-xmp.png").getXmp());
    }

}
