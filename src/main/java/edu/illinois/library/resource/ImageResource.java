package edu.illinois.library.resource;

import java.util.Map;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * Created by alexd on 9/1/15.
 */
public class ImageResource extends ServerResource {

    @Get
    public Representation doGet() {
        Map<String,Object> attrs = this.getRequest().getAttributes();
        String identifier = (String) attrs.get("identifier");
        String region = (String) attrs.get("region");
        String size = (String) attrs.get("size");
        String rotation = (String) attrs.get("rotation");
        String quality = (String) attrs.get("quality");

        return new StringRepresentation("ImageResource");
    }

    @Get("json")
    public Representation doJsonGet() {
        return new StringRepresentation("{ bla: 50 }");
    }

}
