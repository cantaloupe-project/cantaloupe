package edu.illinois.library.cantaloupe.resource.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.resource.JacksonRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationResource extends AbstractAPIResource {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(ConfigurationResource.class);

    private static final Method[] SUPPORTED_METHODS =
            new Method[] { Method.GET, Method.OPTIONS, Method.PUT };

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Method[] getSupportedMethods() {
        return SUPPORTED_METHODS;
    }

    /**
     * Returns JSON application configuration. <strong>This may contain
     * sensitive info and must be protected.</strong>
     */
    @Override
    public void doGET() throws IOException {
        getResponse().setHeader("Content-Type",
                "application/json;charset=UTF-8");

        Configuration config = Configuration.getInstance();
        new JacksonRepresentation(config.toMap())
                .write(getResponse().getOutputStream());
    }

    /**
     * Deserializes submitted JSON data and updates the application
     * configuration instance with it.
     */
    @Override
    public void doPUT() throws IOException {
        final Configuration config = Configuration.getInstance();
        final Map<?, ?> submittedConfig = new ObjectMapper().readValue(
                getRequest().getInputStream(), HashMap.class);

        LOGGER.info("Updating {} configuration keys", submittedConfig.size());

        // Copy configuration keys and values from the request JSON payload to
        // the application configuration.
        submittedConfig.forEach((key, value) ->
                config.setProperty((String) key, value));

        config.save();

        getResponse().setStatus(Status.NO_CONTENT.getCode());
    }

    @Override
    boolean requiresAuth() {
        return true;
    }

}
