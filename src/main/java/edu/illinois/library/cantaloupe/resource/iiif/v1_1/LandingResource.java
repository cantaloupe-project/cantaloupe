package edu.illinois.library.cantaloupe.resource.iiif.v1_1;

import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.restlet.data.MediaType;
import org.restlet.ext.velocity.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

import java.util.Map;

/**
 * Handles the IIIF Image API 1.1 landing page.
 */
public class LandingResource extends AbstractResource {

    @Get
    public Representation doGet() throws Exception {
        Template template = Velocity.getTemplate("iiif_1.1_landing.vm");
        Map<String, Object> vars = edu.illinois.library.cantaloupe.resource.
                LandingResource.getCommonTemplateVars();
        return new TemplateRepresentation(template, vars, MediaType.TEXT_HTML);
    }

}
