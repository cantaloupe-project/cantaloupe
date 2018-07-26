package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.resource.VelocityRepresentation;

/**
 * Handles the IIIF Image API 1.x landing page.
 */
public class LandingResource extends IIIF1Resource {

    private static final Method[] SUPPORTED_METHODS =
            new Method[] { Method.GET, Method.OPTIONS };

    @Override
    public Method[] getSupportedMethods() {
        return SUPPORTED_METHODS;
    }

    @Override
    public void doGET() throws Exception {
        getResponse().setHeader("Content-Type", "text/html;charset=UTF-8");

        new VelocityRepresentation("/iiif_1_landing.vm", getCommonTemplateVars())
                .write(getResponse().getOutputStream());
    }

}
