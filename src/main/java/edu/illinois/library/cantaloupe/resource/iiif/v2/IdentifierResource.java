package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.resource.Controller;
import edu.illinois.library.cantaloupe.resource.LegacyRoute;
import edu.illinois.library.cantaloupe.resource.Request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redirects {@literal /:identifier} to {@literal /:identifier/info.json}.
 */
public class IdentifierResource extends Controller {
    @Override
    public void doGet(Request request) {
        final Reference newRef = new Reference(
                request.getPublicRootReference() +
                LegacyRoute.IIIF_2_PATH +
                "/" + getPublicIdentifier() +
                "/info.json");
        response.setStatus(Status.SEE_OTHER.getCode());
        response.setHeader("Location", newRef.toString(false));
    }

    @Override
    public boolean enabled() {
      return Configuration.getInstance().getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, true);
    }

}
