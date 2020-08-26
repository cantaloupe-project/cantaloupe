package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.operation.Color;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

class BasicStringOverlayService extends BasicOverlayService {

    private Color backgroundColor;
    private Color color;
    private Font font;
    private int minSize;
    private String string;
    private Color strokeColor;
    private float strokeWidth;

    BasicStringOverlayService() throws ConfigurationException {
        super();
        readConfig();
    }

    StringOverlay getOverlay() {
        return new StringOverlay(string, getPosition(), getInset(), font,
                minSize, color, backgroundColor, strokeColor, strokeWidth,
                false);
    }

    private void readConfig() {
        final Configuration config = Configuration.getInstance();

        // Background color
        backgroundColor = Color.fromString(
                config.getString(Key.OVERLAY_STRING_BACKGROUND_COLOR));

        // Fill color
        color = Color.fromString(config.getString(Key.OVERLAY_STRING_COLOR));

        // Font
        final Map<TextAttribute, Object> attributes = new HashMap<>();
        attributes.put(TextAttribute.FAMILY,
                config.getString(Key.OVERLAY_STRING_FONT, "SansSerif"));
        attributes.put(TextAttribute.SIZE,
                config.getInt(Key.OVERLAY_STRING_FONT_SIZE, 18));
        attributes.put(TextAttribute.WEIGHT,
                config.getFloat(Key.OVERLAY_STRING_FONT_WEIGHT, 1f));
        attributes.put(TextAttribute.TRACKING,
                config.getFloat(Key.OVERLAY_STRING_GLYPH_SPACING, 0f));
        font = Font.getFont(attributes);

        // Min size
        minSize = config.getInt(Key.OVERLAY_STRING_FONT_MIN_SIZE, 14);

        // String
        string = config.getString(Key.OVERLAY_STRING_STRING, "");

        // Stroke color
        strokeColor = Color.fromString(
                config.getString(Key.OVERLAY_STRING_STROKE_COLOR, "black"));

        // Stroke width
        strokeWidth = config.getFloat(Key.OVERLAY_STRING_STROKE_WIDTH, 2f);
    }

}
