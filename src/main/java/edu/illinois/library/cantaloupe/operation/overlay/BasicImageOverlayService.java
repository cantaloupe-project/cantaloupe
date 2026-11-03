package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.Key;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

/**
 * Used to acquire overlay images when using BasicStrategy for overlays.
 */
class BasicImageOverlayService extends BasicOverlayService
        implements OverlayService {

    private URI overlayURI;

    BasicImageOverlayService(Configuration configuration) {
        super(configuration);
    }

    /**
     * @return Overlay image corresponding to the application configuration.
     */
    @Override
    public ImageOverlay newOverlay() throws ConfigurationException {
        readConfig();
        return new ImageOverlay(overlayURI, getPosition(), getInset());
    }

    protected void readConfig() throws ConfigurationException {
        super.readConfig();
        readLocation();
    }

    private void readLocation() throws ConfigurationException {
        final String location = configuration.getString(Key.OVERLAY_IMAGE, "");
        if (location.length() < 1) {
            throw new ConfigurationException(Key.OVERLAY_IMAGE + " is not set.");
        }

        try {
            // If the location in the configuration starts with a supported URI
            // scheme, create a new URI for it. Otherwise, get its absolute path
            // and convert that to a file: URI.
            if (ImageOverlay.SUPPORTED_URI_SCHEMES.stream().anyMatch(location::startsWith)) {
                overlayURI = new URI(location);
            } else {
                overlayURI = Paths.get(location).toUri();
            }
        } catch (URISyntaxException e) {
            throw new ConfigurationException(e.getMessage());
        }
    }

}
