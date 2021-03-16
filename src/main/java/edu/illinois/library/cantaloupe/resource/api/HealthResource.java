package edu.illinois.library.cantaloupe.resource.api;

import com.fasterxml.jackson.databind.SerializationFeature;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.resource.JacksonRepresentation;
import edu.illinois.library.cantaloupe.status.Health;
import edu.illinois.library.cantaloupe.status.HealthChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Provides health checks via the HTTP API.
 */
public class HealthResource extends AbstractAPIResource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HealthResource.class);

    private static final Method[] SUPPORTED_METHODS =
            new Method[] { Method.GET, Method.OPTIONS };

    private static final Map<SerializationFeature, Boolean> SERIALIZATION_FEATURES =
            Map.of(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);

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
        Health health;
        final var config = Configuration.getInstance();
        if (config.getBoolean(Key.HEALTH_DEPENDENCY_CHECK, false)) {
            health = new HealthChecker().check();
        } else {
            health = new Health();
        }

        if (!Health.Color.GREEN.equals(health.getColor())) {
            getResponse().setStatus(500);
        }
        getResponse().setHeader("Content-Type",
                "application/json;charset=UTF-8");
        new JacksonRepresentation(health).write(
                getResponse().getOutputStream(),
                SERIALIZATION_FEATURES);
    }

    @Override
    boolean requiresAuth() {
        return false;
    }

}
