package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.http.Method;

public class LandingResource extends AbstractResource {

    private static final Method[] SUPPORTED_METHODS =
            new Method[] { Method.GET, Method.OPTIONS };

    @Override
    public Method[] getSupportedMethods() {
        return SUPPORTED_METHODS;
    }

    @Override
    public void doGET() throws Exception {
        addHeaders();
        new VelocityRepresentation("/landing.vm", getCommonTemplateVars())
                .write(getResponse().getOutputStream());
    }

    private void addHeaders() {
        getResponse().setHeader("Content-Type", "text/html;charset=UTF-8");
        getResponse().setHeader("Cache-Control",
                "public, max-age=" + Integer.MAX_VALUE);
    }

}
