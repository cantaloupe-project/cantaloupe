package edu.illinois.library.cantaloupe.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.operation.OperationList;

/**
 * Spring-managed factory for creating ImageRequestHandler instances
 * with proper dependency injection of Configuration.
 *
 * This factory enables dependency injection of Configuration into
 * ImageRequestHandler instead of using Configuration.getInstance().
 */
@Service
public class ImageRequestHandlerFactory {

    private final Configuration configuration;

    @Autowired
    public ImageRequestHandlerFactory(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Creates a new ImageRequestHandler with injected Configuration.
     *
     * @param operationList The operation list to process.
     * @param request       The IIIF request.
     * @param callback      Callback to receive events during request handling.
     * @return A new ImageRequestHandler instance with injected dependencies.
     */
    public ImageRequestHandler create(OperationList operationList,
                                    IIIFRequest request,
                                    ImageRequestHandler.Callback callback) {
        return new ImageRequestHandler(operationList, request, callback, configuration);
    }
}
