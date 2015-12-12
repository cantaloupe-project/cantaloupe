package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.ConfigurationException;

/**
 * Used to obtain an instance of the {@link Resolver} defined in the
 * configuration.
 */
public abstract class ResolverFactory {

    /**
     * @return The current resolver based on the <code>resolver</code> setting
     * in the configuration.
     * @throws Exception
     * @throws ConfigurationException If there is no resolver specified in the
     * configuration.
     */
    public static Resolver getResolver() throws Exception {
        String resolverName = Application.getConfiguration().
                getString("resolver");
        if (resolverName != null) {
            Class class_ = Class.forName(ResolverFactory.class.getPackage().getName() +
                    "." + resolverName);
            return (Resolver) class_.newInstance();
        } else {
            throw new ConfigurationException("No resolver specified in the " +
                    "configuration. (Check the \"resolver\" key.)");
        }
    }

}
