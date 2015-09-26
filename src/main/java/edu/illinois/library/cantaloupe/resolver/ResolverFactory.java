package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;

public class ResolverFactory {

    /**
     * @return The current resolver based on the <code>resolver</code> setting
     * in the configuration. May return null.
     */
    public static Resolver getResolver() throws Exception {
        String resolverName = Application.getConfiguration().
                getString("resolver", "FilesystemResolver");
        Class class_ = Class.forName(ResolverFactory.class.getPackage().getName() +
                "." + resolverName);
        return (Resolver) class_.newInstance();
    }

}
