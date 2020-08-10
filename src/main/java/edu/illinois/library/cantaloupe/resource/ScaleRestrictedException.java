package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.http.Status;

public class ScaleRestrictedException extends ResourceException {

    public ScaleRestrictedException(Status status, double maxScale) {
        super(status, String.format(
                "Requests for scales in excess of %d%% are not allowed.",
                Math.round(maxScale * 100)));
    }

}
