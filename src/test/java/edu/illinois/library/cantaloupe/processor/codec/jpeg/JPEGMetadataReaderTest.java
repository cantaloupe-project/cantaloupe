package edu.illinois.library.cantaloupe.processor.codec.jpeg;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class JPEGMetadataReaderTest extends BaseTest {

    private JPEGMetadataReader instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new JPEGMetadataReader();
    }

    /* getColorTransform() */

    @Test
    void testGetColorTransformWithNoColorTransform() throws Exception {
        Path file = TestUtil.getImage("jpg-exif.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertNull(instance.getColorTransform());
        }
    }

    @Test
    void testGetColorTransformOnImageWithColorTransform() throws Exception {
        Path file = TestUtil.getImage("jpg-ycck.jpg");
        try (ImageInputStream is =
                     ImageIO.createImageInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            instance.setSource(is);
            assertEquals(JPEGMetadataReader.AdobeColorTransform.YCCK,
                    instance.getColorTransform());
        }
    }

    @Test
    void testGetColorTransformOnNonJPEGImage() throws Exception {
        Path file = TestUtil.getImage("gif-xmp.gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(IOException.class, () -> instance.getColorTransform());
        }
    }

    /* getEXIF() */

    @Test
    void testGetEXIFWithEXIFImage() throws Exception {
        Path file = TestUtil.getImage("jpg-exif.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            byte[] exif = instance.getEXIF();
            assertTrue((exif[0] == 0x49 && exif[1] == 0x49) ||
                    (exif[0] == 0x4d && exif[1] == 0x4d));
        }
    }

    @Test
    void testGetEXIFWithNonEXIFImage() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertNull(instance.getEXIF());
        }
    }

    /* getICCProfile() */

    @Test
    void testGetICCProfileOnImageWithNoProfile() throws Exception {
        Path file = TestUtil.getImage("jpg-exif.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertNull(instance.getICCProfile());
        }
    }

    @Test
    void testGetICCProfileOnImageWithSingleChunkProfile() throws Exception {
        Path file = TestUtil.getImage("jpg-icc.jpg");
        try (ImageInputStream is =
                     ImageIO.createImageInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            instance.setSource(is);
            assertNotNull(instance.getICCProfile());
        }
    }

    @Test
    void testGetICCProfileOnImageWithMultiChunkProfile() throws Exception {
        Path file = TestUtil.getImage("jpg-icc-chunked.jpg"); // 17 chunks
        try (ImageInputStream is =
                     ImageIO.createImageInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            instance.setSource(is);
            assertNotNull(instance.getICCProfile());
        }
    }

    @Test
    void testGetICCProfileOnNonJPEGImage() throws Exception {
        Path file = TestUtil.getImage("gif-xmp.gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(IOException.class, () -> instance.getICCProfile());
        }
    }

    /* getIPTC() */

    @Test
    void testGetIPTCWithIPTCImage() throws Exception {
        Path file = TestUtil.getImage("jpg-iptc.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            byte[] iptc = instance.getIPTC();
            assertEquals(18, iptc.length);
        }
    }

    @Test
    void testGetIPTCWithNonIPTCImage() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertNull(instance.getIPTC());
        }
    }

    /* getWidth() */

    @Test
    void testGetWidth() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(64, instance.getWidth());
        }
    }

    /* getHeight() */

    @Test
    void testGetHeight() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(56, instance.getHeight());
        }
    }

    /* getXMP() */

    @Test
    void testGetXMPWithStandardXMPImage() throws Exception {
        Path file = TestUtil.getImage("jpg-xmp.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            String xmp = instance.getXMP();
            assertTrue(xmp.startsWith("<rdf:RDF"));
            assertTrue(xmp.endsWith("</rdf:RDF>"));
        }
    }

    @Test
    void testGetXMPWithExtendedXMPImage() throws Exception {
        // N.B.: easy XMP embed:
        // exiftool -tagsfromfile file.xmp -all:all jpg.jpg
        Path file = TestUtil.getImage("jpg-xmp-extended.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            String xmp = instance.getXMP();
            assertTrue(xmp.length() > 65502);
            assertTrue(xmp.startsWith("<rdf:RDF"));
            assertTrue(xmp.endsWith("</rdf:RDF>\n"));
            assertFalse(xmp.contains("HasExtendedXMP"));
        }
    }

    @Test
    void testGetXMPWithNonXMPImage() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertNull(instance.getXMP());
        }
    }

    /* hasAdobeSegment() */

    @Test
    void testHasAdobeSegmentOnImageWithNoAdobeSegment() throws Exception {
        Path file = TestUtil.getImage("jpg-exif.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertFalse(instance.hasAdobeSegment());
        }
    }

    @Test
    void testHasAdobeSegmentOnImageWithAdobeSegment() throws Exception {
        Path file = TestUtil.getImage("jpg-ycck.jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertTrue(instance.hasAdobeSegment());
        }
    }

    @Test
    void testHasAdobeSegmentOnNonJPEGImage() throws Exception {
        Path file = TestUtil.getImage("gif-xmp.gif");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(IOException.class, () -> instance.hasAdobeSegment());
        }
    }

}