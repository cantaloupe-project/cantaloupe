package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.test.BaseTest;
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

public class OverlayServiceTest extends BaseTest {

    private OverlayService instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        config.setProperty(Key.OVERLAY_ENABLED, true);
        config.setProperty(Key.OVERLAY_STRATEGY, "BasicStrategy");
        config.setProperty(Key.OVERLAY_TYPE, "image");
        config.setProperty(Key.OVERLAY_INSET, 10);
        config.setProperty(Key.OVERLAY_POSITION, "top left");
        config.setProperty(Key.OVERLAY_IMAGE, "/dev/null");

        instance = new OverlayService();
    }

    @Test
    public void testConstructor() {
        assertTrue(instance.isEnabled());
        assertEquals(OverlayService.Strategy.BASIC, instance.getStrategy());
    }

    @Test
    public void testNewOverlayWithBasicImageStrategy() throws Exception {
        final OperationList opList = new OperationList(
                new Identifier("cats"), Format.JPG);
        final Dimension fullSize = new Dimension(0, 0);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        ImageOverlay overlay = (ImageOverlay) instance.newOverlay(
                opList, fullSize, requestUrl, requestHeaders, clientIp, cookies);
        assertEquals(new File("/dev/null"), overlay.getFile());
        assertEquals(10, overlay.getInset());
        assertEquals(Position.TOP_LEFT, overlay.getPosition());
    }

    @Test
    public void testNewOverlayWithBasicStringStrategy() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.OVERLAY_TYPE, "string");
        config.setProperty(Key.OVERLAY_STRING_STRING, "cats");
        config.setProperty(Key.OVERLAY_STRING_COLOR, "green");
        instance = new OverlayService();

        final OperationList opList = new OperationList(
                new Identifier("cats"), Format.JPG);
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
        assertEquals(new Color(0, 128, 0), overlay.getColor());
    }

    @Test
    public void testNewOverlayWithScriptStrategyReturningImageOverlay()
            throws Exception {
        instance.setStrategy(OverlayService.Strategy.DELEGATE_METHOD);

        final OperationList opList = new OperationList(new Identifier("image"),
                Format.JPG);
        final Dimension fullSize = new Dimension(100, 100);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        ImageOverlay overlay = (ImageOverlay) instance.newOverlay(
                opList, fullSize, requestUrl, requestHeaders, clientIp, cookies);
        assertEquals(new File("/dev/cats"), overlay.getFile());
        assertEquals(5, overlay.getInset());
        assertEquals(Position.BOTTOM_LEFT, overlay.getPosition());
    }

    @Test
    public void testNewOverlayWithScriptStrategyReturningStringOverlay()
            throws Exception {
        instance.setStrategy(OverlayService.Strategy.DELEGATE_METHOD);

        final OperationList opList = new OperationList(
                new Identifier("string"), Format.JPG);
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

        final OperationList opList = new OperationList(new Identifier("bogus"),
                Format.JPG);
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
