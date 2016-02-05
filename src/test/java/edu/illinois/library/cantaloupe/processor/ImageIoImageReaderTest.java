package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.MediaType;

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
                TestUtil.getImage("jpg-rgb-64x56x8-baseline.jpg"));
    }

    @After
    public void tearDown() throws Exception {
        reader.dispose();
    }

    @Test
    public void testSupportedFormats() {
        final HashSet<SourceFormat> formats = new HashSet<>();
        for (String mediaType : ImageIO.getReaderMIMETypes()) {
            final SourceFormat sourceFormat =
                    SourceFormat.getSourceFormat(new MediaType(mediaType));
            if (sourceFormat != null && !sourceFormat.equals(SourceFormat.UNKNOWN)) {
                formats.add(sourceFormat);
            }
        }
        assertEquals(formats, ImageIoImageReader.supportedFormats());
    }

    @Test
    public void testGetNumResolutionsWithFile() throws Exception {
        assertEquals(1, reader.getNumResolutions());
        // TODO: multiresolution
    }

    @Test
    public void testReadSize() throws Exception {
        Dimension expected = new Dimension(64, 56);
        Dimension actual = reader.readSize();
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
        reader.setSource(TestUtil.getImage("tif-rgb-monores-64x56x16-striped-uncompressed.tif"));

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
        reader.setSource(TestUtil.getImage("tif-rgb-monores-64x56x16-striped-uncompressed.tif"));

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
