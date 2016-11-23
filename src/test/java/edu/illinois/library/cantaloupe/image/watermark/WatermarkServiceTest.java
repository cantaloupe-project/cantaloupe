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

import java.awt.Color;
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
        config.setProperty(BasicWatermarkService.TYPE_CONFIG_KEY, "image");
        config.setProperty(BasicWatermarkService.INSET_CONFIG_KEY, 10);
        config.setProperty(BasicWatermarkService.POSITION_CONFIG_KEY, "top left");
        config.setProperty(BasicImageWatermarkService.FILE_CONFIG_KEY, "/dev/null");

        instance = new WatermarkService();
    }

    @Test
    public void testConstructor() {
        assertTrue(instance.isEnabled());
        assertEquals(WatermarkService.Strategy.BASIC, instance.getStrategy());
    }

    @Test
    public void testNewWatermarkWithBasicImageStrategy() throws Exception {
        final OperationList opList = new OperationList();
        final Dimension fullSize = new Dimension(0, 0);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        ImageWatermark watermark = (ImageWatermark) instance.newWatermark(
                opList, fullSize, requestUrl, requestHeaders, clientIp, cookies);
        assertEquals(new File("/dev/null"), watermark.getImage());
        assertEquals(10, watermark.getInset());
        assertEquals(Position.TOP_LEFT, watermark.getPosition());
    }

    @Test
    public void testNewWatermarkWithBasicStringStrategy() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(BasicWatermarkService.TYPE_CONFIG_KEY, "string");
        config.setProperty(BasicStringWatermarkService.STRING_CONFIG_KEY, "cats");
        config.setProperty(BasicStringWatermarkService.COLOR_CONFIG_KEY, "green");
        instance = new WatermarkService();

        final OperationList opList = new OperationList();
        final Dimension fullSize = new Dimension(0, 0);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        StringWatermark watermark = (StringWatermark) instance.newWatermark(
                opList, fullSize, requestUrl, requestHeaders, clientIp, cookies);
        assertEquals("cats", watermark.getString());
        assertEquals(10, watermark.getInset());
        assertEquals(Position.TOP_LEFT, watermark.getPosition());
        assertEquals(Color.green, watermark.getColor());
    }

    @Test
    public void testNewWatermarkWithScriptStrategyReturningImageWatermark()
            throws Exception {
        instance.setStrategy(WatermarkService.Strategy.DELEGATE_METHOD);

        final OperationList opList = new OperationList();
        opList.setIdentifier(new Identifier("image"));
        opList.setOutputFormat(Format.JPG);
        final Dimension fullSize = new Dimension(100, 100);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        ImageWatermark watermark = (ImageWatermark) instance.newWatermark(
                opList, fullSize, requestUrl, requestHeaders, clientIp, cookies);
        assertEquals(new File("/dev/cats"), watermark.getImage());
        assertEquals(5, watermark.getInset());
        assertEquals(Position.BOTTOM_LEFT, watermark.getPosition());
    }

    @Test
    public void testNewWatermarkWithScriptStrategyReturningStringWatermark()
            throws Exception {
        instance.setStrategy(WatermarkService.Strategy.DELEGATE_METHOD);

        final OperationList opList = new OperationList();
        opList.setIdentifier(new Identifier("string"));
        opList.setOutputFormat(Format.JPG);
        final Dimension fullSize = new Dimension(100, 100);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        StringWatermark watermark = (StringWatermark) instance.newWatermark(
                opList, fullSize, requestUrl, requestHeaders, clientIp, cookies);
        assertEquals("dogs\ndogs", watermark.getString());
        assertEquals(5, watermark.getInset());
        assertEquals(Position.BOTTOM_LEFT, watermark.getPosition());
    }

    @Test
    public void testNewWatermarkWithScriptStrategyReturningFalse() throws Exception {
        instance.setStrategy(WatermarkService.Strategy.DELEGATE_METHOD);

        final OperationList opList = new OperationList();
        opList.setIdentifier(new Identifier("bogus"));
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

}
