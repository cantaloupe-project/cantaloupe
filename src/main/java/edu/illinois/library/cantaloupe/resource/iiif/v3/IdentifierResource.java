package edu.illinois.library.cantaloupe.resource.iiif.v3;

import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.resource.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redirects {@literal /:identifier} to {@literal /:identifier/info.json}.
 */
public class IdentifierResource extends IIIF3Resource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(IdentifierResource.class);

    private static final Method[] SUPPORTED_METHODS =
            new Method[] { Method.GET, Method.OPTIONS };

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Method[] getSupportedMethods() {
        return SUPPORTED_METHODS;
    }

    @Override
    public void doGET() {
        final Reference newRef = new Reference(
                getPublicRootReference() +
                Route.IIIF_3_PATH +
                "/" + getPublicIdentifier() +
                "/info.json");
        getResponse().setStatus(Status.SEE_OTHER.getCode());
        getResponse().setHeader("Location", newRef.toString());
    }

}
