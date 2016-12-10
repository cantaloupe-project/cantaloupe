package edu.illinois.library.cantaloupe.operation.watermark;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import org.junit.Before;
import org.junit.Test;

import java.awt.Color;
import java.awt.Dimension;

import static org.junit.Assert.*;

public class BasicStringWatermarkServiceTest {

    private BasicStringWatermarkService instance;

    @Before
    public void setUp() throws Exception {
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        Configuration config = ConfigurationFactory.getInstance();
        config.clear();
        // valid config options
        config.setProperty(WatermarkService.ENABLED_CONFIG_KEY, true);
        config.setProperty(WatermarkService.STRATEGY_CONFIG_KEY, "BasicStrategy");
        config.setProperty(BasicWatermarkService.TYPE_CONFIG_KEY, "string");
        config.setProperty(BasicWatermarkService.INSET_CONFIG_KEY, 10);
        config.setProperty(BasicWatermarkService.POSITION_CONFIG_KEY, "top left");
        config.setProperty(BasicStringWatermarkService.STRING_CONFIG_KEY, "cats");
        config.setProperty(BasicStringWatermarkService.COLOR_CONFIG_KEY, "red");
        config.setProperty(BasicStringWatermarkService.FONT_CONFIG_KEY, "Helvetica");
        config.setProperty(BasicStringWatermarkService.FONT_SIZE_CONFIG_KEY, 14);
        config.setProperty(BasicStringWatermarkService.STROKE_COLOR_CONFIG_KEY, "blue");
        config.setProperty(BasicStringWatermarkService.STROKE_WIDTH_CONFIG_KEY, 3);

        instance = new BasicStringWatermarkService();
    }

    @Test
    public void testGetWatermark() throws Exception {
        final StringWatermark watermark = instance.getWatermark();
        assertEquals("cats", watermark.getString());
        assertEquals((long) 10, watermark.getInset());
        assertEquals(Position.TOP_LEFT, watermark.getPosition());
        assertEquals(Color.red, watermark.getColor());
        assertEquals("Helvetica", watermark.getFont().getFamily());
        assertEquals(14, watermark.getFont().getSize());
        assertEquals(Color.blue, watermark.getStrokeColor());
        assertEquals(3, watermark.getStrokeWidth(), 0.00001f);
    }

    @Test
    public void testShouldApplyToImage() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.clear();

        final Dimension imageSize = new Dimension(100, 100);

        // image width > width threshold, image height > height threshold
        config.setProperty(BasicWatermarkService.OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY, 50);
        config.setProperty(BasicWatermarkService.OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY, 50);
        assertTrue(BasicImageWatermarkService.shouldApplyToImage(imageSize));

        // image width < width threshold, image height < height threshold
        config.setProperty(BasicWatermarkService.OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY, 200);
        config.setProperty(BasicWatermarkService.OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY, 200);
        assertFalse(BasicImageWatermarkService.shouldApplyToImage(imageSize));

        // image width < width threshold, image height > height threshold
        config.setProperty(BasicWatermarkService.OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY, 200);
        config.setProperty(BasicWatermarkService.OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY, 50);
        assertFalse(BasicImageWatermarkService.shouldApplyToImage(imageSize));

        // image width > width threshold, image height < height threshold
        config.setProperty(BasicWatermarkService.OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY, 50);
        config.setProperty(BasicWatermarkService.OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY, 200);
        assertFalse(BasicImageWatermarkService.shouldApplyToImage(imageSize));
    }

}
