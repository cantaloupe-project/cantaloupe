package edu.illinois.library.cantaloupe.config;

import edu.illinois.library.cantaloupe.delegate.DelegateProxyService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Separate component to handle DelegateProxyService initialization
 * without creating circular dependencies with Configuration.
 *
 * This component is loaded after all other beans are initialized,
 * avoiding the circular dependency issue between Configuration,
 * WebConfig, and DelegateProxyService.
 */
@Component
public class DelegateServiceInitializer {

    private Optional<DelegateProxyService> delegateProxyService;

    @Autowired(required = false)
    public void setDelegateProxyService(@Lazy Optional<DelegateProxyService> delegateProxyService) {
        this.delegateProxyService = delegateProxyService;
    }

    /**
     * Initialize DelegateProxyService after Spring context is ready.
     * This ensures the service starts watching for script changes if needed.
     */
    @PostConstruct
    public void initializeDelegateProxyService() {
        if (delegateProxyService != null) {
            delegateProxyService.ifPresent(service -> {
                try {
                    service.startWatching();
                } catch (Exception e) {
                    // Log but don't fail startup if delegate service initialization fails
                    System.err.println("Warning: Failed to initialize DelegateProxyService: " + e.getMessage());
                }
            });
        }
    }
}
