package edu.illinois.library.cantaloupe.controller.iiif.v3;

import org.springframework.beans.factory.annotation.Autowired;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Abstract base controller for IIIF v3 endpoints.
 * Provides common functionality like endpoint checking and CORS headers
 * to eliminate code duplication across IIIF controllers.
 */
public abstract class AbstractIIIFController {

    protected final Configuration configuration;

    @Autowired
    protected AbstractIIIFController(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Checks if the IIIF v3 endpoint is enabled.
     *
     * @throws EndpointDisabledException if the endpoint is disabled
     */
    protected void checkEndpointEnabled() throws EndpointDisabledException {
        if (!configuration.getBoolean(Key.IIIF_3_ENDPOINT_ENABLED, true)) {
            throw new EndpointDisabledException();
        }
    }

    /**
     * Adds standard CORS headers to the response.
     *
     * @param response the HTTP response to add headers to
     */
    protected void addCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
    }

    /**
     * Adds CORS headers with custom allowed methods.
     *
     * @param response the HTTP response to add headers to
     * @param methods the allowed HTTP methods (e.g., "GET, POST, OPTIONS")
     */
    protected void addCorsHeaders(HttpServletResponse response, String methods) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        response.setHeader("Access-Control-Allow-Methods", methods);
    }
}
