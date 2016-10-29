package edu.illinois.library.cantaloupe.image.watermark;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;

import static org.junit.Assert.*;

public class BasicWatermarkServiceTest {

    private BasicWatermarkService instance;

    @Before
    public void setUp() throws Exception {
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        Configuration config = ConfigurationFactory.getInstance();
        config.clear();
        // valid config options
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_ENABLED_CONFIG_KEY,
                "true");
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        config.setProperty(WatermarkService.ENABLED_CONFIG_KEY, true);
        config.setProperty(WatermarkService.STRATEGY_CONFIG_KEY, "BasicStrategy");
        config.setProperty(BasicWatermarkService.BASIC_STRATEGY_FILE_CONFIG_KEY, "/dev/null");
        config.setProperty(BasicWatermarkService.BASIC_STRATEGY_INSET_CONFIG_KEY, 10);
        config.setProperty(BasicWatermarkService.BASIC_STRATEGY_POSITION_CONFIG_KEY, "top left");

        instance = new BasicWatermarkService();
    }

    @Test
    public void testShouldApplyToImage() {
        Configuration config = ConfigurationFactory.getInstance();
        config.clear();

        final Dimension imageSize = new Dimension(100, 100);

        // image width > width threshold, image height > height threshold
        config.setProperty(BasicWatermarkService.BASIC_STRATEGY_OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY, 50);
        config.setProperty(BasicWatermarkService.BASIC_STRATEGY_OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY, 50);
        instance = new BasicWatermarkService();
        assertTrue(instance.shouldApplyToImage(imageSize));

        // image width < width threshold, image height < height threshold
        config.setProperty(BasicWatermarkService.BASIC_STRATEGY_OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY, 200);
        config.setProperty(BasicWatermarkService.BASIC_STRATEGY_OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY, 200);
        instance = new BasicWatermarkService();
        assertFalse(instance.shouldApplyToImage(imageSize));

        // image width < width threshold, image height > height threshold
        config.setProperty(BasicWatermarkService.BASIC_STRATEGY_OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY, 200);
        config.setProperty(BasicWatermarkService.BASIC_STRATEGY_OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY, 50);
        instance = new BasicWatermarkService();
        assertFalse(instance.shouldApplyToImage(imageSize));

        // image width > width threshold, image height < height threshold
        config.setProperty(BasicWatermarkService.BASIC_STRATEGY_OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY, 50);
        config.setProperty(BasicWatermarkService.BASIC_STRATEGY_OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY, 200);
        instance = new BasicWatermarkService();
        assertFalse(instance.shouldApplyToImage(imageSize));
    }

}
