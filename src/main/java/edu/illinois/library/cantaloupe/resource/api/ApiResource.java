package edu.illinois.library.cantaloupe.resource.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.operation.Identifier;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import org.restlet.data.Reference;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class ApiResource extends AbstractResource {

    private static org.slf4j.Logger logger = LoggerFactory.
            getLogger(ApiResource.class);

    static final String ENABLED_CONFIG_KEY = "endpoint.api.enabled";

    @Override
    protected void doInit() throws ResourceException {
        if (!ConfigurationFactory.getInstance().
                getBoolean(ENABLED_CONFIG_KEY, true)) {
            throw new EndpointDisabledException();
        }
        super.doInit();
    }

    /**
     * @throws Exception
     */
    @Delete
    public Representation doPurge() throws Exception {
        final Cache cache = CacheFactory.getDerivativeCache();
        if (cache != null) {
            final String idStr = (String) this.getRequest().getAttributes().
                    get("identifier");
            final Identifier identifier =
                    new Identifier(decodeSlashes(Reference.decode(idStr)));

            cache.purgeImage(identifier);
        }
        return new EmptyRepresentation();
    }

    /**
     * @throws Exception
     */
    @Get("application/json")
    public Representation getConfiguration() throws Exception {
        final Configuration config = ConfigurationFactory.getInstance();

        final Map<String,Object> map = new LinkedHashMap<>();
        final Iterator<String> keys = config.getKeys();
        while (keys.hasNext()) {
            final String key = keys.next();
            map.put(key, config.getProperty(key));
        }
        return new JacksonRepresentation<>(map);
    }

    /**
     * @param rep PUTted JSON configuration
     * @throws Exception
     */
    @Put("application/json")
    public Representation putConfiguration(Representation rep) throws Exception {
        final Configuration config = ConfigurationFactory.getInstance();
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
