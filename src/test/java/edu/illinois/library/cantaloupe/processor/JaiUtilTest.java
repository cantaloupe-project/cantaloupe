package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.Sharpen;
import edu.illinois.library.cantaloupe.image.Transpose;
import edu.illinois.library.cantaloupe.processor.imageio.ImageReader;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import java.awt.image.RenderedImage;

import static org.junit.Assert.*;

public class JaiUtilTest {

    private static final String IMAGE = "images/jpg-rgb-64x56x8-baseline.jpg";

    @Test
    public void testCropImage() throws Exception {
        RenderedOp inImage = getFixture(IMAGE);

        // full
        Crop crop = new Crop();
        crop.setFull(true);
        RenderedOp outImage = JaiUtil.cropImage(inImage, crop);
        assertSame(inImage, outImage);

        // square
        crop = new Crop();
        crop.setShape(Crop.Shape.SQUARE);
        outImage = JaiUtil.cropImage(inImage, crop);
        assertEquals(56, outImage.getWidth());
        assertEquals(56, outImage.getHeight());

        // pixel crop
        crop = new Crop();
        crop.setWidth(50f);
        crop.setHeight(50f);
        outImage = JaiUtil.cropImage(inImage, crop);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // percentage crop
        crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setX(0.5f);
        crop.setY(0.5f);
        crop.setWidth(0.5f);
        crop.setHeight(0.5f);
        outImage = JaiUtil.cropImage(inImage, crop);
        assertEquals(32, outImage.getWidth());
        assertEquals(28, outImage.getHeight());
    }

    @Test
    public void testCropImageWithReductionFactor() {
        // TODO: write this
    }

    @Test
    public void testFilterImage() {
        // TODO: write this
    }

    @Test
    public void testReformatImage() throws Exception {
        final OperationList ops = new OperationList();
        ImageReader reader = new ImageReader(
                TestUtil.getFixture(IMAGE), Format.JPG);
        RenderedImage image = reader.readRendered(ops, Orientation.ROTATE_0,
                new ReductionFactor());
        PlanarImage planarImage = PlanarImage.wrapRenderedImage(image);
        RenderedOp renderedOp = JaiUtil.getAsRenderedOp(planarImage);
        assertEquals(64, renderedOp.getWidth());
        assertEquals(56, renderedOp.getHeight());
    }

