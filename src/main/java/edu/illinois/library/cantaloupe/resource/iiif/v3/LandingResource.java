package edu.illinois.library.cantaloupe.resource.iiif.v3;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.Controller;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.resource.Request;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Handles the IIIF Image API 3.x landing page.</p>
 *
 * <p>This is a convenience feature that is out of the Image API's scope.</p>
 */
public class LandingResource extends Controller {
    public LandingResource(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public void doGet(Request request) throws Exception {
        if (!enabled()) {
            throw new EndpointDisabledException();
        }

        final Map<String,Object> templateVars = new HashMap<>();
        templateVars.put("baseUri", request.getContextPath());

        renderHtml("/iiif_1_landing.vm", templateVars);
    }

    @Override
    public boolean enabled() {
      return Configuration.getInstance().getBoolean(Key.IIIF_3_ENDPOINT_ENABLED, true);
    }
}
