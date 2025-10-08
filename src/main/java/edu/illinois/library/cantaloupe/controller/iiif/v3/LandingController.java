package edu.illinois.library.cantaloupe.controller.iiif.v3;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.controller.Controller;

import edu.illinois.library.cantaloupe.resource.VelocityRepresentation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Handles the IIIF Image API 3.x landing page.</p>
 *
 * <p>This is a convenience feature that is out of the Image API's scope.</p>
 */
public class LandingController extends Controller {
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        response.setHeader("Content-Type", "text/html;charset=UTF-8");

        final Map<String,Object> vars = new HashMap<>();
        vars.put("version", Application.getVersion());
        vars.put("baseUri", request.getContextPath());
        new VelocityRepresentation("/iiif_3_landing.vm", vars)
                .write(response.getOutputStream());
    }

}
