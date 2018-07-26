package edu.illinois.library.cantaloupe.resource.iiif;

import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.Route;

/**
 * Redirects /iiif to a URI for a more specific version of the Image API.
 */
public class RedirectingResource extends AbstractResource {

    private static final Method[] SUPPORTED_METHODS =
            new Method[] { Method.GET, Method.OPTIONS };

    @Override
    public Method[] getSupportedMethods() {
        return SUPPORTED_METHODS;
    }

    @Override
    public void doGET() {
        final Reference newRef = new Reference(
                getPublicRootReference() + Route.IIIF_2_PATH);
        getResponse().setStatus(Status.SEE_OTHER.getCode());
        getResponse().setHeader("Location", newRef.toString());
    }

}
