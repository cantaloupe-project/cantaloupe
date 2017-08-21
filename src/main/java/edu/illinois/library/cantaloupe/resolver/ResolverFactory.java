package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.script.ScriptEngine;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
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
public class ResolverFactory {

    /**
     * How resolvers are chosen by {@link #getResolver(Identifier)}.
     */
    public enum SelectionStrategy {
        STATIC, DELEGATE_SCRIPT
    }

    private static Logger logger = LoggerFactory.
            getLogger(ResolverFactory.class);

    private static final String RESOLVER_CHOOSER_DELEGATE_METHOD =
            "get_resolver";

    /**
     * @return Set of instances of each unique resolver.
     */
    public static Set<Resolver> getAllResolvers() {
        return new HashSet<>(Arrays.asList(
                new AmazonS3Resolver(),
                new AzureStorageResolver(),
                new FilesystemResolver(),
                new HttpResolver(),
                new JdbcResolver()));
    }

    /**
     * If {@link Key#RESOLVER_STATIC} is null or undefined, uses a delegate
     * script method to return an instance of the appropriate resolver for the
     * given identifier. Otherwise, returns an instance of the resolver
     * specified in {@link Key#RESOLVER_STATIC}.
     *
     * @return Instance of the appropriate resolver for the given identifier,
     *         with identifier already set.
     * @throws Exception
     * @throws FileNotFoundException If the specified chooser script is not
     * found.
     */
    public Resolver getResolver(Identifier identifier) throws Exception {
        final Configuration config = Configuration.getInstance();
        if (getSelectionStrategy().equals(SelectionStrategy.DELEGATE_SCRIPT)) {
            Resolver resolver = newDynamicResolver(identifier);
            logger.info("{}() returned a {} for {}",
                    RESOLVER_CHOOSER_DELEGATE_METHOD,
                    resolver.getClass().getSimpleName(), identifier);
            return resolver;
        } else {
            final String resolverName = config.getString(Key.RESOLVER_STATIC);
            if (resolverName != null) {
                return newStaticResolver(resolverName, identifier);
            } else {
                throw new ConfigurationException(Key.RESOLVER_STATIC +
                        " is not set to a valid resolver.");
            }
        }
    }

    /**
     * @return How resolvers are chosen by {@link #getResolver(Identifier)}.
     */
    public SelectionStrategy getSelectionStrategy() {
        final Configuration config = Configuration.getInstance();
        return config.getBoolean(Key.RESOLVER_DELEGATE, false) ?
                SelectionStrategy.DELEGATE_SCRIPT : SelectionStrategy.STATIC;
    }

    /**
     * @param resolverName Resolver name
     * @param identifier Identifier to return a resolver for.
     * @return An instance of the current resolver based on the
     * <code>resolver</code> setting in the configuration.
     * @throws Exception
     * @throws ConfigurationException If there is no resolver specified in the
     * configuration.
     */
    private Resolver newStaticResolver(String resolverName,
                                       Identifier identifier) throws Exception {
        return newResolver(resolverName, identifier);
    }

    private Resolver newResolver(String name, Identifier identifier)
            throws Exception {
        Class<?> class_ = Class.forName(ResolverFactory.class.getPackage().getName() +
                "." + name);
        Resolver resolver = (Resolver) class_.newInstance();
        resolver.setIdentifier(identifier);
        return resolver;
    }

    /**
     * Passes the given identifier to the resolver chooser delegate method.
     *
     * @param identifier Identifier to return a resolver for.
     * @return Pathname of the image file corresponding to the given identifier,
     * as reported by the lookup script, or null.
     * @throws IOException If the lookup script configuration key is undefined
     * @throws ScriptException If the script failed to execute
     * @throws ScriptException If the script is of an unsupported type
     */
    private Resolver newDynamicResolver(final Identifier identifier)
            throws Exception {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final Object result = engine.invoke(RESOLVER_CHOOSER_DELEGATE_METHOD,
                identifier.toString());
        return newResolver((String) result, identifier);
    }

}
