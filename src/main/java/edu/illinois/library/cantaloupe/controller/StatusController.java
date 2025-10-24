package edu.illinois.library.cantaloupe.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.status.ApplicationStatus;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Boot controller for status endpoints.
 * Replaces the previous api.StatusResource class.
 */
@RestController
@RequestMapping("/status")
public class StatusController {
    private Configuration configuration;

    @Autowired
    public StatusController(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Provides live status updates via the HTTP API.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatus(HttpServletResponse response) {
        response.setHeader("Content-Type", "application/json;charset=UTF-8");

        Map<String, Object> statusMap = new ApplicationStatus(configuration).toMap();
        return ResponseEntity.ok(statusMap);
    }

    @RequestMapping(value = "", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> options() {
        return ResponseEntity.noContent()
                .header("Allow", "GET,OPTIONS")
                .build();
    }
}
