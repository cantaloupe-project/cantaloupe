package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Sharpen;
import edu.illinois.library.cantaloupe.operation.Transpose;
import edu.illinois.library.cantaloupe.processor.imageio.ImageReader;
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

import static edu.illinois.library.cantaloupe.test.Assert.*;
import static org.junit.Assert.*;

public class JAIUtilTest extends BaseTest {

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
        final float fudge = 0.000000001f;
        RenderedOp inImage = readImage(IMAGE);

        // full
        Crop crop = new Crop();
        crop.setFull(true);
        ReductionFactor rf = new ReductionFactor(1);
        RenderedOp outImage = JAIUtil.cropImage(inImage, crop, rf);
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
        outImage = JAIUtil.cropImage(inImage, crop, rf);
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
        outImage = JAIUtil.cropImage(inImage, crop, rf);
        assertEquals(inImage.getWidth() / 4f, outImage.getWidth(), fudge);
        assertEquals(inImage.getHeight() / 4f, outImage.getHeight(), fudge);
    }

    /* getAsRenderedOp() */

    @Test
    public void getAsRenderedOp() throws Exception {
        final OperationList ops = new OperationList(new Identifier("cats"),
                Format.JPG);
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
        final double fudge = 0.00000001f;

        // test with Mode.FULL
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        RenderedOp scaledImage = JAIUtil.scaleImage(image, scale, interpolation, rf);
        assertSame(image, scaledImage);

        // Mode.ASPECT_FIT_WIDTH
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        // down
        int width = 50;
        scale.setWidth(width);
        scaledImage = JAIUtil.scaleImage(image, scale, interpolation, rf);
        assertEquals(width, scaledImage.getWidth(), fudge);
        assertEquals(
                Math.round((width / (float) image.getWidth()) * image.getHeight()),
                scaledImage.getHeight(), fudge);
        // up
        width = 90;
        scale.setWidth(width);
        scaledImage = JAIUtil.scaleImage(image, scale, interpolation, rf);
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
        scaledImage = JAIUtil.scaleImage(image, scale, interpolation, rf);
        assertEquals(
                Math.round((height / (float) image.getHeight()) * image.getWidth()),
                scaledImage.getWidth(), fudge);
        assertEquals(height, scaledImage.getHeight(), fudge);
        // up
        height = 90;
        scale.setHeight(height);
        scaledImage = JAIUtil.scaleImage(image, scale, interpolation, rf);
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
        scaledImage = JAIUtil.scaleImage(image, scale, interpolation, rf);
        assertEquals(width, scaledImage.getWidth(), fudge);
        assertEquals(
                Math.round((width / (float) image.getWidth()) * image.getHeight()),
                scaledImage.getHeight(), fudge);
        // up
        width = 90;
        height = 88;
        scale.setWidth(width);
        scale.setHeight(height);
        scaledImage = JAIUtil.scaleImage(image, scale, interpolation, rf);
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
        scaledImage = JAIUtil.scaleImage(image, scale, interpolation, rf);
        assertEquals(width, scaledImage.getWidth(), fudge);
        assertEquals(height, scaledImage.getHeight(), fudge);
        // up
        width = 85;
        height = 82;
        scale.setWidth(width);
        scale.setHeight(height);
        scaledImage = JAIUtil.scaleImage(image, scale, interpolation, rf);
        assertEquals(width, scaledImage.getWidth(), fudge);
        assertEquals(height, scaledImage.getHeight(), fudge);

        // Percent
        scale = new Scale();
        // down
        float percent = 0.5f;
        scale.setPercent(percent);
        scaledImage = JAIUtil.scaleImage(image, scale, interpolation, rf);
        assertEquals(image.getWidth() * percent, scaledImage.getWidth(), fudge);
        assertEquals(image.getHeight() * percent, scaledImage.getHeight(), fudge);
        // up
        percent = 1.1f;
        scale.setPercent(percent);
        scaledImage = JAIUtil.scaleImage(image, scale, interpolation, rf);
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
        final double fudge = 0.00000001f;

        // test with Mode.FULL
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        RenderedOp scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(image, scale, rf);
        assertSame(image, scaledImage);

        // Mode.ASPECT_FIT_WIDTH
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        // down
        int width = 50;
        scale.setWidth(width);
        scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(image, scale, rf);
        assertEquals(width, scaledImage.getWidth(), fudge);
        assertEquals(
                Math.round((width / (float) image.getWidth()) * image.getHeight()),
                scaledImage.getHeight(), fudge);
        // up
        width = 80;
        scale.setWidth(width);
        scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(image, scale, rf);
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
        scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(image, scale, rf);
        assertEquals(
                Math.round((height / (float) image.getHeight()) * image.getWidth()),
                scaledImage.getWidth(), fudge);
        assertEquals(height, scaledImage.getHeight(), fudge);
        // up
        height = 72;
        scale.setHeight(height);
        scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(image, scale, rf);
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
        scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(image, scale, rf);
        assertEquals(width, scaledImage.getWidth(), fudge);
        assertEquals(
                Math.round((height / (float) image.getWidth()) * image.getHeight()),
                scaledImage.getHeight(), fudge);
        // up
        width = 90;
        height = 90;
        scale.setWidth(width);
        scale.setHeight(height);
        scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(image, scale, rf);
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
        scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(image, scale, rf);
        assertEquals(width, scaledImage.getWidth(), fudge);
        assertEquals(height, scaledImage.getHeight(), fudge);
        // up
        width = 90;
        height = 88;
        scale.setWidth(width);
        scale.setHeight(height);
        scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(image, scale, rf);
        assertEquals(width, scaledImage.getWidth(), fudge);
        assertEquals(height, scaledImage.getHeight(), fudge);

        // Percent
        scale = new Scale();
        // down
        float percent = 0.5f;
        scale.setPercent(percent);
        scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(image, scale, rf);
        assertEquals(image.getWidth() * percent, scaledImage.getWidth(), fudge);
        assertEquals(image.getHeight() * percent, scaledImage.getHeight(), fudge);
        // up
        percent = 1.2f;
        scale.setPercent(percent);
        scaledImage = JAIUtil.scaleImageUsingSubsampleAverage(image, scale, rf);
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

    /* stretchContrast() */

    @Test
    public void stretchContrast() throws IOException {
        BufferedImage image = new BufferedImage(100, 100,
                BufferedImage.TYPE_INT_RGB);
        final Rectangle leftHalf = new Rectangle(0, 0, 50, 100);
        final Rectangle rightHalf = new Rectangle(50, 0, 50, 100);

        final Graphics2D g2d = image.createGraphics();
        g2d.setColor(java.awt.Color.DARK_GRAY);
        g2d.fill(leftHalf);
        g2d.setColor(java.awt.Color.LIGHT_GRAY);
        g2d.fill(rightHalf);

        RenderedOp renderedOp = JAIUtil.getAsRenderedOp(RenderedOp.wrapRenderedImage(image));
        renderedOp = JAIUtil.stretchContrast(renderedOp);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(renderedOp, "JPEG", os);
        image = ImageIO.read(new ByteArrayInputStream(os.toByteArray()));

        assertEquals(-16777216, image.getRGB(10, 10));
        assertEquals(-1, image.getRGB(90, 90));
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
        final OperationList ops = new OperationList(new Identifier("cats"),
                Format.JPG);
        ImageReader reader = null;
        try {
            reader = new ImageReader(TestUtil.getImage(name), Format.PNG);
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
