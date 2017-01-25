package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.operation.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.font.TextAttribute;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class DelegateOverlayServiceTest extends BaseTest {

    private DelegateOverlayService instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_ENABLED_CONFIG_KEY, true);
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());

        instance = new DelegateOverlayService();
    }

    @Test
    public void testGetOverlayReturningImageOverlay() throws Exception {
        final OperationList opList = new OperationList();
        opList.setIdentifier(new Identifier("image"));
        opList.setOutputFormat(Format.JPG);
        final Dimension fullSize = new Dimension(100, 100);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        final ImageOverlay overlay =
                (ImageOverlay) instance.getOverlay(
                        opList, fullSize, requestUrl, requestHeaders, clientIp,
                        cookies);
        assertEquals(new File("/dev/cats"), overlay.getFile());
        assertEquals((long) 5, overlay.getInset());
        assertEquals(Position.BOTTOM_LEFT, overlay.getPosition());
    }

    @Test
    public void testGetOverlayReturningStringOverlay() throws Exception {
        final OperationList opList = new OperationList();
        opList.setIdentifier(new Identifier("string"));
        opList.setOutputFormat(Format.JPG);
        final Dimension fullSize = new Dimension(100, 100);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        final StringOverlay overlay = (StringOverlay) instance.getOverlay(
                opList, fullSize, requestUrl, requestHeaders, clientIp,
                cookies);
        assertEquals("dogs\ndogs", overlay.getString());
        assertEquals("Helvetica", overlay.getFont().getFamily());
        assertEquals(20, overlay.getFont().getSize());
        assertEquals(11, overlay.getMinSize());
        assertEquals(1.5f, overlay.getFont().getAttributes().get(TextAttribute.WEIGHT));
        assertEquals((long) 5, overlay.getInset());
        assertEquals(Position.BOTTOM_LEFT, overlay.getPosition());
        assertEquals(Color.red, overlay.getColor());
        assertEquals(Color.blue, overlay.getStrokeColor());
        assertEquals(3, overlay.getStrokeWidth(), 0.00001f);
    }

    @Test
    public void testGetOverlayReturningFalse() throws Exception {
        final OperationList opList = new OperationList();
        opList.setIdentifier(new Identifier("bogus"));
        opList.setOutputFormat(Format.JPG);
        final Dimension fullSize = new Dimension(100, 100);
        final URL requestUrl = new URL("http://example.org/");
        final Map<String,String> requestHeaders = new HashMap<>();
        final String clientIp = "";
        final Map<String,String> cookies = new HashMap<>();

        Overlay overlay = instance.getOverlay(
                opList, fullSize, requestUrl, requestHeaders, clientIp, cookies);
        assertNull(overlay);
    }

}
