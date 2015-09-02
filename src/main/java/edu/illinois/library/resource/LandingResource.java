package edu.illinois.library.resource;

import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * Handles the landing page.
 */
public class LandingResource extends ServerResource {

    @Get
    public Representation doGet() {
        return new StringRepresentation("Cantaloupe IIIF 2.0 Server");
    }

}
