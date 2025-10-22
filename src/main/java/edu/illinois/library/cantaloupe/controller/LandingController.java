package edu.illinois.library.cantaloupe.controller;

import edu.illinois.library.cantaloupe.resource.TemplateVariables;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Boot controller for the landing page endpoints.
 * Replaces the previous LandingResource class.
 */
@Controller
public class LandingController {

    @GetMapping("/")
    public String landing(Model model, HttpServletRequest request, HttpServletResponse response) {
        addHeaders(response);

        // Add template variables to the model
        model.addAllAttributes(TemplateVariables.getDefault(createRequestWrapper(request)).getVars());

        return "landing";
    }

    @RequestMapping(value = "/", method = RequestMethod.OPTIONS)
    public void options(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        response.setHeader("Allow", "GET,OPTIONS");
    }

    private void addHeaders(HttpServletResponse response) {
        response.setHeader("Content-Type", "text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "public, max-age=" + Integer.MAX_VALUE);
    }

    /**
     * Creates a request wrapper that's compatible with the existing TemplateVariables system.
     * This is a temporary bridge until we fully migrate the template system.
     */
    private edu.illinois.library.cantaloupe.resource.Request createRequestWrapper(HttpServletRequest request) {
        return new edu.illinois.library.cantaloupe.resource.Request(request, java.util.Collections.emptyList());
    }
}
