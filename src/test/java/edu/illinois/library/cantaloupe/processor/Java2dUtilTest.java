package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.Sharpen;
import edu.illinois.library.cantaloupe.image.Transpose;
import edu.illinois.library.cantaloupe.image.redaction.Redaction;
import edu.illinois.library.cantaloupe.image.watermark.ImageWatermark;
import edu.illinois.library.cantaloupe.image.watermark.Position;
import edu.illinois.library.cantaloupe.image.watermark.StringWatermark;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class Java2dUtilTest {

    @Before
    public void setUp() {
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        Configuration config = ConfigurationFactory.getInstance();
        config.clear();
    }

    @Test
    public void testApplyRedactions() throws Exception {
        // read the base image into a BufferedImage
        final File fixture = TestUtil.getImage("bmp-rgb-64x56x8.bmp");
        final BufferedImage baseImage = ImageIO.read(fixture);
        final ReductionFactor rf = new ReductionFactor(0);

        int pixel = baseImage.getRGB(0, 0);
        int alpha = (pixel >> 24) & 0xff;
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        assertEquals(255, alpha);
        assertEquals(106, red);
        assertEquals(90, green);
        assertEquals(60, blue);

        // create some Redactions
        List<Redaction> redactions = new ArrayList<>();
        redactions.add(new Redaction(new Rectangle(0, 0, 20, 20)));
        redactions.add(new Redaction(new Rectangle(20, 20, 20, 20)));
        final Crop crop = new Crop(0, 0, baseImage.getWidth(),
                baseImage.getTileHeight());

        // apply them
        final BufferedImage redactedImage = Java2dUtil.applyRedactions(
                baseImage, crop, rf, redactions);

        // test for the first one
        pixel = redactedImage.getRGB(0, 0);
        alpha = (pixel >> 24) & 0xff;
        red = (pixel >> 16) & 0xff;
        green = (pixel >> 8) & 0xff;
        blue = (pixel) & 0xff;
        assertEquals(255, alpha);
        assertEquals(0, red);
        assertEquals(0, green);
        assertEquals(0, blue);

        // test for the second one
        pixel = redactedImage.getRGB(25, 25);
        alpha = (pixel >> 24) & 0xff;
        red = (pixel >> 16) & 0xff;
        green = (pixel >> 8) & 0xff;
        blue = (pixel) & 0xff;
        assertEquals(255, alpha);
        assertEquals(0, red);
        assertEquals(0, green);
        assertEquals(0, blue);
    }

    @Test
    public void testApplyWatermarkWithImageWatermark() throws Exception {
        // read the base image into a BufferedImage
        final File fixture = TestUtil.getImage("bmp-rgb-64x56x8.bmp");
        final BufferedImage baseImage = ImageIO.read(fixture);

        int pixel = baseImage.getRGB(0, 0);
        int alpha = (pixel >> 24) & 0xff;
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        assertEquals(255, alpha);
        assertEquals(106, red);
        assertEquals(90, green);
        assertEquals(60, blue);

        // create a Watermark
        final ImageWatermark watermark = new ImageWatermark(
                TestUtil.getImage("png-rgb-1x1x8.png"),
                Position.TOP_LEFT, 0);

        // apply it
        final BufferedImage watermarkedImage = Java2dUtil.applyWatermark(
                baseImage, watermark);

        pixel = watermarkedImage.getRGB(0, 0);
        alpha = (pixel >> 24) & 0xff;
        red = (pixel >> 16) & 0xff;
        green = (pixel >> 8) & 0xff;
        blue = (pixel) & 0xff;
        assertEquals(255, alpha);
        assertEquals(0, red);
        assertEquals(0, green);
        assertEquals(0, blue);
    }

    @Test
    public void testApplyWatermarkWithImageWatermarkAndInset() throws Exception {
        // read the base image into a BufferedImage
        final File fixture = TestUtil.getImage("bmp-rgb-64x56x8.bmp");
        final BufferedImage baseImage = ImageIO.read(fixture);

        int pixel = baseImage.getRGB(
                baseImage.getWidth() - 2, baseImage.getHeight() - 2);
        int alpha = (pixel >> 24) & 0xff;
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        assertEquals(255, alpha);
        assertEquals(231, red);
        assertEquals(222, green);
        assertEquals(203, blue);

        // create a Watermark
        final int inset = 2;
        final ImageWatermark watermark = new ImageWatermark(
                TestUtil.getImage("png-rgb-1x1x8.png"), Position.BOTTOM_RIGHT,
                inset);

        // apply it
        final BufferedImage watermarkedImage = Java2dUtil.applyWatermark(
                baseImage, watermark);

        pixel = watermarkedImage.getRGB(
                baseImage.getWidth() - inset - 1,
                baseImage.getHeight() - inset - 1);
        alpha = (pixel >> 24) & 0xff;
        red = (pixel >> 16) & 0xff;
        green = (pixel >> 8) & 0xff;
        blue = (pixel) & 0xff;
        assertEquals(255, alpha);
        assertEquals(0, red);
        assertEquals(0, green);
        assertEquals(0, blue);
    }

    @Test
    public void testApplyWatermarkWithStringWatermark() throws Exception {
        // read the base image into a BufferedImage
        final File fixture = TestUtil.getImage("bmp-rgb-64x56x8.bmp");
        final BufferedImage baseImage = ImageIO.read(fixture);

        // create a Watermark
        final StringWatermark watermark = new StringWatermark(
                "XXXXXX", Position.TOP_LEFT, 0,
                new Font("Helvetica", Font.PLAIN, 4), Color.black);

        // apply it
        final BufferedImage watermarkedImage = Java2dUtil.applyWatermark(
                baseImage, watermark);

        int pixel = watermarkedImage.getRGB(0, 0);
        int alpha = (pixel >> 24) & 0xff;
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        assertEquals(255, alpha);
        assertEquals(0, red);
        assertEquals(0, green);
        assertEquals(0, blue);
    }

    @Test
    public void testCropImage() {
        BufferedImage inImage = new BufferedImage(200, 100,
                BufferedImage.TYPE_INT_RGB);
        // full
        Crop crop = new Crop();
        crop.setFull(true);
        BufferedImage outImage = Java2dUtil.cropImage(inImage, crop);
        assertSame(inImage, outImage);

        // square
        crop = new Crop();
        crop.setShape(Crop.Shape.SQUARE);
        crop.setWidth(50f);
        crop.setHeight(50f);
        outImage = Java2dUtil.cropImage(inImage, crop);
        assertEquals(100, outImage.getWidth());
        assertEquals(100, outImage.getHeight());

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
        assertEquals(100, outImage.getWidth());
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
    public void testGetWatermarkImage() throws Exception {
        ImageWatermark watermark = new ImageWatermark(
                TestUtil.getImage("png"), Position.BOTTOM_RIGHT, 0);

        assertNotNull(Java2dUtil.getWatermarkImage(watermark));
    }

    @Test
    public void testReduceTo8Bits() throws IOException {
        // assert that an 8-bit image is untouched
        BufferedImage image = new BufferedImage(100, 100,
                BufferedImage.TYPE_INT_RGB);
        BufferedImage result = Java2dUtil.reduceTo8Bits(image);
        assertSame(image, result);

        // assert that a 16-bit image is downsampled
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
    public void testScaleImage() {
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

        // scale-by percent
        scale = new Scale();
        scale.setPercent(0.25f);
        outImage = Java2dUtil.scaleImage(inImage, scale);
        assertEquals(25, outImage.getWidth());
        assertEquals(25, outImage.getHeight());
    }

    @Test
    public void testSharpenImage() {
        BufferedImage inImage = new BufferedImage(200, 100,
                BufferedImage.TYPE_INT_RGB);
        Sharpen sharpen = new Sharpen(0.1f);
        BufferedImage outImage = Java2dUtil.sharpenImage(inImage, sharpen);

        assertEquals(200, outImage.getWidth());
        assertEquals(100, outImage.getHeight());
    }

    @Test
    public void testScaleImageWithReductionFactor() {
        BufferedImage inImage = new BufferedImage(100, 100,
                BufferedImage.TYPE_INT_RGB);

        // Scale.Mode.ASPECT_FIT_WIDTH
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        scale.setWidth(50);
        ReductionFactor rf = new ReductionFactor(1);
        BufferedImage outImage = Java2dUtil.scaleImage(inImage, scale, rf);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Scale.Mode.ASPECT_FIT_HEIGHT
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        scale.setHeight(50);
        rf = new ReductionFactor(1);
        outImage = Java2dUtil.scaleImage(inImage, scale, rf);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Scale.Mode.ASPECT_FIT_INSIDE
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setWidth(50);
        scale.setHeight(50);
        rf = new ReductionFactor(1);
        outImage = Java2dUtil.scaleImage(inImage, scale, rf);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // scale-by-percent
        scale = new Scale();
        scale.setPercent(0.25f);
        rf = new ReductionFactor(2);
        outImage = Java2dUtil.scaleImage(inImage, scale, rf);
        assertEquals(100, outImage.getWidth());
        assertEquals(100, outImage.getHeight());
    }

    @Test
    public void testStretchContrast() {
        BufferedImage image = new BufferedImage(100, 100,
                BufferedImage.TYPE_INT_RGB);
        final Rectangle leftHalf = new Rectangle(0, 0, 50, 100);
        final Rectangle rightHalf = new Rectangle(50, 0, 50, 100);

        final Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.DARK_GRAY);
        g2d.fill(leftHalf);
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fill(rightHalf);

        image = Java2dUtil.stretchContrast(image);

        assertEquals(-16777216, image.getRGB(10, 10));
        assertEquals(-1, image.getRGB(90, 90));
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
