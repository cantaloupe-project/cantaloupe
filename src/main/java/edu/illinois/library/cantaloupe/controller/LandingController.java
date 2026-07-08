package edu.illinois.library.cantaloupe.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.resource.TemplateVariables;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Boot controller for the landing page endpoints.
 * Replaces the previous LandingResource class.
 */
@Controller
public class LandingController {
    private Configuration configuration;

    @Autowired
    public LandingController(Configuration configuration) {
        this.configuration = configuration;
    }

    @GetMapping("/")
    public String landing(Model model, HttpServletRequest request, HttpServletResponse response) {
        addHeaders(response);

        // Add template variables to the model
        model.addAllAttributes(TemplateVariables.getDefault(request.getHeader("X-Forwarded-Path")).getVars());

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
}
