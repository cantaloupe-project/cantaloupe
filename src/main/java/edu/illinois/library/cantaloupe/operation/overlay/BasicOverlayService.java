package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;

import java.awt.Dimension;

abstract class BasicOverlayService {

    static final String INSET_CONFIG_KEY =
            "overlays.BasicStrategy.inset";
    static final String OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY =
            "overlays.BasicStrategy.output_height_threshold";
    static final String OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY =
            "overlays.BasicStrategy.output_width_threshold";
    static final String POSITION_CONFIG_KEY =
            "overlays.BasicStrategy.position";
    static final String TYPE_CONFIG_KEY = "overlays.BasicStrategy.type";

    private int inset;
    private Position position;

    /**
     * @param outputImageSize
     * @return Whether an overlay should be applied to an output image with
     * the given dimensions.
     */
    static boolean shouldApplyToImage(Dimension outputImageSize) {
        final Configuration config = ConfigurationFactory.getInstance();
        final int minOutputWidth =
                config.getInt(OUTPUT_WIDTH_THRESHOLD_CONFIG_KEY, 0);
        final int minOutputHeight =
                config.getInt(OUTPUT_HEIGHT_THRESHOLD_CONFIG_KEY, 0);
        return (outputImageSize.width >= minOutputWidth &&
                outputImageSize.height >= minOutputHeight);
    }

    BasicOverlayService() throws ConfigurationException {
        readPosition();
        readInset();
    }

    /**
     * @return Overlay inset.
     */
    protected int getInset() {
        return inset;
    }

    /**
     * @return Overlay position.
     */
    protected Position getPosition() {
        return position;
    }

    abstract Overlay getOverlay();

    private void readInset() {
        final Configuration config = ConfigurationFactory.getInstance();
        inset = config.getInt(INSET_CONFIG_KEY, 0);
    }

    private void readPosition() throws ConfigurationException {
        final Configuration config = ConfigurationFactory.getInstance();
        final String configValue = config.
                getString(POSITION_CONFIG_KEY, "");
        if (configValue.length() > 0) {
            try {
                position = Position.fromString(configValue);
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException(
                        "Invalid " + POSITION_CONFIG_KEY +
                                " value: " + configValue);
            }
        } else {
            throw new ConfigurationException(
                    POSITION_CONFIG_KEY + " is not set.");
        }
    }

}
