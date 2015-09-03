package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import org.apache.commons.configuration.ConfigurationException;

public class ResolverFactory {

    /**
     * @return The current image processor based on the
     * <code>resolver</code> setting in the configuration. May return null.
     */
    public static Resolver getResolver() {
        try {
            Class class_ = Class.forName(ResolverFactory.class.getPackage().getName() +
                    "." + getResolverName());
            return (Resolver) class_.newInstance();
        } catch (ClassNotFoundException e) {
            return null; // TODO: log
        } catch (InstantiationException e) {
            return null; // TODO: log
        } catch (IllegalAccessException e) {
            return null; // TODO: log
        }
    }

    private static String getResolverName() {
        String name;
        try {
            name = Application.getConfiguration().getString("resolver");
        } catch (ConfigurationException e) {
            return "FilesystemResolver";
        }
        return name;
    }

}
