package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import org.apache.commons.configuration.ConfigurationException;

public class ResolverFactory {

    /**
     * @return The current image processor based on the
     * <code>resolver</code> setting in the configuration.
     */
    public static Resolver getResolver() throws Exception {
        Class class_ = Class.forName(ResolverFactory.class.getPackage().getName() +
                "." + getResolverName());
        return (Resolver) class_.newInstance();
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
