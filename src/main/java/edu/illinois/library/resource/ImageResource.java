package edu.illinois.library.resource;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * Created by alexd on 9/1/15.
 */
public class ImageResource extends ServerResource {

    @Get
    public String represent() {
        return "hello, world";
    }

}
