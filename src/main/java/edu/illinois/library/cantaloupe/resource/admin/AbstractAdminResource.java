package edu.illinois.library.cantaloupe.resource.admin;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import org.restlet.resource.ResourceException;

abstract class AbstractAdminResource extends AbstractResource {

    static final String CONTROL_PANEL_ENABLED_CONFIG_KEY = "admin.enabled";

    @Override
    protected void doInit() throws ResourceException {
        if (!Configuration.getInstance().getBoolean(
                CONTROL_PANEL_ENABLED_CONFIG_KEY, false)) {
            throw new EndpointDisabledException();
        }
        super.doInit();
    }

}
