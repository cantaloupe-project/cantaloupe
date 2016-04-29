package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class ImageInfoFactoryTest {

    private Identifier identifier;
    private String imageUri;
    private ImageInfo info;
    private Processor processor;

    @Before
    public void setUp() throws Exception {
        Configuration config = Configuration.getInstance();
        config.clear();
        config.setProperty("processor.fallback", "Java2dProcessor");

        identifier = new Identifier("bla");
        imageUri = "http://example.org/bla";
        processor = ProcessorFactory.getProcessor(Format.JPG);
        ((FileProcessor) processor).setSourceFile(
                TestUtil.getImage("jpg-rgb-594x522x8-baseline.jpg"));
        info = ImageInfoFactory.newImageInfo(identifier, imageUri, processor,
                processor.getImageInfo());
    }

    @Test
    public void testNewImageInfoContext() {
        assertEquals("http://iiif.io/api/image/2/context.json", info.context);
    }

    @Test
    public void testNewImageInfoId() {
        assertEquals("http://example.org/bla", info.id);
    }

    @Test
    public void testNewImageInfoProtocol() {
        assertEquals("http://iiif.io/api/image", info.protocol);
    }

    @Test
    public void testNewImageInfoWidth() {
        assertEquals(594, (long) info.width);
    }

    @Test
    public void testNewImageInfoHeight() {
        assertEquals(522, (long) info.height);
    }

    @Test
    public void testNewImageInfoSizes() {
        assertEquals(3, info.sizes.size());
        assertEquals(74, (long) info.sizes.get(0).width);
        assertEquals(65, (long) info.sizes.get(0).height);
        assertEquals(149, (long) info.sizes.get(1).width);
        assertEquals(131, (long) info.sizes.get(1).height);
        assertEquals(297, (long) info.sizes.get(2).width);
        assertEquals(261, (long) info.sizes.get(2).height);
    }

    @Test
    public void testNewImageInfoTilesWithUntiledImage() {
        assertEquals(1, info.tiles.size());
        assertEquals(594, (long) info.tiles.get(0).width);
        assertEquals(522, (long) info.tiles.get(0).height);

        assertEquals(4, (long) info.tiles.get(0).scaleFactors.size());
        assertEquals(1, (long) info.tiles.get(0).scaleFactors.get(0));
        assertEquals(2, (long) info.tiles.get(0).scaleFactors.get(1));
        assertEquals(4, (long) info.tiles.get(0).scaleFactors.get(2));
        assertEquals(8, (long) info.tiles.get(0).scaleFactors.get(3));
    }

    @Test
    public void testNewImageInfoTilesWithTiledImage() throws Exception {
        processor.setSourceFormat(Format.TIF);
        ((FileProcessor) processor).setSourceFile(
                TestUtil.getImage("tif-rgb-monores-64x56x8-tiled-uncompressed.tif"));
        info = ImageInfoFactory.newImageInfo(identifier, imageUri, processor,
                processor.getImageInfo());

        assertEquals(1, info.tiles.size());
        assertEquals(64, (long) info.tiles.get(0).width);
        assertEquals(56, (long) info.tiles.get(0).height);

        assertEquals(1, (long) info.tiles.get(0).scaleFactors.size());
        assertEquals(1, (long) info.tiles.get(0).scaleFactors.get(0));
    }

    @Test
    public void testNewImageInfoProfile() throws Exception {
        assertEquals("http://iiif.io/api/image/2/level2.json",
                info.profile.get(0));

        // If some are present, we will assume the rest are. (The exact
        // contents of the sets are processor-dependent and this is not a
        // processor test.)
        ((Set) ((Map) info.profile.get(1)).get("formats")).contains("gif");
        ((Set) ((Map) info.profile.get(1)).get("qualities")).contains("color");

        final Set supportsSet = (Set) ((Map) info.profile.get(1)).get("supports");
        assertTrue(supportsSet.contains("baseUriRedirect"));
        assertTrue(supportsSet.contains("canonicalLinkHeader"));
        assertTrue(supportsSet.contains("cors"));
        assertTrue(supportsSet.contains("jsonldMediaType"));
        assertTrue(supportsSet.contains("profileLinkHeader"));
        assertTrue(supportsSet.contains("sizeByConfinedWh"));
        assertTrue(supportsSet.contains("sizeByWhListed"));
    }

    // the service key will be tested in InformationResourceTest

}
