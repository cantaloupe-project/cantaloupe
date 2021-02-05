package edu.illinois.library.cantaloupe.processor.codec.jpeg2000;

import edu.illinois.library.cantaloupe.image.iptc.Reader;
import edu.illinois.library.cantaloupe.processor.SourceFormatException;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class JPEG2000MetadataReaderTest extends BaseTest {

    private JPEG2000MetadataReader instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new JPEG2000MetadataReader();
    }

    /* getComponentSize() */

    @Test
    void testGetComponentSizeWithValidImage() throws Exception {
        Path file = TestUtil.getImage("jp2");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(8, instance.getComponentSize());
        }
    }

    @Test
    void testGetComponentSizeWithInvalidImage1() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getComponentSize());
        }
    }

    @Test
    void testGetComponentSizeWithInvalidImage2() throws Exception {
        Path file = TestUtil.getImage("unknown");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getComponentSize());
        }
    }

    @Test
    void testGetComponentSizeWithEmptyImage() throws Exception {
        Path file = TestUtil.getImage("empty");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getComponentSize());
        }
    }

    @Test
    void testGetComponentSizeWithSourceNotSet() {
        assertThrows(IllegalStateException.class,
                () -> instance.getComponentSize());
    }

    /* getEXIF() */

    @Test
    void testGetEXIFWithValidImageContainingEXIF() throws Exception {
        Path file = TestUtil.getImage("jp2-exif.jp2");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            try (edu.illinois.library.cantaloupe.image.exif.Reader reader =
                         new edu.illinois.library.cantaloupe.image.exif.Reader()) {
                reader.setSource(instance.getEXIF());
                assertEquals(5, reader.read().size());
            }
        }
    }

    @Test
    void testGetEXIFWithValidImageNotContainingEXIF() throws Exception {
        Path file = TestUtil.getImage("jp2");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertNull(instance.getEXIF());
        }
    }

    @Test
    void testGetEXIFWithInvalidImage1() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getEXIF());
        }
    }

    @Test
    void testGetEXIFWithInvalidImage2() throws Exception {
        Path file = TestUtil.getImage("unknown");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getEXIF());
        }
    }

    @Test
    void testGetEXIFWithEmptyImage() throws Exception {
        Path file = TestUtil.getImage("empty");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getEXIF());
        }
    }

    @Test
    void testGetEXIFWithSourceNotSet() {
        assertThrows(IllegalStateException.class, () -> instance.getEXIF());
    }

    /* getHeight() */

    @Test
    void testGetHeightWithValidImage() throws Exception {
        Path file = TestUtil.getImage("jp2");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(56, instance.getHeight());
        }
    }

    @Test
    void testGetHeightWithInvalidImage1() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getHeight());
        }
    }

    @Test
    void testGetHeightWithInvalidImage2() throws Exception {
        Path file = TestUtil.getImage("unknown");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getHeight());
        }
    }

    @Test
    void testGetHeightWithEmptyImage() throws Exception {
        Path file = TestUtil.getImage("empty");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getHeight());
        }
    }

    @Test
    void testGetHeightWithSourceNotSet() {
        assertThrows(IllegalStateException.class, () -> instance.getHeight());
    }

    /* getIPTC() */

    @Test
    void testGetIPTCWithValidImageContainingIPTC() throws Exception {
        Path file = TestUtil.getImage("jp2-iptc.jp2");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            try (edu.illinois.library.cantaloupe.image.iptc.Reader reader =
                         new edu.illinois.library.cantaloupe.image.iptc.Reader()) {
                reader.setSource(instance.getIPTC());
                assertEquals(2, reader.read().size());
            }
        }
    }

    @Test
    void testGetIPTCWithValidImageNotContainingIPTC() throws Exception {
        Path file = TestUtil.getImage("jp2");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertNull(instance.getIPTC());
        }
    }

    @Test
    void testGetIPTCWithInvalidImage1() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getIPTC());
        }
    }

    @Test
    void testGetIPTCWithInvalidImage2() throws Exception {
        Path file = TestUtil.getImage("unknown");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getIPTC());
        }
    }

    @Test
    void testGetIPTCWithEmptyImage() throws Exception {
        Path file = TestUtil.getImage("empty");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getIPTC());
        }
    }

    @Test
    void testGetIPTCWithSourceNotSet() {
        assertThrows(IllegalStateException.class, () -> instance.getIPTC());
    }

    /* getNumComponents() */

    @Test
    void testGetNumComponentsWithValidImage() throws Exception {
        Path file = TestUtil.getImage("jp2-5res-rgb-64x56x8-monotiled-lossy.jp2");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(3, instance.getNumComponents());
        }
    }

    @Test
    void testGetNumComponentsWithInvalidImage1() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getNumComponents());
        }
    }

    @Test
    void testGetNumComponentsWithInvalidImage2() throws Exception {
        Path file = TestUtil.getImage("unknown");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getNumComponents());
        }
    }

    @Test
    void testGetNumComponentsWithEmptyImage() throws Exception {
        Path file = TestUtil.getImage("empty");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getNumComponents());
        }
    }

    @Test
    void testGetNumComponentsWithSourceNotSet() {
        assertThrows(IllegalStateException.class,
                () -> instance.getNumComponents());
    }

    /* getNumDecompositionLevels() */

    @Test
    void testGetNumDecompositionLevelsWithValidImage() throws Exception {
        Path file = TestUtil.getImage("jp2");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(5, instance.getNumDecompositionLevels());
        }
    }

    @Test
    void testGetNumDecompositionLevelsWithInvalidImage1() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class,
                    () -> instance.getNumDecompositionLevels());
        }
    }

    @Test
    void testGetNumDecompositionLevelsWithInvalidImage2() throws Exception {
        Path file = TestUtil.getImage("unknown");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class,
                    () -> instance.getNumDecompositionLevels());
        }
    }

    @Test
    void testGetNumDecompositionLevelsWithEmptyImage() throws Exception {
        Path file = TestUtil.getImage("empty");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class,
                    () -> instance.getNumDecompositionLevels());
        }
    }

    @Test
    void testGetNumDecompositionLevelsWithSourceNotSet() {
        assertThrows(IllegalStateException.class,
                () -> instance.getNumDecompositionLevels());
    }

    /* getTileHeight() */

    @Test
    void testGetTileHeightWithValidImage() throws Exception {
        Path file = TestUtil.getImage("jp2");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(56, instance.getTileHeight());
        }
    }

    @Test
    void testGetTileHeightWithInvalidImage1() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getTileHeight());
        }
    }

    @Test
    void testGetTileHeightWithInvalidImage2() throws Exception {
        Path file = TestUtil.getImage("unknown");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getTileHeight());
        }
    }

    @Test
    void testGetTileHeightWithEmptyImage() throws Exception {
        Path file = TestUtil.getImage("empty");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getTileHeight());
        }
    }

    @Test
    void testGetTileHeightWithSourceNotSet() {
        assertThrows(IllegalStateException.class,
                () -> instance.getTileHeight());
    }

    /* getTileWidth() */

    @Test
    void testGetTileWidthWithValidImage() throws Exception {
        Path file = TestUtil.getImage("jp2");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(64, instance.getTileWidth());
        }
    }

    @Test
    void testGetTileWidthWithInvalidImage1() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getTileWidth());
        }
    }

    @Test
    void testGetTileWidthWithInvalidImage2() throws Exception {
        Path file = TestUtil.getImage("unknown");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getTileWidth());
        }
    }

    @Test
    void testGetTileWidthWithEmptyImage() throws Exception {
        Path file = TestUtil.getImage("empty");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getTileWidth());
        }
    }

    @Test
    void testGetTileWidthWithSourceNotSet() {
        assertThrows(IllegalStateException.class, () -> instance.getTileWidth());
    }

    /* getWidth() */

    @Test
    void testGetWidthWithValidImage() throws Exception {
        Path file = TestUtil.getImage("jp2");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(64, instance.getWidth());
        }
    }

    @Test
    void testGetWidthWithInvalidImage1() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getWidth());
        }
    }

    @Test
    void testGetWidthWithInvalidImage2() throws Exception {
        Path file = TestUtil.getImage("unknown");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getWidth());
        }
    }

    @Test
    void testGetWidthWithEmptyImage() throws Exception {
        Path file = TestUtil.getImage("empty");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getWidth());
        }
    }

    @Test
    void testGetWidthWithSourceNotSet() {
        assertThrows(IllegalStateException.class, () -> instance.getWidth());
    }

    /* getXMP() */

    @Test
    void testGetXMPWithValidImageContainingXMP() throws Exception {
        Path file = TestUtil.getImage("jp2-xmp.jp2");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);

            String xmpStr = instance.getXMP();
            assertTrue(xmpStr.startsWith("<rdf:RDF "));
            assertTrue(xmpStr.endsWith("</rdf:RDF>"));
        }
    }

    @Test
    void testGetXMPWithValidImageNotContainingXMP() throws Exception {
        Path file = TestUtil.getImage("jp2");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertNull(instance.getXMP());
        }
    }

    @Test
    void testGetXMPWithInvalidImage1() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getXMP());
        }
    }

    @Test
    void testGetXMPWithInvalidImage2() throws Exception {
        Path file = TestUtil.getImage("unknown");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getXMP());
        }
    }

    @Test
    void testGetXMPWithEmptyImage() throws Exception {
        Path file = TestUtil.getImage("empty");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertThrows(SourceFormatException.class, () -> instance.getXMP());
        }
    }

    @Test
    void testGetXMPWithSourceNotSet() {
        assertThrows(IllegalStateException.class, () -> instance.getXMP());
    }

}