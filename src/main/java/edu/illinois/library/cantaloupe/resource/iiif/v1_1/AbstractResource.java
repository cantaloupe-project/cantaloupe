package edu.illinois.library.cantaloupe.resource.iiif.v1_1;

import org.restlet.resource.ResourceException;

abstract class AbstractResource extends edu.illinois.library.cantaloupe.resource.AbstractResource {

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        // 6.2: http://iiif.io/api/image/1.1/#server-responses-error
        if (this.getReference().toString().length() > 1024) {
            throw new ResourceException(414);
        }
        getResponseCacheDirectives().addAll(getCacheDirectives());
    }

}
