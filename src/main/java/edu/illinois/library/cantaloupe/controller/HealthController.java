package edu.illinois.library.cantaloupe.controller;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.status.Health;
import edu.illinois.library.cantaloupe.status.HealthChecker;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Boot controller for health check endpoints.
 * Replaces the previous HealthResource class.
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Health> health(HttpServletResponse response) throws EndpointDisabledException {
        final Configuration config = Configuration.getInstance();
        if (!config.getBoolean(Key.HEALTH_ENDPOINT_ENABLED, false)) {
            throw new EndpointDisabledException();
        }

        // Set cache headers
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Content-Type", "application/json;charset=UTF-8");

        Health health;
        if (config.getBoolean(Key.HEALTH_DEPENDENCY_CHECK, false)) {
            health = new HealthChecker().checkConcurrently();
        } else {
            health = new Health();
        }

        HttpStatus status = Health.Color.GREEN.equals(health.getColor())
            ? HttpStatus.OK
            : HttpStatus.INTERNAL_SERVER_ERROR;

        return ResponseEntity.status(status).body(health);
    }

    @RequestMapping(value = "", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> options() {
        return ResponseEntity.noContent()
                .header("Allow", "GET,OPTIONS")
                .build();
    }
}
