package edu.illinois.library.cantaloupe.image;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Utility class to provide static access to the Spring-managed FormatRegistry instance.
 * This allows classes like Format to maintain their static API while using dependency injection internally.
 *
 * This class bridges the gap between the static singleton pattern and Spring dependency injection,
 * enabling gradual migration of the codebase to full dependency injection.
 */
@Component
public class FormatRegistryAccessor implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    /**
     * Set by Spring during application startup
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        FormatRegistryAccessor.applicationContext = applicationContext;
    }

    /**
     * Gets the Spring-managed FormatRegistry instance.
     *
     * @return The Spring-managed FormatRegistry instance
     * @throws IllegalStateException if Spring context is not yet initialized
     */
    public static FormatRegistry getFormatRegistry() {
        if (applicationContext == null) {
            throw new IllegalStateException(
                "Spring ApplicationContext not initialized. " +
                "FormatRegistryAccessor can only be used after Spring Boot startup."
            );
        }

        return applicationContext.getBean(FormatRegistry.class);
    }

    /**
     * Checks if the Spring context and FormatRegistry are available.
     *
     * @return true if FormatRegistry can be accessed via Spring DI
     */
    public static boolean isAvailable() {
        return applicationContext != null;
    }

    /**
     * Gets all formats from the Spring-managed FormatRegistry, or fallback to static method.
     *
     * @return Unmodifiable set of all formats
     */
    public static Set<Format> getAllFormats() {
        if (isAvailable()) {
            return getFormatRegistry().allFormats();
        } else {
            return FormatRegistry.allFormatsStatic();
        }
    }

    /**
     * Gets a format with the specified key from the Spring-managed FormatRegistry, or fallback to static method.
     *
     * @param key Format key
     * @return Format with the given key, or null if no such format exists
     */
    public static Format getFormatWithKey(String key) {
        if (isAvailable()) {
            return getFormatRegistry().formatWithKey(key);
        } else {
            return FormatRegistry.formatWithKeyStatic(key);
        }
    }

    /**
     * Clears the format cache in the Spring-managed FormatRegistry, or fallback to static method.
     * For testing purposes only.
     */
    public static void clear() {
        if (isAvailable()) {
            getFormatRegistry().clear();
        } else {
            FormatRegistry.clearStatic();
        }
    }
}
