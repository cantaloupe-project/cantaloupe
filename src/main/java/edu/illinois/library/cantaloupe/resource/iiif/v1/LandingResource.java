package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.restlet.data.MediaType;
import org.restlet.ext.velocity.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 * Handles the IIIF Image API 1.1 landing page.
 */
public class LandingResource extends AbstractResource {

    @Override
    protected void doInit() throws ResourceException {
        if (!Application.getConfiguration().
                getBoolean("endpoint.iiif.1.enabled", true)) {
            throw new EndpointDisabledException();
        }
        super.doInit();
    }

    @Get
    public Representation doGet() throws Exception {
        Template template = Velocity.getTemplate("iiif_1_landing.vm");
        return new TemplateRepresentation(template,
                getCommonTemplateVars(getRequest()), MediaType.TEXT_HTML);
    }

}
