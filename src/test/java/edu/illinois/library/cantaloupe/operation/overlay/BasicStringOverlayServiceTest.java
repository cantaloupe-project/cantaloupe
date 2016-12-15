package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import org.junit.Before;
import org.junit.Test;

import java.awt.Color;
import java.awt.Dimension;

import static org.junit.Assert.*;

public class BasicStringOverlayServiceTest {

    private BasicStringOverlayService instance;

    @Before
    public void setUp() throws Exception {
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        Configuration config = ConfigurationFactory.getInstance();
        config.clear();
        // valid config options
        config.setProperty(OverlayService.ENABLED_CONFIG_KEY, true);
        config.setProperty(OverlayService.STRATEGY_CONFIG_KEY, "BasicStrategy");
        config.setProperty(BasicOverlayService.TYPE_CONFIG_KEY, "string");
        config.setProperty(BasicOverlayService.INSET_CONFIG_KEY, 10);
        config.setProperty(BasicOverlayService.POSITION_CONFIG_KEY, "top left");
        config.setProperty(BasicStringOverlayService.STRING_CONFIG_KEY, "cats");
        config.setProperty(BasicStringOverlayService.COLOR_CONFIG_KEY, "red");
        config.setProperty(BasicStringOverlayService.FONT_CONFIG_KEY, "Helvetica");
        config.setProperty(BasicStringOverlayService.FONT_SIZE_CONFIG_KEY, 14);
        config.setProperty(BasicStringOverlayService.STROKE_COLOR_CONFIG_KEY, "blue");
        config.setProperty(BasicStringOverlayService.STROKE_WIDTH_CONFIG_KEY, 3);

        instance = new BasicStringOverlayService();
    }

    @Test
    public void testGetOverlay() throws Exception {
        final StringOverlay overlay = instance.getOverlay();
        assertEquals("cats", overlay.getString());
        assertEquals((long) 10, overlay.getInset());
        assertEquals(Position.TOP_LEFT, overlay.getPosition());
        assertEquals(Color.red, overlay.getColor());
        assertEquals("Helvetica", overlay.getFont().getFamily());
        assertEquals(14, overlay.getFont().getSize());
        assertEquals(Color.blue, overlay.getStrokeColor());
        assertEquals(3, overlay.getStrokeWidth(), 0.00001f);
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
