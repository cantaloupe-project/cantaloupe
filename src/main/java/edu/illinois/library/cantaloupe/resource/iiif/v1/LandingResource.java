package edu.illinois.library.cantaloupe.resource.iiif.v1;

import org.restlet.representation.Representation;
import org.restlet.resource.Get;

/**
 * Handles the IIIF Image API 1.1 landing page.
 */
public class LandingResource extends IIIF1Resource {

    @Get
    public Representation doGet() throws Exception {
        return template("/iiif_1_landing.vm");
    }

}
