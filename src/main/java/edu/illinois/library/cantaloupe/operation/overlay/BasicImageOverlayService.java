package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Used to acquire overlay images when using BasicStrategy for overlays.
 */
class BasicImageOverlayService extends BasicOverlayService {

    static final String IMAGE_CONFIG_KEY = "overlays.BasicStrategy.image";

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
        location = config.getString(IMAGE_CONFIG_KEY, "");
        if (location.length() < 1) {
            throw new ConfigurationException(IMAGE_CONFIG_KEY + " is not set.");
        }
    }

}
