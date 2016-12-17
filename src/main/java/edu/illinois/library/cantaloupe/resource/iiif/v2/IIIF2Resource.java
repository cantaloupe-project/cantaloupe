package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.resource.iiif.IIIFResource;
import org.restlet.resource.ResourceException;

abstract class IIIF2Resource extends IIIFResource {

    public static final String ENDPOINT_ENABLED_CONFIG_KEY =
            "endpoint.iiif.2.enabled";

    @Override
    protected void doInit() throws ResourceException {
        if (!ConfigurationFactory.getInstance().
                getBoolean(ENDPOINT_ENABLED_CONFIG_KEY, true)) {
            throw new EndpointDisabledException();
        }
        super.doInit();
    }

}
