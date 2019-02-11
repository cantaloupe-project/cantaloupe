package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.*;

public class BasicImageOverlayServiceTest extends BaseTest {

    private BasicImageOverlayService instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.OVERLAY_ENABLED, true);
        config.setProperty(Key.OVERLAY_STRATEGY, "BasicStrategy");
        config.setProperty(Key.OVERLAY_TYPE, "image");
        config.setProperty(Key.OVERLAY_INSET, 10);
        config.setProperty(Key.OVERLAY_POSITION, "top left");
        config.setProperty(Key.OVERLAY_IMAGE, "/dev/null");

        instance = new BasicImageOverlayService();
    }

    @Test
    public void testGetOverlay() throws Exception {
        final ImageOverlay overlay = instance.getOverlay();
        if (SystemUtils.IS_OS_WINDOWS) {
            assertEquals(new URI("file:///C:/dev/null"), overlay.getURI());
        } else {
            assertEquals(new URI("file:///dev/null"), overlay.getURI());
        }
        assertEquals((long) 10, overlay.getInset());
        assertEquals(Position.TOP_LEFT, overlay.getPosition());
    }

    @Test
    public void testShouldApplyToImage() {
        Configuration config = Configuration.getInstance();
        config.clear();

        final Dimension imageSize = new Dimension(100, 100);

        // image width > width threshold, image height > height threshold
        config.setProperty(Key.OVERLAY_OUTPUT_WIDTH_THRESHOLD, 50);
        config.setProperty(Key.OVERLAY_OUTPUT_HEIGHT_THRESHOLD, 50);
        assertTrue(BasicImageOverlayService.shouldApplyToImage(imageSize));

        // image width < width threshold, image height < height threshold
        config.setProperty(Key.OVERLAY_OUTPUT_WIDTH_THRESHOLD, 200);
        config.setProperty(Key.OVERLAY_OUTPUT_HEIGHT_THRESHOLD, 200);
        assertFalse(BasicImageOverlayService.shouldApplyToImage(imageSize));

        // image width < width threshold, image height > height threshold
        config.setProperty(Key.OVERLAY_OUTPUT_WIDTH_THRESHOLD, 200);
        config.setProperty(Key.OVERLAY_OUTPUT_HEIGHT_THRESHOLD, 50);
        assertFalse(BasicImageOverlayService.shouldApplyToImage(imageSize));

        // image width > width threshold, image height < height threshold
        config.setProperty(Key.OVERLAY_OUTPUT_WIDTH_THRESHOLD, 50);
        config.setProperty(Key.OVERLAY_OUTPUT_HEIGHT_THRESHOLD, 200);
        assertFalse(BasicImageOverlayService.shouldApplyToImage(imageSize));
    }

}
