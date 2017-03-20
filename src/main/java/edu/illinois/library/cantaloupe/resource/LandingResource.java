package edu.illinois.library.cantaloupe.resource;

import org.restlet.data.CacheDirective;
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
        // Add a Cache-Control header to tell clients this page never expires.
        final List<CacheDirective> directives = new ArrayList<>();
        directives.add(CacheDirective.maxAge(Integer.MAX_VALUE));
        directives.add(CacheDirective.publicInfo());
        getResponseCacheDirectives().addAll(directives);
    }

    @Get
    public Representation doGet() throws Exception {
        return template("/landing.vm");
    }

}
