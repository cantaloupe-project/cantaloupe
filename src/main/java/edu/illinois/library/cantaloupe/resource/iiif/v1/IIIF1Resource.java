package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import edu.illinois.library.cantaloupe.resource.iiif.IIIFResource;
import org.restlet.resource.ResourceException;

abstract class IIIF1Resource extends IIIFResource {

    public static final String ENDPOINT_ENABLED_CONFIG_KEY =
            "endpoint.iiif.1.enabled";

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();

        // 6.2: http://iiif.io/api/image/1.1/#server-responses-error
        if (this.getReference().toString().length() > 1024) {
            throw new ResourceException(414);
        }

        if (!ConfigurationFactory.getInstance().
                getBoolean(ENDPOINT_ENABLED_CONFIG_KEY, true)) {
            throw new EndpointDisabledException();
        }
    }

}
