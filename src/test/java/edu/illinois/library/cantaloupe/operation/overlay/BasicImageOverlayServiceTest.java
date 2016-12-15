package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.io.File;

import static org.junit.Assert.*;

public class BasicImageOverlayServiceTest {

    private BasicImageOverlayService instance;

    @Before
    public void setUp() throws Exception {
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        Configuration config = ConfigurationFactory.getInstance();
        config.clear();
        // valid config options
        config.setProperty(OverlayService.ENABLED_CONFIG_KEY, true);
        config.setProperty(OverlayService.STRATEGY_CONFIG_KEY, "BasicStrategy");
        config.setProperty(BasicOverlayService.TYPE_CONFIG_KEY, "image");
        config.setProperty(BasicOverlayService.INSET_CONFIG_KEY, 10);
        config.setProperty(BasicOverlayService.POSITION_CONFIG_KEY, "top left");
        config.setProperty(BasicImageOverlayService.FILE_CONFIG_KEY, "/dev/null");

        instance = new BasicImageOverlayService();
    }

    @Test
    public void testGetOverlay() {
        final ImageOverlay overlay = instance.getOverlay();
        assertEquals(new File("/dev/null"), overlay.getImage());
        assertEquals((long) 10, overlay.getInset());
        assertEquals(Position.TOP_LEFT, overlay.getPosition());
    }

    @Test
    public void testShouldApplyToImage() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.clear();

        final Dimension imageSize = new Dimension(100, 100);

        // image width > width threshold, image height > height threshold
        config.setProperty(BasicOverlayService.OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY, 50);
        config.setProperty(BasicOverlayService.OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY, 50);
        assertTrue(BasicImageOverlayService.shouldApplyToImage(imageSize));

        // image width < width threshold, image height < height threshold
        config.setProperty(BasicOverlayService.OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY, 200);
        config.setProperty(BasicOverlayService.OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY, 200);
        assertFalse(BasicImageOverlayService.shouldApplyToImage(imageSize));

        // image width < width threshold, image height > height threshold
        config.setProperty(BasicOverlayService.OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY, 200);
        config.setProperty(BasicOverlayService.OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY, 50);
        assertFalse(BasicImageOverlayService.shouldApplyToImage(imageSize));

        // image width > width threshold, image height < height threshold
        config.setProperty(BasicOverlayService.OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY, 50);
        config.setProperty(BasicOverlayService.OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY, 200);
        assertFalse(BasicImageOverlayService.shouldApplyToImage(imageSize));
    }

}
