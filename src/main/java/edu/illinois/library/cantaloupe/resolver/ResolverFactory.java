package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.ConfigurationException;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Used to obtain an instance of a {@link Resolver} defined in the
 * configuration, or returned by a delegate method.
 */
public abstract class ResolverFactory {

    /**
     * @return How resolvers are chosen by {@link #getResolver(Identifier)}.
     */
    public enum LookupStrategy {
        STATIC, DELEGATE_SCRIPT
    }

    private static Logger logger = LoggerFactory.
            getLogger(ResolverFactory.class);

    public static final String DELEGATE_RESOLVER_CONFIG_KEY =
            "resolver.delegate";
    public static final String RESOLVER_CHOOSER_DELEGATE_METHOD =
            "get_resolver";
    public static final String STATIC_RESOLVER_CONFIG_KEY = "resolver.static";

    /**
     * @return Set of instances of each unique resolver.
     */
    public static Set<Resolver> getAllResolvers() {
        return new HashSet<Resolver>(Arrays.asList(
                new AmazonS3Resolver(),
                new AzureStorageResolver(),
                new FilesystemResolver(),
                new HttpResolver(),
                new JdbcResolver()));
    }

    public static LookupStrategy getLookupStrategy() {
        final Configuration config = Application.getConfiguration();
        return config.getBoolean(DELEGATE_RESOLVER_CONFIG_KEY, false) ?
                LookupStrategy.DELEGATE_SCRIPT : LookupStrategy.STATIC;
    }

    /**
     * If {@link #STATIC_RESOLVER_CONFIG_KEY} is null or undefined, uses a
     * delegate script method to return an instance of the appropriate
     * resolver for the given identifier. Otherwise, returns an instance of
     * the resolver specified in {@link #STATIC_RESOLVER_CONFIG_KEY}.
     *
     * @return Instance of the appropriate resolver for the given identifier,
     *         with identifier already set.
     * @throws Exception
     * @throws FileNotFoundException If the specified chooser script is not
     * found.
     */
    public static Resolver getResolver(Identifier identifier) throws Exception {
        final Configuration config = Application.getConfiguration();
        if (getLookupStrategy().equals(LookupStrategy.DELEGATE_SCRIPT)) {
            Resolver resolver = newDynamicResolver(identifier);
            logger.info("{}() returned a {} for {}",
                    RESOLVER_CHOOSER_DELEGATE_METHOD,
                    resolver.getClass().getSimpleName(), identifier);
            return resolver;
        } else {
            final String resolverName = config.
                    getString(STATIC_RESOLVER_CONFIG_KEY);
            if (resolverName != null) {
                final Resolver resolver = newStaticResolver(resolverName);
                resolver.setIdentifier(identifier);
                return resolver;
            } else {
                throw new ConfigurationException(STATIC_RESOLVER_CONFIG_KEY +
                        " is not set to a valid resolver.");
            }
        }
    }

    /**
     * @param resolverName Resolver name
     * @return An instance of the current resolver based on the
     * <code>resolver</code> setting in the configuration.
     * @throws Exception
     * @throws ConfigurationException If there is no resolver specified in the
     * configuration.
     */
    private static Resolver newStaticResolver(String resolverName)
            throws Exception {
        return newResolver(resolverName);
    }

    private static Resolver newResolver(String name) throws Exception {
        Class class_ = Class.forName(ResolverFactory.class.getPackage().getName() +
                "." + name);
        return (Resolver) class_.newInstance();
    }

    /**
     * Passes the given identifier to the resolver chooser delegate method.
     *
     * @param identifier Identifier to return a resolver for
     * @return Pathname of the image file corresponding to the given identifier,
     * as reported by the lookup script, or null.
     * @throws IOException If the lookup script configuration key is undefined
     * @throws ScriptException If the script failed to execute
     * @throws ScriptException If the script is of an unsupported type
     */
    private static Resolver newDynamicResolver(final Identifier identifier)
            throws Exception {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final String[] args = {identifier.toString()};

        final Object result = engine.invoke(RESOLVER_CHOOSER_DELEGATE_METHOD, args);
        return newResolver((String) result);
    }

}
