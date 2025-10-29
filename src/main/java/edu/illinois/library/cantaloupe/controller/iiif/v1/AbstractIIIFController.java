package edu.illinois.library.cantaloupe.controller.iiif.v1;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.image.MetaIdentifier;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.resource.IIIFRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AbstractIIIFController {
    protected final Configuration configuration;

    @Autowired
    protected AbstractIIIFController(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Checks if the IIIF v1 endpoint is enabled.
     *
     * @throws EndpointDisabledException if the endpoint is disabled
     */
    protected void checkEndpointEnabled() throws EndpointDisabledException {
        if (!configuration.getBoolean(Key.IIIF_1_ENDPOINT_ENABLED, true)) {
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
    
    /**
     * <p>If an identifier is present in the URI, and it contains a scale
     * constraint suffix in a non-normalized form, this method redirects to
     * a normalized URI.</p>
     *
     * <p>Examples:</p>
     *
     * <dl>
     *     <dt>1:2</dt>
     *     <dd>No redirect</dd>
     *     <dt>2:4</dt>
     *     <dd>Redirect to 1:2</dd>
     *     <dt>1:1 and 5:5</dt>
     *     <dd>Redirect to no constraint</dd>
     * </dl>
     *
     * @return {@code true} if redirecting. Clients should stop processing if
     *         this is the case.
     */
    protected final boolean redirectToNormalizedScaleConstraint(IIIFRequest iiifrequest, HttpServletResponse response) {
        MetaIdentifier newMetaId = iiifrequest.getMetaIdentifier().getNormalizedScaleConstraintMetaIdentifier();
        if (newMetaId == null) {
            return false;
        }
        Reference newRef = iiifrequest.getPublicReference(newMetaId, iiifrequest.getIdentifierPathComponent(), iiifrequest.getDelegateProxy());
        response.setStatus(301);
        response.setHeader("Location", newRef.toString());
        return true;
    }
}
