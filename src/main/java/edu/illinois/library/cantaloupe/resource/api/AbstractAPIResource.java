package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;

abstract class AbstractAPIResource extends AbstractResource {

    static final String BASIC_REALM = Application.getName() + " API Realm";

    @Override
    public void doInit() throws Exception {
        super.doInit();

        getResponse().setHeader("Cache-Control", "no-cache");

        final Configuration config = Configuration.getInstance();

        if (!config.getBoolean(Key.API_ENABLED, false)) {
            throw new EndpointDisabledException();
        }

        if (requiresAuth()) {
            authenticateUsingBasic(BASIC_REALM, user -> {
                final String configUser = config.getString(Key.API_USERNAME, "");
                if (!configUser.isEmpty() && configUser.equals(user)) {
                    return config.getString(Key.API_SECRET);
                }
                return null;
            });
        }
    }

    abstract boolean requiresAuth();

}
