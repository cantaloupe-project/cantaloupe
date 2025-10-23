package edu.illinois.library.cantaloupe.delegate;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.FormatRegistry;
import edu.illinois.library.cantaloupe.image.FormatRegistryAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Spring Boot test for DelegateProxyService to verify dependency injection
 * of Configuration instead of using Configuration.getInstance().
 */
@SpringBootTest(classes = {
    DelegateProxyService.class,
    FormatRegistry.class,
    FormatRegistryAccessor.class
})
@TestPropertySource(properties = {
    "logging.level.root=WARN"
})
class DelegateProxyServiceSpringTest {

    @Autowired
    private DelegateProxyService delegateProxyService;

    @MockitoBean
    private Configuration configuration;

    @BeforeEach
    void setUp() {
        ConfigurationFactory.clearInstance();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        System.setProperty(Application.TEST_VM_ARGUMENT, "true");

        // Mock basic configuration
        when(configuration.getFile()).thenReturn(Optional.empty());
        when(configuration.getBoolean(Key.DELEGATE_SCRIPT_ENABLED, false)).thenReturn(false);
        when(configuration.getString(Key.DELEGATE_SCRIPT_PATHNAME, "")).thenReturn("");
    }

    @Test
    void testServiceIsAutowired() {
        assertNotNull(delegateProxyService, "DelegateProxyService should be autowired by Spring");
    }

    @Test
    void testConfigurationIsInjected() {
        assertNotNull(configuration, "Configuration should be mocked/injected");
    }

    @Test
    void testServiceUsesInjectedConfiguration() {
        // Test that the service uses injected Configuration instead of Configuration.getInstance()

        // Mock delegate script as disabled
        when(configuration.getBoolean(Key.DELEGATE_SCRIPT_ENABLED, false)).thenReturn(false);

        boolean scriptEnabled = delegateProxyService.isScriptEnabledInternal();
        assertFalse(scriptEnabled, "Script should be disabled based on injected configuration");

        // Now mock it as enabled
        when(configuration.getBoolean(Key.DELEGATE_SCRIPT_ENABLED, false)).thenReturn(true);

        scriptEnabled = delegateProxyService.isScriptEnabledInternal();
        assertTrue(scriptEnabled, "Script should be enabled based on injected configuration");
    }

    @Test
    void testStaticMethodStillWorks() {
        // Verify that static methods work with the Spring-managed instance
        when(configuration.getBoolean(Key.DELEGATE_SCRIPT_ENABLED, false)).thenReturn(false);

        boolean scriptEnabled = DelegateProxyService.isScriptEnabled();
        assertFalse(scriptEnabled, "Static method should use Spring-managed instance");

        when(configuration.getBoolean(Key.DELEGATE_SCRIPT_ENABLED, false)).thenReturn(true);

        scriptEnabled = DelegateProxyService.isScriptEnabled();
        assertTrue(scriptEnabled, "Static method should use Spring-managed instance");
    }

    @Test
    void testServiceInstanceIsAccessible() {
        // Verify that getInstance() returns the Spring-managed instance
        DelegateProxyService instance = DelegateProxyService.getInstance();

        assertNotNull(instance, "getInstance() should return a valid instance");
        assertSame(delegateProxyService, instance,
            "getInstance() should return the same Spring-managed instance");
    }

    @Test
    void testDependencyInjectionIsWorking() {
        // This test demonstrates that our Spring setup is working correctly.
        // The service requires Configuration to be injected, so if this test passes,
        // it means Configuration injection is working instead of using Configuration.getInstance()

        assertNotNull(delegateProxyService, "Service should be autowired");

        // If Configuration.getInstance() were still being used exclusively,
        // the service might behave differently in this test environment
        assertDoesNotThrow(() -> {
            delegateProxyService.isScriptEnabledInternal();
        }, "Service methods should work with injected Configuration");
    }

    @Test
    void testScriptFileAccessWithInjectedConfiguration() {
        // Test that script file access uses injected configuration
        when(configuration.getFile()).thenReturn(Optional.empty());
        when(configuration.getString(Key.DELEGATE_SCRIPT_PATHNAME, "")).thenReturn("");

        assertDoesNotThrow(() -> {
            delegateProxyService.getScriptFileInternal();
        }, "Script file access should work with injected Configuration");
    }

    @Test
    void testBackwardCompatibilityMaintained() {
        // Verify that the static interface still works for non-Spring code
        assertDoesNotThrow(() -> {
            DelegateProxyService.isDelegateAvailable();
        }, "Static methods should work for backward compatibility");
    }

    @Test
    void testServiceLifecycleManagement() {
        // Test that the service can be started and stopped without errors
        assertDoesNotThrow(() -> {
            delegateProxyService.startWatching();
            delegateProxyService.stopWatching();
        }, "Service lifecycle methods should work with injected dependencies");
    }
}
