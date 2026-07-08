package edu.illinois.library.cantaloupe.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.auth.BasicAuth;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.resource.ResourceException;
import edu.illinois.library.cantaloupe.status.ApplicationStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Boot controller for status endpoints.
 * Replaces the previous api.StatusResource class.
 */
@RestController
@RequestMapping("/status")
public class StatusController {

    static final String BASIC_REALM = Application.getName() + " API Realm";

    private Configuration configuration;

    @Autowired
    public StatusController(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Provides live status updates via the HTTP API.
     * @throws ResourceException 
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatus(HttpServletRequest request, HttpServletResponse response) throws ResourceException {
        beforeAll(request, response);
  
        response.setHeader("Content-Type", "application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        return ResponseEntity.ok(new ApplicationStatus(configuration).toMap());
    }

    @RequestMapping(value = "", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> options(HttpServletRequest request, HttpServletResponse response) throws ResourceException {
        beforeAll(request, response);
        
        return ResponseEntity.noContent()
                .header("Allow", "GET,OPTIONS")
                .build();
    }

    private void beforeAll(HttpServletRequest request, HttpServletResponse response) throws ResourceException {
        if (!configuration.getBoolean(Key.API_ENABLED, false)) {
            throw new EndpointDisabledException();
        }
        // Perform HTTP Basic Authentication
        BasicAuth.authenticateUsingBasic(BASIC_REALM, user -> {
            final String configUser = configuration.getString(Key.API_USERNAME, "");
            if (!configUser.isEmpty() && configUser.equals(user)) {
                return configuration.getString(Key.API_SECRET);
            }
            return null;
        }, request, response);
    }
}
