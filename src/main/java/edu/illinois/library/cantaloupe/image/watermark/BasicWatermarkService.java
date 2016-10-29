package edu.illinois.library.cantaloupe.image.watermark;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;

import java.awt.Dimension;
import java.io.File;

class BasicWatermarkService {

    private enum BasicStrategyType {
        /** Use an image watermark. */
        IMAGE,

        /** Use a string watermark. */
        STRING
    }

    public static final String BASIC_STRATEGY_TYPE_CONFIG_KEY =
            "watermark.BasicStrategy.type";
    public static final String BASIC_STRATEGY_FILE_CONFIG_KEY =
            "watermark.BasicStrategy.image";
    public static final String BASIC_STRATEGY_INSET_CONFIG_KEY =
            "watermark.BasicStrategy.inset";
    public static final String BASIC_STRATEGY_OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY =
            "watermark.BasicStrategy.output_height_threshold";
    public static final String BASIC_STRATEGY_OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY =
            "watermark.BasicStrategy.output_width_threshold";
    public static final String BASIC_STRATEGY_POSITION_CONFIG_KEY =
            "watermark.BasicStrategy.position";

    /**
     * Returns the value of {@link #BASIC_STRATEGY_FILE_CONFIG_KEY}.
     *
     * @return File
     * @throws ConfigurationException
     */
    File getImage() throws ConfigurationException {
        final Configuration config = ConfigurationFactory.getInstance();
        final String path = config.getString(BASIC_STRATEGY_FILE_CONFIG_KEY, "");
        if (path.length() > 0) {
            return new File(path);
        }
        throw new ConfigurationException(
                BASIC_STRATEGY_FILE_CONFIG_KEY + " is not set.");
    }

    /**
     * @return Watermark inset.
     */
    int getInset() {
        final Configuration config = ConfigurationFactory.getInstance();
        return config.getInt(BASIC_STRATEGY_INSET_CONFIG_KEY, 0);
    }

    /**
     * @return Watermark position.
     * @throws ConfigurationException
     */
    Position getPosition() throws ConfigurationException {
        final Configuration config = ConfigurationFactory.getInstance();
        final String configValue = config.
                getString(BASIC_STRATEGY_POSITION_CONFIG_KEY, "");
        if (configValue.length() > 0) {
            try {
                return Position.fromString(configValue);
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException(
                        "Invalid " + BASIC_STRATEGY_POSITION_CONFIG_KEY +
                                " value: " + configValue);
            }
        }
        throw new ConfigurationException(
                BASIC_STRATEGY_POSITION_CONFIG_KEY + " is not set.");
    }

    /**
     * @return Watermark type.
     * @throws ConfigurationException
     */
    BasicStrategyType getType() throws ConfigurationException {
        final Configuration config = ConfigurationFactory.getInstance();
        final String configValue = config.
                getString(BASIC_STRATEGY_TYPE_CONFIG_KEY, "");
        switch (configValue) {
            case "image":
                return BasicStrategyType.IMAGE;
            case "string":
                return BasicStrategyType.STRING;
            default:
                throw new ConfigurationException("Unsupported value for " +
                        BASIC_STRATEGY_TYPE_CONFIG_KEY);
        }
    }

    /**
     * @param outputImageSize
     * @return Whether a watermark should be applied to an output image with
     * the given dimensions.
     */
    boolean shouldApplyToImage(Dimension outputImageSize) {
        final Configuration config = ConfigurationFactory.getInstance();
        final int minOutputWidth =
                config.getInt(BASIC_STRATEGY_OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY, 0);
        final int minOutputHeight =
                config.getInt(BASIC_STRATEGY_OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY, 0);
        return (outputImageSize.width >= minOutputWidth &&
                outputImageSize.height >= minOutputHeight);
    }

}
