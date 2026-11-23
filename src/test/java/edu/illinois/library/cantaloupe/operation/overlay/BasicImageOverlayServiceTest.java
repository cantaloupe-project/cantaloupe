package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BasicImageOverlayServiceTest extends BaseTest {

    private BasicImageOverlayService instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.OVERLAY_ENABLED, true);
        config.setProperty(Key.OVERLAY_STRATEGY, "BasicStrategy");
        config.setProperty(Key.OVERLAY_TYPE, "image");
        config.setProperty(Key.OVERLAY_INSET, 10);
        config.setProperty(Key.OVERLAY_POSITION, "top left");
        config.setProperty(Key.OVERLAY_IMAGE, "/dev/null");

        instance = new BasicImageOverlayService(config);
    }

    @Test
    void testGetOverlay() throws Exception {
        final ImageOverlay overlay = instance.newOverlay();
        if (SystemUtils.IS_OS_WINDOWS) {
            assertEquals(new URI("file:///C:/dev/null"), overlay.getURI());
        } else {
            assertEquals(new URI("file:///dev/null"), overlay.getURI());
        }
        assertEquals((long) 10, overlay.getInset());
        assertEquals(Position.TOP_LEFT, overlay.getPosition());
    }
}
