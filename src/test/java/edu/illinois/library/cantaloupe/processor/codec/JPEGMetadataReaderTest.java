package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class JPEGMetadataReaderTest {

    private JPEGMetadataReader instance;

    @Before
    public void setUp() throws Exception {
        instance = new JPEGMetadataReader();
    }

    /* getColorTransform() */

    @Test
    public void testGetColorTransformWithNoColorTransform() throws Exception {
        Path file = TestUtil.getImage("jpg-exif.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertNull(instance.getColorTransform());
        }
    }

    @Test
    public void testGetColorTransformOnImageWithColorTransform() throws Exception {
        Path file = TestUtil.getImage("jpg-ycck.jpg");
        try (ImageInputStream is =
                     ImageIO.createImageInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            instance.setSource(is);
            assertEquals(JPEGMetadataReader.AdobeColorTransform.YCCK,
                    instance.getColorTransform());
        }
    }

    @Test(expected = IOException.class)
    public void testGetColorTransformOnNonJPEGImage() throws Exception {
        Path file = TestUtil.getImage("gif-xmp.gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getColorTransform();
        }
    }

    /* getICCProfile() */

    @Test
    public void testGetICCProfileOnImageWithNoProfile() throws Exception {
        Path file = TestUtil.getImage("jpg-exif.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertNull(instance.getICCProfile());
        }
    }

    @Test
    public void testGetICCProfileOnImageWithSingleChunkProfile() throws Exception {
        Path file = TestUtil.getImage("jpg-icc.jpg");
        try (ImageInputStream is =
                     ImageIO.createImageInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            instance.setSource(is);
            assertNotNull(instance.getICCProfile());
        }
    }

    @Test
    public void testGetICCProfileOnImageWithMultiChunkProfile() throws Exception {
        Path file = TestUtil.getImage("jpg-icc-chunked.jpg"); // 17 chunks
        try (ImageInputStream is =
                     ImageIO.createImageInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            instance.setSource(is);
            assertNotNull(instance.getICCProfile());
        }
    }

    @Test(expected = IOException.class)
    public void testGetICCProfileOnNonJPEGImage() throws Exception {
        Path file = TestUtil.getImage("gif-xmp.gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getICCProfile();
        }
    }

    /* hasAdobeSegment() */

    @Test
    public void testHasAdobeSegmentOnImageWithNoAdobeSegment() throws Exception {
        Path file = TestUtil.getImage("jpg-exif.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertFalse(instance.hasAdobeSegment());
        }
    }

    @Test
    public void testHasAdobeSegmentOnImageWithAdobeSegment() throws Exception {
        Path file = TestUtil.getImage("jpg-ycck.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertTrue(instance.hasAdobeSegment());
        }
    }

    @Test(expected = IOException.class)
    public void testHasAdobeSegmentOnNonJPEGImage() throws Exception {
        Path file = TestUtil.getImage("gif-xmp.gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.hasAdobeSegment();
        }
    }

}