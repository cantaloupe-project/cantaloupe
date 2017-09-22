package edu.illinois.library.cantaloupe.resource.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.resource.JSONRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Resource for retrieving and updating the application configuration object
 * via XHR in the Control Panel.
 */
public class ConfigurationResource extends AbstractResource {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(ConfigurationResource.class);

    /**
     * @return JSON application configuration. <strong>This may contain
     *         sensitive info and must be protected.</strong>
     */
    @Get("json")
    public Representation doGet() throws Exception {
        return new JSONRepresentation(configurationAsMap());
    }

    /**
     * Deserializes submitted JSON data and updates the application
     * configuration instance with it.
     */
    @Put("json")
    public Representation doPut(Representation rep) throws IOException {
        final Configuration config = Configuration.getInstance();
        final Map submittedConfig = new ObjectMapper().readValue(
                rep.getStream(), HashMap.class);

        // Copy configuration keys and values from the request JSON payload to
        // the application configuration.
        for (final Object key : submittedConfig.keySet()) {
            final Object value = submittedConfig.get(key);
            LOGGER.debug("Setting {} = {}", key, value);
            config.setProperty((String) key, value);
        }

        config.save();

        return new EmptyRepresentation();
    }

    /**
     * @return Map representation of the application configuration.
     */
    private Map<String,Object> configurationAsMap() {
        final Configuration config = Configuration.getInstance();
        final Map<String,Object> configMap = new HashMap<>();
        final Iterator<String> it = config.getKeys();
        while (it.hasNext()) {
            final String key = it.next();
            final Object value = config.getProperty(key);
            configMap.put(key, value);
        }
        return configMap;
    }

}
