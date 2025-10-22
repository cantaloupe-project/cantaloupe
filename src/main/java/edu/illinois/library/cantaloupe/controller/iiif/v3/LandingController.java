package edu.illinois.library.cantaloupe.controller.iiif.v3;

import java.util.Collections;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import edu.illinois.library.cantaloupe.resource.Request;
import edu.illinois.library.cantaloupe.resource.TemplateVariables;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Boot controller for IIIF Image API 3.x landing page.
 * Replaces the previous iiif.v3.LandingResource class.
 */
@Controller("v3LandingController")
@RequestMapping("/iiif/3")
public class LandingController {

    @GetMapping
    public String iiif3Landing(Model model, HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Content-Type", "text/html;charset=UTF-8");

        // Create a minimal request wrapper for template variables
        Request requestWrapper = new Request(request, Collections.emptyList());
        model.addAllAttributes(TemplateVariables.getDefault(requestWrapper).getVars());

        return "iiif_3_landing";
    }

    @RequestMapping(value = "", method = RequestMethod.OPTIONS)
    public void options(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        response.setHeader("Allow", "GET,OPTIONS");
    }
}
