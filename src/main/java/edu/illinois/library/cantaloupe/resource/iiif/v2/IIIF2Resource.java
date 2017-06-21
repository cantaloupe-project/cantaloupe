package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import org.restlet.resource.ResourceException;

abstract class IIIF2Resource extends AbstractResource {

    @Override
    protected void doInit() throws ResourceException {
        if (!ConfigurationFactory.getInstance().
                getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, true)) {
            throw new EndpointDisabledException();
        }
        super.doInit();
    }

}
