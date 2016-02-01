package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Test;

import java.awt.Dimension;
import java.io.File;

import static org.junit.Assert.*;

public class WatermarkServiceTest {

    @Test
    public void testGetWatermarkImage() {
        Configuration config = new BaseConfiguration();
        Application.setConfiguration(config);
        // null value
        config.setProperty(WatermarkService.WATERMARK_FILE_CONFIG_KEY, null);
        assertNull(WatermarkService.getWatermarkImage());
        // empty value
        config.setProperty(WatermarkService.WATERMARK_FILE_CONFIG_KEY, "");
        assertNull(WatermarkService.getWatermarkImage());
        // invalid value
        config.setProperty(WatermarkService.WATERMARK_FILE_CONFIG_KEY, "bogus");
        assertEquals(new File("bogus"), WatermarkService.getWatermarkImage());
        // valid value
        config.setProperty(WatermarkService.WATERMARK_FILE_CONFIG_KEY, "/dev/null");
        assertEquals(new File("/dev/null"), WatermarkService.getWatermarkImage());
    }

    @Test
    public void testGetWatermarkInset() {
        Configuration config = new BaseConfiguration();
        Application.setConfiguration(config);
        // null value
        config.setProperty(WatermarkService.WATERMARK_INSET_CONFIG_KEY, null);
        assertEquals(0, WatermarkService.getWatermarkInset());
        // empty value
        config.setProperty(WatermarkService.WATERMARK_INSET_CONFIG_KEY, "");
        assertEquals(0, WatermarkService.getWatermarkInset());
        // invalid value
        config.setProperty(WatermarkService.WATERMARK_INSET_CONFIG_KEY, "bogus");
        assertEquals(0, WatermarkService.getWatermarkInset());
        // valid value
        config.setProperty(WatermarkService.WATERMARK_INSET_CONFIG_KEY, "50");
        assertEquals(50, WatermarkService.getWatermarkInset());
    }

    @Test
    public void testGetWatermarkPosition() {
        Configuration config = new BaseConfiguration();
        Application.setConfiguration(config);
        // null value
        config.setProperty(WatermarkService.WATERMARK_POSITION_CONFIG_KEY, null);
        assertNull(WatermarkService.getWatermarkPosition());
        // empty value
        config.setProperty(WatermarkService.WATERMARK_POSITION_CONFIG_KEY, "");
        assertNull(WatermarkService.getWatermarkPosition());
        // invalid value
        config.setProperty(WatermarkService.WATERMARK_POSITION_CONFIG_KEY, "bogus");
        assertNull(WatermarkService.getWatermarkPosition());
        // valid value
        config.setProperty(WatermarkService.WATERMARK_POSITION_CONFIG_KEY, "top left");
        assertEquals(Position.TOP_LEFT, WatermarkService.getWatermarkPosition());
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
