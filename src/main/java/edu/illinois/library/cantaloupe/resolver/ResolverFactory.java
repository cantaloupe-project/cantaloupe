package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResolverFactory {

    private static Logger logger = LoggerFactory.getLogger(ResolverFactory.class);

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
