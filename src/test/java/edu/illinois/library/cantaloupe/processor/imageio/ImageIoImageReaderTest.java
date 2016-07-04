package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.Format;
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

public class ImageIoImageReaderTest {

    private ImageIoImageReader reader;

    @Before
    public void setUp() throws Exception {
        reader = new ImageIoImageReader(
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
        assertEquals(formats, ImageIoImageReader.supportedFormats());
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
        reader = new ImageIoImageReader(
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
        reader = new ImageIoImageReader(
                TestUtil.getImage("tif-rgb-multires-64x56x16-tiled-uncompressed.tif"),
                Format.TIF);
        Dimension expected = new Dimension(16, 14);
        Dimension actual = reader.getSize(2);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetTileSizeWithTiledSource() throws Exception {
        reader = new ImageIoImageReader(
                TestUtil.getImage("tif-rgb-monores-64x56x8-tiled-uncompressed.tif"),
                Format.TIF);
        Dimension expected = new Dimension(16, 16);
        Dimension actual = reader.getTileSize(0);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetTileSizeWithUntiledSource() throws Exception {
        reader = new ImageIoImageReader(
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
        ReductionFactor rf = new ReductionFactor();
        Set<ImageIoImageReader.ReaderHint> hints = new HashSet<>();

        BufferedImage image = reader.read(ops, rf, hints);

        assertEquals(40, image.getWidth());
        assertEquals(40, image.getHeight());
        assertEquals(0, rf.factor);
        assertTrue(hints.contains(ImageIoImageReader.ReaderHint.ALREADY_CROPPED));
    }

    @Test
    public void testReadWithMonoResolutionImageAndScaleFactor() throws Exception {
        OperationList ops = new OperationList();
        Crop crop = new Crop();
        crop.setX(10f);
        crop.setY(10f);
        crop.setWidth(40f);
        crop.setHeight(40f);
        ops.add(crop);
        Scale scale = new Scale();
        scale.setWidth(10);
        scale.setHeight(10);
        ops.add(scale);
        ReductionFactor rf = new ReductionFactor();
        Set<ImageIoImageReader.ReaderHint> hints = new HashSet<>();

        BufferedImage image = reader.read(ops, rf, hints);

        assertEquals(10, image.getWidth());
        assertEquals(10, image.getHeight());
        assertEquals(2, rf.factor);
        assertTrue(hints.contains(ImageIoImageReader.ReaderHint.ALREADY_CROPPED));
    }

    @Test
    public void testReadWithMonoResolutionImageAndSubsamplingAndAbsoluteScale() throws Exception {
        reader = new ImageIoImageReader(
                TestUtil.getImage("tif-rgb-monores-64x56x16-striped-uncompressed.tif"),
                Format.TIF);

        OperationList ops = new OperationList();
        Scale scale = new Scale();
        scale.setWidth(16);
        scale.setHeight(14);
        ops.add(scale);
        ReductionFactor rf = new ReductionFactor();
        Set<ImageIoImageReader.ReaderHint> hints = new HashSet<>();

        BufferedImage image = reader.read(ops, rf, hints);

        assertEquals(16, image.getWidth());
        assertEquals(14, image.getHeight());
        assertEquals(2, rf.factor);
        assertTrue(hints.contains(ImageIoImageReader.ReaderHint.ALREADY_CROPPED));
    }

    @Test
    public void testReadWithMonoResolutionImageAndSubsamplingAndPercentScale() throws Exception {
        reader = new ImageIoImageReader(
                TestUtil.getImage("tif-rgb-monores-64x56x16-striped-uncompressed.tif"),
                Format.TIF);

        OperationList ops = new OperationList();
        Scale scale = new Scale();
        scale.setPercent(0.25f);
        ops.add(scale);
        ReductionFactor rf = new ReductionFactor();
        Set<ImageIoImageReader.ReaderHint> hints = new HashSet<>();

        BufferedImage image = reader.read(ops, rf, hints);

        assertEquals(16, image.getWidth());
        assertEquals(14, image.getHeight());
        assertEquals(2, rf.factor);
        assertTrue(hints.contains(ImageIoImageReader.ReaderHint.ALREADY_CROPPED));
    }

    @Test
    public void testReadWithMultiResolutionImage() {
        // TODO: write this
    }

}
