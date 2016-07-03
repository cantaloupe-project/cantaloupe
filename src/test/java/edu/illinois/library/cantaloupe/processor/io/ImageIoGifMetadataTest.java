package edu.illinois.library.cantaloupe.processor.io;

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

public class ImageIoGifMetadataTest {

    private ImageIoGifMetadata getInstance(String fixtureName)
            throws IOException {
        final Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("GIF");
        final ImageReader reader = it.next();
        final File srcFile = TestUtil.getImage(fixtureName);
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile)) {
            reader.setInput(is);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            return new ImageIoGifMetadata(metadata,
                    metadata.getNativeMetadataFormatName());
        }
    }

    @Test
    public void testGetXmp() throws IOException {
        assertNotNull(getInstance("gif-xmp.gif").getXmp());
    }

}
