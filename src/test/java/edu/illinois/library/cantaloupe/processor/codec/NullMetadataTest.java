package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

import static org.junit.Assert.*;

public class NullMetadataTest extends BaseTest {

    private NullMetadata getInstance(String fixtureName) throws IOException {
        final Path srcFile = TestUtil.getImage(fixtureName);
        final Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("BMP");
        final ImageReader reader = it.next();
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile.toFile())) {
            reader.setInput(is);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            return new NullMetadata(metadata,
                    metadata.getNativeMetadataFormatName());
        } finally {
            reader.dispose();
        }
    }

    @Test
    public void testGetExif() throws IOException {
        assertNull(getInstance("bmp-rgb-64x56x8.bmp").getXMP());
    }

    @Test
    public void testGetIptc() throws IOException {
        assertNull(getInstance("bmp-rgb-64x56x8.bmp").getIPTC());
    }

    @Test
    public void testGetOrientation() throws IOException {
        assertEquals(Orientation.ROTATE_0,
                getInstance("bmp-rgb-64x56x8.bmp").getOrientation());
    }

    @Test
    public void testGetXmp() throws IOException {
        assertNull(getInstance("bmp-rgb-64x56x8.bmp").getXMP());
    }

}
