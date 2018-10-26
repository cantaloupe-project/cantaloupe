package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class GIFMetadataTest extends BaseTest {

    private ImageInputStream newImageInputStream(String fixtureName)
            throws IOException {
        final Path srcFile = TestUtil.getImage(fixtureName);
        return ImageIO.createImageInputStream(srcFile.toFile());
    }

    @Test
    public void testGetDelayTimeOfStaticImage() throws Exception {
        try (GIFMetadataReader reader = new GIFMetadataReader()) {
            reader.setSource(newImageInputStream("gif"));
            GIFMetadata metadata = new GIFMetadata(reader);
            assertEquals(0, metadata.getDelayTime());
        }
    }

    @Test
    public void testGetDelayTimeOfAnimatedImage() throws Exception {
        try (GIFMetadataReader reader = new GIFMetadataReader()) {
            reader.setSource(newImageInputStream("gif-animated-looping.gif"));
            GIFMetadata metadata = new GIFMetadata(reader);
            assertEquals(15, metadata.getDelayTime());
        }
    }

    @Test
    public void testGetLoopCountWithStaticImage() throws Exception {
        try (GIFMetadataReader reader = new GIFMetadataReader()) {
            reader.setSource(newImageInputStream("gif"));
            GIFMetadata metadata = new GIFMetadata(reader);
            assertEquals(0, metadata.getLoopCount());
        }
    }

    @Test
    public void testGetLoopCountWithAnimatedLoopingImage() throws Exception {
        try (GIFMetadataReader reader = new GIFMetadataReader()) {
            reader.setSource(newImageInputStream("gif-animated-looping.gif"));
            GIFMetadata metadata = new GIFMetadata(reader);
            assertEquals(2, metadata.getLoopCount());
        }
    }

    @Test
    public void testGetLoopCountWithAnimatedNonLoopingImage() throws Exception {
        try (GIFMetadataReader reader = new GIFMetadataReader()) {
            reader.setSource(newImageInputStream("gif-animated-non-looping.gif"));
            GIFMetadata metadata = new GIFMetadata(reader);
            assertEquals(0, metadata.getLoopCount());
        }
    }

    @Test
    public void testGetOrientation() throws Exception {
        try (GIFMetadataReader reader = new GIFMetadataReader()) {
            reader.setSource(newImageInputStream("gif-rotated.gif"));
            GIFMetadata metadata = new GIFMetadata(reader);
            assertEquals(Orientation.ROTATE_90, metadata.getOrientation());
        }
    }

    @Test
    public void testGetXMP() throws Exception {
        try (GIFMetadataReader reader = new GIFMetadataReader()) {
            reader.setSource(newImageInputStream("gif-xmp.gif"));
            GIFMetadata metadata = new GIFMetadata(reader);
            assertNotNull(metadata.getXMP());
        }
    }

}
