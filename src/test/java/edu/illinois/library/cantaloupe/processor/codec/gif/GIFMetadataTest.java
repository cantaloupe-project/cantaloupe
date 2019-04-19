package edu.illinois.library.cantaloupe.processor.codec.gif;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class GIFMetadataTest extends BaseTest {

    private ImageInputStream newImageInputStream(String fixtureName)
            throws IOException {
        final Path srcFile = TestUtil.getImage(fixtureName);
        return ImageIO.createImageInputStream(srcFile.toFile());
    }

    @Test
    void testGetMetadataWithStaticImage() throws Exception {
        try (GIFMetadataReader reader = new GIFMetadataReader()) {
            reader.setSource(newImageInputStream("gif"));
            GIFMetadata metadata = new GIFMetadata(reader);
            assertEquals(0, metadata.getNativeMetadata().get().getDelayTime());
            assertEquals(0, metadata.getNativeMetadata().get().getLoopCount());
        }
    }

    @Test
    void testGetMetadataWithAnimatedImage() throws Exception {
        try (GIFMetadataReader reader = new GIFMetadataReader()) {
            reader.setSource(newImageInputStream("gif-animated-looping.gif"));
            GIFMetadata metadata = new GIFMetadata(reader);
            assertEquals(15, metadata.getNativeMetadata().get().getDelayTime());
            assertEquals(2, metadata.getNativeMetadata().get().getLoopCount());
        }
    }

    @Test
    void testGetMetadataWithAnimatedNonLoopingImage() throws Exception {
        try (GIFMetadataReader reader = new GIFMetadataReader()) {
            reader.setSource(newImageInputStream("gif-animated-non-looping.gif"));
            GIFMetadata metadata = new GIFMetadata(reader);
            assertEquals(15, metadata.getNativeMetadata().get().getDelayTime());
            assertEquals(0, metadata.getNativeMetadata().get().getLoopCount());
        }
    }

    @Test
    void testGetXMP() throws Exception {
        try (GIFMetadataReader reader = new GIFMetadataReader()) {
            reader.setSource(newImageInputStream("gif-xmp.gif"));
            GIFMetadata metadata = new GIFMetadata(reader);
            assertTrue(metadata.getXMP().isPresent());
        }
    }

}
