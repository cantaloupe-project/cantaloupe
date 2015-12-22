package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.Transpose;
import edu.illinois.library.cantaloupe.test.TestUtil;

import javax.imageio.ImageIO;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.util.HashSet;
import java.util.Set;

public class ProcessorUtilTest extends CantaloupeTestCase {

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

    public void testCropImageWithRenderedOp() throws Exception {
        RenderedOp image = getFixture("jpg");

        // test with no-op crop
        Crop crop = new Crop();
        crop.setFull(true);
        RenderedOp croppedImage = ProcessorUtil.cropImage(image, crop);
        assertSame(image, croppedImage);

        // test with non-no-op crop
        crop = new Crop();
        crop.setX(0f);
        crop.setY(0f);
        crop.setWidth(50f);
        crop.setHeight(50f);
        croppedImage = ProcessorUtil.cropImage(image, crop);
        assertEquals(50, croppedImage.getWidth());
        assertEquals(50, croppedImage.getHeight());
    }

    public void testCropImageWithRenderedOpAndReductionFactor() {
        // TODO: write this
    }

    public void testFilterImageWithBufferedImage() {
        // TODO: write this
    }

    public void testFilterImageWithRenderedOp() {
        // TODO: write this
    }

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

    public void testGetScale() {
        final double fudge = 0.0000001f;
        assertTrue(Math.abs(ProcessorUtil.getScale(0)) - Math.abs(1.0f) < fudge);
        assertTrue(Math.abs(ProcessorUtil.getScale(1)) - Math.abs(0.5f) < fudge);
        assertTrue(Math.abs(ProcessorUtil.getScale(2)) - Math.abs(0.25f) < fudge);
        assertTrue(Math.abs(ProcessorUtil.getScale(3)) - Math.abs(0.125f) < fudge);
        assertTrue(Math.abs(ProcessorUtil.getScale(4)) - Math.abs(0.0625f) < fudge);
        assertTrue(Math.abs(ProcessorUtil.getScale(5)) - Math.abs(0.03125f) < fudge);
    }

    public void testGetSizeWithFile() throws Exception {
        Dimension expected = new Dimension(100, 88);
        Dimension actual = ProcessorUtil.getSize(TestUtil.getFixture("jpg"),
                SourceFormat.JPG);
        assertEquals(expected, actual);
    }

    public void testGetSizeWithInputStream() throws Exception {
        Dimension expected = new Dimension(100, 88);
        ReadableByteChannel readableChannel =
                new FileInputStream(TestUtil.getFixture("jpg")).getChannel();
        Dimension actual = ProcessorUtil.getSize(readableChannel,
                SourceFormat.JPG);
        assertEquals(expected, actual);
    }

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

    public void testReadImageIntoBufferedImageWithFile() {
        // this will be tested in ProcessorTest
    }

    public void testReadImageIntoBufferedImageWithInputStream() {
        // this will be tested in ProcessorTest
    }

    public void testReadImageWithJaiWithFile() {
        // this will be tested in ProcessorTest
    }

    public void testReadImageWithJaiWithInputStream() {
        // this will be tested in ProcessorTest
    }

    public void testReformatImage() throws Exception {
        final Dimension fullSize = new Dimension(100, 88);
        final OperationList ops = new OperationList();
        final ReductionFactor reductionFactor = new ReductionFactor();
        RenderedImage image = ProcessorUtil.readImageWithJai(
                TestUtil.getFixture("jpg"), SourceFormat.JPG, ops, fullSize,
                reductionFactor);
        PlanarImage planarImage = PlanarImage.wrapRenderedImage(image);
        RenderedOp renderedOp = ProcessorUtil.reformatImage(planarImage,
                new Dimension(512, 512));
        assertEquals(100, renderedOp.getWidth());
        assertEquals(88, renderedOp.getHeight());
    }

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

    public void testRotateImageWithRenderedOp() throws Exception {
        RenderedOp inImage = getFixture("jpg");

        // test with no-op rotate
        Rotate rotate = new Rotate(0);
        RenderedOp rotatedImage = ProcessorUtil.rotateImage(inImage, rotate);
        assertSame(inImage, rotatedImage);

        // test with non-no-op crop
        rotate = new Rotate(45);
        rotatedImage = ProcessorUtil.rotateImage(inImage, rotate);

        final int sourceWidth = inImage.getWidth();
        final int sourceHeight = inImage.getHeight();
        final double radians = Math.toRadians(rotate.getDegrees());
        // note that JAI appears to use flooring instead of rounding
        final int expectedWidth = (int) Math.floor(Math.abs(sourceWidth *
                Math.cos(radians)) + Math.abs(sourceHeight *
                Math.sin(radians)));
        final int expectedHeight = (int) Math.floor(Math.abs(sourceHeight *
                Math.cos(radians)) + Math.abs(sourceWidth *
                Math.sin(radians)));

        assertEquals(expectedWidth, rotatedImage.getWidth());
        assertEquals(expectedHeight, rotatedImage.getHeight());
    }

    public void testScaleImageWithRenderedOp() throws Exception {
        RenderedOp image = getFixture("jpg");

        // test with no-op scale
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        RenderedOp scaledImage = ProcessorUtil.scaleImage(image, scale);
        assertSame(image, scaledImage);

        // test with non-no-op crop
        scale = new Scale();
        scale.setPercent(0.5f);
        scaledImage = ProcessorUtil.scaleImage(image, scale);
        assertEquals(50, scaledImage.getWidth());
        assertEquals(44, scaledImage.getHeight());
    }

    public void testScaleImageWithRenderedOpWithReductionFactor() {
        // TODO: write this
    }

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

    public void testTransposeImageWithBufferedImage() {
        BufferedImage inImage = new BufferedImage(200, 100,
                BufferedImage.TYPE_INT_RGB);
        Transpose transpose = Transpose.HORIZONTAL;
        BufferedImage outImage = ProcessorUtil.transposeImage(inImage, transpose);

        assertEquals(200, outImage.getWidth());
        assertEquals(100, outImage.getHeight());
    }

    public void testTransposeImageWithRenderedOp() throws Exception {
        // TODO: this test could be better
        RenderedOp image = getFixture("jpg");
        // horizontal
        Transpose transpose = Transpose.HORIZONTAL;
        RenderedOp result = ProcessorUtil.transposeImage(image, transpose);
        assertEquals(100, result.getWidth());
        assertEquals(88, result.getHeight());
        // vertical
        transpose = Transpose.VERTICAL;
        result = ProcessorUtil.transposeImage(image, transpose);
        assertEquals(100, result.getWidth());
        assertEquals(88, result.getHeight());
    }

    public void testWriteImageWithBufferedImage() {
        // TODO: write this
    }

    public void testWriteImageWithRenderedOp() {
        // TODO: write this
    }

    private RenderedOp getFixture(final String name) throws Exception {
        final Dimension fullSize = new Dimension(100, 88);
        final OperationList ops = new OperationList();
        final ReductionFactor reductionFactor = new ReductionFactor();
        RenderedImage image = ProcessorUtil.readImageWithJai(
                TestUtil.getFixture(name), SourceFormat.JPG, ops, fullSize,
                reductionFactor);
        PlanarImage planarImage = PlanarImage.wrapRenderedImage(image);
        return ProcessorUtil.reformatImage(planarImage,
                new Dimension(512, 512));
    }

}
