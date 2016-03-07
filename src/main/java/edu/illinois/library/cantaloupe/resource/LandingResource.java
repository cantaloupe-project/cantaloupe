package edu.illinois.library.cantaloupe.resource;

import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.restlet.data.CacheDirective;
import org.restlet.data.MediaType;
import org.restlet.ext.velocity.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 * Handles the landing page.
 */
public class LandingResource extends AbstractResource {

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        // add a "Cache-Control: no-cache" header because this page contains
        // live information pertaining to the function of the application
        getResponseCacheDirectives().add(CacheDirective.noCache());
    }

    @Get
    public Representation doGet() throws Exception {
        Template template = Velocity.getTemplate("landing.vm");
        return new TemplateRepresentation(template,
                getCommonTemplateVars(getRequest()),
                MediaType.TEXT_HTML);
    }

}
