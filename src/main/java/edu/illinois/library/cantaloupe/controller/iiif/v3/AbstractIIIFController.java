package edu.illinois.library.cantaloupe.controller.iiif.v3;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.resource.IIIFRequest;
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

    protected void addHeaders(HttpServletResponse response, IIIFRequest iiifrequest) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Vary", "Accept, Accept-Charset, Accept-Encoding, Accept-Language, Origin");
        if (!iiifrequest.isBypassingCache()) {
            if (configuration.getBoolean(Key.CLIENT_CACHE_ENABLED, false)) {
                final List<String> directives = new ArrayList<>();
                final String maxAge = configuration.getString(Key.CLIENT_CACHE_MAX_AGE, "");
                if (!maxAge.isEmpty()) {
                    directives.add("max-age=" + maxAge);
                }
                String sMaxAge = configuration.getString(Key.CLIENT_CACHE_SHARED_MAX_AGE, "");
                if (!sMaxAge.isEmpty()) {
                    directives.add("s-maxage=" + sMaxAge);
                }
                if (configuration.getBoolean(Key.CLIENT_CACHE_PUBLIC, true)) {
                    directives.add("public");
                } else if (configuration.getBoolean(Key.CLIENT_CACHE_PRIVATE, false)) {
                    directives.add("private");
                }
                if (configuration.getBoolean(Key.CLIENT_CACHE_NO_CACHE, false)) {
                    directives.add("no-cache");
                }
                if (configuration.getBoolean(Key.CLIENT_CACHE_NO_STORE, false)) {
                    directives.add("no-store");
                }
                if (configuration.getBoolean(Key.CLIENT_CACHE_MUST_REVALIDATE, false)) {
                    directives.add("must-revalidate");
                }
                if (configuration.getBoolean(Key.CLIENT_CACHE_PROXY_REVALIDATE, false)) {
                    directives.add("proxy-revalidate");
                }
                if (configuration.getBoolean(Key.CLIENT_CACHE_NO_TRANSFORM, false)) {
                    directives.add("no-transform");
                }
                response.setHeader("Cache-Control",
                        String.join(", ", directives));
            }
        }
    }
}
