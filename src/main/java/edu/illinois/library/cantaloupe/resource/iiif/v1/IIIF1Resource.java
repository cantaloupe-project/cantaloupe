package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import org.restlet.resource.ResourceException;

abstract class IIIF1Resource extends AbstractResource {

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();

        // 6.2: http://iiif.io/api/image/1.1/#server-responses-error
        if (this.getReference().toString().length() > 1024) {
            throw new ResourceException(414);
        }

        if (!Configuration.getInstance().
                getBoolean(Key.IIIF_1_ENDPOINT_ENABLED, true)) {
            throw new EndpointDisabledException();
        }
    }

}
