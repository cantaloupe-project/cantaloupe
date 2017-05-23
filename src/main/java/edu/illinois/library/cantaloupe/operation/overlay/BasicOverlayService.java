package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;

import java.awt.Dimension;

abstract class BasicOverlayService {

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
                config.getInt(Key.OVERLAY_OUTPUT_WIDTH_THRESHOLD, 0);
        final int minOutputHeight =
                config.getInt(Key.OVERLAY_OUTPUT_HEIGHT_THRESHOLD, 0);
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
        inset = config.getInt(Key.OVERLAY_INSET, 0);
    }

    private void readPosition() throws ConfigurationException {
        final Configuration config = ConfigurationFactory.getInstance();
        final String configValue = config.getString(Key.OVERLAY_POSITION, "");
        if (configValue.length() > 0) {
            try {
                position = Position.fromString(configValue);
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException("Invalid " +
                        Key.OVERLAY_POSITION + " value: " + configValue);
            }
        } else {
            throw new ConfigurationException(Key.OVERLAY_POSITION +
                    " is not set.");
        }
    }

}
