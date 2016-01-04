package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.ConfigurationException;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Used to obtain an instance of a {@link Resolver} defined in the
 * configuration, or returned by a delegate method.
 */
public abstract class ResolverFactory {

    private static Logger logger = LoggerFactory.
            getLogger(ResolverFactory.class);

    public static final String RESOLVER_CONFIG_KEY = "resolver";

    /**
     * If {@link #RESOLVER_CONFIG_KEY} is null or undefined, uses a
     * delegate script method to return an instance of the appropriate
     * resolver for the given identifier. Otherwise, returns an instance of
     * the resolver specified in {@link #RESOLVER_CONFIG_KEY}.
     *
     * @return An instance of the appropriate resolver for the given
     * identifier.
     * @throws Exception
     * @throws FileNotFoundException If the specified chooser script is not
     * found.
     */
    public static Resolver getResolver(Identifier identifier) throws Exception {
        final String scriptValue = Application.getConfiguration().
                getString(RESOLVER_CONFIG_KEY);
        if (scriptValue == null) {
            final String resolverName = (String) invokeGetResolverDelegateMethod(identifier);
            return newResolver(resolverName);
        }
        return getStaticResolver();
    }

    /**
     * @return An instance of the current resolver based on the
     * <code>resolver</code> setting in the configuration.
     * @throws Exception
     * @throws ConfigurationException If there is no resolver specified in the
     * configuration.
     */
    private static Resolver getStaticResolver() throws Exception {
        String resolverName = Application.getConfiguration().
                getString(RESOLVER_CONFIG_KEY);
        if (resolverName != null) {
            return newResolver(resolverName);
        } else {
            throw new ConfigurationException("No resolver specified in the " +
                    "configuration. (Check the \"" +
                    RESOLVER_CONFIG_KEY + "\" key.)");
        }
    }

    private static Resolver newResolver(String name) throws Exception {
        Class class_ = Class.forName(ResolverFactory.class.getPackage().getName() +
                "." + name);
        return (Resolver) class_.newInstance();
    }

    /**
     * Passes the given identifier to the .
     *
     * @param identifier Identifier to return a resolver for
     * @return Pathname of the image file corresponding to the given identifier,
     * as reported by the lookup script, or null.
     * @throws IOException If the lookup script configuration key is undefined
     * @throws ScriptException If the script failed to execute
     * @throws ScriptException If the script is of an unsupported type
     */
    private static Object invokeGetResolverDelegateMethod(
            final Identifier identifier) throws ScriptException, IOException {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final String functionName = "Cantaloupe::get_resolver";
        final String[] args = { identifier.toString() };

        final long msec = System.currentTimeMillis();
        final Object result = engine.invoke(functionName, args);
        logger.debug("{}() load+exec time: {} msec",
                functionName, System.currentTimeMillis() - msec);

        return result;
    }

}
