package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class JPEG2000MetadataReaderTest {

    private JPEG2000MetadataReader instance;

    @Before
    public void setUp() throws Exception {
        instance = new JPEG2000MetadataReader();
    }

    /* getComponentSize() */

    @Test
    public void testGetComponentSizeWithValidImage() throws Exception {
        Path file = TestUtil.getImage("jp2");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(8, instance.getComponentSize());
        }
    }

    @Test(expected = IOException.class)
    public void testGetComponentSizeWithInvalidImage1() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getComponentSize();
        }
    }

    @Test(expected = IOException.class)
    public void testGetComponentSizeWithInvalidImage2() throws Exception {
        Path file = TestUtil.getImage("unknown");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getComponentSize();
        }
    }

    @Test(expected = IOException.class)
    public void testGetComponentSizeWithEmptyImage() throws Exception {
        Path file = TestUtil.getImage("empty");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getComponentSize();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testGetComponentSizeWithSourceNotSet() throws Exception {
        instance.getComponentSize();
    }

    /* getHeight() */

    @Test
    public void testGetHeightWithValidImage() throws Exception {
        Path file = TestUtil.getImage("jp2");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(56, instance.getHeight());
        }
    }

    @Test(expected = IOException.class)
    public void testGetHeightWithInvalidImage1() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getHeight();
        }
    }

    @Test(expected = IOException.class)
    public void testGetHeightWithInvalidImage2() throws Exception {
        Path file = TestUtil.getImage("unknown");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getHeight();
        }
    }

    @Test(expected = IOException.class)
    public void testGetHeightWithEmptyImage() throws Exception {
        Path file = TestUtil.getImage("empty");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getHeight();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testGetHeightWithSourceNotSet() throws Exception {
        instance.getHeight();
    }

    /* getNumComponents() */

    @Test
    public void testGetNumComponentsWithValidImage() throws Exception {
        Path file = TestUtil.getImage("jp2-5res-rgb-64x56x8-monotiled-lossy.jp2");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(3, instance.getNumComponents());
        }
    }

    @Test(expected = IOException.class)
    public void testGetNumComponentsWithInvalidImage1() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getNumComponents();
        }
    }

    @Test(expected = IOException.class)
    public void testGetNumComponentsWithInvalidImage2() throws Exception {
        Path file = TestUtil.getImage("unknown");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getNumComponents();
        }
    }

    @Test(expected = IOException.class)
    public void testGetNumComponentsWithEmptyImage() throws Exception {
        Path file = TestUtil.getImage("empty");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getNumComponents();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testGetNumComponentsWithSourceNotSet() throws Exception {
        instance.getNumComponents();
    }

    /* getNumDecompositionLevels() */

    @Test
    public void testGetNumDecompositionLevelsWithValidImage() throws Exception {
        Path file = TestUtil.getImage("jp2");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(5, instance.getNumDecompositionLevels());
        }
    }

    @Test(expected = IOException.class)
    public void testGetNumDecompositionLevelsWithInvalidImage1() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getNumDecompositionLevels();
        }
    }

    @Test(expected = IOException.class)
    public void testGetNumDecompositionLevelsWithInvalidImage2() throws Exception {
        Path file = TestUtil.getImage("unknown");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getNumDecompositionLevels();
        }
    }

    @Test(expected = IOException.class)
    public void testGetNumDecompositionLevelsWithEmptyImage() throws Exception {
        Path file = TestUtil.getImage("empty");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getNumDecompositionLevels();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testGetNumDecompositionLevelsWithSourceNotSet() throws Exception {
        instance.getNumDecompositionLevels();
    }

    /* getTileHeight() */

    @Test
    public void testGetTileHeightWithValidImage() throws Exception {
        Path file = TestUtil.getImage("jp2");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(56, instance.getTileHeight());
        }
    }

    @Test(expected = IOException.class)
    public void testGetTileHeightWithInvalidImage1() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getTileHeight();
        }
    }

    @Test(expected = IOException.class)
    public void testGetTileHeightWithInvalidImage2() throws Exception {
        Path file = TestUtil.getImage("unknown");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getTileHeight();
        }
    }

    @Test(expected = IOException.class)
    public void testGetTileHeightWithEmptyImage() throws Exception {
        Path file = TestUtil.getImage("empty");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getTileHeight();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testGetTileHeightWithSourceNotSet() throws Exception {
        instance.getTileHeight();
    }

    /* getTileWidth() */

    @Test
    public void testGetTileWidthWithValidImage() throws Exception {
        Path file = TestUtil.getImage("jp2");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(64, instance.getTileWidth());
        }
    }

    @Test(expected = IOException.class)
    public void testGetTileWidthWithInvalidImage1() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getTileWidth();
        }
    }

    @Test(expected = IOException.class)
    public void testGetTileWidthWithInvalidImage2() throws Exception {
        Path file = TestUtil.getImage("unknown");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getTileWidth();
        }
    }

    @Test(expected = IOException.class)
    public void testGetTileWidthWithEmptyImage() throws Exception {
        Path file = TestUtil.getImage("empty");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getTileWidth();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testGetTileWidthWithSourceNotSet() throws Exception {
        instance.getTileWidth();
    }

    /* getWidth() */

    @Test
    public void testGetWidthWithValidImage() throws Exception {
        Path file = TestUtil.getImage("jp2");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            assertEquals(64, instance.getWidth());
        }
    }

    @Test(expected = IOException.class)
    public void testGetWidthWithInvalidImage1() throws Exception {
        Path file = TestUtil.getImage("jpg");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getWidth();
        }
    }

    @Test(expected = IOException.class)
    public void testGetWidthWithInvalidImage2() throws Exception {
        Path file = TestUtil.getImage("unknown");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getWidth();
        }
    }

    @Test(expected = IOException.class)
    public void testGetWidthWithEmptyImage() throws Exception {
        Path file = TestUtil.getImage("empty");
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
        Path file = TestUtil.getImage("jp2-xmp.jp2");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);

            String xmpStr = instance.getXMP();
            assertTrue(xmpStr.startsWith("<rdf:RDF "));
            assertTrue(xmpStr.endsWith("</rdf:RDF>"));
        }
    }

    @Test
    public void testGetXMPWithValidImageNotContainingXMP() throws Exception {
        Path file = TestUtil.getImage("jp2");
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

    @Test(expected = IOException.class)
    public void testGetXMPWithUnknownImage() throws Exception {
        Path file = TestUtil.getImage("unknown");
        try (ImageInputStream is = ImageIO.createImageInputStream(file.toFile())) {
            instance.setSource(is);
            instance.getXMP();
        }
    }

    @Test(expected = IOException.class)
    public void testGetXMPWithEmptyImage() throws Exception {
        Path file = TestUtil.getImage("empty");
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