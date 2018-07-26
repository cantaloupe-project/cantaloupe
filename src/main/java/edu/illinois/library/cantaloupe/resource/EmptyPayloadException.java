package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.http.Status;

class EmptyPayloadException extends ResourceException {

    EmptyPayloadException() {
        super(Status.BAD_REQUEST, "The requested image has a zero-dimension.");
    }

}
