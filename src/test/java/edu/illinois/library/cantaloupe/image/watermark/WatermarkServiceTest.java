package edu.illinois.library.cantaloupe.image.watermark;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class WatermarkServiceTest {

    private WatermarkService instance;

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

        instance = new WatermarkService();
    }

    @Test
    public void testNewWatermarkWithBasicStrategy() throws Exception {
        final OperationList opList = new OperationList();
        final Dimension fullSize = new Dimension(0, 0);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        Watermark watermark = instance.newWatermark(opList, fullSize,
                requestUrl, requestHeaders, clientIp, cookies);
        assertEquals(new File("/dev/null"), watermark.getImage());
        assertEquals(10, watermark.getInset());
        assertEquals(Position.TOP_LEFT, watermark.getPosition());
    }

    @Test
    public void testNewWatermarkWithScriptStrategyReturningWatermark() throws Exception {
        instance.setStrategy(WatermarkService.Strategy.DELEGATE_METHOD);

        final OperationList opList = new OperationList();
        opList.setIdentifier(new Identifier("cats"));
        opList.setOutputFormat(Format.JPG);
        final Dimension fullSize = new Dimension(100, 100);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        Watermark watermark = instance.newWatermark(opList, fullSize,
                requestUrl, requestHeaders, clientIp, cookies);
        assertEquals(new File("/dev/cats"), watermark.getImage());
        assertEquals(5, watermark.getInset());
        assertEquals(Position.BOTTOM_LEFT, watermark.getPosition());
    }

    @Test
    public void testNewWatermarkWithScriptStrategyReturningFalse() throws Exception {
        instance.setStrategy(WatermarkService.Strategy.DELEGATE_METHOD);

        final OperationList opList = new OperationList();
        opList.setIdentifier(new Identifier("dogs"));
        opList.setOutputFormat(Format.JPG);
        final Dimension fullSize = new Dimension(100, 100);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        Watermark watermark = instance.newWatermark(opList, fullSize,
                requestUrl, requestHeaders, clientIp, cookies);
        assertNull(watermark);
    }

    @Test
    public void testIsEnabled() {
        Configuration config = ConfigurationFactory.getInstance();
        config.clear();
        // false
        config.setProperty(WatermarkService.ENABLED_CONFIG_KEY, false);
        instance = new WatermarkService();
        assertFalse(instance.isEnabled());
        // true
        config.setProperty(WatermarkService.ENABLED_CONFIG_KEY, true);
        instance = new WatermarkService();
        assertTrue(instance.isEnabled());
    }

}
