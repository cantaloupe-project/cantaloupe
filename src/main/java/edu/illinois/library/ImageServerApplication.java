package edu.illinois.library;

import edu.illinois.library.resource.ImageResource;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

/**
 * Created by alexd on 9/1/15.
 */
public class ImageServerApplication extends Application {

    /**
     * Creates a root Restlet that will receive all incoming calls.
     */
    @Override
    public synchronized Restlet createInboundRoot() {
        Router router = new Router(getContext());
        router.attach("/test", ImageResource.class);
        return router;
    }

}