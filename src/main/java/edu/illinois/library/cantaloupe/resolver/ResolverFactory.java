package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;

public class ResolverFactory {

    /**
     * @return The current resolver based on the <code>resolver</code> setting
     * in the configuration. May return null.
     */
    public static Resolver getResolver() {
        try {
            String resolverName = Application.getConfiguration().
                    getString("resolver", "FilesystemResolver");
            Class class_ = Class.forName(ResolverFactory.class.getPackage().getName() +
                    "." + resolverName);
            return (Resolver) class_.newInstance();
        } catch (ClassNotFoundException e) {
            return null; // TODO: log fatal error
        } catch (InstantiationException e) {
            return null; // TODO: log fatal error
        } catch (IllegalAccessException e) {
            return null; // TODO: log fatal error
        }
    }

}
