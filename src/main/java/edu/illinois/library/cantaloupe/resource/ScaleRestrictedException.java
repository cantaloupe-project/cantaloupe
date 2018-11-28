package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.resource.ResourceException;

public class ScaleRestrictedException extends ResourceException {

    public ScaleRestrictedException() {
        super(Status.FORBIDDEN,
                "Requests for scales in excess of 100% are not allowed.");
    }

}
