package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.operation.Format;
import edu.illinois.library.cantaloupe.operation.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
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

public class OverlayServiceTest {

    private OverlayService instance;

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
        config.setProperty(OverlayService.ENABLED_CONFIG_KEY, true);
        config.setProperty(OverlayService.STRATEGY_CONFIG_KEY, "BasicStrategy");
        config.setProperty(BasicOverlayService.TYPE_CONFIG_KEY, "image");
        config.setProperty(BasicOverlayService.INSET_CONFIG_KEY, 10);
        config.setProperty(BasicOverlayService.POSITION_CONFIG_KEY, "top left");
        config.setProperty(BasicImageOverlayService.IMAGE_CONFIG_KEY, "/dev/null");

        instance = new OverlayService();
    }

    @Test
    public void testConstructor() {
        assertTrue(instance.isEnabled());
        assertEquals(OverlayService.Strategy.BASIC, instance.getStrategy());
    }

    @Test
    public void testNewOverlayWithBasicImageStrategy() throws Exception {
        final OperationList opList = new OperationList();
        final Dimension fullSize = new Dimension(0, 0);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        ImageOverlay overlay = (ImageOverlay) instance.newOverlay(
                opList, fullSize, requestUrl, requestHeaders, clientIp, cookies);
        assertEquals(new File("/dev/null"), overlay.getImage());
        assertEquals(10, overlay.getInset());
        assertEquals(Position.TOP_LEFT, overlay.getPosition());
    }

    @Test
    public void testNewOverlayWithBasicStringStrategy() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(BasicOverlayService.TYPE_CONFIG_KEY, "string");
        config.setProperty(BasicStringOverlayService.STRING_CONFIG_KEY, "cats");
        config.setProperty(BasicStringOverlayService.COLOR_CONFIG_KEY, "green");
        instance = new OverlayService();

        final OperationList opList = new OperationList();
        final Dimension fullSize = new Dimension(0, 0);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        StringOverlay overlay = (StringOverlay) instance.newOverlay(
                opList, fullSize, requestUrl, requestHeaders, clientIp, cookies);
        assertEquals("cats", overlay.getString());
        assertEquals(10, overlay.getInset());
        assertEquals(Position.TOP_LEFT, overlay.getPosition());
        assertEquals(Color.green, overlay.getColor());
    }

    @Test
    public void testNewOverlayWithScriptStrategyReturningImageOverlay()
            throws Exception {
        instance.setStrategy(OverlayService.Strategy.DELEGATE_METHOD);

        final OperationList opList = new OperationList();
        opList.setIdentifier(new Identifier("image"));
        opList.setOutputFormat(Format.JPG);
        final Dimension fullSize = new Dimension(100, 100);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        ImageOverlay overlay = (ImageOverlay) instance.newOverlay(
                opList, fullSize, requestUrl, requestHeaders, clientIp, cookies);
        assertEquals(new File("/dev/cats"), overlay.getImage());
        assertEquals(5, overlay.getInset());
        assertEquals(Position.BOTTOM_LEFT, overlay.getPosition());
    }

    @Test
    public void testNewOverlayWithScriptStrategyReturningStringOverlay()
            throws Exception {
        instance.setStrategy(OverlayService.Strategy.DELEGATE_METHOD);

        final OperationList opList = new OperationList();
        opList.setIdentifier(new Identifier("string"));
        opList.setOutputFormat(Format.JPG);
        final Dimension fullSize = new Dimension(100, 100);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        StringOverlay overlay = (StringOverlay) instance.newOverlay(
                opList, fullSize, requestUrl, requestHeaders, clientIp, cookies);
        assertEquals("dogs\ndogs", overlay.getString());
        assertEquals(5, overlay.getInset());
        assertEquals(Position.BOTTOM_LEFT, overlay.getPosition());
    }

    @Test
    public void testNewOverlayWithScriptStrategyReturningFalse() throws Exception {
        instance.setStrategy(OverlayService.Strategy.DELEGATE_METHOD);

        final OperationList opList = new OperationList();
        opList.setIdentifier(new Identifier("bogus"));
        opList.setOutputFormat(Format.JPG);
        final Dimension fullSize = new Dimension(100, 100);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        Overlay overlay = instance.newOverlay(opList, fullSize,
                requestUrl, requestHeaders, clientIp, cookies);
        assertNull(overlay);
    }

}
