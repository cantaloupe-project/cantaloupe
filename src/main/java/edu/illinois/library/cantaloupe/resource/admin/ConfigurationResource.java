package edu.illinois.library.cantaloupe.resource.admin;

import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;

import java.io.IOException;

/**
 * <p>Resource for retrieving and updating the application configuration object
 * via XHR in the Control Panel.</p>
 *
 * <p>N.B.: This class works almost exactly the same as
 * {@link edu.illinois.library.cantaloupe.resource.api.ConfigurationResource},
 * but requires a different {@link #doInit()} implementation.</p>
 */
public class ConfigurationResource extends AbstractAdminResource {

    private edu.illinois.library.cantaloupe.resource.api.ConfigurationResource wrappedResource =
            new edu.illinois.library.cantaloupe.resource.api.ConfigurationResource();

    /**
     * @return JSON application configuration. <strong>This may contain
     *         sensitive info and must be protected.</strong>
     */
    @Get("json")
    public Representation getConfiguration() throws Exception {
        return wrappedResource.getConfiguration();
    }

    /**
     * Deserializes submitted JSON data and updates the application
     * configuration instance with it.
     */
    @Put("json")
    public Representation putConfiguration(Representation rep)
            throws IOException {
        return wrappedResource.putConfiguration(rep);
    }

}
