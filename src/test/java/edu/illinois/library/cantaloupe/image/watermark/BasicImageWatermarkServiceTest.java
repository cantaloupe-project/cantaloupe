package edu.illinois.library.cantaloupe.image.watermark;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.io.File;

import static org.junit.Assert.*;

public class BasicImageWatermarkServiceTest {

    private BasicImageWatermarkService instance;

    @Before
    public void setUp() throws Exception {
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        Configuration config = ConfigurationFactory.getInstance();
        config.clear();
        // valid config options
        config.setProperty(WatermarkService.ENABLED_CONFIG_KEY, true);
        config.setProperty(WatermarkService.STRATEGY_CONFIG_KEY, "BasicStrategy");
        config.setProperty(BasicWatermarkService.TYPE_CONFIG_KEY, "image");
        config.setProperty(BasicWatermarkService.INSET_CONFIG_KEY, 10);
        config.setProperty(BasicWatermarkService.POSITION_CONFIG_KEY, "top left");
        config.setProperty(BasicImageWatermarkService.FILE_CONFIG_KEY, "/dev/null");

        instance = new BasicImageWatermarkService();
    }

    @Test
    public void testGetWatermark() {
        final ImageWatermark watermark = instance.getWatermark();
        assertEquals(new File("/dev/null"), watermark.getImage());
        assertEquals((long) 10, watermark.getInset());
        assertEquals(Position.TOP_LEFT, watermark.getPosition());
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
