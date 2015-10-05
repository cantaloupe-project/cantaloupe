package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.request.Region;
import edu.illinois.library.cantaloupe.request.Rotation;
import edu.illinois.library.cantaloupe.request.Size;
import junit.framework.TestCase;

import java.awt.image.BufferedImage;

public class ProcessorUtilTest extends TestCase {

    public void testConvertToRgb() {
        // TODO: write this
    }

    public void testCropImage() {
        BufferedImage inImage = new BufferedImage(100, 100,
                BufferedImage.TYPE_INT_RGB);

        // full region
        Region region = Region.fromUri("full");
        BufferedImage outImage = ProcessorUtil.cropImage(inImage, region);
        assertSame(inImage, outImage);

        // pixel region
        region = Region.fromUri("0,0,50,50");
        outImage = ProcessorUtil.cropImage(inImage, region);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // percentage region
        region = Region.fromUri("pct:50,50,50,50");
        outImage = ProcessorUtil.cropImage(inImage, region);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());
    }

    public void testCropImageWithReductionFactor() {
        BufferedImage inImage = new BufferedImage(100, 100,
                BufferedImage.TYPE_INT_RGB);

        // full region
        Region region = Region.fromUri("full");
        int reductionFactor = 1;
        BufferedImage outImage = ProcessorUtil.cropImage(inImage, region,
                reductionFactor);
        assertSame(inImage, outImage);

        // pixel region
        region = Region.fromUri("0,0,50,50");
        reductionFactor = 1;
        outImage = ProcessorUtil.cropImage(inImage, region, reductionFactor);
        assertEquals(25, outImage.getWidth());
        assertEquals(25, outImage.getHeight());

        // percentage region
        region = Region.fromUri("pct:50,50,50,50");
        reductionFactor = 1;
        outImage = ProcessorUtil.cropImage(inImage, region, reductionFactor);
        assertEquals(25, outImage.getWidth());
        assertEquals(25, outImage.getHeight());
    }

    public void testFilterImageWithBufferedImage() {
        // TODO: write this
    }

    public void testFilteredImageWithRenderedOp() {
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

    public void testGetSize() {
        // TODO: write this
    }

    public void testImageIoOutputFormats() {
        // TODO: write this
    }

    public void testRotateImageWithBufferedImage() {
        // no mirroring
        BufferedImage inImage = new BufferedImage(100, 100,
                BufferedImage.TYPE_INT_RGB);
        final int sourceWidth = inImage.getWidth();
        final int sourceHeight = inImage.getHeight();

        Rotation rotation = Rotation.fromUri("15");
        BufferedImage outImage = ProcessorUtil.rotateImage(inImage, rotation);

        double radians = Math.toRadians(rotation.getDegrees());
        int expectedWidth = (int) Math.round(Math.abs(sourceWidth *
                Math.cos(radians)) + Math.abs(sourceHeight *
                Math.sin(radians)));
        int expectedHeight = (int) Math.round(Math.abs(sourceHeight *
                Math.cos(radians)) + Math.abs(sourceWidth *
                Math.sin(radians)));

        assertEquals(expectedWidth, outImage.getWidth());
        assertEquals(expectedHeight, outImage.getWidth());

        // mirroring
        rotation = Rotation.fromUri("!15");
        outImage = ProcessorUtil.rotateImage(inImage, rotation);

        radians = Math.toRadians(rotation.getDegrees());
        expectedWidth = (int) Math.round(Math.abs(sourceWidth *
                Math.cos(radians)) + Math.abs(sourceHeight *
                Math.sin(radians)));
        expectedHeight = (int) Math.round(Math.abs(sourceHeight *
                Math.cos(radians)) + Math.abs(sourceWidth *
                Math.sin(radians)));

        assertEquals(expectedWidth, outImage.getWidth());
        assertEquals(expectedHeight, outImage.getWidth());
    }

    public void testRotateImageWithRenderedOp() {
        // TODO: write this
    }

    public void testScaleImageWithRenderedOp() {
        // TODO: write this
    }

    public void testScaleImageWithRenderedOpWithReductionFactor() {
        // TODO: write this
    }

    public void testScaleImageWithAffineTransform() {
        BufferedImage inImage = new BufferedImage(100, 100,
                BufferedImage.TYPE_INT_RGB);

        // Size.ScaleMode.FULL
        Size size = Size.fromUri("full");
        BufferedImage outImage = ProcessorUtil.scaleImageWithAffineTransform(inImage, size);
        assertSame(inImage, outImage);

        // Size.ScaleMode.ASPECT_FIT_WIDTH
        size = Size.fromUri("50,");
        outImage = ProcessorUtil.scaleImageWithAffineTransform(inImage, size);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Size.ScaleMode.ASPECT_FIT_HEIGHT
        size = Size.fromUri("50,");
        outImage = ProcessorUtil.scaleImageWithAffineTransform(inImage, size);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Size.ScaleMode.ASPECT_FIT_INSIDE
        size = Size.fromUri("50,50");
        outImage = ProcessorUtil.scaleImageWithAffineTransform(inImage, size);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());
    }

    public void testScaleImageWithG2d() {
        BufferedImage inImage = new BufferedImage(100, 100,
                BufferedImage.TYPE_INT_RGB);

        // Size.ScaleMode.FULL
        Size size = Size.fromUri("full");
        BufferedImage outImage = ProcessorUtil.scaleImageWithG2d(inImage, size);
        assertSame(inImage, outImage);

        // Size.ScaleMode.ASPECT_FIT_WIDTH
        size = Size.fromUri("50,");
        outImage = ProcessorUtil.scaleImageWithG2d(inImage, size);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Size.ScaleMode.ASPECT_FIT_HEIGHT
        size = Size.fromUri("50,");
        outImage = ProcessorUtil.scaleImageWithG2d(inImage, size);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Size.ScaleMode.ASPECT_FIT_INSIDE
        size = Size.fromUri("50,50");
        outImage = ProcessorUtil.scaleImageWithG2d(inImage, size);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());
    }

    public void testScaleImageWithG2dWithReductionFactor() {
        BufferedImage inImage = new BufferedImage(50, 50,
                BufferedImage.TYPE_INT_RGB);

        // Size.ScaleMode.ASPECT_FIT_WIDTH
        Size size = Size.fromUri("50,");
        int reductionFactor = 1;
        BufferedImage outImage = ProcessorUtil.scaleImageWithG2d(inImage, size,
                reductionFactor);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Size.ScaleMode.ASPECT_FIT_HEIGHT
        size = Size.fromUri("50,");
        reductionFactor = 1;
        outImage = ProcessorUtil.scaleImageWithG2d(inImage, size,
                reductionFactor);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Size.ScaleMode.ASPECT_FIT_INSIDE
        size = Size.fromUri("50,50");
        reductionFactor = 1;
        outImage = ProcessorUtil.scaleImageWithG2d(inImage, size,
                reductionFactor);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());
    }

    public void testWriteImage() {
        // TODO: write this
    }

}
