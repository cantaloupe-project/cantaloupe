package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.processor.ReductionFactor;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ImageReaderTest {

    private ImageReader reader;

    @Before
    public void setUp() throws Exception {
        reader = new ImageReader(
                TestUtil.getImage("jpg-rgb-64x56x8-baseline.jpg"), Format.JPG);
    }

    @After
    public void tearDown() throws Exception {
        reader.dispose();
    }

    @Test
    public void testSupportedFormats() {
        final HashSet<Format> formats = new HashSet<>();
        for (String mediaType : ImageIO.getReaderMIMETypes()) {
            if (mediaType.equals("image/jp2")) {
                continue;
            }
            final Format format = Format.getFormat(mediaType);
            if (format != null && !format.equals(Format.UNKNOWN)) {
                formats.add(format);
            }
        }
        assertEquals(formats, ImageReader.supportedFormats());
    }

    @Test
    public void testGetMetadata() throws Exception {
        assertNotNull(reader.getMetadata(0));
    }

    @Test
    public void testGetNumResolutions() throws Exception {
        // monoresolution
        assertEquals(1, reader.getNumResolutions());
        // multiresolution
        reader = new ImageReader(
                TestUtil.getImage("tif-rgb-multires-64x56x16-tiled-uncompressed.tif"),
                Format.TIF);
        assertEquals(3, reader.getNumResolutions());
    }

    @Test
    public void testGetSize() throws Exception {
        Dimension expected = new Dimension(64, 56);
        Dimension actual = reader.getSize();
        assertEquals(expected, actual);
    }

    @Test
    public void testGetSizeWithIndex() throws Exception {
        reader = new ImageReader(
                TestUtil.getImage("tif-rgb-multires-64x56x16-tiled-uncompressed.tif"),
                Format.TIF);
        Dimension expected = new Dimension(16, 14);
        Dimension actual = reader.getSize(2);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetTileSizeWithTiledSource() throws Exception {
        reader = new ImageReader(
                TestUtil.getImage("tif-rgb-monores-64x56x8-tiled-uncompressed.tif"),
                Format.TIF);
        Dimension expected = new Dimension(16, 16);
        Dimension actual = reader.getTileSize(0);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetTileSizeWithUntiledSource() throws Exception {
        reader = new ImageReader(
                TestUtil.getImage("tif-rgb-monores-64x56x8-striped-uncompressed.tif"),
                Format.TIF);
        Dimension expected = new Dimension(64, 42);
        Dimension actual = reader.getTileSize(0);
        assertEquals(expected, actual);
    }

    @Test
    public void testReadWithMonoResolutionImageAndNoScaleFactor() throws Exception {
        OperationList ops = new OperationList();
        Crop crop = new Crop();
        crop.setX(10f);
        crop.setY(10f);
        crop.setWidth(40f);
        crop.setHeight(40f);
        ops.add(crop);
        Scale scale = new Scale();
        scale.setWidth(35);
        scale.setHeight(35);
        ops.add(scale);
        Orientation orientation = Orientation.ROTATE_0;
        ReductionFactor rf = new ReductionFactor();
        Set<ImageReader.Hint> hints = new HashSet<>();

        BufferedImage image = reader.read(ops, orientation, rf, hints);

        assertEquals(40, image.getWidth());
        assertEquals(40, image.getHeight());
        assertEquals(0, rf.factor);
        assertTrue(hints.contains(ImageReader.Hint.ALREADY_CROPPED));
    }

    @Test
    public void testReadWithMultiResolutionImage() {
        // TODO: write this
    }

}
