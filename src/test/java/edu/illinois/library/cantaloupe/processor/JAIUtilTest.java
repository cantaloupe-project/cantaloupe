package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Sharpen;
import edu.illinois.library.cantaloupe.operation.Transpose;
import edu.illinois.library.cantaloupe.processor.codec.ImageReader;
import edu.illinois.library.cantaloupe.processor.codec.ImageReaderFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static edu.illinois.library.cantaloupe.test.Assert.ImageAssert.*;
import static org.junit.Assert.*;

public class JAIUtilTest extends BaseTest {

    private static final double DELTA = 0.00000001;
    private static final String IMAGE = "png-rgb-64x56x8.png";

    /* cropImage(RenderedOp, Crop) */

    @Test
    public void cropImage() throws Exception {
        RenderedOp inImage = readImage(IMAGE);

        // full
        Crop crop = new Crop();
        crop.setFull(true);
        RenderedOp outImage = JAIUtil.cropImage(inImage, crop);
        assertSame(inImage, outImage);

        // square
        crop = new Crop();
        crop.setShape(Crop.Shape.SQUARE);
        outImage = JAIUtil.cropImage(inImage, crop);
        assertEquals(56, outImage.getWidth());
        assertEquals(56, outImage.getHeight());

        // pixel crop
        crop = new Crop();
        crop.setWidth(50f);
        crop.setHeight(50f);
        outImage = JAIUtil.cropImage(inImage, crop);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // percentage crop
        crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setX(0.5f);
        crop.setY(0.5f);
        crop.setWidth(0.5f);
        crop.setHeight(0.5f);
        outImage = JAIUtil.cropImage(inImage, crop);
        assertEquals(32, outImage.getWidth());
        assertEquals(28, outImage.getHeight());
    }

    /* cropImage(RenderedOp, Crop, ReductionFactor) */

    @Test
    public void cropImageWithReductionFactor() throws Exception {
        RenderedOp inImage = readImage(IMAGE);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        // full
        Crop crop = new Crop();
        crop.setFull(true);
        ReductionFactor rf = new ReductionFactor(1);
        RenderedOp outImage = JAIUtil.cropImage(
                inImage, scaleConstraint, crop, rf);
        assertSame(inImage, outImage);

        // square
        crop = new Crop();
        crop.setShape(Crop.Shape.SQUARE);
        crop.setWidth(50f);
        crop.setHeight(50f);
        outImage = JAIUtil.cropImage(inImage, crop);
        assertEquals(inImage.getHeight(), outImage.getWidth());
        assertEquals(inImage.getHeight(), outImage.getHeight());

        // pixel crop
        crop = new Crop();
        crop.setWidth(50f);
        crop.setHeight(50f);
        rf = new ReductionFactor(1);
        outImage = JAIUtil.cropImage(inImage, scaleConstraint, crop, rf);
        assertEquals(25, outImage.getWidth());
        assertEquals(25, outImage.getHeight());

        // percentage crop
        crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setX(0.5f);
        crop.setY(0.5f);
        crop.setWidth(0.5f);
        crop.setHeight(0.5f);
        rf = new ReductionFactor(1);
        outImage = JAIUtil.cropImage(inImage, scaleConstraint, crop, rf);
        assertEquals(inImage.getWidth() / 4f, outImage.getWidth(), DELTA);
        assertEquals(inImage.getHeight() / 4f, outImage.getHeight(), DELTA);
    }

    /* getAsRenderedOp() */

    @Test
    public void getAsRenderedOp() throws Exception {
        RenderedImage image = readImage(IMAGE);
        PlanarImage planarImage = PlanarImage.wrapRenderedImage(image);
        RenderedOp renderedOp = JAIUtil.getAsRenderedOp(planarImage);
        assertEquals(64, renderedOp.getWidth());
        assertEquals(56, renderedOp.getHeight());
    }

    /* reduceTo8Bits() */

    @Test
    public void reduceTo8BitsWith16BitImage() throws Exception {
        RenderedOp inImage = readImage("png-rgb-64x56x16.png");
        assertEquals(16, inImage.getColorModel().getComponentSize(0));

        RenderedOp outImage = JAIUtil.reduceTo8Bits(inImage);
        assertEquals(8, outImage.getColorModel().getComponentSize(0));
    }

