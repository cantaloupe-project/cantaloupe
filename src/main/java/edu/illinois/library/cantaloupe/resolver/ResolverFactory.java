package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.RequestContext;
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
     * How resolvers are chosen by {@link #newResolver}.
     */
    public enum SelectionStrategy {
        STATIC, DELEGATE_SCRIPT
    }

    private static final Logger LOGGER = LoggerFactory.
            getLogger(ResolverFactory.class);

    private static final String RESOLVER_CHOOSER_DELEGATE_METHOD =
            "get_resolver";

    /**
     * @return Set of instances of each unique resolver.
     */
    public static Set<Resolver> getAllResolvers() {
        return new HashSet<>(Arrays.asList(
                new AzureStorageResolver(),
                new FilesystemResolver(),
                new HttpResolver(),
                new JdbcResolver(),
                new S3Resolver()));
    }

    /**
     * If {@link Key#RESOLVER_STATIC} is null or undefined, uses a delegate
     * script method to return an instance of the appropriate resolver for the
     * given identifier. Otherwise, returns an instance of the resolver
     * specified in {@link Key#RESOLVER_STATIC}.
     *
     * @return Instance of the appropriate resolver for the given identifier,
     *         with identifier already set.
     * @throws FileNotFoundException If the specified chooser script is not
     *                               found.
     */
    public Resolver newResolver(Identifier identifier,
                                RequestContext context) throws Exception {
        final Configuration config = Configuration.getInstance();
        if (getSelectionStrategy().equals(SelectionStrategy.DELEGATE_SCRIPT)) {
            Resolver resolver = newDynamicResolver(identifier, context);
            LOGGER.info("{}() returned a {} for {}",
                    RESOLVER_CHOOSER_DELEGATE_METHOD,
                    resolver.getClass().getSimpleName(), identifier);
            return resolver;
        } else {
            final String resolverName = config.getString(Key.RESOLVER_STATIC);
            if (resolverName != null) {
                return newResolver(resolverName, identifier, context);
            } else {
                throw new ConfigurationException(Key.RESOLVER_STATIC +
                        " is not set to a valid resolver.");
            }
        }
    }

    /**
     * @return How resolvers are chosen by {@link #newResolver}.
     */
    public SelectionStrategy getSelectionStrategy() {
        final Configuration config = Configuration.getInstance();
        return config.getBoolean(Key.RESOLVER_DELEGATE, false) ?
                SelectionStrategy.DELEGATE_SCRIPT : SelectionStrategy.STATIC;
    }

    private Resolver newResolver(String name,
                                 Identifier identifier,
                                 RequestContext context) throws Exception {
        Class<?> class_ = Class.forName(
                ResolverFactory.class.getPackage().getName() + "." + name);
        Resolver resolver = (Resolver) class_.newInstance();
        resolver.setIdentifier(identifier);
        resolver.setContext(context);
        return resolver;
    }

    /**
     * Passes the given identifier to the resolver chooser delegate method.
     *
     * @param identifier Identifier to return a resolver for.
     * @param context    Request context.
     * @return Resolver for the given identifier as returned from a delegate
     *         method.
     * @throws IOException If the lookup script configuration key is undefined
     * @throws ScriptException If the script failed to execute.
     */
    private Resolver newDynamicResolver(Identifier identifier,
                                        RequestContext context)
            throws Exception {
        final ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        final Object result = engine.invoke(RESOLVER_CHOOSER_DELEGATE_METHOD,
                identifier.toString());
        return newResolver((String) result, identifier, context);
    }

}
