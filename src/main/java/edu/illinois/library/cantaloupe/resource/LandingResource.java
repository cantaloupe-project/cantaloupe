package edu.illinois.library.cantaloupe.resource;

import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

/**
 * Handles the landing page.
 */
public class LandingResource extends AbstractResource {

    @Get
    public Representation doGet() {
        return new StringRepresentation("🍈 Cantaloupe IIIF 2.0 Server");
    }

}
