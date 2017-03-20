package edu.illinois.library.cantaloupe.resource.iiif.v2;

import org.restlet.representation.Representation;
import org.restlet.resource.Get;

/**
 * Handles the IIIF Image API 2.0 landing page.
 */
public class LandingResource extends IIIF2Resource {

    @Get
    public Representation doGet() throws Exception {
        return template("/iiif_2_landing.vm");
    }

}
