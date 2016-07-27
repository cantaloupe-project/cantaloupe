package edu.illinois.library.cantaloupe.resource;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

public class EmptyPayloadException extends ResourceException {

    public EmptyPayloadException() {
        super(Status.CLIENT_ERROR_BAD_REQUEST,
                "The requested image has a zero-dimension.");
    }

}
