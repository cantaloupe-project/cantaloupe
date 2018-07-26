package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.http.Status;

public class EndpointDisabledException extends ResourceException {

    public EndpointDisabledException() {
        super(Status.FORBIDDEN, "This endpoint is disabled.");
    }

}
