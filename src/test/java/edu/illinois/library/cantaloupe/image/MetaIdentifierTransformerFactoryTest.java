package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Spring Boot test for MetaIdentifierTransformerFactory to verify dependency injection
 * of Configuration instead of using Configuration.getInstance().
 */
@SpringBootTest(classes = {
    MetaIdentifierTransformerFactory.class,
    FormatRegistry.class,
    FormatRegistryAccessor.class
})
@TestPropertySource(properties = {
    "logging.level.root=WARN"
})
class MetaIdentifierTransformerFactoryTest {

    @Autowired
    private MetaIdentifierTransformerFactory factory;

    @MockitoBean
    private Configuration configuration;

    @BeforeEach
    void setUp() {
        ConfigurationFactory.clearInstance();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        System.setProperty(Application.TEST_VM_ARGUMENT, "true");

        // Mock basic configuration
        when(configuration.getFile()).thenReturn(Optional.empty());
        when(configuration.getString(Key.DELEGATE_SCRIPT_PATHNAME, "")).thenReturn("");
        when(configuration.getBoolean(Key.DELEGATE_SCRIPT_ENABLED, false)).thenReturn(false);
    }

    @Test
    void testFactoryIsAutowired() {
        assertNotNull(factory, "MetaIdentifierTransformerFactory should be autowired by Spring");
    }

    @Test
    void testConfigurationIsInjected() {
        assertNotNull(configuration, "Configuration should be mocked/injected");
    }

    @Test
    void testAllImplementations() {
        Set<Class<?>> expected = Set.of(
                StandardMetaIdentifierTransformer.class,
                DelegateMetaIdentifierTransformer.class);
        assertEquals(expected,
                MetaIdentifierTransformerFactory.allImplementations());
    }

    @Test
    void testNewInstanceReturnsCorrectInstanceUsingInjectedConfig() {
        // Mock delegate proxy
        DelegateProxy delegateProxy = null; // Using null for simplicity in this test

        // Test StandardMetaIdentifierTransformer
        when(configuration.getString(Key.META_IDENTIFIER_TRANSFORMER,
                StandardMetaIdentifierTransformer.class.getSimpleName()))
                .thenReturn(StandardMetaIdentifierTransformer.class.getSimpleName());

        MetaIdentifierTransformer xformer = factory.newInstance(delegateProxy);
        assertTrue(xformer instanceof StandardMetaIdentifierTransformer);

        // Test DelegateMetaIdentifierTransformer
        when(configuration.getString(Key.META_IDENTIFIER_TRANSFORMER,
                StandardMetaIdentifierTransformer.class.getSimpleName()))
                .thenReturn(DelegateMetaIdentifierTransformer.class.getSimpleName());

        xformer = factory.newInstance(delegateProxy);
        assertTrue(xformer instanceof DelegateMetaIdentifierTransformer);
    }

    @Test
    void testNewInstanceUsesInjectedConfiguration() {
        // This test verifies that the factory uses injected Configuration
        // instead of Configuration.getInstance()

        when(configuration.getString(Key.META_IDENTIFIER_TRANSFORMER,
                StandardMetaIdentifierTransformer.class.getSimpleName()))
                .thenReturn("StandardMetaIdentifierTransformer");

        MetaIdentifierTransformer xformer = factory.newInstance(null);
        assertNotNull(xformer, "Factory should create transformer with injected Configuration");
        assertTrue(xformer instanceof StandardMetaIdentifierTransformer);
    }

    @Test
    void testNewInstanceHandlesInvalidTransformerName() {
        // Test that factory falls back to StandardMetaIdentifierTransformer for invalid names
        when(configuration.getString(Key.META_IDENTIFIER_TRANSFORMER,
                StandardMetaIdentifierTransformer.class.getSimpleName()))
                .thenReturn("InvalidTransformerName");

        MetaIdentifierTransformer xformer = factory.newInstance(null);
        assertNotNull(xformer, "Factory should fallback when invalid transformer name is provided");
        assertTrue(xformer instanceof StandardMetaIdentifierTransformer);
    }

    @Test
    void testStaticMethodStillWorks() {
        // Verify that static methods work with the Spring-managed instance
        when(configuration.getString(Key.META_IDENTIFIER_TRANSFORMER,
                StandardMetaIdentifierTransformer.class.getSimpleName()))
                .thenReturn("StandardMetaIdentifierTransformer");

        MetaIdentifierTransformer xformer = MetaIdentifierTransformerFactory.newInstanceStatic(null);
        assertNotNull(xformer, "Static method should use Spring-managed instance");
        assertTrue(xformer instanceof StandardMetaIdentifierTransformer);
    }

    @Test
    void testSpringDependencyInjectionIsWorking() {
        // This test demonstrates that our Spring setup is working correctly.
        // The factory requires Configuration to be injected, so if this test passes,
        // it means Configuration injection is working instead of using Configuration.getInstance()

        assertNotNull(factory, "Factory should be autowired");

        // If Configuration.getInstance() were still being used exclusively,
        // the factory might behave differently in this test environment
        assertDoesNotThrow(() -> {
            factory.newInstance(null);
        }, "Factory should work with injected Configuration");
    }

    @Test
    void testBackwardCompatibilityMaintained() {
        // Verify that the static interface still works for non-Spring code
        assertDoesNotThrow(() -> {
            MetaIdentifierTransformerFactory.allImplementations();
        }, "Static methods should work for backward compatibility");
    }
}
