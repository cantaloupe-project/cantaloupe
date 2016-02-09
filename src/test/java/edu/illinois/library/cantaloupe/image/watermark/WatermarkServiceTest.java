package edu.illinois.library.cantaloupe.image.watermark;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.ConfigurationException;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.io.File;

import static org.junit.Assert.*;

public class WatermarkServiceTest {

    @Before
    public void setUp() {
        Configuration config = new BaseConfiguration();
        Application.setConfiguration(config);
        // valid config options
        config.setProperty(WatermarkService.WATERMARK_ENABLED_CONFIG_KEY, true);
        config.setProperty(WatermarkService.WATERMARK_FILE_CONFIG_KEY, "/dev/null");
        config.setProperty(WatermarkService.WATERMARK_INSET_CONFIG_KEY, 10);
        config.setProperty(WatermarkService.WATERMARK_POSITION_CONFIG_KEY, "top left");
    }

    @Test
    public void testNewWatermarkWithValidConfig() throws Exception {
        Watermark watermark = WatermarkService.newWatermark();
        assertEquals(new File("/dev/null"), watermark.getImage());
        assertEquals(10, watermark.getInset());
        assertEquals(Position.TOP_LEFT, watermark.getPosition());
    }

    @Test
    public void testNewWatermarkWithInvalidConfig() throws Exception {
        Configuration config = Application.getConfiguration();
        config.setProperty(WatermarkService.WATERMARK_FILE_CONFIG_KEY, null);
        try {
            WatermarkService.newWatermark();
            fail();
        } catch (ConfigurationException e) {
            // pass
        }
    }

    @Test
    public void testIsEnabled() {
        Configuration config = new BaseConfiguration();
        Application.setConfiguration(config);
        // null value
        config.setProperty(WatermarkService.WATERMARK_ENABLED_CONFIG_KEY, null);
        assertFalse(WatermarkService.isEnabled());
        // false
        config.setProperty(WatermarkService.WATERMARK_ENABLED_CONFIG_KEY, false);
        assertFalse(WatermarkService.isEnabled());
        // true
        config.setProperty(WatermarkService.WATERMARK_ENABLED_CONFIG_KEY, true);
        assertTrue(WatermarkService.isEnabled());
    }

    @Test
    public void testShouldApplyToImage() {
        Configuration config = new BaseConfiguration();
        Application.setConfiguration(config);

        final Dimension imageSize = new Dimension(100, 100);

        // image width > width threshold, image height > height threshold
        config.setProperty(WatermarkService.WATERMARK_OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY, 50);
        config.setProperty(WatermarkService.WATERMARK_OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY, 50);
        assertTrue(WatermarkService.shouldApplyToImage(imageSize));

        // image width < width threshold, image height < height threshold
        config.setProperty(WatermarkService.WATERMARK_OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY, 200);
        config.setProperty(WatermarkService.WATERMARK_OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY, 200);
        assertFalse(WatermarkService.shouldApplyToImage(imageSize));

        // image width < width threshold, image height > height threshold
        config.setProperty(WatermarkService.WATERMARK_OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY, 200);
        config.setProperty(WatermarkService.WATERMARK_OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY, 50);
        assertFalse(WatermarkService.shouldApplyToImage(imageSize));

        // image width > width threshold, image height < height threshold
        config.setProperty(WatermarkService.WATERMARK_OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY, 50);
        config.setProperty(WatermarkService.WATERMARK_OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY, 200);
        assertFalse(WatermarkService.shouldApplyToImage(imageSize));
    }

}
