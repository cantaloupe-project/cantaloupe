package edu.illinois.library.cantaloupe.resource.admin;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import org.restlet.resource.ResourceException;

abstract class AbstractAdminResource extends AbstractResource {

    @Override
    protected void doInit() throws ResourceException {
        if (!Configuration.getInstance().getBoolean(Key.ADMIN_ENABLED, false)) {
            throw new EndpointDisabledException();
        }
        super.doInit();
    }

}
