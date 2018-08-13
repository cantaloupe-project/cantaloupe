package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ImageInfoFactoryTest extends BaseTest {

    private String imageUri;
    private ImageInfo imageInfo;
    private Processor processor;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_FALLBACK, "Java2dProcessor");

        imageUri = "http://example.org/bla";
        processor = new ProcessorFactory().newProcessor(Format.JPG);
        ((FileProcessor) processor).setSourceFile(
                TestUtil.getImage("jpg-rgb-594x522x8-baseline.jpg"));

        Info info = processor.readImageInfo();
        Info.Image infoImage = info.getImages().get(0);
        imageInfo = new ImageInfoFactory().newImageInfo(
                imageUri,
                processor,
                infoImage,
                info.getNumResolutions(),
                new ScaleConstraint(1, 1));
    }

    @Override
    public void tearDown() throws Exception {
        try {
            super.tearDown();
        } finally {
            processor.close();
        }
    }

    private void setUpForRotatedImage() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_RESPECT_ORIENTATION, true);

        processor.close();
        processor = new ProcessorFactory().newProcessor(Format.JPG);
        ((FileProcessor) processor).setSourceFile(
                TestUtil.getImage("jpg-rotated.jpg"));

        Info info = processor.readImageInfo();
        Info.Image infoImage = info.getImages().get(0);
        imageInfo = new ImageInfoFactory().newImageInfo(
                imageUri,
                processor,
                infoImage,
                info.getNumResolutions(),
                new ScaleConstraint(1, 1));
    }

    private void setUpForScaleConstrainedImage() throws Exception {
        processor.close();
        processor = new ProcessorFactory().newProcessor(Format.JPG);
        ((FileProcessor) processor).setSourceFile(
                TestUtil.getImage("jpg-rgb-594x522x8-baseline.jpg"));

        Info info = processor.readImageInfo();
        Info.Image infoImage = info.getImages().get(0);
        imageInfo = new ImageInfoFactory().newImageInfo(
                imageUri,
                processor,
                infoImage,
                info.getNumResolutions(),
                new ScaleConstraint(1, 2));
    }

    @Test
    public void newImageInfoContext() {
        assertEquals("http://library.stanford.edu/iiif/image-api/1.1/context.json",
                imageInfo.context);
    }

    @Test
    public void newImageInfoId() {
        assertEquals("http://example.org/bla", imageInfo.id);
    }

    @Test
    public void newImageInfoWidth() {
        assertEquals(594, (long) imageInfo.width);
    }

    @Test
    public void newImageInfoWidthWithRotatedImage() throws Exception {
        setUpForRotatedImage();
        assertEquals(64, (long) imageInfo.width);
    }

    @Test
    public void newImageInfoWidthWithScaleConstraint() throws Exception {
        setUpForScaleConstrainedImage();
        assertEquals(297, (long) imageInfo.width);
    }

    @Test
    public void newImageInfoHeight() {
        assertEquals(522, (long) imageInfo.height);
    }

    @Test
    public void newImageInfoHeightWithRotatedImage() throws Exception {
        setUpForRotatedImage();
        assertEquals(56, (long) imageInfo.height);
    }

    @Test
    public void newImageInfoHeightWithScaleConstraint() throws Exception {
        setUpForScaleConstrainedImage();
        assertEquals(261, (long) imageInfo.height);
    }

    @Test
    public void newImageInfoScaleFactors() {
        assertEquals(4, imageInfo.scaleFactors.size());
        assertEquals(1, (long) imageInfo.scaleFactors.get(0));
        assertEquals(2, (long) imageInfo.scaleFactors.get(1));
        assertEquals(4, (long) imageInfo.scaleFactors.get(2));
        assertEquals(8, (long) imageInfo.scaleFactors.get(3));
    }

    @Test
    public void newImageInfoScaleFactorsWithScaleConstrainedImage() throws Exception {
        setUpForScaleConstrainedImage();
        assertEquals(3, imageInfo.scaleFactors.size());
        assertEquals(1, (long) imageInfo.scaleFactors.get(0));
        assertEquals(2, (long) imageInfo.scaleFactors.get(1));
        assertEquals(4, (long) imageInfo.scaleFactors.get(2));
    }

    @Test
    public void newImageInfoTileWidthWithUntiledImage() {
        assertEquals(594, (long) imageInfo.tileWidth);
    }

    @Test
    public void newImageInfoTileWidthWithRotatedImage() throws Exception {
        setUpForRotatedImage();
        assertEquals(64, (long) imageInfo.tileWidth);
    }

    @Test
    public void newImageInfoTileWidthWithUntiledImageWithScaleConstraint()
            throws Exception {
        setUpForScaleConstrainedImage();
        assertEquals(297, (long) imageInfo.tileWidth);
    }

    @Test
    public void newImageInfoTileWidthWithTiledImage() throws Exception {
        processor.setSourceFormat(Format.TIF);
        ((FileProcessor) processor).setSourceFile(
                TestUtil.getImage("tif-rgb-1res-64x56x8-tiled-uncompressed.tif"));
        Info info = processor.readImageInfo();
        Info.Image infoImage = info.getImages().get(0);
        imageInfo = new ImageInfoFactory().newImageInfo(
                imageUri,
                processor,
                infoImage,
                info.getNumResolutions(),
                new ScaleConstraint(1, 1));

        assertEquals(64, (long) imageInfo.tileWidth);
    }

    @Test
    public void newImageInfoTileHeightWithUntiledImage() {
        assertEquals(522, (long) imageInfo.tileHeight);
    }

    @Test
    public void newImageInfoTileHeightWithRotatedImage() throws Exception {
        setUpForRotatedImage();
        assertEquals(56, (long) imageInfo.tileHeight);
    }

    @Test
    public void newImageInfoTileHeightWithUntiledImageWithScaleConstraint()
            throws Exception {
        setUpForScaleConstrainedImage();
        assertEquals(261, (long) imageInfo.tileHeight);
    }

    @Test
    public void newImageInfoTileHeightWithTiledImage() throws Exception {
        processor.setSourceFormat(Format.TIF);
        ((FileProcessor) processor).setSourceFile(
                TestUtil.getImage("tif-rgb-1res-64x56x8-tiled-uncompressed.tif"));
        Info info = processor.readImageInfo();
        Info.Image infoImage = info.getImages().get(0);
        imageInfo = new ImageInfoFactory().newImageInfo(
                imageUri,
                processor,
                infoImage,
                info.getNumResolutions(),
                new ScaleConstraint(1, 1));

        assertEquals(64, (long) imageInfo.tileWidth);
        assertEquals(56, (long) imageInfo.tileHeight);
    }

    @Test
    public void newImageInfoFormats() {
        assertTrue(imageInfo.formats.contains("jpg"));
    }

    @Test
    public void newImageInfoQualities() {
        assertTrue(imageInfo.qualities.contains("color"));
    }

    @Test
    public void newImageInfoProfile() {
        assertEquals("http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level2",
                imageInfo.profile);
    }

}
