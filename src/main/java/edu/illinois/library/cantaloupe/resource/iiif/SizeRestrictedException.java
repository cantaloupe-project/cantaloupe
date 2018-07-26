package edu.illinois.library.cantaloupe.resource.iiif;

import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.resource.ResourceException;

public class SizeRestrictedException extends ResourceException {

    public SizeRestrictedException() {
        super(Status.FORBIDDEN,
                "Available sizes are limited to those in the info.json response.");
    }

}
