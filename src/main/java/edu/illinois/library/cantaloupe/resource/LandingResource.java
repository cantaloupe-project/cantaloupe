package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LandingResource extends AbstractResource {

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
