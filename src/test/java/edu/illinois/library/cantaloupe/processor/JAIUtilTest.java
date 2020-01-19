package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.CropByPercent;
import edu.illinois.library.cantaloupe.operation.CropByPixels;
import edu.illinois.library.cantaloupe.operation.CropToSquare;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.ScaleByPercent;
import edu.illinois.library.cantaloupe.operation.ScaleByPixels;
import edu.illinois.library.cantaloupe.operation.Sharpen;
import edu.illinois.library.cantaloupe.operation.Transpose;
import edu.illinois.library.cantaloupe.processor.codec.ImageReader;
import edu.illinois.library.cantaloupe.processor.codec.ImageReaderFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.Test;

import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import static edu.illinois.library.cantaloupe.test.Assert.ImageAssert.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("deprecation")
class JAIUtilTest extends BaseTest {

    private static final double DELTA = 0.00000001;
    private static final String IMAGE = "png-rgb-64x56x8.png";

    /* cropImage(RenderedOp, Crop) */

    @Test
    void cropImageWithCropByPixels() throws Exception {
        RenderedOp inImage = readImage(IMAGE);

        CropByPixels crop = new CropByPixels(0, 0, 50, 50);
        crop.setWidth(50);
        crop.setHeight(50);
        RenderedOp outImage = JAIUtil.cropImage(inImage, crop);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());
    }

    @Test
    void cropImageWithCropByPercent() throws Exception {
        RenderedOp inImage = readImage(IMAGE);

        CropByPercent crop = new CropByPercent();
        crop.setX(0.5);
        crop.setY(0.5);
        crop.setWidth(0.5);
        crop.setHeight(0.5);
        RenderedOp outImage = JAIUtil.cropImage(inImage, crop);
        assertEquals(32, outImage.getWidth());
        assertEquals(28, outImage.getHeight());
    }

    @Test
    void cropImageWithCropToSquare() throws Exception {
        RenderedOp inImage = readImage(IMAGE);

        Crop crop = new CropToSquare();
        RenderedOp outImage = JAIUtil.cropImage(inImage, crop);
        assertEquals(56, outImage.getWidth());
        assertEquals(56, outImage.getHeight());
    }

    /* cropImage(RenderedOp, Crop, ReductionFactor) */

    @Test
    void cropImageWithReductionFactorWithCropByPixels() throws Exception {
        RenderedOp inImage = readImage(IMAGE);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        CropByPixels crop   = new CropByPixels(0, 0, 50, 50);
        ReductionFactor rf  = new ReductionFactor(1);
        RenderedOp outImage = JAIUtil.cropImage(inImage, scaleConstraint, crop, rf);
        assertEquals(25, outImage.getWidth());
        assertEquals(25, outImage.getHeight());
    }

    @Test
    void cropImageWithReductionFactorWithCropByPercent() throws Exception {
        RenderedOp inImage = readImage(IMAGE);

        ReductionFactor rf = new ReductionFactor(1);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        CropByPercent crop = new CropByPercent(0.5, 0.5, 0.5, 0.5);

        RenderedOp outImage = JAIUtil.cropImage(
                inImage, scaleConstraint, crop, rf);
        assertEquals(inImage.getWidth() * crop.getWidth(),
                outImage.getWidth(), DELTA);
        assertEquals(inImage.getHeight() * crop.getHeight(),
                outImage.getHeight(), DELTA);
    }

    @Test
    void cropImageWithReductionFactorWithCropToSquare() throws Exception {
        RenderedOp inImage = readImage(IMAGE);

        Crop crop = new CropToSquare();
        RenderedOp outImage = JAIUtil.cropImage(inImage, crop);
        assertEquals(inImage.getHeight(), outImage.getWidth());
        assertEquals(inImage.getHeight(), outImage.getHeight());
    }

    /* getAsRenderedOp() */

    @Test
    void getAsRenderedOp() throws Exception {
        RenderedImage image = readImage(IMAGE);
        PlanarImage planarImage = PlanarImage.wrapRenderedImage(image);
        RenderedOp renderedOp = JAIUtil.getAsRenderedOp(planarImage);
        assertEquals(64, renderedOp.getWidth());
        assertEquals(56, renderedOp.getHeight());
    }

    /* reduceTo8Bits() */

    @Test
    void reduceTo8BitsWith16BitImage() throws Exception {
        RenderedOp inImage = readImage("png-rgb-64x56x16.png");
        assertEquals(16, inImage.getColorModel().getComponentSize(0));

        RenderedOp outImage = JAIUtil.reduceTo8Bits(inImage);
        assertEquals(8, outImage.getColorModel().getComponentSize(0));
    }

    @Test
    void reduceTo8BitsWith8BitImage() throws Exception {
        RenderedOp inImage = readImage("png-rgb-64x56x8.png");
        assertEquals(8, inImage.getColorModel().getComponentSize(0));

        RenderedOp outImage = JAIUtil.reduceTo8Bits(inImage);
        assertSame(inImage, outImage);
    }

    /* rescalePixels() */

    @Test
    void rescalePixels() {
        // TODO: write this
    }

    /* rotateImage() */

    @Test
    void rotateImage() throws Exception {
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
    void scaleImageWithAspectFitWidthMode() throws Exception {
        RenderedOp image = readImage(IMAGE);
        final Interpolation interpolation =
                Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
        final ReductionFactor rf = new ReductionFactor();
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        ScaleByPixels scale = new ScaleByPixels();
        scale.setMode(ScaleByPixels.Mode.ASPECT_FIT_WIDTH);
        // down
        int width = 50;
        scale.setWidth(width);
        RenderedOp scaledImage = JAIUtil.scaleImage(
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
    }

    @Test
    void scaleImageWithAspectFitHeightMode() throws Exception {
        RenderedOp image = readImage(IMAGE);
        final Interpolation interpolation =
                Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
        final ReductionFactor rf = new ReductionFactor();
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        ScaleByPixels scale = new ScaleByPixels();
        scale.setMode(ScaleByPixels.Mode.ASPECT_FIT_HEIGHT);
        // down
        int height = 25;
        scale.setHeight(height);
        RenderedOp scaledImage = JAIUtil.scaleImage(
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
    }

    @Test
    void scaleImageWithAspectFitInsideMode() throws Exception {
        RenderedOp image = readImage(IMAGE);
        final Interpolation interpolation =
                Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
        final ReductionFactor rf = new ReductionFactor();
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        ScaleByPixels scale = new ScaleByPixels();
        scale.setMode(ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        // down
        int width = 40;
        int height = 38;
        scale.setWidth(width);
        scale.setHeight(height);
        RenderedOp scaledImage = JAIUtil.scaleImage(
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
    }

    @Test
    void scaleImageWithNonAspectFillMode() throws Exception {
        RenderedOp image = readImage(IMAGE);
        final Interpolation interpolation =
                Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
        final ReductionFactor rf = new ReductionFactor();
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        ScaleByPixels scale = new ScaleByPixels();
        scale.setMode(ScaleByPixels.Mode.NON_ASPECT_FILL);
        // down
        int width = 45;
        int height = 42;
        scale.setWidth(width);
        scale.setHeight(height);
        RenderedOp scaledImage = JAIUtil.scaleImage(
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
    }

    @Test
    void scaleImageByPercent() throws Exception {
        RenderedOp image = readImage(IMAGE);
        final Interpolation interpolation =
                Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
        final ReductionFactor rf = new ReductionFactor();
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        ScaleByPercent scale = new ScaleByPercent();
        // down
        double percent = 0.5;
        scale.setPercent(percent);
        RenderedOp scaledImage = JAIUtil.scaleImage(
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
    void scaleImageUsingSubsampleAverageWithAspectFitWidthMode() throws Exception {
        RenderedOp image = readImage(IMAGE);
        final ReductionFactor rf = new ReductionFactor();
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        ScaleByPixels scale = new ScaleByPixels();
        scale.setMode(ScaleByPixels.Mode.ASPECT_FIT_WIDTH);
        // down
        int width = 50;
        scale.setWidth(width);
        RenderedOp scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(
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
    }

    @Test
    void scaleImageUsingSubsampleAverageWithAspectFitHeightMode() throws Exception {
        RenderedOp image = readImage(IMAGE);
        final ReductionFactor rf = new ReductionFactor();
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        ScaleByPixels scale = new ScaleByPixels();
        scale.setMode(ScaleByPixels.Mode.ASPECT_FIT_HEIGHT);
        // down
        int height = 36;
        scale.setHeight(height);
        RenderedOp scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(
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
    }

    @Test
    void scaleImageUsingSubsampleAverageWithAspectFitInsideMode() throws Exception {
        RenderedOp image = readImage(IMAGE);
        final ReductionFactor rf = new ReductionFactor();
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        ScaleByPixels scale = new ScaleByPixels();
        scale.setMode(ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        // down
        int width = 40;
        int height = 40;
        scale.setWidth(width);
        scale.setHeight(height);
        RenderedOp scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(
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
    }

    @Test
    void scaleImageUsingSubsampleAverageWithNonAspectFillMode() throws Exception {
        RenderedOp image = readImage(IMAGE);
        final ReductionFactor rf = new ReductionFactor();
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        ScaleByPixels scale = new ScaleByPixels();
        scale.setMode(ScaleByPixels.Mode.NON_ASPECT_FILL);
        // down
        int width = 45;
        int height = 42;
        scale.setWidth(width);
        scale.setHeight(height);
        RenderedOp scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(
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
    }

    @Test
    void scaleImageUsingSubsampleAverageWithPercent() throws Exception {
        RenderedOp image = readImage(IMAGE);
        final ReductionFactor rf = new ReductionFactor();
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        ScaleByPercent scale = new ScaleByPercent();
        // down
        double percent = 0.5;
        scale.setPercent(percent);
        RenderedOp scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(
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
    void sharpenImage() throws Exception {
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
    void transformColorToBitonal() throws Exception {
        RenderedOp image = readImage(IMAGE);
        ColorTransform transform = ColorTransform.BITONAL;

        RenderedOp result = JAIUtil.transformColor(image, transform);
        BufferedImage bimage = result.getAsBufferedImage();
        assertRGBA(bimage.getRGB(0, 0), 0, 0, 0, 255);
    }

    @Test
    void transformColorToGray() throws Exception {
        RenderedOp image = readImage(IMAGE);
        ColorTransform transform = ColorTransform.GRAY;

        RenderedOp result = JAIUtil.transformColor(image, transform);
        assertEquals(1, result.getSampleModel().getNumBands());

        BufferedImage bimage = result.getAsBufferedImage();
        assertGray(bimage.getRGB(0, 0));
    }

    /* transposeImage() */

    @Test
    void transposeImage() throws Exception {
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
                    Format.PNG, TestUtil.getImage(name));

            Crop crop          = (Crop) ops.getFirst(Crop.class);
            Scale scale        = (Scale) ops.getFirst(Scale.class);
            ScaleConstraint sc = new ScaleConstraint(1, 1);
            ReductionFactor rf = new ReductionFactor();

            RenderedImage image = reader.readRendered(
                    crop, scale, sc, rf, null);
            PlanarImage planarImage = PlanarImage.wrapRenderedImage(image);
            return JAIUtil.getAsRenderedOp(planarImage);
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
    }

}
