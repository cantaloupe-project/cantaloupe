package edu.illinois.library.cantaloupe.resource.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.cfg.DateTimeFeature;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.resource.JacksonRepresentation;
import edu.illinois.library.cantaloupe.status.ApplicationStatus;

/**
 * Provides live status updates via the HTTP API.
 */
public class StatusResource extends AbstractAPIResource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(StatusResource.class);

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
        Map<DateTimeFeature, Boolean> features = new HashMap<>();
        features.put(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, true);

        getResponse().setHeader("Content-Type",
                "application/json;charset=UTF-8");
        new JacksonRepresentation(new ApplicationStatus(Configuration.getInstance()).toMap())
                .write(getResponse().getOutputStream(), features);
    }

}
