package edu.illinois.library.cantaloupe.resource;

import java.util.Map;

import edu.illinois.library.cantaloupe.image.Parameters;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
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
        String format = (String) attrs.get("format");
        String region = (String) attrs.get("region");
        String size = (String) attrs.get("size");
        String rotation = (String) attrs.get("rotation");
        String quality = (String) attrs.get("quality");
        Parameters params = new Parameters(identifier, region, size, rotation,
                quality, format);

        Processor proc = ProcessorFactory.getProcessor();
        proc.setParameters(params);

        return new StringRepresentation("ImageResource");
    }

}
