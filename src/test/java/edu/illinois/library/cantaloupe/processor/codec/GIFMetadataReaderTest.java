package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class GIFMetadataReaderTest {

    private GIFMetadataReader instance;

    @Before
    public void setUp() throws Exception {
        instance = new GIFMetadataReader();
    }

    /* getDelayTime() */

    @Test
    public void testGetDelayTime() throws Exception {
        Path file = TestUtil.getImage("gif-animated-looping.gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(15, instance.getDelayTime());
        }
    }

    /* getHeight() */

    @Test
    public void testGetHeightWithValidImage() throws Exception {
        Path file = TestUtil.getImage("gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(56, instance.getHeight());
        }
    }

    @Test(expected = IOException.class)
    public void testGetHeightWithInvalidImage() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getHeight();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testGetHeightWithSourceNotSet() throws Exception {
        instance.getHeight();
    }

    /* getLoopCount() */

    @Test
    public void testGetLoopCountWithLoopingImage() throws Exception {
        Path file = TestUtil.getImage("gif-animated-looping.gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(2, instance.getLoopCount());
        }
    }

    @Test
    public void testGetLoopCountWithNonLoopingImage() throws Exception {
        Path file = TestUtil.getImage("gif-animated-non-looping.gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(0, instance.getLoopCount());
        }
    }

    /* getWidth() */

    @Test
    public void testGetWidthWithValidImage() throws Exception {
        Path file = TestUtil.getImage("gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(64, instance.getWidth());
        }
    }

    @Test(expected = IOException.class)
    public void testGetWidthWithInvalidImage() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getWidth();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testGetWidthWithSourceNotSet() throws Exception {
        instance.getWidth();
    }

    /* getXMP() */

    @Test
    public void testGetXMPWithValidImageContainingXMP() throws Exception {
        Path file = TestUtil.getImage("gif-xmp.gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);

            String xmpStr = instance.getXMP();
            assertTrue(xmpStr.startsWith("<rdf:RDF"));
            assertTrue(xmpStr.endsWith("</rdf:RDF>"));
        }
    }

    @Test
    public void testGetXMPWithValidImageNotContainingXMP() throws Exception {
        Path file = TestUtil.getImage("gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertNull(instance.getXMP());
        }
    }

    @Test(expected = IOException.class)
    public void testGetXMPWithInvalidImage() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getXMP();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testGetXMPWithSourceNotSet() throws Exception {
        instance.getXMP();
    }

}