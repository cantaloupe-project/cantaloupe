package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.script.DelegateMethod;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static edu.illinois.library.cantaloupe.source.SourceFactory.SelectionStrategy.DELEGATE_SCRIPT;

/**
 * Used to obtain an instance of a {@link Source} defined in the
 * configuration, or returned by a delegate method.
 */
public final class SourceFactory {

    /**
     * How sources are chosen by {@link #newSource}.
     */
    public enum SelectionStrategy {

        /**
         * A global source is specified using the {@link Key#SOURCE_STATIC}
         * configuration key.
         */
        STATIC,

        /**
         * A source specific to the request is acquired from the {@link
         * DelegateMethod#SOURCE} delegate method.
         */
        DELEGATE_SCRIPT

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SourceFactory.class);

    /**
     * @return Set of instances of each unique source.
     */
    public static Set<Source> getAllSources() {
        return new HashSet<>(Arrays.asList(
                new AzureStorageSource(),
                new FilesystemSource(),
                new HttpSource(),
                new JdbcSource(),
                new S3Source()));
    }

    /**
     * <p>If {@link Key#SOURCE_STATIC} is not set, uses a delegate method to
     * return an instance of a source for the given identifier. Otherwise,
     * returns an instance of the source specified in {@link
     * Key#SOURCE_STATIC}.</p>
     *
     * <p>Source names, whether acquired from the configuration or from a
     * delegate method, may be full class names including package name, or
     * simple class names, in which case they will be assumed to reside in
     * this package.</p>
     *
     * @param identifier Identifier of the source image.
     * @param proxy      Delegate proxy. May be {@literal null} if
     *                   {@link #getSelectionStrategy()} returns {@link
     *                   SelectionStrategy#STATIC}.
     * @return Instance of the appropriate source for the given identifier,
     *         with identifier already set.
     * @throws IllegalArgumentException if the {@literal proxy} argument is
     *                                  {@literal null} while using {@link
     *                                  SelectionStrategy#DELEGATE_SCRIPT}.
     */
    public Source newSource(Identifier identifier,
                            DelegateProxy proxy) throws Exception {
        switch (getSelectionStrategy()) {
            case DELEGATE_SCRIPT:
                if (proxy == null) {
                    throw new IllegalArgumentException("The " +
                            DelegateProxy.class.getSimpleName() +
                            " argument must be non-null when using " +
                            getSelectionStrategy() + ".");
                }
                Source source = newDynamicSource(identifier, proxy);
                LOGGER.info("{}() returned a {} for {}",
                        DelegateMethod.SOURCE,
                        source.getClass().getSimpleName(),
                        identifier);
                return source;
            default:
                final Configuration config = Configuration.getInstance();
                final String sourceName = config.getString(Key.SOURCE_STATIC);
                if (sourceName != null) {
                    return newSource(sourceName, identifier, proxy);
                } else {
                    throw new ConfigurationException(Key.SOURCE_STATIC +
                            " is not set to a valid source.");
                }
        }
    }

    /**
     * @return How sources are chosen by {@link #newSource}.
     */
    public SelectionStrategy getSelectionStrategy() {
        final Configuration config = Configuration.getInstance();
        return config.getBoolean(Key.SOURCE_DELEGATE, false) ?
                DELEGATE_SCRIPT : SelectionStrategy.STATIC;
    }

    private Source newSource(String name,
                             Identifier identifier,
                             DelegateProxy proxy) throws Exception {
        // If the name contains a dot, assume it's a  full class name,
        // including package. Otherwise, assume it's a simple class name in
        // this package.
        String fullName = name.contains(".") ?
                name : SourceFactory.class.getPackage().getName() + "." + name;
        Class<?> class_ = Class.forName(fullName);

        Source source = (Source) class_.newInstance();
        source.setIdentifier(identifier);
        source.setDelegateProxy(proxy);
        return source;
    }

    /**
     * @param identifier Identifier to return a source for.
     * @param proxy      Delegate proxy from which to acquire the source
     *                   name.
     * @return           Source as returned from the given delegate proxy.
     * @throws IOException     if the lookup script configuration key is
     *                         undefined.
     * @throws ScriptException if the delegate method failed to execute.
     */
    private Source newDynamicSource(Identifier identifier,
                                    DelegateProxy proxy) throws Exception {
        return newSource(proxy.getSource(), identifier, proxy);
    }

}
