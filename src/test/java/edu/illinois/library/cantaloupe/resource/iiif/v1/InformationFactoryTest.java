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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InformationFactoryTest extends BaseTest {

    private String imageUri;
    private Information imageInfo;
    private Processor processor;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_SELECTION_STRATEGY, "ManualSelectionStrategy");
        config.setProperty(Key.PROCESSOR_FALLBACK, "Java2dProcessor");

        imageUri = "http://example.org/bla";
        processor = new ProcessorFactory().newProcessor(Format.get("jpg"));
        ((FileProcessor) processor).setSourceFile(
                TestUtil.getImage("jpg-rgb-594x522x8-baseline.jpg"));

        Info info = processor.readInfo();
        imageInfo = new InformationFactory().newImageInfo(
                imageUri,
                processor.getAvailableOutputFormats(),
                info,
                0,
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
        processor.close();
        processor = new ProcessorFactory().newProcessor(Format.get("jpg"));
        ((FileProcessor) processor).setSourceFile(
                TestUtil.getImage("jpg-xmp-orientation-90.jpg"));

        Info info = processor.readInfo();
        imageInfo = new InformationFactory().newImageInfo(
                imageUri, processor.getAvailableOutputFormats(),
                info, 0, new ScaleConstraint(1, 1));
    }

    private void setUpForScaleConstrainedImage() throws Exception {
        processor.close();
        processor = new ProcessorFactory().newProcessor(Format.get("jpg"));
        ((FileProcessor) processor).setSourceFile(
                TestUtil.getImage("jpg-rgb-594x522x8-baseline.jpg"));

        Info info = processor.readInfo();
        imageInfo = new InformationFactory().newImageInfo(
                imageUri, processor.getAvailableOutputFormats(),
                info, 0, new ScaleConstraint(1, 2));
    }

    @Test
    void newImageInfoContext() {
        assertEquals("http://library.stanford.edu/iiif/image-api/1.1/context.json",
                imageInfo.context);
    }

    @Test
    void newImageInfoId() {
        assertEquals("http://example.org/bla", imageInfo.id);
    }

    @Test
    void newImageInfoWidth() {
        assertEquals(594, (long) imageInfo.width);
    }

    @Test
    void newImageInfoWidthWithRotatedImage() throws Exception {
        setUpForRotatedImage();
        assertEquals(64, (long) imageInfo.width);
    }

    @Test
    void newImageInfoWidthWithScaleConstraint() throws Exception {
        setUpForScaleConstrainedImage();
        assertEquals(297, (long) imageInfo.width);
    }

    @Test
    void newImageInfoHeight() {
        assertEquals(522, (long) imageInfo.height);
    }

    @Test
    void newImageInfoHeightWithRotatedImage() throws Exception {
        setUpForRotatedImage();
        assertEquals(56, (long) imageInfo.height);
    }

    @Test
    void newImageInfoHeightWithScaleConstraint() throws Exception {
        setUpForScaleConstrainedImage();
        assertEquals(261, (long) imageInfo.height);
    }

    @Test
    void newImageInfoScaleFactors() {
        assertEquals(4, imageInfo.scaleFactors.size());
        assertEquals(1, (long) imageInfo.scaleFactors.get(0));
        assertEquals(2, (long) imageInfo.scaleFactors.get(1));
        assertEquals(4, (long) imageInfo.scaleFactors.get(2));
        assertEquals(8, (long) imageInfo.scaleFactors.get(3));
    }

    @Test
    void newImageInfoScaleFactorsWithScaleConstrainedImage() throws Exception {
        setUpForScaleConstrainedImage();
        assertEquals(3, imageInfo.scaleFactors.size());
        assertEquals(1, (long) imageInfo.scaleFactors.get(0));
        assertEquals(2, (long) imageInfo.scaleFactors.get(1));
        assertEquals(4, (long) imageInfo.scaleFactors.get(2));
    }

    @Test
    void newImageInfoTileWidthWithUntiledImage() {
        assertEquals(594, (long) imageInfo.tileWidth);
    }

    @Test
    void newImageInfoTileWidthWithRotatedImage() throws Exception {
        setUpForRotatedImage();
        assertEquals(64, (long) imageInfo.tileWidth);
    }

    @Test
    void newImageInfoTileWidthWithUntiledImageWithScaleConstraint()
            throws Exception {
        setUpForScaleConstrainedImage();
        assertEquals(297, (long) imageInfo.tileWidth);
    }

    @Test
    void newImageInfoTileWidthWithTiledImage() throws Exception {
        processor.setSourceFormat(Format.get("tif"));
        ((FileProcessor) processor).setSourceFile(
                TestUtil.getImage("tif-rgb-1res-64x56x8-tiled-uncompressed.tif"));
        Info info = processor.readInfo();
        imageInfo = new InformationFactory().newImageInfo(
                imageUri, processor.getAvailableOutputFormats(),
                info, 0, new ScaleConstraint(1, 1));

        assertEquals(64, (long) imageInfo.tileWidth);
    }

    @Test
    void newImageInfoTileHeightWithUntiledImage() {
        assertEquals(522, (long) imageInfo.tileHeight);
    }

    @Test
    void newImageInfoTileHeightWithRotatedImage() throws Exception {
        setUpForRotatedImage();
        assertEquals(56, (long) imageInfo.tileHeight);
    }

    @Test
    void newImageInfoTileHeightWithUntiledImageWithScaleConstraint()
            throws Exception {
        setUpForScaleConstrainedImage();
        assertEquals(261, (long) imageInfo.tileHeight);
    }

    @Test
    void newImageInfoTileHeightWithTiledImage() throws Exception {
        processor.setSourceFormat(Format.get("tif"));
        ((FileProcessor) processor).setSourceFile(
                TestUtil.getImage("tif-rgb-1res-64x56x8-tiled-uncompressed.tif"));
        Info info = processor.readInfo();
        imageInfo = new InformationFactory().newImageInfo(
                imageUri, processor.getAvailableOutputFormats(),
                info, 0, new ScaleConstraint(1, 1));

        assertEquals(64, (long) imageInfo.tileWidth);
        assertEquals(56, (long) imageInfo.tileHeight);
    }

    @Test
    void newImageInfoFormats() {
        assertTrue(imageInfo.formats.contains("jpg"));
    }

    @Test
    void newImageInfoQualities() {
        assertTrue(imageInfo.qualities.contains("color"));
    }

    @Test
    void newImageInfoProfile() {
        assertEquals("http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level2",
                imageInfo.profile);
    }

}
