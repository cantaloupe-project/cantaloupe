package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.config.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Spring-managed factory for creating InformationRequestHandler instances
 * with proper dependency injection of Configuration.
 *
 * This factory enables dependency injection of Configuration into
 * InformationRequestHandler instead of using Configuration.getInstance().
 */
@Service
public class InformationRequestHandlerFactory {

    private final Configuration configuration;

    @Autowired
    public InformationRequestHandlerFactory(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Creates a new InformationRequestHandler with injected Configuration.
     *
     * @param request  The IIIF request.
     * @param callback Callback to receive events during request handling.
     * @return A new InformationRequestHandler instance with injected dependencies.
     */
    public InformationRequestHandler create(IIIFRequest request,
                                          InformationRequestHandler.Callback callback) {
        return new InformationRequestHandler(request, callback, configuration);
    }

    /**
     * Creates a new InformationRequestHandler with injected Configuration and no-op callback.
     *
     * @param request The IIIF request.
     * @return A new InformationRequestHandler instance with injected dependencies.
     */
    public InformationRequestHandler create(IIIFRequest request) {
        return new InformationRequestHandler(request, new NoOpCallback(), configuration);
    }

    /**
     * Default no-op callback implementation for convenience.
     */
    private static class NoOpCallback implements InformationRequestHandler.Callback {
        @Override
        public boolean authorize() {
            return true;
        }

        @Override
        public void sourceAccessed(edu.illinois.library.cantaloupe.source.StatResult result) {
            // No-op
        }

        @Override
        public void knowAvailableOutputFormats(java.util.Set<edu.illinois.library.cantaloupe.image.Format> availableOutputFormats) {
            // No-op
        }
    }
}
