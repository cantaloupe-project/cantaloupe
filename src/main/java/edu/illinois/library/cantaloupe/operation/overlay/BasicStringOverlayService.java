package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.operation.Color;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

class BasicStringOverlayService extends BasicOverlayService {

    static final String BACKGROUND_COLOR_CONFIG_KEY =
            "overlays.BasicStrategy.string.background.color";
    static final String COLOR_CONFIG_KEY =
            "overlays.BasicStrategy.string.color";
    static final String FONT_CONFIG_KEY =
            "overlays.BasicStrategy.string.font";
    static final String FONT_MIN_SIZE_CONFIG_KEY =
            "overlays.BasicStrategy.string.font.min_size";
    static final String FONT_SIZE_CONFIG_KEY =
            "overlays.BasicStrategy.string.font.size";
    static final String FONT_WEIGHT_CONFIG_KEY =
            "overlays.BasicStrategy.string.font.weight";
    static final String GLYPH_SPACING_CONFIG_KEY =
            "overlays.BasicStrategy.string.glyph_spacing";
    static final String STRING_CONFIG_KEY =
            "overlays.BasicStrategy.string";
    static final String STROKE_COLOR_CONFIG_KEY =
            "overlays.BasicStrategy.string.stroke.color";
    static final String STROKE_WIDTH_CONFIG_KEY =
            "overlays.BasicStrategy.string.stroke.width";

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
                minSize, color, backgroundColor, strokeColor, strokeWidth);
    }

    private void readConfig() throws ConfigurationException {
        final Configuration config = ConfigurationFactory.getInstance();

        // Background color
        backgroundColor = Color.fromString(
                config.getString(BACKGROUND_COLOR_CONFIG_KEY));

        // Fill color
        color = Color.fromString(config.getString(COLOR_CONFIG_KEY));

        // Font
        final Map<TextAttribute, Object> attributes = new HashMap<>();
        attributes.put(TextAttribute.FAMILY,
                config.getString(FONT_CONFIG_KEY, "Helvetica"));
        attributes.put(TextAttribute.SIZE,
                config.getInt(FONT_SIZE_CONFIG_KEY, 18));
        attributes.put(TextAttribute.WEIGHT,
                config.getFloat(FONT_WEIGHT_CONFIG_KEY, 1f));
        attributes.put(TextAttribute.TRACKING,
                config.getFloat(GLYPH_SPACING_CONFIG_KEY, 0f));
        font = Font.getFont(attributes);

        // Min size
        minSize = config.getInt(FONT_MIN_SIZE_CONFIG_KEY, 14);

        // String
        string = config.getString(STRING_CONFIG_KEY, "");

        // Stroke color
        strokeColor = Color.fromString(
                config.getString(STROKE_COLOR_CONFIG_KEY, "black"));

        // Stroke width
        strokeWidth = config.getFloat(STROKE_WIDTH_CONFIG_KEY, 2f);
    }

}
