package edu.illinois.library.cantaloupe.resource.iiif;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import org.restlet.data.Reference;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

/**
 * Redirects /iiif to a URI for a more specific version of the Image API.
 */
public class RedirectingResource extends AbstractResource {

    @Get
    public Representation doGet() throws Exception {
        final Reference newRef = new Reference(
                getPublicRootReference() + RestletApplication.IIIF_2_PATH);
        redirectSeeOther(newRef);
        return new EmptyRepresentation();
    }

}
