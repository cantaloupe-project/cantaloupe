package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.resource.VelocityRepresentation;

/**
 * Handles the IIIF Image API 2.x landing page.
 */
public class LandingResource extends IIIF2Resource {

    private static final Method[] SUPPORTED_METHODS =
            new Method[] { Method.GET, Method.OPTIONS };

    @Override
    public Method[] getSupportedMethods() {
        return SUPPORTED_METHODS;
    }

    @Override
    public void doGET() throws Exception {
        getResponse().setHeader("Content-Type", "text/html;charset=UTF-8");

        new VelocityRepresentation("/iiif_2_landing.vm", getCommonTemplateVars())
                .write(getResponse().getOutputStream());
    }

}
