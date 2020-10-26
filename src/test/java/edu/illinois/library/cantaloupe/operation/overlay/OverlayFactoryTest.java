package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.Color;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class OverlayFactoryTest extends BaseTest {

    private OverlayFactory instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").toString());
        config.setProperty(Key.OVERLAY_ENABLED, true);
        config.setProperty(Key.OVERLAY_STRATEGY, "BasicStrategy");
        config.setProperty(Key.OVERLAY_TYPE, "image");
        config.setProperty(Key.OVERLAY_INSET, 10);
        config.setProperty(Key.OVERLAY_POSITION, "top left");
        config.setProperty(Key.OVERLAY_IMAGE, "/dev/null");

        instance = new OverlayFactory();
    }

    @Test
    void testConstructor() {
        assertEquals(OverlayFactory.Strategy.BASIC, instance.getStrategy());
    }

    @Test
    void testNewOverlayWithBasicImageStrategy() throws Exception {
        Optional<Overlay> result = instance.newOverlay(null);
        ImageOverlay overlay = (ImageOverlay) result.get();
        if (SystemUtils.IS_OS_WINDOWS) {
            assertEquals(new URI("file:///C:/dev/null"), overlay.getURI());
        } else {
            assertEquals(new URI("file:///dev/null"), overlay.getURI());
        }
        assertEquals(10, overlay.getInset());
        assertEquals(Position.TOP_LEFT, overlay.getPosition());
    }

    @Test
    void testNewOverlayWithBasicStringStrategy() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.OVERLAY_TYPE, "string");
        config.setProperty(Key.OVERLAY_STRING_STRING, "cats");
        config.setProperty(Key.OVERLAY_STRING_COLOR, "green");
        instance = new OverlayFactory();

        Optional<Overlay> result = instance.newOverlay(null);
        StringOverlay overlay = (StringOverlay) result.get();
        assertEquals("cats", overlay.getString());
        assertEquals(10, overlay.getInset());
        assertEquals(Position.TOP_LEFT, overlay.getPosition());
        assertEquals(new Color(0, 128, 0), overlay.getColor());
    }

    @Test
    void testNewOverlayWithScriptStrategyReturningImageOverlay()
            throws Exception {
        instance.setStrategy(OverlayFactory.Strategy.DELEGATE_METHOD);

        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(new Identifier("image"));

        Optional<Overlay> result = instance.newOverlay(proxy);
        ImageOverlay overlay = (ImageOverlay) result.get();
        if (SystemUtils.IS_OS_WINDOWS) {
            assertEquals(new URI("file:///C:/dev/cats"), overlay.getURI());
        } else {
            assertEquals(new URI("file:///dev/cats"), overlay.getURI());
        }
        assertEquals(5, overlay.getInset());
        assertEquals(Position.BOTTOM_LEFT, overlay.getPosition());
    }

    @Test
    void testNewOverlayWithScriptStrategyReturningStringOverlay()
            throws Exception {
        instance.setStrategy(OverlayFactory.Strategy.DELEGATE_METHOD);

        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(new Identifier("string"));

        Optional<Overlay> result = instance.newOverlay(proxy);
        StringOverlay overlay = (StringOverlay) result.get();
        assertEquals("dogs\ndogs", overlay.getString());
        assertEquals(5, overlay.getInset());
        assertEquals(Position.BOTTOM_LEFT, overlay.getPosition());
    }

    @Test
    void testNewOverlayWithScriptStrategyReturningNil() throws Exception {
        instance.setStrategy(OverlayFactory.Strategy.DELEGATE_METHOD);

        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(new Identifier("bogus"));

        Optional<Overlay> result = instance.newOverlay(proxy);
        assertFalse(result.isPresent());
    }

}
