package edu.illinois.library.cantaloupe.resource;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

public class EndpointDisabledException extends ResourceException {

    public EndpointDisabledException() {
        super(Status.CLIENT_ERROR_FORBIDDEN, "This endpoint is disabled.");
    }

}
