package edu.illinois.library.cantaloupe.resource;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

public class AccessDeniedException extends ResourceException {

    public AccessDeniedException() {
        super(Status.CLIENT_ERROR_FORBIDDEN, "Access denied.");
    }

}
