package edu.illinois.library.cantaloupe.operation.overlay;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.Map;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.operation.Color;

class BasicStringOverlayService extends BasicOverlayService
        implements OverlayService {

    private Color backgroundColor;
    private Color color;
    private Font font;
    private int minSize;
    private String string;
    private Color strokeColor;
    private float strokeWidth;

    BasicStringOverlayService(Configuration configuration) {
        super(configuration);
    }

    @Override
    public StringOverlay newOverlay() throws ConfigurationException {
        readConfig();

        return new StringOverlay(string, getPosition(), getInset(), font,
                minSize, color, backgroundColor, strokeColor, strokeWidth,
                false);
    }

    @Override
    protected void readConfig() throws ConfigurationException {
        super.readConfig();
        // Background color
        backgroundColor = Color.fromString(
                configuration.getString(Key.OVERLAY_STRING_BACKGROUND_COLOR));

        // Fill color
        color = Color.fromString(configuration.getString(Key.OVERLAY_STRING_COLOR));

        // Font
        final Map<TextAttribute, Object> attributes = Map.of(
                TextAttribute.FAMILY,
                configuration.getString(Key.OVERLAY_STRING_FONT, "SansSerif"),
                TextAttribute.SIZE,
                configuration.getInt(Key.OVERLAY_STRING_FONT_SIZE, 18),
                TextAttribute.WEIGHT,
                configuration.getFloat(Key.OVERLAY_STRING_FONT_WEIGHT, 1f),
                TextAttribute.TRACKING,
                configuration.getFloat(Key.OVERLAY_STRING_GLYPH_SPACING, 0f));
        font = Font.getFont(attributes);

        // Min size
        minSize = configuration.getInt(Key.OVERLAY_STRING_FONT_MIN_SIZE, 14);

        // String
        string = configuration.getString(Key.OVERLAY_STRING_STRING, "");

        // Stroke color
        strokeColor = Color.fromString(
                configuration.getString(Key.OVERLAY_STRING_STROKE_COLOR, "black"));

        // Stroke width
        strokeWidth = configuration.getFloat(Key.OVERLAY_STRING_STROKE_WIDTH, 2f);
    }

}
