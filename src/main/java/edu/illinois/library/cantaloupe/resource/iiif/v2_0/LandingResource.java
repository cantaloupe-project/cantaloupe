package edu.illinois.library.cantaloupe.resource.iiif.v2_0;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.velocity.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import java.util.Map;

/**
 * Handles the IIIF Image API 2.0 landing page.
 */
public class LandingResource extends AbstractResource {

    @Override
    protected void doInit() throws ResourceException {
        if (!Application.getConfiguration().
                getBoolean("endpoint.iiif.2.0.enabled", true)) {
            throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
                    "The IIIF Image API 2.0 endpoint is disabled.");
        }
        super.doInit();
    }

    @Get
    public Representation doGet() throws Exception {
        Template template = Velocity.getTemplate("iiif_2.0_landing.vm");
        Map<String, Object> vars = edu.illinois.library.cantaloupe.resource.
                LandingResource.getCommonTemplateVars();
        return new TemplateRepresentation(template, vars, MediaType.TEXT_HTML);
    }

}
