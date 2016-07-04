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

public class BmpMetadataTest {

    private BmpMetadata getInstance(String fixtureName)
            throws IOException {
        final Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("BMP");
        final ImageReader reader = it.next();
        final File srcFile = TestUtil.getImage(fixtureName);
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile)) {
            reader.setInput(is);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            return new BmpMetadata(metadata,
                    metadata.getNativeMetadataFormatName());
        }
    }

    @Test
    public void testGetExif() throws IOException {
        assertNull(getInstance("bmp-rgb-64x56x8.bmp").getXmp());
    }

    @Test
    public void testGetIptc() throws IOException {
        assertNull(getInstance("bmp-rgb-64x56x8.bmp").getIptc());
    }

    @Test
    public void testGetOrientation() throws IOException {
        assertEquals(Orientation.ROTATE_0,
                getInstance("bmp-rgb-64x56x8.bmp").getOrientation());
    }

    @Test
    public void testGetXmp() throws IOException {
        assertNull(getInstance("bmp-rgb-64x56x8.bmp").getXmp());
    }

}
