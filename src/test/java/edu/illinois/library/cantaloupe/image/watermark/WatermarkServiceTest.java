package edu.illinois.library.cantaloupe.image.watermark;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.ConfigurationException;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class WatermarkServiceTest {

    @Before
    public void setUp() throws Exception {
        Configuration config = new BaseConfiguration();
        Application.setConfiguration(config);
        // valid config options
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_ENABLED_CONFIG_KEY,
                "true");
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        config.setProperty(WatermarkService.WATERMARK_ENABLED_CONFIG_KEY, true);
        config.setProperty(WatermarkService.WATERMARK_STRATEGY_CONFIG_KEY, "BasicStrategy");
        config.setProperty(WatermarkService.WATERMARK_FILE_CONFIG_KEY, "/dev/null");
        config.setProperty(WatermarkService.WATERMARK_INSET_CONFIG_KEY, 10);
        config.setProperty(WatermarkService.WATERMARK_POSITION_CONFIG_KEY, "top left");
    }

    @Test
    public void testNewWatermarkWithBasicStrategy() throws Exception {
        final OperationList opList = new OperationList();
        final Dimension fullSize = new Dimension(0, 0);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        Watermark watermark = WatermarkService.newWatermark(opList, fullSize,
                requestUrl, requestHeaders, clientIp, cookies);
        assertEquals(new File("/dev/null"), watermark.getImage());
        assertEquals(10, watermark.getInset());
        assertEquals(Position.TOP_LEFT, watermark.getPosition());
    }

    @Test
    public void testNewWatermarkWithBasicStrategyAndInvalidConfig() throws Exception {
        final OperationList opList = new OperationList();
        final Dimension fullSize = new Dimension(0, 0);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        Configuration config = Application.getConfiguration();
        config.setProperty(WatermarkService.WATERMARK_FILE_CONFIG_KEY, null);
        try {
            WatermarkService.newWatermark(opList, fullSize, requestUrl,
                    requestHeaders, clientIp, cookies);
            fail();
        } catch (ConfigurationException e) {
            // pass
        }
    }

    @Test
    public void testNewWatermarkWithScriptStrategyReturningWatermark() throws Exception {
        Application.getConfiguration().setProperty(
                WatermarkService.WATERMARK_STRATEGY_CONFIG_KEY, "ScriptStrategy");

        final OperationList opList = new OperationList();
        opList.setIdentifier(new Identifier("cats"));
        opList.setOutputFormat(Format.JPG);
        final Dimension fullSize = new Dimension(100, 100);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        Watermark watermark = WatermarkService.newWatermark(opList, fullSize,
                requestUrl, requestHeaders, clientIp, cookies);
        assertEquals(new File("/dev/cats"), watermark.getImage());
        assertEquals(5, watermark.getInset());
        assertEquals(Position.BOTTOM_LEFT, watermark.getPosition());
    }

    @Test
    public void testNewWatermarkWithScriptStrategyReturningFalse() throws Exception {
        Application.getConfiguration().setProperty(
                WatermarkService.WATERMARK_STRATEGY_CONFIG_KEY, "ScriptStrategy");

        final OperationList opList = new OperationList();
        opList.setIdentifier(new Identifier("dogs"));
        opList.setOutputFormat(Format.JPG);
        final Dimension fullSize = new Dimension(100, 100);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        Watermark watermark = WatermarkService.newWatermark(opList, fullSize,
                requestUrl, requestHeaders, clientIp, cookies);
        assertNull(watermark);
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