    @Test
    public void testRotateImage() throws Exception {
        RenderedOp inImage = getFixture(IMAGE);

        // test with no-op rotate
        Rotate rotate = new Rotate(0);
        RenderedOp rotatedImage = JaiUtil.rotateImage(inImage, rotate);
        assertSame(inImage, rotatedImage);

        // test with non-no-op crop
        rotate = new Rotate(45);
        rotatedImage = JaiUtil.rotateImage(inImage, rotate);

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

    @Test
    public void testScaleImage() throws Exception {
        RenderedOp image = getFixture(IMAGE);
        final Interpolation interpolation =
                Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
        final ReductionFactor rf = new ReductionFactor();
        final double fudge = 0.00000001f;

        // test with Mode.FULL
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        RenderedOp scaledImage = JaiUtil.scaleImage(image, scale, interpolation, rf);
        assertSame(image, scaledImage);

        // Mode.ASPECT_FIT_WIDTH
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        // down
        int width = 50;
        scale.setWidth(width);
        scaledImage = JaiUtil.scaleImage(image, scale, interpolation, rf);
        assertEquals(width, scaledImage.getWidth(), fudge);
        assertEquals(
                Math.round((width / (float) image.getWidth()) * image.getHeight()),
                scaledImage.getHeight(), fudge);
        // up
        width = 90;
        scale.setWidth(width);
        scaledImage = JaiUtil.scaleImage(image, scale, interpolation, rf);
        assertEquals(width, scaledImage.getWidth(), fudge);
        assertEquals(
                Math.round((width / (float) image.getWidth()) * image.getHeight()),
                scaledImage.getHeight(), fudge);

        // Mode.ASPECT_FIT_HEIGHT
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        // down
        int height = 25;
        scale.setHeight(height);
        scaledImage = JaiUtil.scaleImage(image, scale, interpolation, rf);
        assertEquals(
                Math.round((height / (float) image.getHeight()) * image.getWidth()),
                scaledImage.getWidth(), fudge);
        assertEquals(height, scaledImage.getHeight(), fudge);
        // up
        height = 90;
        scale.setHeight(height);
        scaledImage = JaiUtil.scaleImage(image, scale, interpolation, rf);
        assertEquals(
                Math.round((height / (float) image.getHeight()) * image.getWidth()),
                scaledImage.getWidth(), fudge);
        assertEquals(height, scaledImage.getHeight(), fudge);

        // Mode.ASPECT_FIT_INSIDE
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        // down
        width = 40;
        height = 38;
        scale.setWidth(width);
        scale.setHeight(height);
        scaledImage = JaiUtil.scaleImage(image, scale, interpolation, rf);
        assertEquals(width, scaledImage.getWidth(), fudge);
        assertEquals(
                Math.round((width / (float) image.getWidth()) * image.getHeight()),
                scaledImage.getHeight(), fudge);
        // up
        width = 90;
        height = 88;
        scale.setWidth(width);
        scale.setHeight(height);
        scaledImage = JaiUtil.scaleImage(image, scale, interpolation, rf);
        assertEquals(width, scaledImage.getWidth(), fudge);
        assertEquals(
                Math.round((width / (float) image.getWidth()) * image.getHeight()),
                scaledImage.getHeight(), fudge);

        // Mode.NON_ASPECT_FILL
        scale = new Scale();
        scale.setMode(Scale.Mode.NON_ASPECT_FILL);
        // down
        width = 45;
        height = 42;
        scale.setWidth(width);
        scale.setHeight(height);
        scaledImage = JaiUtil.scaleImage(image, scale, interpolation, rf);
        assertEquals(width, scaledImage.getWidth(), fudge);
        assertEquals(height, scaledImage.getHeight(), fudge);
        // up
        width = 85;
        height = 82;
        scale.setWidth(width);
        scale.setHeight(height);
        scaledImage = JaiUtil.scaleImage(image, scale, interpolation, rf);
        assertEquals(width, scaledImage.getWidth(), fudge);
        assertEquals(height, scaledImage.getHeight(), fudge);

        // Percent
        scale = new Scale();
        // down
        float percent = 0.5f;
        scale.setPercent(percent);
        scaledImage = JaiUtil.scaleImage(image, scale, interpolation, rf);
        assertEquals(image.getWidth() * percent, scaledImage.getWidth(), fudge);
        assertEquals(image.getHeight() * percent, scaledImage.getHeight(), fudge);
        // up
        percent = 1.1f;
        scale.setPercent(percent);
        scaledImage = JaiUtil.scaleImage(image, scale, interpolation, rf);
        assertEquals(Math.round(image.getWidth() * percent),
                scaledImage.getWidth());
        assertEquals(Math.round(image.getHeight() * percent),
                scaledImage.getHeight());
    }

    @Test
    public void testScaleImageUsingSubsampleAverage() throws Exception {
        RenderedOp image = getFixture(IMAGE);
        final ReductionFactor rf = new ReductionFactor();
        final double fudge = 0.00000001f;

        // test with Mode.FULL
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        RenderedOp scaledImage = JaiUtil.scaleImageUsingSubsampleAverage(image, scale, rf);
        assertSame(image, scaledImage);

        // Mode.ASPECT_FIT_WIDTH
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        // down
        int width = 50;
        scale.setWidth(width);
        scaledImage = JaiUtil.scaleImageUsingSubsampleAverage(image, scale, rf);
        assertEquals(width, scaledImage.getWidth(), fudge);
        assertEquals(
                Math.round((width / (float) image.getWidth()) * image.getHeight()),
                scaledImage.getHeight(), fudge);
        // up
        width = 80;
        scale.setWidth(width);
        scaledImage = JaiUtil.scaleImageUsingSubsampleAverage(image, scale, rf);
        assertEquals(width, scaledImage.getWidth(), fudge);
        assertEquals(
                Math.round((width / (float) image.getWidth()) * image.getHeight()),
                scaledImage.getHeight(), fudge);

        // Mode.ASPECT_FIT_HEIGHT
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        // down
        int height = 36;
        scale.setHeight(height);
        scaledImage = JaiUtil.scaleImageUsingSubsampleAverage(image, scale, rf);
        assertEquals(
                Math.round((height / (float) image.getHeight()) * image.getWidth()),
                scaledImage.getWidth(), fudge);
        assertEquals(height, scaledImage.getHeight(), fudge);
        // up
        height = 72;
        scale.setHeight(height);
        scaledImage = JaiUtil.scaleImageUsingSubsampleAverage(image, scale, rf);
        assertEquals(
                Math.round((height / (float) image.getHeight()) * image.getWidth()),
                scaledImage.getWidth(), fudge);
        assertEquals(height, scaledImage.getHeight(), fudge);

        // Mode.ASPECT_FIT_INSIDE
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        // down
        width = 40;
        height = 40;
        scale.setWidth(width);
        scale.setHeight(height);
        scaledImage = JaiUtil.scaleImageUsingSubsampleAverage(image, scale, rf);
        assertEquals(width, scaledImage.getWidth(), fudge);
        assertEquals(
                Math.round((height / (float) image.getWidth()) * image.getHeight()),
                scaledImage.getHeight(), fudge);
        // up
        width = 90;
        height = 90;
        scale.setWidth(width);
        scale.setHeight(height);
        scaledImage = JaiUtil.scaleImageUsingSubsampleAverage(image, scale, rf);
        assertEquals(width, scaledImage.getWidth(), fudge);
        assertEquals(
                Math.round((width / (float) image.getWidth()) * image.getHeight()),
                scaledImage.getHeight(), fudge);

        // Mode.NON_ASPECT_FILL
        scale = new Scale();
        scale.setMode(Scale.Mode.NON_ASPECT_FILL);
        // down
        width = 45;
        height = 42;
        scale.setWidth(width);
        scale.setHeight(height);
        scaledImage = JaiUtil.scaleImageUsingSubsampleAverage(image, scale, rf);
        assertEquals(width, scaledImage.getWidth(), fudge);
        assertEquals(height, scaledImage.getHeight(), fudge);
        // up
        width = 90;
        height = 88;
        scale.setWidth(width);
        scale.setHeight(height);
        scaledImage = JaiUtil.scaleImageUsingSubsampleAverage(image, scale, rf);
        assertEquals(width, scaledImage.getWidth(), fudge);
        assertEquals(height, scaledImage.getHeight(), fudge);

        // Percent
        scale = new Scale();
        // down
        float percent = 0.5f;
        scale.setPercent(percent);
        scaledImage = JaiUtil.scaleImageUsingSubsampleAverage(image, scale, rf);
        assertEquals(image.getWidth() * percent, scaledImage.getWidth(), fudge);
        assertEquals(image.getHeight() * percent, scaledImage.getHeight(), fudge);
        // up
        percent = 1.2f;
        scale.setPercent(percent);
        scaledImage = JaiUtil.scaleImageUsingSubsampleAverage(image, scale, rf);
        assertEquals(Math.round(image.getWidth() * percent),
                scaledImage.getWidth());
        assertEquals(Math.round(image.getHeight() * percent),
                scaledImage.getHeight());
    }

    @Test
    public void testSharpenImage() throws Exception {
        RenderedOp image = getFixture(IMAGE);

        // test with no-op sharpen
        Sharpen sharpen = new Sharpen();
        sharpen.setAmount(0);
        RenderedOp sharpenedImage = JaiUtil.sharpenImage(image, sharpen);
        assertSame(image, sharpenedImage);

        // test with non-no-op sharpen
        sharpen = new Sharpen();
        sharpen.setAmount(0.5f);
        sharpenedImage = JaiUtil.sharpenImage(image, sharpen);
        assertNotSame(image, sharpenedImage);
    }

    @Test
    public void testTransposeImage() throws Exception {
        // TODO: this test could be better
        RenderedOp image = getFixture(IMAGE);
        // horizontal
        Transpose transpose = Transpose.HORIZONTAL;
        RenderedOp result = JaiUtil.transposeImage(image, transpose);
        assertEquals(image.getWidth(), result.getWidth());
        assertEquals(image.getHeight(), result.getHeight());
        // vertical
        transpose = Transpose.VERTICAL;
        result = JaiUtil.transposeImage(image, transpose);
        assertEquals(image.getWidth(), result.getWidth());
        assertEquals(image.getHeight(), result.getHeight());
    }

    private RenderedOp getFixture(final String name) throws Exception {
        final OperationList ops = new OperationList();
        ImageReader reader = new ImageReader(
                TestUtil.getFixture(name), Format.JPG);
        RenderedImage image = reader.readRendered(ops, Orientation.ROTATE_0,
                new ReductionFactor());
        PlanarImage planarImage = PlanarImage.wrapRenderedImage(image);
        return JaiUtil.getAsRenderedOp(planarImage);
    }

}