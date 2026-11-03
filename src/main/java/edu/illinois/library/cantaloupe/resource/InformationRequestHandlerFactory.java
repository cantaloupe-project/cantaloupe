package edu.illinois.library.cantaloupe.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.illinois.library.cantaloupe.config.Configuration;

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
}
