package edu.illinois.library.cantaloupe.resource.iiif.v1;

import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.restlet.data.MediaType;
import org.restlet.ext.velocity.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

/**
 * Handles the IIIF Image API 1.1 landing page.
 */
public class LandingResource extends Iiif1Resource {

    @Get
    public Representation doGet() throws Exception {
        Template template = Velocity.getTemplate("iiif_1_landing.vm");
        return new TemplateRepresentation(template,
                getCommonTemplateVars(getRequest()), MediaType.TEXT_HTML);
    }

}
