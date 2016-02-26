package edu.illinois.library.cantaloupe.resource.iiif;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

public class SizeRestrictedException extends ResourceException {

    public SizeRestrictedException() {
        super(Status.CLIENT_ERROR_FORBIDDEN,
                "Available sizes are limited to those in the info.json response.");
    }

}
