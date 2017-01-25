package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.operation.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class ImageInfoFactoryTest extends BaseTest {

    private Identifier identifier;
    private String imageUri;
    private ImageInfo imageInfo;
    private Processor processor;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(ProcessorFactory.FALLBACK_PROCESSOR_CONFIG_KEY,
                "Java2dProcessor");
        config.setProperty(AbstractResource.MAX_PIXELS_CONFIG_KEY, 100);

        identifier = new Identifier("bla");
        imageUri = "http://example.org/bla";
        processor = ProcessorFactory.getProcessor(Format.JPG);
        ((FileProcessor) processor).setSourceFile(
                TestUtil.getImage("jpg-rgb-594x522x8-baseline.jpg"));
        imageInfo = ImageInfoFactory.newImageInfo(identifier, imageUri, processor,
                processor.readImageInfo());
    }

    private void setUpForRotatedImage() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty("metadata.respect_orientation", true);

        identifier = new Identifier("bla");
        processor = ProcessorFactory.getProcessor(Format.JPG);
        ((FileProcessor) processor).setSourceFile(
                TestUtil.getImage("jpg-rotated.jpg"));

        imageInfo = ImageInfoFactory.newImageInfo(identifier, imageUri, processor,
                processor.readImageInfo());
    }

    @Test
    public void testNewImageInfoContext() {
        assertEquals("http://iiif.io/api/image/2/context.json",
                imageInfo.get("@context"));
    }

    @Test
    public void testNewImageInfoId() {
        assertEquals("http://example.org/bla", imageInfo.get("@id"));
    }

    @Test
    public void testNewImageInfoProtocol() {
        assertEquals("http://iiif.io/api/image", imageInfo.get("protocol"));
    }

    @Test
    public void testNewImageInfoWidth() {
        assertEquals(594, (int) imageInfo.get("width"));
    }

    @Test
    public void testNewImageInfoWidthWithRotatedImage() throws Exception {
        setUpForRotatedImage();
        assertEquals(64, imageInfo.get("width"));
    }

    @Test
    public void testNewImageInfoHeight() {
        assertEquals(522, (int) imageInfo.get("height"));
    }

    @Test
    public void testNewImageInfoHeightWithRotatedImage() throws Exception {
        setUpForRotatedImage();
        assertEquals(56, imageInfo.get("height"));
    }

    @Test
    public void testNewImageInfoSizes() {
        @SuppressWarnings("unchecked")
        List<ImageInfo.Size> sizes =
                (List<ImageInfo.Size>) imageInfo.get("sizes");
        assertEquals(3, sizes.size());
        assertEquals(74, (long) sizes.get(0).width);
        assertEquals(65, (long) sizes.get(0).height);
        assertEquals(149, (long) sizes.get(1).width);
        assertEquals(131, (long) sizes.get(1).height);
        assertEquals(297, (long) sizes.get(2).width);
        assertEquals(261, (long) sizes.get(2).height);
    }

    @Test
    public void testNewImageInfoSizesWithRotatedImage() throws Exception {
        // TODO: write this (need a bigger rotated image)
    }

    @Test
    public void testNewImageInfoTilesWithUntiledImage() {
        @SuppressWarnings("unchecked")
        List<ImageInfo.Tile> tiles =
                (List<ImageInfo.Tile>) imageInfo.get("tiles");
        assertEquals(1, tiles.size());
        assertEquals(594, (long) tiles.get(0).width);
        assertEquals(522, (long) tiles.get(0).height);

        assertEquals(4, (long) tiles.get(0).scaleFactors.size());
        assertEquals(1, (long) tiles.get(0).scaleFactors.get(0));
        assertEquals(2, (long) tiles.get(0).scaleFactors.get(1));
        assertEquals(4, (long) tiles.get(0).scaleFactors.get(2));
        assertEquals(8, (long) tiles.get(0).scaleFactors.get(3));
    }

    @Test
    public void testNewImageInfoTilesWithRotatedImage() throws Exception {
        setUpForRotatedImage();
        @SuppressWarnings("unchecked")
        List<ImageInfo.Tile> tiles =
                (List<ImageInfo.Tile>) imageInfo.get("tiles");
        assertEquals(64, (long) tiles.get(0).width);
        assertEquals(56, (long) tiles.get(0).height);
    }

    @Test
    public void testNewImageInfoTilesWithTiledImage() throws Exception {
        processor.setSourceFormat(Format.TIF);
        ((FileProcessor) processor).setSourceFile(
                TestUtil.getImage("tif-rgb-monores-64x56x8-tiled-uncompressed.tif"));
        imageInfo = ImageInfoFactory.newImageInfo(identifier, imageUri, processor,
                processor.readImageInfo());

        @SuppressWarnings("unchecked")
        List<ImageInfo.Tile> tiles =
                (List<ImageInfo.Tile>) imageInfo.get("tiles");
        assertEquals(1, tiles.size());
        assertEquals(64, (long) tiles.get(0).width);
        assertEquals(56, (long) tiles.get(0).height);

        assertEquals(1, (long) tiles.get(0).scaleFactors.size());
        assertEquals(1, (long) tiles.get(0).scaleFactors.get(0));
    }

    @Test
    public void testNewImageInfoProfile() throws Exception {
        List profile = (List) imageInfo.get("profile");
        assertEquals("http://iiif.io/api/image/2/level2.json", profile.get(0));
    }

    @Test
    public void testNewImageInfoFormats() throws Exception {
        List profile = (List) imageInfo.get("profile");
        // If some are present, we will assume the rest are. (The exact
        // contents of the sets are processor-dependent and this is not a
        // processor test.)
        assertTrue(((Set) ((Map) profile.get(1)).get("formats")).contains("gif"));
    }

    @Test
    public void testNewImageInfoQualities() throws Exception {
        List profile = (List) imageInfo.get("profile");
        // If some are present, we will assume the rest are. (The exact
        // contents of the sets are processor-dependent and this is not a
        // processor test.)
        assertTrue(((Set) ((Map) profile.get(1)).get("qualities")).contains("color"));
    }

    @Test
    public void testNewImageInfoMaxArea() throws Exception {
        List profile = (List) imageInfo.get("profile");

        // with max_pixels > 0
        assertTrue(((Map) profile.get(1)).get("maxArea").
                equals(ConfigurationFactory.getInstance().
                        getInt(AbstractResource.MAX_PIXELS_CONFIG_KEY)));

        // with max_pixels == 0
        ConfigurationFactory.getInstance().
                setProperty(AbstractResource.MAX_PIXELS_CONFIG_KEY, 0);
        imageInfo = ImageInfoFactory.newImageInfo(identifier, imageUri,
                processor, processor.readImageInfo());
        profile = (List) imageInfo.get("profile");
        assertFalse(((Map) profile.get(1)).containsKey("maxArea"));
    }

    @Test
    public void testNewImageInfoSupports() throws Exception {
        List profile = (List) imageInfo.get("profile");

        final Set supportsSet = (Set) ((Map) profile.get(1)).get("supports");
        assertTrue(supportsSet.contains("baseUriRedirect"));
        assertTrue(supportsSet.contains("canonicalLinkHeader"));
        assertTrue(supportsSet.contains("cors"));
        assertTrue(supportsSet.contains("jsonldMediaType"));
        assertTrue(supportsSet.contains("profileLinkHeader"));
        assertTrue(supportsSet.contains("sizeByConfinedWh"));
        assertTrue(supportsSet.contains("sizeByWhListed"));
    }

    @Test
    public void testNewImageInfoDelegateScriptKeys() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(
                ScriptEngineFactory.DELEGATE_SCRIPT_ENABLED_CONFIG_KEY, true);
        config.setProperty(
                ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        imageInfo = ImageInfoFactory.newImageInfo(identifier, imageUri,
                processor, processor.readImageInfo());

        assertEquals("Copyright My Great Organization. All rights reserved.",
                imageInfo.get("attribution"));
        assertEquals("http://example.org/license.html",
                imageInfo.get("license"));
    }

}
