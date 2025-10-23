package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

/**
 * Spring-managed factory for creating {@link MetaIdentifierTransformer} instances.
 * Uses dependency injection instead of Configuration.getInstance().
 *
 * @since 5.0
 */
@Service
public class MetaIdentifierTransformerFactory {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MetaIdentifierTransformerFactory.class);

    private static final Set<Class<?>> ALL_IMPLEMENTATIONS = Set.of(
            StandardMetaIdentifierTransformer.class,
            DelegateMetaIdentifierTransformer.class);

    // Static fallback instance for backward compatibility with non-Spring code
    private static MetaIdentifierTransformerFactory staticInstance;

    private final Configuration configuration;

    @Autowired
    public MetaIdentifierTransformerFactory(Configuration configuration) {
        this.configuration = configuration;
        // Set the static instance for backward compatibility
        staticInstance = this;
    }

    public static Set<Class<?>> allImplementations() {
        return ALL_IMPLEMENTATIONS;
    }

    /**
     * Static method for backward compatibility with existing code.
     * Uses Spring-managed instance when available, falls back to singleton pattern otherwise.
     */
    public static MetaIdentifierTransformer newInstanceStatic(DelegateProxy delegateProxy) {
        if (staticInstance != null) {
            return staticInstance.newInstance(delegateProxy);
        } else {
            // Fallback for non-Spring contexts
            return createInstanceWithFallback(delegateProxy);
        }
    }

    /**
     * Fallback method for non-Spring contexts.
     */
    private static MetaIdentifierTransformer createInstanceWithFallback(DelegateProxy delegateProxy) {
        Configuration config = edu.illinois.library.cantaloupe.config.Configuration.getInstance();
        String xformerName = config.getString(Key.META_IDENTIFIER_TRANSFORMER,
                StandardMetaIdentifierTransformer.class.getSimpleName());
        try {
            return newInstance(xformerName, delegateProxy);
        } catch (Exception e) {
            MetaIdentifierTransformer xformer = new StandardMetaIdentifierTransformer();
            LOGGER.error("createInstanceWithFallback(): {} (falling back to returning a {})",
                    e.getMessage(), xformer.getClass().getSimpleName());
            return xformer;
        }
    }

    /**
     * Instance method that uses injected Configuration.
     */
    public MetaIdentifierTransformer newInstance(DelegateProxy delegateProxy) {
        String xformerName = configuration.getString(Key.META_IDENTIFIER_TRANSFORMER,
                StandardMetaIdentifierTransformer.class.getSimpleName());
        try {
            return newInstance(xformerName, delegateProxy);
        } catch (Exception e) {
            MetaIdentifierTransformer xformer = new StandardMetaIdentifierTransformer();
            LOGGER.error("newInstance(): {} (falling back to returning a {})",
                    e.getMessage(), xformer.getClass().getSimpleName());
            return xformer;
        }
    }

    /**
     * @param unqualifiedName Unqualified class name.
     * @return                Qualified class name (package name + class name).
     */
    private static String getQualifiedName(String unqualifiedName) {
        return unqualifiedName.contains(".") ?
                unqualifiedName :
                MetaIdentifierTransformerFactory.class.getPackage().getName() +
                        "." + unqualifiedName;
    }

    /**
     * Retrieves an instance by name.
     *
     * @param name          Class name. If the package name is omitted, it is
     *                      assumed to be the current package.
     * @param delegateProxy Delegate proxy for delegate-based transformers.
     * @return              Instance with the given name.
     */
    private static MetaIdentifierTransformer newInstance(String name,
                                                         DelegateProxy delegateProxy)
            throws ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException,
            InvocationTargetException {
        String qualifiedName = getQualifiedName(name);
        Class<?> implClass = Class.forName(qualifiedName);
        MetaIdentifierTransformer xformer =
                (MetaIdentifierTransformer) implClass.getDeclaredConstructor().newInstance();
        if (xformer instanceof DelegateMetaIdentifierTransformer) {
            DelegateMetaIdentifierTransformer delegateXformer =
                    (DelegateMetaIdentifierTransformer) xformer;
            delegateXformer.setDelegateProxy(delegateProxy);
        }
        return xformer;
    }

    /**
     * For testing only!
     */
    public static synchronized void clearInstance() {
        staticInstance = null;
    }
}
