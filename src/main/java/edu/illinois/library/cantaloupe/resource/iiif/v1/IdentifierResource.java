package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.resource.Route;

/**
 * Redirects {@literal /:identifier} to {@literal /:identifier/info.json}.
 */
public class IdentifierResource extends IIIF1Resource {

    private static final Method[] SUPPORTED_METHODS =
            new Method[] { Method.GET, Method.OPTIONS };

    @Override
    public Method[] getSupportedMethods() {
        return SUPPORTED_METHODS;
    }

    @Override
    public void doGET() {
        final Reference newRef = new Reference(getPublicRootReference() +
                Route.IIIF_1_PATH + "/" + getPublicIdentifier() +
                "/info.json");
        getResponse().setStatus(Status.SEE_OTHER.getCode());
        getResponse().setHeader("Location", newRef.toString());
    }

}
