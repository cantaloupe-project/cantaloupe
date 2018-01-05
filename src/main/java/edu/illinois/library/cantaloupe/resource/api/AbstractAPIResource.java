package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import org.restlet.data.CacheDirective;
import org.restlet.resource.ResourceException;

abstract class AbstractAPIResource extends AbstractResource {

    @Override
    protected void doInit() throws ResourceException {
        if (!Configuration.getInstance().getBoolean(Key.API_ENABLED, false)) {
            throw new EndpointDisabledException();
        }

        // Add a "Cache-Control: no-cache" header because API resources
        // generally contain live status info.
        getResponseCacheDirectives().add(CacheDirective.noCache());

        super.doInit();
    }

}
