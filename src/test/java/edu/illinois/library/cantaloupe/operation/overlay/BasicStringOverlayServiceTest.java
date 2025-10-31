package edu.illinois.library.cantaloupe.operation.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.font.TextAttribute;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.operation.Color;
import edu.illinois.library.cantaloupe.test.BaseTest;

public class BasicStringOverlayServiceTest extends BaseTest {

    private BasicStringOverlayService instance;

    public static Configuration setUpConfiguration() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.OVERLAY_ENABLED, true);
        config.setProperty(Key.OVERLAY_STRATEGY, "BasicStrategy");
        config.setProperty(Key.OVERLAY_TYPE, "string");
        config.setProperty(Key.OVERLAY_INSET, 10);
        config.setProperty(Key.OVERLAY_POSITION, "top left");
        config.setProperty(Key.OVERLAY_STRING_BACKGROUND_COLOR, "rgba(12, 23, 34, 45)");
        config.setProperty(Key.OVERLAY_STRING_COLOR, "red");
        config.setProperty(Key.OVERLAY_STRING_FONT, "SansSerif");
        config.setProperty(Key.OVERLAY_STRING_FONT_MIN_SIZE, 11);
        config.setProperty(Key.OVERLAY_STRING_FONT_SIZE, 14);
        config.setProperty(Key.OVERLAY_STRING_FONT_WEIGHT, 2f);
        config.setProperty(Key.OVERLAY_STRING_GLYPH_SPACING, 0.2f);
        config.setProperty(Key.OVERLAY_STRING_STRING, "cats");
        config.setProperty(Key.OVERLAY_STRING_STROKE_COLOR, "orange");
        config.setProperty(Key.OVERLAY_STRING_STROKE_WIDTH, 3);
        return config;
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        instance = new BasicStringOverlayService(setUpConfiguration());
    }

    @Test
    void testGetOverlay() throws ConfigurationException {
        final StringOverlay overlay = instance.newOverlay();
        assertEquals("cats", overlay.getString());
        assertEquals(new Color(12, 23, 34, 45), overlay.getBackgroundColor());
        assertEquals(Color.RED, overlay.getColor());
        assertEquals("SansSerif", overlay.getFont().getName());
        assertEquals((long) 10, overlay.getInset());
        assertEquals(Position.TOP_LEFT, overlay.getPosition());
        assertEquals(14, overlay.getFont().getSize());
        assertEquals(11, overlay.getMinSize());
        assertEquals(2f, overlay.getFont().getAttributes().get(TextAttribute.WEIGHT));
        assertEquals(0.2f, overlay.getFont().getAttributes().get(TextAttribute.TRACKING));
        assertEquals(Color.ORANGE, overlay.getStrokeColor());
        assertEquals(3, overlay.getStrokeWidth(), 0.00001f);
    }
}
