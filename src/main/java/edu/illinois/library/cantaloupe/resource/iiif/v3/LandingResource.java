package edu.illinois.library.cantaloupe.resource.iiif.v3;

import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.resource.VelocityRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Handles the IIIF Image API 3.x landing page.</p>
 *
 * <p>This is a convenience feature that is out of the Image API's scope.</p>
 */
public class LandingResource extends IIIF3Resource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(LandingResource.class);

    private static final Method[] SUPPORTED_METHODS =
            new Method[] { Method.GET, Method.OPTIONS };

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Method[] getSupportedMethods() {
        return SUPPORTED_METHODS;
    }

    @Override
    public void doGET() throws Exception {
        getResponse().setHeader("Content-Type", "text/html;charset=UTF-8");

        new VelocityRepresentation("/iiif_3_landing.vm", getCommonTemplateVars())
                .write(getResponse().getOutputStream());
    }

}
