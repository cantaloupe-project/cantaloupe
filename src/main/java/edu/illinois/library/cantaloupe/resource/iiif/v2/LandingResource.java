package edu.illinois.library.cantaloupe.resource.iiif.v2;

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
 * Handles the IIIF Image API 2.x landing page.
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

        renderHtml("/iiif_2_landing.vm", templateVars);
    }

    @Override
    public boolean enabled() {
      return Configuration.getInstance().getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, true);
    }

}
