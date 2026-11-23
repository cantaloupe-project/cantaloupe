package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.Key;

abstract class BasicOverlayService {

    private int inset;
    private Position position;
    protected Configuration configuration;

    BasicOverlayService(Configuration configuration) {
        this.configuration = configuration;
    }

    protected void readConfig() throws ConfigurationException {
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

    public boolean isAvailable() {
        return configuration.getBoolean(Key.OVERLAY_ENABLED, false);
    }

    protected void readInset() {
        inset = configuration.getInt(Key.OVERLAY_INSET, 0);
    }

    protected void readPosition() throws ConfigurationException {
        final String configValue = configuration.getString(Key.OVERLAY_POSITION, "");
        if (!configValue.isEmpty()) {
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
