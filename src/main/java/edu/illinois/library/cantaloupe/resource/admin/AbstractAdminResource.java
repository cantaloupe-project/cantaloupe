package edu.illinois.library.cantaloupe.resource.admin;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;

abstract class AbstractAdminResource extends AbstractResource {

    static final String BASIC_REALM = Application.getName() + " Control Panel";

    @Override
    public void doInit() throws Exception {
        super.doInit();

        getResponse().setHeader("Cache-Control", "no-cache");

        final Configuration config = Configuration.getInstance();

        if (!config.getBoolean(Key.ADMIN_ENABLED, false)) {
            throw new EndpointDisabledException();
        }

        authenticateUsingBasic(BASIC_REALM, user -> {
            final String configUser = config.getString(Key.ADMIN_USERNAME, "");
            if (!configUser.isEmpty() && configUser.equals(user)) {
                return config.getString(Key.ADMIN_SECRET);
            }
            return null;
        });
    }

}
