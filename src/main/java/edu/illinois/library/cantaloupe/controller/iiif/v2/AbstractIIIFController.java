package edu.illinois.library.cantaloupe.controller.iiif.v2;

import org.springframework.beans.factory.annotation.Autowired;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;

public class AbstractIIIFController {
    protected final Configuration configuration;

    @Autowired
    protected AbstractIIIFController(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Checks if the IIIF v2 endpoint is enabled.
     *
     * @throws EndpointDisabledException if the endpoint is disabled
     */
    protected void checkEndpointEnabled() throws EndpointDisabledException {
        if (!configuration.getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, true)) {
            throw new EndpointDisabledException();
        }
    }

}
