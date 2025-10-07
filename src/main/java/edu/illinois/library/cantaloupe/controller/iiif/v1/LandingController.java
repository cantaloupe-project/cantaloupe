package edu.illinois.library.cantaloupe.controller.iiif.v1;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.resource.VelocityRepresentation;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;
import java.io.IOException;


/**
 * Handles the IIIF Image API 1.x landing page.
 */
@WebServlet("iiifV1Landing")
public class LandingController  extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        response.setHeader("Content-Type", "text/html;charset=UTF-8");
        final Map<String,Object> vars = new HashMap<>();
        vars.put("version", Application.getVersion());
        vars.put("baseUri", request.getContextPath());

        new VelocityRepresentation("/iiif_1_landing.vm", vars)
                .write(response.getOutputStream());

    }
}
