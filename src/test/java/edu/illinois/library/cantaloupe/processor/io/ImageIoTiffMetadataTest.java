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

public class ImageIoTiffMetadataTest {

    private ImageIoTiffMetadata getInstance(String fixtureName)
            throws IOException {
        final Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("TIFF");
        final ImageReader reader = it.next();
        final File srcFile = TestUtil.getImage(fixtureName);
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile)) {
            reader.setInput(is);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            return new ImageIoTiffMetadata(metadata,
                    metadata.getNativeMetadataFormatName());
        } finally {
            reader.dispose();
        }
    }

    @Test
    public void testGetExif() throws IOException {
        assertNotNull(getInstance("tif-exif.tif").getXmp());
    }

    @Test
    public void testGetIptc() throws IOException {
        assertNotNull(getInstance("tif-iptc.tif").getXmp());
    }

    @Test
    public void testGetOrientation() throws IOException {
        assertEquals(ImageIoMetadata.Orientation.ROTATE_90,
                getInstance("tif-rotated.tif").getOrientation());
    }

    @Test
    public void testGetXmp() throws IOException {
        assertNotNull(getInstance("tif-xmp.tif").getXmp());
    }

}
