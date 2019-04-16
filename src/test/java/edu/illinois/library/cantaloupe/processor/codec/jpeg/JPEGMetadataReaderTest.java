package edu.illinois.library.cantaloupe.processor.codec.jpeg;

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

    /* getEXIF() */

    @Test
    public void testGetEXIFWithEXIFImage() throws Exception {
        Path file = TestUtil.getImage("jpg-exif.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            byte[] exif = instance.getEXIF();
            assertTrue((exif[0] == 0x49 && exif[1] == 0x49) ||
                    (exif[0] == 0x4d && exif[1] == 0x4d));
        }
    }

    @Test
    public void testGetEXIFWithNonEXIFImage() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertNull(instance.getEXIF());
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

    /* getIPTC() */

    @Test
    public void testGetIPTCWithIPTCImage() throws Exception {
        Path file = TestUtil.getImage("jpg-iptc.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            byte[] iptc = instance.getIPTC();
            assertEquals(18, iptc.length);
        }
    }

    @Test
    public void testGetIPTCWithNonIPTCImage() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertNull(instance.getIPTC());
        }
    }

    /* getWidth() */

    @Test
    public void testGetWidth() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(64, instance.getWidth());
        }
    }

    /* getHeight() */

    @Test
    public void testGetHeight() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(56, instance.getHeight());
        }
    }

    /* getXMP() */

    @Test
    public void testGetXMPWithXMPImage() throws Exception {
        Path file = TestUtil.getImage("jpg-xmp.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            byte[] xmp = instance.getXMP();
            assertNotNull(xmp);
        }
    }

    @Test
    public void testGetXMPWithNonXMPImage() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertNull(instance.getXMP());
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