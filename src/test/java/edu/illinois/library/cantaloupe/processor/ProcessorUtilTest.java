package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.Transpose;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ProcessorUtilTest {

    @Test
    public void testConvertToRgb() throws IOException {
        // test that input image of TYPE_INT_RGB is returned with no conversion
        BufferedImage custom = new BufferedImage(10, 10,
                BufferedImage.TYPE_INT_RGB);
        assertSame(custom, ProcessorUtil.convertToRgb(custom));

        // test with image of TYPE_CUSTOM
        custom = ImageIO.read(TestUtil.getFixture("tif"));
        BufferedImage output = ProcessorUtil.convertToRgb(custom);
        assertEquals(BufferedImage.TYPE_INT_RGB, output.getType());
    }

    @Test
    public void testCropImageWithBufferedImage() {
        BufferedImage inImage = new BufferedImage(100, 100,
                BufferedImage.TYPE_INT_RGB);
        // full
        Crop crop = new Crop();
        crop.setFull(true);
        BufferedImage outImage = ProcessorUtil.cropImage(inImage, crop);
        assertSame(inImage, outImage);

        // pixel crop
        crop = new Crop();
        crop.setWidth(50f);
        crop.setHeight(50f);
        outImage = ProcessorUtil.cropImage(inImage, crop);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // percentage crop
        crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setX(0.5f);
        crop.setY(0.5f);
        crop.setWidth(0.5f);
        crop.setHeight(0.5f);
        outImage = ProcessorUtil.cropImage(inImage, crop);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());
    }

    @Test
    public void testCropImageWithBufferedImageAndReductionFactor() {
        BufferedImage inImage = new BufferedImage(100, 100,
                BufferedImage.TYPE_INT_RGB);

        // full crop
        Crop crop = new Crop();
        crop.setFull(true);
        int reductionFactor = 1;
        BufferedImage outImage = ProcessorUtil.cropImage(inImage, crop,
                reductionFactor);
        assertSame(inImage, outImage);

        // pixel crop
        crop = new Crop();
        crop.setWidth(50f);
        crop.setHeight(50f);
        reductionFactor = 1;
        outImage = ProcessorUtil.cropImage(inImage, crop, reductionFactor);
        assertEquals(25, outImage.getWidth());
        assertEquals(25, outImage.getHeight());

        // percentage crop
        crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setX(0.5f);
        crop.setY(0.5f);
        crop.setWidth(0.5f);
        crop.setHeight(0.5f);
        reductionFactor = 1;
        outImage = ProcessorUtil.cropImage(inImage, crop, reductionFactor);
        assertEquals(25, outImage.getWidth());
        assertEquals(25, outImage.getHeight());
    }

    @Test
    public void testFilterImageWithBufferedImage() {
        // TODO: write this
    }

    @Test
    public void testGetReductionFactor() {
        assertEquals(0, ProcessorUtil.getReductionFactor(0.75f, 5));
        assertEquals(1, ProcessorUtil.getReductionFactor(0.5f, 5));
        assertEquals(1, ProcessorUtil.getReductionFactor(0.45f, 5));
        assertEquals(2, ProcessorUtil.getReductionFactor(0.25f, 5));
        assertEquals(2, ProcessorUtil.getReductionFactor(0.2f, 5));
        assertEquals(3, ProcessorUtil.getReductionFactor(0.125f, 5));
        assertEquals(4, ProcessorUtil.getReductionFactor(0.0625f, 5));
        assertEquals(5, ProcessorUtil.getReductionFactor(0.03125f, 5));
        // max
        assertEquals(1, ProcessorUtil.getReductionFactor(0.2f, 1));
    }

    @Test
    public void testGetScale() {
        final double fudge = 0.0000001f;
        assertTrue(Math.abs(ProcessorUtil.getScale(0)) - Math.abs(1.0f) < fudge);
        assertTrue(Math.abs(ProcessorUtil.getScale(1)) - Math.abs(0.5f) < fudge);
        assertTrue(Math.abs(ProcessorUtil.getScale(2)) - Math.abs(0.25f) < fudge);
        assertTrue(Math.abs(ProcessorUtil.getScale(3)) - Math.abs(0.125f) < fudge);
        assertTrue(Math.abs(ProcessorUtil.getScale(4)) - Math.abs(0.0625f) < fudge);
        assertTrue(Math.abs(ProcessorUtil.getScale(5)) - Math.abs(0.03125f) < fudge);
    }

    @Test
    public void testGetSizeWithFile() throws Exception {
        Dimension expected = new Dimension(100, 88);
        Dimension actual = ProcessorUtil.getSize(TestUtil.getFixture("jpg"),
                SourceFormat.JPG);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetSizeWithInputStream() throws Exception {
        Dimension expected = new Dimension(100, 88);
        ReadableByteChannel readableChannel =
                new FileInputStream(TestUtil.getFixture("jpg")).getChannel();
        Dimension actual = ProcessorUtil.getSize(readableChannel,
                SourceFormat.JPG);
        assertEquals(expected, actual);
    }

    @Test
    public void testImageIoOutputFormats() {
        // assemble a set of all ImageIO output formats
        final String[] writerMimeTypes = ImageIO.getWriterMIMETypes();
        final Set<OutputFormat> outputFormats = new HashSet<>();
        for (OutputFormat outputFormat : OutputFormat.values()) {
            for (String mimeType : writerMimeTypes) {
                if (outputFormat.getMediaType().equals(mimeType.toLowerCase())) {
                    outputFormats.add(outputFormat);
                }
            }
        }
        assertEquals(outputFormats, ProcessorUtil.imageIoOutputFormats());
    }

    @Test
    public void testReadImageIntoBufferedImageWithFile() {
        // this will be tested in ProcessorTest
    }

    @Test
    public void testReadImageIntoBufferedImageWithInputStream() {
        // this will be tested in ProcessorTest
    }

    @Test
    public void testRotateImageWithBufferedImage() {
        BufferedImage inImage = new BufferedImage(200, 100,
                BufferedImage.TYPE_INT_RGB);
        final int sourceWidth = inImage.getWidth();
        final int sourceHeight = inImage.getHeight();

        Rotate rotate = new Rotate(15);
        BufferedImage outImage = ProcessorUtil.rotateImage(inImage, rotate);

        final double radians = Math.toRadians(rotate.getDegrees());
        final int expectedWidth = (int) Math.round(Math.abs(sourceWidth *
                Math.cos(radians)) + Math.abs(sourceHeight *
                Math.sin(radians)));
        final int expectedHeight = (int) Math.round(Math.abs(sourceHeight *
                Math.cos(radians)) + Math.abs(sourceWidth *
                Math.sin(radians)));

        assertEquals(expectedWidth, outImage.getWidth());
        assertEquals(expectedHeight, outImage.getHeight());
    }

    @Test
    public void testScaleImageWithAffineTransform() {
        BufferedImage inImage = new BufferedImage(100, 100,
                BufferedImage.TYPE_INT_RGB);

        // Scale.Mode.FULL
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        BufferedImage outImage = ProcessorUtil.scaleImageWithAffineTransform(inImage, scale);
        assertSame(inImage, outImage);

        // Scale.Mode.ASPECT_FIT_WIDTH
        scale.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        scale.setWidth(50);
        outImage = ProcessorUtil.scaleImageWithAffineTransform(inImage, scale);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Scale.Mode.ASPECT_FIT_HEIGHT
        scale.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        scale.setHeight(50);
        outImage = ProcessorUtil.scaleImageWithAffineTransform(inImage, scale);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Scale.Mode.ASPECT_FIT_INSIDE
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setWidth(50);
        scale.setHeight(50);
        outImage = ProcessorUtil.scaleImageWithAffineTransform(inImage, scale);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());
    }

    @Test
    public void testScaleImageWithG2d() {
        BufferedImage inImage = new BufferedImage(100, 100,
                BufferedImage.TYPE_INT_RGB);

        // Scale.Mode.FULL
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        BufferedImage outImage = ProcessorUtil.scaleImageWithG2d(inImage, scale);
        assertSame(inImage, outImage);

        // Scale.Mode.ASPECT_FIT_WIDTH
        scale.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        scale.setWidth(50);
        outImage = ProcessorUtil.scaleImageWithG2d(inImage, scale);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Scale.Mode.ASPECT_FIT_HEIGHT
        scale.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        scale.setHeight(50);
        outImage = ProcessorUtil.scaleImageWithG2d(inImage, scale);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Scale.Mode.ASPECT_FIT_INSIDE
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setWidth(50);
        scale.setHeight(50);
        outImage = ProcessorUtil.scaleImageWithG2d(inImage, scale);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());
    }

    @Test
    public void testScaleImageWithG2dWithReductionFactor() {
        BufferedImage inImage = new BufferedImage(50, 50,
                BufferedImage.TYPE_INT_RGB);

        // Scale.Mode.ASPECT_FIT_WIDTH
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        scale.setWidth(50);
        int reductionFactor = 1;
        BufferedImage outImage = ProcessorUtil.scaleImageWithG2d(inImage, scale,
                reductionFactor, true);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Scale.Mode.ASPECT_FIT_HEIGHT
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        scale.setHeight(50);
        reductionFactor = 1;
        outImage = ProcessorUtil.scaleImageWithG2d(inImage, scale,
                reductionFactor, false);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Scale.Mode.ASPECT_FIT_INSIDE
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setWidth(50);
        scale.setHeight(50);
        reductionFactor = 1;
        outImage = ProcessorUtil.scaleImageWithG2d(inImage, scale,
                reductionFactor, true);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());
    }

    @Test
    public void testTransposeImage() {
        BufferedImage inImage = new BufferedImage(200, 100,
                BufferedImage.TYPE_INT_RGB);
        Transpose transpose = Transpose.HORIZONTAL;
        BufferedImage outImage = ProcessorUtil.transposeImage(inImage, transpose);

        assertEquals(200, outImage.getWidth());
        assertEquals(100, outImage.getHeight());
    }

    @Test
    public void testWriteImage() {
        // TODO: write this
    }

}
