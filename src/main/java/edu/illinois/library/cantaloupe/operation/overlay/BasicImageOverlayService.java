package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Used to acquire overlay images when using BasicStrategy for overlays.
 */
class BasicImageOverlayService extends BasicOverlayService {

    private String location;

    BasicImageOverlayService() throws ConfigurationException {
        super();
        readLocation();
    }

    /**
     * @return Overlay image corresponding to the application configuration.
     */
    ImageOverlay getOverlay() {
        try {
            URL url = new URL(location);
            return new ImageOverlay(url, getPosition(), getInset());
        } catch (MalformedURLException e) {
            return new ImageOverlay(new File(location), getPosition(), getInset());
        }
    }

    private void readLocation() throws ConfigurationException {
        final Configuration config = ConfigurationFactory.getInstance();
        location = config.getString(Key.OVERLAY_IMAGE, "");
        if (location.length() < 1) {
            throw new ConfigurationException(Key.OVERLAY_IMAGE + " is not set.");
        }
    }

}
