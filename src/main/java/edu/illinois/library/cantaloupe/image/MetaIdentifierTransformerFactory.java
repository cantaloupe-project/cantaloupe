package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

/**
 * Used to obtain new {@link MetaIdentifierTransformer}s.
 *
 * @since 5.0
 */
public final class MetaIdentifierTransformerFactory {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MetaIdentifierTransformerFactory.class);

    private static final Set<Class<?>> ALL_IMPLEMENTATIONS = Set.of(
            StandardMetaIdentifierTransformer.class,
            DelegateMetaIdentifierTransformer.class);

    public static Set<Class<?>> allImplementations() {
        return ALL_IMPLEMENTATIONS;
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

    public MetaIdentifierTransformer newInstance(DelegateProxy delegateProxy) {
        Configuration config = Configuration.getInstance();
        String xformerName = config.getString(Key.META_IDENTIFIER_TRANSFORMER,
                StandardMetaIdentifierTransformer.class.getSimpleName());
        try {
            return newInstance(xformerName, delegateProxy);
        } catch (Exception e) {
            MetaIdentifierTransformer xformer =
                    new StandardMetaIdentifierTransformer();
            LOGGER.error("newInstance(): {} (falling back to returning a {})",
                    e.getMessage(), xformer.getClass().getSimpleName());
            return xformer;
        }
    }

    /**
     * Retrieves an instance by name.
     *
     * @param name          Class name. If the package name is omitted, it is
     *                      assumed to be the current package.
     * @param delegateProxy
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

}
