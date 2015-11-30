package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;

/**
 * Used to obtain an instance of the {@link Resolver} defined in the
 * configuration.
 */
public abstract class ResolverFactory {

    /**
     * @return The current resolver based on the <code>resolver</code> setting
     * in the configuration.
     */
    public static Resolver getResolver() throws Exception {
        String resolverName = Application.getConfiguration().
                getString("resolver", "FilesystemResolver");
        Class class_ = Class.forName(ResolverFactory.class.getPackage().getName() +
                "." + resolverName);
        return (Resolver) class_.newInstance();
    }

}
