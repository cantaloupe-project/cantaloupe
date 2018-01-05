package edu.illinois.library.cantaloupe.resource;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

class PayloadTooLargeException extends ResourceException {

    PayloadTooLargeException() {
        super(Status.CLIENT_ERROR_FORBIDDEN,
                "The requested image pixel area exceeds the maximum " +
                        "threshold set in the configuration.");
    }

}
