package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.resource.ResourceException;
import edu.illinois.library.cantaloupe.resource.iiif.IIIFResource;

abstract class IIIF1Resource extends IIIFResource {

    @Override
    public void doInit() throws Exception {
        super.doInit();

        // 6.2: http://iiif.io/api/image/1.1/#server-responses-error
        if (getRequest().getReference().toString().length() > 1024) {
            throw new ResourceException(Status.URI_TOO_LONG);
        }

        if (!Configuration.getInstance().
                getBoolean(Key.IIIF_1_ENDPOINT_ENABLED, true)) {
            throw new EndpointDisabledException();
        }
    }

}
