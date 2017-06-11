package edu.illinois.library.cantaloupe.resource.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.resource.JSONRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigurationResource extends APIResource {

    private static org.slf4j.Logger logger = LoggerFactory.
            getLogger(ConfigurationResource.class);

    /**
     * @throws Exception
     */
    @Get("json")
    public Representation getConfiguration() throws Exception {
        final Configuration config = Configuration.getInstance();

        final Map<String,Object> map = new LinkedHashMap<>();
        final Iterator<String> keys = config.getKeys();
        while (keys.hasNext()) {
            final String key = keys.next();
            map.put(key, config.getProperty(key));
        }
        return new JSONRepresentation(map);
    }

    /**
     * @param rep PUTted JSON configuration
     * @throws Exception
     */
    @Put("json")
    public Representation putConfiguration(Representation rep) throws Exception {
        final Configuration config = Configuration.getInstance();
        final Map submittedConfig = new ObjectMapper().readValue(
                rep.getStream(), HashMap.class);

        // Copy configuration keys and values from the request JSON payload to
        // the application configuration.
        for (final Object key : submittedConfig.keySet()) {
            final Object value = submittedConfig.get(key);
            logger.debug("Setting {} = {}", key, value);
            config.setProperty((String) key, value);
        }
        config.save();

        return new EmptyRepresentation();
    }

}
