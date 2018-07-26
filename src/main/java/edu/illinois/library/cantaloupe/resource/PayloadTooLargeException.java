package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.http.Status;

class PayloadTooLargeException extends ResourceException {

    PayloadTooLargeException() {
        super(Status.FORBIDDEN,
                "The requested image pixel area exceeds the maximum " +
                        "threshold set in the configuration.");
    }

}
