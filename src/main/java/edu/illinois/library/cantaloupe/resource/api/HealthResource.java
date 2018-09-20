package edu.illinois.library.cantaloupe.resource.api;

import com.fasterxml.jackson.databind.SerializationFeature;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.resource.JacksonRepresentation;
import edu.illinois.library.cantaloupe.status.HealthChecker;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides health checks via the HTTP API.
 */
public class HealthResource extends AbstractAPIResource {

    private static final Method[] SUPPORTED_METHODS =
            new Method[] { Method.GET, Method.OPTIONS };

    @Override
    public Method[] getSupportedMethods() {
        return SUPPORTED_METHODS;
    }

    @Override
    public void doGET() throws IOException {
        Map<SerializationFeature, Boolean> features = new HashMap<>();
        features.put(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);

        getResponse().setHeader("Content-Type",
                "application/json;charset=UTF-8");
        new JacksonRepresentation(new HealthChecker().check())
                .write(getResponse().getOutputStream(), features);
    }

}
