package edu.illinois.library.cantaloupe.resource.iiif.v2;

import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.restlet.data.MediaType;
import org.restlet.ext.velocity.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

/**
 * Handles the IIIF Image API 2.0 landing page.
 */
public class LandingResource extends IIIF2Resource {

    @Get
    public Representation doGet() throws Exception {
        Template template = Velocity.getTemplate("iiif_2_landing.vm");
        return new TemplateRepresentation(template,
                getCommonTemplateVars(getRequest()), MediaType.TEXT_HTML);
    }

}
