package edu.illinois.library.cantaloupe.resource.api;

import com.fasterxml.jackson.databind.SerializationFeature;
import edu.illinois.library.cantaloupe.ApplicationStatus;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.resource.JacksonRepresentation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides live status updates via the HTTP API.
 */
public class StatusResource extends AbstractAPIResource {

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
        new JacksonRepresentation(new ApplicationStatus().toMap())
                .write(getResponse().getOutputStream(), features);
    }

}