    @Test
    public void reduceTo8BitsWith8BitImage() throws Exception {
        RenderedOp inImage = readImage("png-rgb-64x56x8.png");
        assertEquals(8, inImage.getColorModel().getComponentSize(0));

        RenderedOp outImage = JAIUtil.reduceTo8Bits(inImage);
        assertSame(inImage, outImage);
    }

    /* rescalePixels() */

    @Test
    public void rescalePixels() {
        // TODO: write this
    }

    /* rotateImage() */

    @Test
    public void rotateImage() throws Exception {
        RenderedOp inImage = readImage(IMAGE);

        // test with no-op rotate
        Rotate rotate = new Rotate(0);
        RenderedOp rotatedImage = JAIUtil.rotateImage(inImage, rotate);
        assertSame(inImage, rotatedImage);

        // test with non-no-op crop
        rotate = new Rotate(45);
        rotatedImage = JAIUtil.rotateImage(inImage, rotate);

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

    /* scaleImage() */

    @Test
    public void scaleImage() throws Exception {
        RenderedOp image = readImage(IMAGE);
        final Interpolation interpolation =
                Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
        final ReductionFactor rf = new ReductionFactor();
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        // test with Mode.FULL
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        RenderedOp scaledImage = JAIUtil.scaleImage(
                image, scale, scaleConstraint, interpolation, rf);
        assertSame(image, scaledImage);

        // Mode.ASPECT_FIT_WIDTH
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        // down
        int width = 50;
        scale.setWidth(width);
        scaledImage = JAIUtil.scaleImage(
                image, scale, scaleConstraint, interpolation, rf);
        assertEquals(width, scaledImage.getWidth(), DELTA);
        assertEquals(
                Math.round((width / (float) image.getWidth()) * image.getHeight()),
                scaledImage.getHeight(), DELTA);
        // up
        width = 90;
        scale.setWidth(width);
        scaledImage = JAIUtil.scaleImage(
                image, scale, scaleConstraint, interpolation, rf);
        assertEquals(width, scaledImage.getWidth(), DELTA);
        assertEquals(
                Math.round((width / (float) image.getWidth()) * image.getHeight()),
                scaledImage.getHeight(), DELTA);

        // Mode.ASPECT_FIT_HEIGHT
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        // down
        int height = 25;
        scale.setHeight(height);
        scaledImage = JAIUtil.scaleImage(
                image, scale, scaleConstraint, interpolation, rf);
        assertEquals(
                Math.round((height / (float) image.getHeight()) * image.getWidth()),
                scaledImage.getWidth(), DELTA);
        assertEquals(height, scaledImage.getHeight(), DELTA);
        // up
        height = 90;
        scale.setHeight(height);
        scaledImage = JAIUtil.scaleImage(
                image, scale, scaleConstraint, interpolation, rf);
        assertEquals(
                Math.round((height / (float) image.getHeight()) * image.getWidth()),
                scaledImage.getWidth(), DELTA);
        assertEquals(height, scaledImage.getHeight(), DELTA);

        // Mode.ASPECT_FIT_INSIDE
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        // down
        width = 40;
        height = 38;
        scale.setWidth(width);
        scale.setHeight(height);
        scaledImage = JAIUtil.scaleImage(
                image, scale, scaleConstraint, interpolation, rf);
        assertEquals(width, scaledImage.getWidth(), DELTA);
        assertEquals(
                Math.round((width / (float) image.getWidth()) * image.getHeight()),
                scaledImage.getHeight(), DELTA);
        // up
        width = 90;
        height = 88;
        scale.setWidth(width);
        scale.setHeight(height);
        scaledImage = JAIUtil.scaleImage(
                image, scale, scaleConstraint, interpolation, rf);
        assertEquals(width, scaledImage.getWidth(), DELTA);
        assertEquals(
                Math.round((width / (float) image.getWidth()) * image.getHeight()),
                scaledImage.getHeight(), DELTA);

        // Mode.NON_ASPECT_FILL
        scale = new Scale();
        scale.setMode(Scale.Mode.NON_ASPECT_FILL);
        // down
        width = 45;
        height = 42;
        scale.setWidth(width);
        scale.setHeight(height);
        scaledImage = JAIUtil.scaleImage(
                image, scale, scaleConstraint, interpolation, rf);
        assertEquals(width, scaledImage.getWidth(), DELTA);
        assertEquals(height, scaledImage.getHeight(), DELTA);
        // up
        width = 85;
        height = 82;
        scale.setWidth(width);
        scale.setHeight(height);
        scaledImage = JAIUtil.scaleImage(
                image, scale, scaleConstraint, interpolation, rf);
        assertEquals(width, scaledImage.getWidth(), DELTA);
        assertEquals(height, scaledImage.getHeight(), DELTA);

        // Percent
        scale = new Scale();
        // down
        double percent = 0.5;
        scale.setPercent(percent);
        scaledImage = JAIUtil.scaleImage(
                image, scale, scaleConstraint, interpolation, rf);
        assertEquals(image.getWidth() * percent, scaledImage.getWidth(), DELTA);
        assertEquals(image.getHeight() * percent, scaledImage.getHeight(), DELTA);
        // up
        percent = 1.1;
        scale.setPercent(percent);
        scaledImage = JAIUtil.scaleImage(
                image, scale, scaleConstraint, interpolation, rf);
        assertEquals(Math.round(image.getWidth() * percent),
                scaledImage.getWidth());
        assertEquals(Math.round(image.getHeight() * percent),
                scaledImage.getHeight());
    }

    /* scaleImageUsingSubsampleAverage() */

    @Test
    public void scaleImageUsingSubsampleAverage() throws Exception {
        RenderedOp image = readImage(IMAGE);
        final ReductionFactor rf = new ReductionFactor();
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        // test with Mode.FULL
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        RenderedOp scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(
                image, scale, scaleConstraint, rf);
        assertSame(image, scaledImage);

        // Mode.ASPECT_FIT_WIDTH
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        // down
        int width = 50;
        scale.setWidth(width);
        scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(
                image, scale, scaleConstraint, rf);
        assertEquals(width, scaledImage.getWidth(), DELTA);
        assertEquals(
                Math.floor(width / (double) image.getWidth() * image.getHeight()),
                scaledImage.getHeight(), DELTA);
        // up
        width = 80;
        scale.setWidth(width);
        scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(
                image, scale, scaleConstraint, rf);
        assertEquals(width, scaledImage.getWidth(), DELTA);
        assertEquals(
                width / (double) image.getWidth() * image.getHeight(),
                scaledImage.getHeight(), DELTA);

        // Mode.ASPECT_FIT_HEIGHT
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        // down
        int height = 36;
        scale.setHeight(height);
        scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(
                image, scale, scaleConstraint, rf);
        assertEquals(
                Math.floor(height / (double) image.getHeight() * image.getWidth()),
                scaledImage.getWidth(), DELTA);
        assertEquals(height, scaledImage.getHeight(), DELTA);
        // up
        height = 72;
        scale.setHeight(height);
        scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(
                image, scale, scaleConstraint, rf);
        assertEquals(
                Math.floor(height / (double) image.getHeight() * image.getWidth()),
                scaledImage.getWidth(), DELTA);
        assertEquals(height, scaledImage.getHeight(), DELTA);

        // Mode.ASPECT_FIT_INSIDE
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        // down
        width = 40;
        height = 40;
        scale.setWidth(width);
        scale.setHeight(height);
        scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(
                image, scale, scaleConstraint, rf);
        assertEquals(width, scaledImage.getWidth(), DELTA);
        assertEquals(
                height / (double) image.getWidth() * image.getHeight(),
                scaledImage.getHeight(), DELTA);
        // up
        width = 90;
        height = 90;
        scale.setWidth(width);
        scale.setHeight(height);
        scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(
                image, scale, scaleConstraint, rf);
        assertEquals(width, scaledImage.getWidth(), DELTA);
        assertEquals(
                Math.ceil(width / (double) image.getWidth() * image.getHeight()),
                scaledImage.getHeight(), DELTA);

        // Mode.NON_ASPECT_FILL
        scale = new Scale();
        scale.setMode(Scale.Mode.NON_ASPECT_FILL);
        // down
        width = 45;
        height = 42;
        scale.setWidth(width);
        scale.setHeight(height);
        scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(
                image, scale, scaleConstraint, rf);
        assertEquals(width, scaledImage.getWidth(), DELTA);
        assertEquals(height, scaledImage.getHeight(), DELTA);
        // up
        width = 90;
        height = 88;
        scale.setWidth(width);
        scale.setHeight(height);
        scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(
                image, scale, scaleConstraint, rf);
        assertEquals(width, scaledImage.getWidth(), DELTA);
        assertEquals(height, scaledImage.getHeight(), DELTA);

        // Percent
        scale = new Scale();
        // down
        double percent = 0.5;
        scale.setPercent(percent);
        scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(
                image, scale, scaleConstraint, rf);
        assertEquals(image.getWidth() * percent, scaledImage.getWidth(), DELTA);
        assertEquals(image.getHeight() * percent, scaledImage.getHeight(), DELTA);
        // up
        percent = 1.2;
        scale.setPercent(percent);
        scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(
                image, scale, scaleConstraint, rf);
        assertEquals(Math.round(image.getWidth() * percent),
                scaledImage.getWidth());
        assertEquals(Math.round(image.getHeight() * percent),
                scaledImage.getHeight());
    }

    /* sharpenImage() */

    @Test
    public void sharpenImage() throws Exception {
        RenderedOp image = readImage(IMAGE);

        // test with no-op sharpen
        Sharpen sharpen = new Sharpen();
        sharpen.setAmount(0);
        RenderedOp sharpenedImage = JAIUtil.sharpenImage(image, sharpen);
        assertSame(image, sharpenedImage);

        // test with non-no-op sharpen
        sharpen = new Sharpen();
        sharpen.setAmount(0.5f);
        sharpenedImage = JAIUtil.sharpenImage(image, sharpen);
        assertNotSame(image, sharpenedImage);
    }

    /* transformColor() */

    @Test
    public void transformColorToBitonal() throws Exception {
        RenderedOp image = readImage(IMAGE);
        ColorTransform transform = ColorTransform.BITONAL;

        RenderedOp result = JAIUtil.transformColor(image, transform);
        BufferedImage bimage = result.getAsBufferedImage();
        assertRGBA(bimage.getRGB(0, 0), 0, 0, 0, 255);
    }

    @Test
    public void transformColorToGray() throws Exception {
        RenderedOp image = readImage(IMAGE);
        ColorTransform transform = ColorTransform.GRAY;

        RenderedOp result = JAIUtil.transformColor(image, transform);
        assertEquals(1, result.getSampleModel().getNumBands());

        BufferedImage bimage = result.getAsBufferedImage();
        assertGray(bimage.getRGB(0, 0));
    }

    /* transposeImage() */

    @Test
    public void transposeImage() throws Exception {
        RenderedOp image = readImage(IMAGE);
        // horizontal
        RenderedOp result = JAIUtil.transposeImage(image, Transpose.HORIZONTAL);
        assertEquals(image.getWidth(), result.getWidth());
        assertEquals(image.getHeight(), result.getHeight());
        // vertical
        result = JAIUtil.transposeImage(image, Transpose.VERTICAL);
        assertEquals(image.getWidth(), result.getWidth());
        assertEquals(image.getHeight(), result.getHeight());
    }

    private RenderedOp readImage(final String name) throws Exception {
        final OperationList ops = new OperationList();
        ImageReader reader = null;
        try {
            reader = new ImageReaderFactory().newImageReader(
                    TestUtil.getImage(name), Format.PNG);
            RenderedImage image = reader.readRendered(
                    ops, Orientation.ROTATE_0, new ReductionFactor(), null);
            PlanarImage planarImage = PlanarImage.wrapRenderedImage(image);
            return JAIUtil.getAsRenderedOp(planarImage);
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
    }

}
