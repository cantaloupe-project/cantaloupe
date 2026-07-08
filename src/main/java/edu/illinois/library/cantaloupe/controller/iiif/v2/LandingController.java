package edu.illinois.library.cantaloupe.controller.iiif.v2;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.resource.TemplateVariables;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Boot controller for IIIF Image API 2.x landing page.
 * Replaces the previous iiif.v2.LandingResource class.
 */
@Controller("v2LandingController")
@RequestMapping("/iiif/2")
public class LandingController extends AbstractIIIFController {

    @Autowired
    public LandingController(Configuration configuration) {
        super(configuration);
    }

    @GetMapping
    public String iiif2Landing(Model model, HttpServletRequest request, HttpServletResponse response) throws EndpointDisabledException {
        checkEndpointEnabled();

        response.setHeader("Content-Type", "text/html;charset=UTF-8");

        model.addAllAttributes(TemplateVariables.getDefault(request.getHeader("X-Forwarded-Path")).getVars());

        return "iiif_2_landing";
    }

    @GetMapping("/")
    public ResponseEntity<Void> redirectToBase(HttpServletRequest request,
                                               HttpServletResponse response) throws EndpointDisabledException {
        checkEndpointEnabled();
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(URI.create(request.getContextPath() + "/iiif/2"))
                .build();
    }

    @RequestMapping(value = "", method = RequestMethod.OPTIONS)
    public void options(HttpServletResponse response) throws EndpointDisabledException {
        checkEndpointEnabled();
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        response.setHeader("Allow", "GET,OPTIONS");
    }
}
