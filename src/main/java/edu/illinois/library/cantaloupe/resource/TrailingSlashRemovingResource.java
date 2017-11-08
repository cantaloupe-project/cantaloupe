package edu.illinois.library.cantaloupe.resource;

import org.apache.commons.lang3.StringUtils;
import org.restlet.data.Reference;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

/**
 * Redirects <code>/some/path/</code> to <code>/some/path</code>,
 * respecting the Servlet context root.
 */
public class TrailingSlashRemovingResource extends AbstractResource {

    @Get
    public Representation doGet() {
        Reference ref = getPublicReference();
        ref = new Reference(StringUtils.stripEnd(ref.toString(), "/"));
        redirectPermanent(ref);
        return new EmptyRepresentation();
    }

}
