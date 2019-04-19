package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.Color;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import edu.illinois.library.cantaloupe.script.DelegateProxyService;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.font.TextAttribute;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

public class DelegateOverlayServiceTest extends BaseTest {

    private DelegateOverlayService instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").toString());

        instance = new DelegateOverlayService();
    }

    @Test
    void testGetOverlayReturningImageOverlay() throws Exception {
        final Identifier identifier = new Identifier("image");
        final Dimension fullSize = new Dimension(500, 500);
        final OperationList opList = new OperationList(
                identifier, new Encode(Format.JPG));
        final RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        context.setOperationList(opList, fullSize);

        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        final ImageOverlay overlay = (ImageOverlay) instance.getOverlay(proxy);
        if (SystemUtils.IS_OS_WINDOWS) {
            assertEquals(new URI("file:///C:/dev/cats"), overlay.getURI());
        } else {
            assertEquals(new URI("file:///dev/cats"), overlay.getURI());
        }
        assertEquals((long) 5, overlay.getInset());
        assertEquals(Position.BOTTOM_LEFT, overlay.getPosition());
    }

    @Test
    void testGetOverlayReturningStringOverlay() throws Exception {
        final Identifier identifier = new Identifier("string");
        final Dimension fullSize = new Dimension(500, 500);
        final OperationList opList = new OperationList(
                identifier, new Encode(Format.JPG));
        final RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        context.setOperationList(opList, fullSize);

        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        final StringOverlay overlay =
                (StringOverlay) instance.getOverlay(proxy);
        assertEquals("dogs\ndogs", overlay.getString());
        assertEquals("SansSerif", overlay.getFont().getName());
        assertEquals(20, overlay.getFont().getSize());
        assertEquals(11, overlay.getMinSize());
        assertEquals(1.5f, overlay.getFont().getAttributes().get(TextAttribute.WEIGHT));
        assertEquals(0.1f, overlay.getFont().getAttributes().get(TextAttribute.TRACKING));
        assertEquals((long) 5, overlay.getInset());
        assertEquals(Position.BOTTOM_LEFT, overlay.getPosition());
        assertEquals(Color.RED, overlay.getColor());
        assertEquals(Color.BLUE, overlay.getStrokeColor());
        assertEquals(new Color(12, 23, 34, 45), overlay.getBackgroundColor());
        assertEquals(3, overlay.getStrokeWidth(), 0.00001f);
    }

    @Test
    void testGetOverlayReturningNull() throws Exception {
        final RequestContext context = new RequestContext();
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);

        Overlay overlay = instance.getOverlay(proxy);
        assertNull(overlay);
    }

}
