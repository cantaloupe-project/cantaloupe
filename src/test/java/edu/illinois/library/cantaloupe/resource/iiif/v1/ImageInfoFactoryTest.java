package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ImageInfoFactoryTest {

    private String imageUri;
    private ImageInfo info;
    private Processor processor;

    @Before
    public void setUp() throws Exception {
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(ProcessorFactory.FALLBACK_PROCESSOR_CONFIG_KEY,
                "Java2dProcessor");

        imageUri = "http://example.org/bla";
        processor = ProcessorFactory.getProcessor(Format.JPG);
        ((FileProcessor) processor).setSourceFile(
                TestUtil.getImage("jpg-rgb-594x522x8-baseline.jpg"));

        info = ImageInfoFactory.newImageInfo(imageUri, processor,
                processor.getImageInfo());
    }

    private void setUpForRotatedImage() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty("metadata.respect_orientation", true);

        processor = ProcessorFactory.getProcessor(Format.JPG);
        ((FileProcessor) processor).setSourceFile(
                TestUtil.getImage("jpg-rotated.jpg"));

        info = ImageInfoFactory.newImageInfo(imageUri, processor,
                processor.getImageInfo());
    }

    @Test
    public void testNewImageInfoContext() {
        assertEquals("http://library.stanford.edu/iiif/image-api/1.1/context.json", info.context);
    }

    @Test
    public void testNewImageInfoId() {
        assertEquals("http://example.org/bla", info.id);
    }

    @Test
    public void testNewImageInfoWidth() {
        assertEquals(594, (long) info.width);
    }

    @Test
    public void testNewImageInfoWidthWithRotatedImage() throws Exception {
        setUpForRotatedImage();
        assertEquals(64, (long) info.width);
    }

    @Test
    public void testNewImageInfoHeight() {
        assertEquals(522, (long) info.height);
    }

    @Test
    public void testNewImageInfoHeightWithRotatedImage() throws Exception {
        setUpForRotatedImage();
        assertEquals(56, (long) info.height);
    }

    @Test
    public void testNewImageInfoScaleFactors() {
        assertEquals(4, info.scaleFactors.size());
        assertEquals(1, (long) info.scaleFactors.get(0));
        assertEquals(2, (long) info.scaleFactors.get(1));
        assertEquals(4, (long) info.scaleFactors.get(2));
        assertEquals(8, (long) info.scaleFactors.get(3));
    }

    @Test
    public void testNewImageInfoTileWidthWithUntiledImage() {
        assertEquals(594, (long) info.tileWidth);
    }

    @Test
    public void testNewImageInfoTileWidthWithRotatedImage() throws Exception {
        setUpForRotatedImage();
        assertEquals(64, (long) info.tileWidth);
    }

    @Test
    public void testNewImageInfoTileWidthWithTiledImage() throws Exception {
        processor.setSourceFormat(Format.TIF);
        ((FileProcessor) processor).setSourceFile(
                TestUtil.getImage("tif-rgb-monores-64x56x8-tiled-uncompressed.tif"));
        info = ImageInfoFactory.newImageInfo(imageUri, processor,
                processor.getImageInfo());

        assertEquals(64, (long) info.tileWidth);
    }

    @Test
    public void testNewImageInfoTileHeightWithUntiledImage() {
        assertEquals(522, (long) info.tileHeight);
    }

    @Test
    public void testNewImageInfoTileHeightWithRotatedImage() throws Exception {
        setUpForRotatedImage();
        assertEquals(56, (long) info.tileHeight);
    }

    @Test
    public void testNewImageInfoTileHeightWithTiledImage() throws Exception {
        processor.setSourceFormat(Format.TIF);
        ((FileProcessor) processor).setSourceFile(
                TestUtil.getImage("tif-rgb-monores-64x56x8-tiled-uncompressed.tif"));
        info = ImageInfoFactory.newImageInfo(imageUri, processor,
                processor.getImageInfo());

        assertEquals(64, (long) info.tileWidth);
        assertEquals(56, (long) info.tileHeight);
    }

    @Test
    public void testNewImageInfoFormats() {
        assertTrue(info.formats.contains("jpg"));
    }

    @Test
    public void testNewImageInfoQualities() {
        assertTrue(info.qualities.contains("color"));
    }

    @Test
    public void testNewImageInfoProfile() {
        assertEquals("http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level2",
                info.profile);
    }

}
