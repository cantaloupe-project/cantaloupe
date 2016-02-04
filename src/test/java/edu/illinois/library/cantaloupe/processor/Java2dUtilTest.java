package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.Transpose;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.junit.Assert.*;

public class Java2dUtilTest {

    @Test
    public void testConvertToRgb() throws IOException {
        // test that input image of TYPE_INT_RGB is returned with no conversion
        BufferedImage custom = new BufferedImage(10, 10,
                BufferedImage.TYPE_INT_RGB);
        assertSame(custom, Java2dUtil.convertCustomToRgb(custom));

        // test with image of TYPE_CUSTOM
        custom = ImageIO.read(TestUtil.
                getFixture("images/tif-rgb-64x56x8-striped-uncompressed.tif"));
        BufferedImage output = Java2dUtil.convertCustomToRgb(custom);
        assertEquals(BufferedImage.TYPE_INT_RGB, output.getType());
    }

    @Test
    public void testCropImage() {
        BufferedImage inImage = new BufferedImage(100, 100,
                BufferedImage.TYPE_INT_RGB);
        // full
        Crop crop = new Crop();
        crop.setFull(true);
        BufferedImage outImage = Java2dUtil.cropImage(inImage, crop);
        assertSame(inImage, outImage);

        // pixel crop
        crop = new Crop();
        crop.setWidth(50f);
        crop.setHeight(50f);
        outImage = Java2dUtil.cropImage(inImage, crop);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // percentage crop
        crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setX(0.5f);
        crop.setY(0.5f);
        crop.setWidth(0.5f);
        crop.setHeight(0.5f);
        outImage = Java2dUtil.cropImage(inImage, crop);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());
    }

    @Test
    public void testCropImageWithReductionFactor() {
        BufferedImage inImage = new BufferedImage(100, 100,
                BufferedImage.TYPE_INT_RGB);

        // full crop
        Crop crop = new Crop();
        crop.setFull(true);
        ReductionFactor rf = new ReductionFactor(1);
        BufferedImage outImage = Java2dUtil.cropImage(inImage, crop, rf);
        assertSame(inImage, outImage);

        // pixel crop
        crop = new Crop();
        crop.setWidth(50f);
        crop.setHeight(50f);
        rf = new ReductionFactor(1);
        outImage = Java2dUtil.cropImage(inImage, crop, rf);
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
        outImage = Java2dUtil.cropImage(inImage, crop, rf);
        assertEquals(25, outImage.getWidth());
        assertEquals(25, outImage.getHeight());
    }

    @Test
    public void testFilterImage() {
        // TODO: write this
    }

    @Test
    public void testRemoveAlpha() {
        // TODO: write this
    }

    @Test
    public void testRotateImage() {
        BufferedImage inImage = new BufferedImage(200, 100,
                BufferedImage.TYPE_INT_RGB);
        final int sourceWidth = inImage.getWidth();
        final int sourceHeight = inImage.getHeight();

        Rotate rotate = new Rotate(15);
        BufferedImage outImage = Java2dUtil.rotateImage(inImage, rotate);

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
    public void testScaleImageWithG2d() {
        BufferedImage inImage = new BufferedImage(100, 100,
                BufferedImage.TYPE_INT_RGB);

        // Scale.Mode.FULL
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        BufferedImage outImage = Java2dUtil.scaleImage(inImage, scale);
        assertSame(inImage, outImage);

        // Scale.Mode.ASPECT_FIT_WIDTH
        scale.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        scale.setWidth(50);
        outImage = Java2dUtil.scaleImage(inImage, scale);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Scale.Mode.ASPECT_FIT_HEIGHT
        scale.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        scale.setHeight(50);
        outImage = Java2dUtil.scaleImage(inImage, scale);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Scale.Mode.ASPECT_FIT_INSIDE
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setWidth(50);
        scale.setHeight(50);
        outImage = Java2dUtil.scaleImage(inImage, scale);
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
        ReductionFactor rf = new ReductionFactor(1);
        BufferedImage outImage = Java2dUtil.scaleImage(inImage, scale,
                rf, true);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Scale.Mode.ASPECT_FIT_HEIGHT
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        scale.setHeight(50);
        rf = new ReductionFactor(1);
        outImage = Java2dUtil.scaleImage(inImage, scale, rf, false);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Scale.Mode.ASPECT_FIT_INSIDE
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setWidth(50);
        scale.setHeight(50);
        rf = new ReductionFactor(1);
        outImage = Java2dUtil.scaleImage(inImage, scale, rf, true);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());
    }

    @Test
    public void testTransposeImage() {
        BufferedImage inImage = new BufferedImage(200, 100,
                BufferedImage.TYPE_INT_RGB);
        Transpose transpose = Transpose.HORIZONTAL;
        BufferedImage outImage = Java2dUtil.transposeImage(inImage, transpose);

        assertEquals(200, outImage.getWidth());
        assertEquals(100, outImage.getHeight());
    }

}
