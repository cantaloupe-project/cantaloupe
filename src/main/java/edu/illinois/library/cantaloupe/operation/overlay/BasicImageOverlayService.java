package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;

import java.io.File;

class BasicImageOverlayService extends BasicOverlayService {

    static final String IMAGE_CONFIG_KEY = "overlays.BasicStrategy.image";

    private File image;

    BasicImageOverlayService() throws ConfigurationException {
        super();
        readImage();
    }

    /**
     * @return File
     */
    private File getImage() {
        return image;
    }

    ImageOverlay getOverlay() {
        return new ImageOverlay(getImage(), getPosition(), getInset());
    }

    private void readImage() throws ConfigurationException {
        final Configuration config = ConfigurationFactory.getInstance();
        final String path = config.getString(IMAGE_CONFIG_KEY, "");
        if (path.length() > 0) {
            image = new File(path);
        } else {
            throw new ConfigurationException(IMAGE_CONFIG_KEY + " is not set.");
        }
    }

}
