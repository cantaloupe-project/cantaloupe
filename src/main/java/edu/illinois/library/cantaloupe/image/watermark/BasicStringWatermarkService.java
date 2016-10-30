package edu.illinois.library.cantaloupe.image.watermark;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;

import java.awt.Color;

class BasicStringWatermarkService extends BasicWatermarkService {

    static final String COLOR_CONFIG_KEY =
            "watermark.BasicStrategy.string.color";
    static final String STRING_CONFIG_KEY =
            "watermark.BasicStrategy.string";

    private Color color;
    private String string;

    BasicStringWatermarkService() throws ConfigurationException {
        super();
        readColor();
        readString();
    }

    /**
     * @return Color
     */
    private Color getColor() {
        return color;
    }

    /**
     * @return String
     */
    private String getString() {
        return string;
    }

    StringWatermark getWatermark() {
        return new StringWatermark(getString(), getPosition(), getInset(),
                getColor());
    }

    private void readColor() throws ConfigurationException {
        final Configuration config = ConfigurationFactory.getInstance();
        color = ColorUtil.fromString(config.getString(COLOR_CONFIG_KEY));
    }

    private void readString() throws ConfigurationException {
        final Configuration config = ConfigurationFactory.getInstance();
        string = config.getString(STRING_CONFIG_KEY, "");
    }

}
