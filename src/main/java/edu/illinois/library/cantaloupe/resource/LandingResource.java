package edu.illinois.library.cantaloupe.resource;

import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.restlet.data.CacheDirective;
import org.restlet.data.MediaType;
import org.restlet.ext.velocity.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the landing page.
 */
public class LandingResource extends AbstractResource {

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        // Add a Cache-Control header that tells clients this page never
        // expires.
        final List<CacheDirective> directives = new ArrayList<>();
        directives.add(CacheDirective.maxAge(Integer.MAX_VALUE));
        directives.add(CacheDirective.publicInfo());
        getResponseCacheDirectives().addAll(directives);
    }

    @Get
    public Representation doGet() throws Exception {
        Template template = Velocity.getTemplate("landing.vm");
        return new TemplateRepresentation(template,
                getCommonTemplateVars(getRequest()),
                MediaType.TEXT_HTML);
    }

}
