package edu.illinois.library.cantaloupe.image.watermark;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;

import java.awt.Color;
import java.awt.Font;

class BasicStringWatermarkService extends BasicWatermarkService {

    static final String COLOR_CONFIG_KEY =
            "watermark.BasicStrategy.string.color";
    static final String FONT_CONFIG_KEY =
            "watermark.BasicStrategy.string.font";
    static final String FONT_SIZE_CONFIG_KEY =
            "watermark.BasicStrategy.string.font_size";
    static final String STRING_CONFIG_KEY =
            "watermark.BasicStrategy.string";

    private Color color;
    private Font font;
    private String string;

    BasicStringWatermarkService() throws ConfigurationException {
        super();
        readColor();
        readFont();
        readString();
    }

    StringWatermark getWatermark() {
        return new StringWatermark(string, getPosition(), getInset(), font,
                color);
    }

    private void readColor() throws ConfigurationException {
        final Configuration config = ConfigurationFactory.getInstance();
        color = ColorUtil.fromString(config.getString(COLOR_CONFIG_KEY));
    }

    private void readFont() throws ConfigurationException {
        final Configuration config = ConfigurationFactory.getInstance();
        font = new Font(config.getString(FONT_CONFIG_KEY), Font.PLAIN,
                config.getInt(FONT_SIZE_CONFIG_KEY, 18));
    }

    private void readString() throws ConfigurationException {
        final Configuration config = ConfigurationFactory.getInstance();
        string = config.getString(STRING_CONFIG_KEY, "");
    }

}
