package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
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
    private ImageInfo<String, Object> imageInfo;
    private Processor processor;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_FALLBACK, "Java2dProcessor");
        config.setProperty(Key.MAX_PIXELS, 0);

        identifier = new Identifier("bla");
        imageUri = "http://example.org/bla";
        processor = new ProcessorFactory().newProcessor(Format.JPG);
        ((FileProcessor) processor).setSourceFile(
                TestUtil.getImage("jpg-rgb-594x522x8-baseline.jpg"));
        imageInfo = new ImageInfoFactory().newImageInfo(
                identifier, imageUri, processor, processor.readImageInfo());
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

        identifier = new Identifier("bla");
        processor.close();
        processor = new ProcessorFactory().newProcessor(Format.JPG);
        ((FileProcessor) processor).setSourceFile(
                TestUtil.getImage("jpg-rotated.jpg"));

        imageInfo = new ImageInfoFactory().newImageInfo(
                identifier, imageUri, processor, processor.readImageInfo());
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
        assertEquals(4, sizes.size());
        assertEquals(74, (long) sizes.get(0).width);
        assertEquals(65, (long) sizes.get(0).height);
        assertEquals(149, (long) sizes.get(1).width);
        assertEquals(131, (long) sizes.get(1).height);
        assertEquals(297, (long) sizes.get(2).width);
        assertEquals(261, (long) sizes.get(2).height);
        assertEquals(594, (long) sizes.get(3).width);
        assertEquals(522, (long) sizes.get(3).height);
    }

    @Test
    public void testNewImageInfoSizesMinSize() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_MIN_SIZE, 200);

        imageInfo = new ImageInfoFactory().newImageInfo(
                identifier, imageUri, processor, processor.readImageInfo());
        @SuppressWarnings("unchecked")
        List<ImageInfo.Size> sizes =
                (List<ImageInfo.Size>) imageInfo.get("sizes");
        assertEquals(2, sizes.size());
        assertEquals(297, (long) sizes.get(0).width);
        assertEquals(261, (long) sizes.get(0).height);
        assertEquals(594, (long) sizes.get(1).width);
        assertEquals(522, (long) sizes.get(1).height);
    }

    @Test
    public void testNewImageInfoSizesMaxSize() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.MAX_PIXELS, 10000);

        imageInfo = new ImageInfoFactory().newImageInfo(
                identifier, imageUri, processor, processor.readImageInfo());
        @SuppressWarnings("unchecked")
        List<ImageInfo.Size> sizes =
                (List<ImageInfo.Size>) imageInfo.get("sizes");
        assertEquals(1, sizes.size());
        assertEquals(74, (long) sizes.get(0).width);
        assertEquals(65, (long) sizes.get(0).height);
    }

    @Test
    public void testNewImageInfoSizesWithRotatedImage() {
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
                TestUtil.getImage("tif-rgb-1res-64x56x8-tiled-uncompressed.tif"));
        imageInfo = new ImageInfoFactory().newImageInfo(
                identifier, imageUri, processor, processor.readImageInfo());

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
    public void testNewImageInfoProfile() {
        List<?> profile = (List<?>) imageInfo.get("profile");
        assertEquals("http://iiif.io/api/image/2/level2.json", profile.get(0));
    }

    @Test
    public void testNewImageInfoFormats() {
        List<?> profile = (List<?>) imageInfo.get("profile");
        // If some are present, we will assume the rest are. (The exact
        // contents of the sets are processor-dependent and this is not a
        // processor test.)
        assertTrue(((Set<?>) ((Map<?, ?>) profile.get(1)).get("formats")).contains("gif"));
    }

    @Test
    public void testNewImageInfoQualities() {
        List<?> profile = (List<?>) imageInfo.get("profile");
        // If some are present, we will assume the rest are. (The exact
        // contents of the sets are processor-dependent and this is not a
        // processor test.)
        assertTrue(((Set<?>) ((Map<?, ?>) profile.get(1)).get("qualities")).contains("color"));
    }

    @Test
    public void testNewImageInfoMaxAreaWithPositiveMaxPixels() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.MAX_PIXELS, 100);

        imageInfo = new ImageInfoFactory().newImageInfo(
                identifier, imageUri, processor, processor.readImageInfo());
        List<?> profile = (List<?>) imageInfo.get("profile");
        assertTrue(((Map<?, ?>) profile.get(1)).get("maxArea").
                equals(config.getInt(Key.MAX_PIXELS)));
    }

    @Test
    public void testNewImageInfoMaxAreaWithZeroMaxPixels() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.MAX_PIXELS, 0);

        imageInfo = new ImageInfoFactory().newImageInfo(
                identifier, imageUri, processor, processor.readImageInfo());
        List<?> profile = (List<?>) imageInfo.get("profile");
        assertFalse(((Map<?, ?>) profile.get(1)).containsKey("maxArea"));
    }

    @Test
    public void testNewImageInfoSupports() {
        List<?> profile = (List<?>) imageInfo.get("profile");

        final Set<?> supportsSet = (Set<?>) ((Map<?, ?>) profile.get(1)).get("supports");
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
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").toString());
        imageInfo = new ImageInfoFactory().newImageInfo(identifier, imageUri,
                processor, processor.readImageInfo());

        assertEquals("Copyright My Great Organization. All rights reserved.",
                imageInfo.get("attribution"));
        assertEquals("http://example.org/license.html",
                imageInfo.get("license"));
    }

}
