package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.resource.PublicResource;

abstract class IIIF2Resource extends PublicResource {

    @Override
    public void doInit() throws Exception {
        super.doInit();

        if (!Configuration.getInstance().
                getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, true)) {
            throw new EndpointDisabledException();
        }
    }

}
