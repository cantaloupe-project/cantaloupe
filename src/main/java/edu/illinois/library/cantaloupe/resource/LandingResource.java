package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;

public class LandingResource extends Controller {
    public LandingResource(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    public void doGet(Request request) throws Exception {
        response.setHeader("Cache-Control", "public, max-age=" + Integer.MAX_VALUE);

        final Map<String,Object> templateVars = new HashMap<>();
        templateVars.put("baseUri", request.getContextPath());
        templateVars.put("version", Application.getVersion());

        renderHtml("/landing.vm", templateVars);
    }
}
