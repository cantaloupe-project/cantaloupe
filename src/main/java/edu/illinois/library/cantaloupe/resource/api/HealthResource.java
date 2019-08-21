package edu.illinois.library.cantaloupe.resource.api;

import com.fasterxml.jackson.databind.SerializationFeature;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.resource.JacksonRepresentation;
import edu.illinois.library.cantaloupe.status.Health;
import edu.illinois.library.cantaloupe.status.HealthChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides health checks via the HTTP API.
 */
public class HealthResource extends AbstractAPIResource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HealthResource.class);

    private static final Method[] SUPPORTED_METHODS =
            new Method[] { Method.GET, Method.OPTIONS };

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Method[] getSupportedMethods() {
        return SUPPORTED_METHODS;
    }

    @Override
    public void doGET() throws IOException {
        Map<SerializationFeature, Boolean> features = new HashMap<>();
        features.put(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);

        final Health health = new HealthChecker().check();

        if (!Health.Color.GREEN.equals(health.getColor())) {
            getResponse().setStatus(500);
        }
        getResponse().setHeader("Content-Type",
                "application/json;charset=UTF-8");

        new JacksonRepresentation(health)
                .write(getResponse().getOutputStream(), features);
    }

    @Override
    boolean requiresAuth() {
        return false;
    }

}
