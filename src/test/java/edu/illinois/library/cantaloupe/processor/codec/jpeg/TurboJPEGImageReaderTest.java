package edu.illinois.library.cantaloupe.processor.codec.jpeg;

import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.Rational;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("WeakerAccess") // a JMH test extends this
public class TurboJPEGImageReaderTest extends BaseTest {

    private TurboJPEGImageReader instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new TurboJPEGImageReader();

        Path image = TestUtil.getImage("jpg");
        InputStream is = Files.newInputStream(image);
        instance.setSource(is);

        assertEquals(16, instance.getBlockWidth());
        assertEquals(16, instance.getBlockHeight());
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        instance.close();
    }

    @Test
    public void testGetMCUSafeRegionAligningWithBlockGrid() {
        final int width = 4096;
        final int height = 4096;
        final int blockWidth = 8;
        final int blockHeight = 8;
        final Rectangle region = new Rectangle(1024, 1024, 1024, 1024);
        final Rectangle roiWithinRegion = new Rectangle();

        Rectangle safeRegion = TurboJPEGImageReader.getMCUSafeRegion(
                region, width, height, blockWidth, blockHeight, roiWithinRegion);
        assertEquals(new Rectangle(1024, 1024, 1024, 1024), safeRegion);
        assertEquals(new Rectangle(0, 0, 1024, 1024), roiWithinRegion);
    }

    @Test
    public void testGetMCUSafeRegionNotAligningWithBlockGrid() {
        final int width = 621;
        final int height = 509;
        final int blockWidth = 16;
        final int blockHeight = 16;
        final Rectangle region = new Rectangle(18, 18, 571, 403);
        final Rectangle roiWithinRegion = new Rectangle();

        Rectangle safeRegion = TurboJPEGImageReader.getMCUSafeRegion(
                region, width, height, blockWidth, blockHeight, roiWithinRegion);
        assertEquals(new Rectangle(16, 16, 576, 416), safeRegion);
        assertEquals(new Rectangle(2, 2, 571, 403), roiWithinRegion);
    }

    @Test
    public void testGetBlockWidth() throws Exception {
        assertEquals(16, instance.getBlockWidth());
    }

    @Test
    public void testGetBlockHeight() throws Exception {
        assertEquals(16, instance.getBlockHeight());
    }

    @Test
    public void testGetWidth() throws Exception {
        assertEquals(64, instance.getWidth());
    }

    @Test
    public void testGetHeight() throws Exception {
        assertEquals(56, instance.getHeight());
    }

    @Test
    public void testGetSubsampling() throws Exception {
        assertEquals(2, instance.getSubsampling());
    }

    @Test
    public void testIsTransformableWithNonBlockSizeMultipleDimensions()
            throws Exception {
        assertFalse(instance.isTransformable());
    }

    @Test
    public void testIsTransformableWithTooSmallDimensions() throws Exception {
        instance.close();
        instance = new TurboJPEGImageReader();

        Path image = TestUtil.getImage("jpg-rgb-48x42x8.jpg");
        InputStream is = Files.newInputStream(image);
        instance.setSource(is);
        assertFalse(instance.isTransformable());
    }

    @Test
    public void testIsTransformableWithTransformableImage() throws Exception {
        instance.close();
        instance = new TurboJPEGImageReader();

        Path image = TestUtil.getImage("jpg-rgb-128x96x8.jpg");
        InputStream is = Files.newInputStream(image);
        instance.setSource(is);
        assertTrue(instance.isTransformable());
    }

    @Test
    public void testRead() throws Exception {
        TurboJPEGImage image = instance.read();
        assertEquals(64, image.getScaledWidth());
        assertEquals(56, image.getScaledHeight());
    }

    @Test
    public void testReadWithRegion() throws Exception {
        instance.close();

        instance = new TurboJPEGImageReader();
        Path file = TestUtil.getImage("jpg-rgb-128x96x8.jpg");
        InputStream is = Files.newInputStream(file);
        instance.setSource(is);
        instance.setRegion(19, 19, 30, 30);

        TurboJPEGImage image = instance.read();
        assertEquals(128, image.getScaledWidth());
        assertEquals(96, image.getScaledHeight());
    }

    @Test
    @Disabled // rotation doesn't work yet
    public void testReadWithRotate90() throws Exception {
        instance.close();

        instance = new TurboJPEGImageReader();
        Path file = TestUtil.getImage("jpg-rgb-64x48x8.jpg");
        InputStream is = Files.newInputStream(file);
        instance.setSource(is);
        instance.setTransform(TurboJPEGImageReader.Transform.ROTATE_90);

        TurboJPEGImage image = instance.read();
        assertEquals(48, image.getScaledWidth());
        assertEquals(64, image.getScaledHeight());
    }

    @Test
    @Disabled // rotation doesn't work yet
    public void testReadWithRotate180() throws Exception {
        instance.close();

        instance = new TurboJPEGImageReader();
        Path file = TestUtil.getImage("jpg-rgb-64x48x8.jpg");
        InputStream is = Files.newInputStream(file);
        instance.setSource(is);
        instance.setTransform(TurboJPEGImageReader.Transform.ROTATE_180);

        TurboJPEGImage image = instance.read();
        assertEquals(64, image.getScaledWidth());
        assertEquals(48, image.getScaledHeight());
    }

    @Test
    @Disabled // rotation doesn't work yet
    public void testReadWithRotate270() throws Exception {
        instance.close();

        instance = new TurboJPEGImageReader();
        Path file = TestUtil.getImage("jpg-rgb-64x48x8.jpg");
        InputStream is = Files.newInputStream(file);
        instance.setSource(is);
        instance.setTransform(TurboJPEGImageReader.Transform.ROTATE_270);

        TurboJPEGImage image = instance.read();
        assertEquals(48, image.getScaledWidth());
        assertEquals(64, image.getScaledHeight());
    }

    @Test
    public void testReadAsBufferedImage() throws Exception {
        final Rectangle margin = new Rectangle();
        BufferedImage image = instance.readAsBufferedImage(margin);
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    @Test
    public void testReadAsBufferedImageWithRegion() throws Exception {
        instance.close();

        instance = new TurboJPEGImageReader();
        Path file = TestUtil.getImage("jpg-rgb-128x96x8.jpg");
        InputStream is = Files.newInputStream(file);
        instance.setSource(is);
        instance.setRegion(19, 19, 30, 30);

        final Rectangle roiWithinImage = new Rectangle();
        BufferedImage image = instance.readAsBufferedImage(roiWithinImage);
        assertEquals(128, image.getWidth());
        assertEquals(96, image.getHeight());
        assertEquals(19, roiWithinImage.intX());
        assertEquals(19, roiWithinImage.intY());
        assertEquals(30, roiWithinImage.intWidth());
        assertEquals(30, roiWithinImage.intHeight());
    }

    @Test
    public void testReadAsBufferedImageWithSupportedScale() throws Exception {
        instance.setScale(new Rational(1, 4));

        final Rectangle margin = new Rectangle();
        BufferedImage image = instance.readAsBufferedImage(margin);
        assertEquals(16, image.getWidth());
        assertEquals(14, image.getHeight());
    }

    @Test
    @Disabled // rotation doesn't work yet
    public void testReadAsBufferedImageWithRotate90() throws Exception {
        instance.close();

        instance = new TurboJPEGImageReader();
        Path file = TestUtil.getImage("jpg-rgb-64x48x8.jpg");
        InputStream is = Files.newInputStream(file);
        instance.setSource(is);
        instance.setTransform(TurboJPEGImageReader.Transform.ROTATE_90);

        final Rectangle margin = new Rectangle();
        BufferedImage image = instance.readAsBufferedImage(margin);
        assertEquals(48, image.getWidth());
        assertEquals(64, image.getHeight());
    }

    @Test
    @Disabled // rotation doesn't work yet
    void testReadAsBufferedImageWithRotate180() throws Exception {
        instance.close();

        instance = new TurboJPEGImageReader();
        Path file = TestUtil.getImage("jpg-rgb-64x48x8.jpg");
        InputStream is = Files.newInputStream(file);
        instance.setSource(is);
        instance.setTransform(TurboJPEGImageReader.Transform.ROTATE_180);

        final Rectangle margin = new Rectangle();
        BufferedImage image = instance.readAsBufferedImage(margin);
        assertEquals(64, image.getWidth());
        assertEquals(48, image.getHeight());
    }

    @Test
    @Disabled // rotation doesn't work yet
    public void testReadAsBufferedImageWithRotate270() throws Exception {
        instance.close();

        instance = new TurboJPEGImageReader();
        Path file = TestUtil.getImage("jpg-rgb-64x48x8.jpg");
        InputStream is = Files.newInputStream(file);
        instance.setSource(is);
        instance.setTransform(TurboJPEGImageReader.Transform.ROTATE_270);

        final Rectangle margin = new Rectangle();
        BufferedImage image = instance.readAsBufferedImage(margin);
        assertEquals(48, image.getWidth());
        assertEquals(64, image.getHeight());
    }

    @Test
    public void testSetScaleWithSupportedScale() throws Exception {
        instance.setScale(new Rational(1, 2));
    }

    @Test
    public void testSetScaleWithUnsupportedScale() {
        assertThrows(TransformationNotSupportedException.class,
                () -> instance.setScale(new Rational(1, 341)));
    }

    @Test
    public void testSetTransformWithSupportedRotation() throws Exception {
        instance.close();

        instance = new TurboJPEGImageReader();
        Path file = TestUtil.getImage("jpg-rgb-128x96x8.jpg");
        InputStream is = Files.newInputStream(file);
        instance.setSource(is);

        instance.setTransform(TurboJPEGImageReader.Transform.ROTATE_90);
    }

    @Test
    public void testSetTransformWithUnsupportedRotation() {
        assertThrows(TransformationNotSupportedException.class,
                () -> instance.setTransform(TurboJPEGImageReader.Transform.ROTATE_90));
    }

    @Test
    public void testSetUseGrayscaleConversionWhenSupported() throws Exception {
        instance.close();

        instance = new TurboJPEGImageReader();
        Path file = TestUtil.getImage("jpg-rgb-128x96x8.jpg");
        InputStream is = Files.newInputStream(file);
        instance.setSource(is);

        instance.setUseGrayscaleConversion(true);
    }

    @Test
    public void testSetUseGrayscaleConversionWhenNotSupported() {
        assertThrows(TransformationNotSupportedException.class,
                () -> instance.setUseGrayscaleConversion(true));
    }

}
