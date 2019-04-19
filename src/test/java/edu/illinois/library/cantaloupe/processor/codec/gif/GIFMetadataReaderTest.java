package edu.illinois.library.cantaloupe.processor.codec.gif;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class GIFMetadataReaderTest extends BaseTest {

    private GIFMetadataReader instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new GIFMetadataReader();
    }

    /* getDelayTime() */

    @Test
    void testGetDelayTime() throws Exception {
        Path file = TestUtil.getImage("gif-animated-looping.gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(15, instance.getDelayTime());
        }
    }

    /* getHeight() */

    @Test
    void testGetHeightWithValidImage() throws Exception {
        Path file = TestUtil.getImage("gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(56, instance.getHeight());
        }
    }

    @Test
    void testGetHeightWithInvalidImage() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(IOException.class, () -> instance.getHeight());
        }
    }

    @Test
    void testGetHeightWithSourceNotSet() {
        assertThrows(IllegalStateException.class, () -> instance.getHeight());
    }

    /* getLoopCount() */

    @Test
    void testGetLoopCountWithLoopingImage() throws Exception {
        Path file = TestUtil.getImage("gif-animated-looping.gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(2, instance.getLoopCount());
        }
    }

    @Test
    void testGetLoopCountWithNonLoopingImage() throws Exception {
        Path file = TestUtil.getImage("gif-animated-non-looping.gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(0, instance.getLoopCount());
        }
    }

    /* getWidth() */

    @Test
    void testGetWidthWithValidImage() throws Exception {
        Path file = TestUtil.getImage("gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(64, instance.getWidth());
        }
    }

    @Test
    void testGetWidthWithInvalidImage() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(IOException.class, () -> instance.getWidth());
        }
    }

    @Test
    void testGetWidthWithSourceNotSet() {
        assertThrows(IllegalStateException.class, () -> instance.getWidth());
    }

    /* getXMP() */

    @Test
    void testGetXMPWithValidImageContainingXMP() throws Exception {
        Path file = TestUtil.getImage("gif-xmp.gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);

            String xmpStr = instance.getXMP();
            assertTrue(xmpStr.startsWith("<rdf:RDF"));
            assertTrue(xmpStr.endsWith("</rdf:RDF>"));
        }
    }

    @Test
    void testGetXMPWithValidImageNotContainingXMP() throws Exception {
        Path file = TestUtil.getImage("gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertNull(instance.getXMP());
        }
    }

    @Test
    void testGetXMPWithInvalidImage() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(IOException.class, () -> instance.getXMP());
        }
    }

    @Test
    void testGetXMPWithSourceNotSet() {
        assertThrows(IllegalStateException.class, () -> instance.getXMP());
    }

}