package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.resource.PublicResource;
import org.restlet.resource.ResourceException;

abstract class IIIF2Resource extends PublicResource {

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();

        if (!Configuration.getInstance().
                getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, true)) {
            throw new EndpointDisabledException();
        }
    }

}
