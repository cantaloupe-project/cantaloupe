package edu.illinois.library.cantaloupe.resource.health;

import com.fasterxml.jackson.databind.SerializationFeature;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.resource.JacksonRepresentation;
import edu.illinois.library.cantaloupe.status.Health;
import edu.illinois.library.cantaloupe.status.HealthChecker;

import edu.illinois.library.cantaloupe.resource.Controller;
import edu.illinois.library.cantaloupe.resource.Request;

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Provides health checks via the HTTP API.
 */
public class HealthResource extends Controller {
    public HealthResource(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    private static final Map<SerializationFeature, Boolean> SERIALIZATION_FEATURES =
            Map.of(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);

    @Override
    public void doGet(Request request) throws Exception {
        final Configuration config = Configuration.getInstance();
        if (!config.getBoolean(Key.HEALTH_ENDPOINT_ENABLED, false)) {
            throw new EndpointDisabledException();
        }

        Health health;
        if (config.getBoolean(Key.HEALTH_DEPENDENCY_CHECK, false)) {
            health = new HealthChecker().checkConcurrently();
        } else {
            health = new Health();
        }

        if (!Health.Color.GREEN.equals(health.getColor())) {
            response.setStatus(500);
        }

        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Content-Type", "application/json;charset=UTF-8");

  
        new JacksonRepresentation(health).write(
                response.getOutputStream(),
                SERIALIZATION_FEATURES);
    }

}
