package edu.illinois.library.cantaloupe.resource;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Parameters;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class ImageResource extends ServerResource {

    class ImageRepresentation extends OutputRepresentation {

        Parameters params;

        public ImageRepresentation(MediaType mediaType, Parameters params) {
            super(mediaType);
            this.params = params;
        }

        public void write(OutputStream outputStream) throws IOException {
            try {
                Processor proc = ProcessorFactory.getProcessor();
                proc.process(this.params, outputStream);
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

    }

    @Get
    public Representation doGet() throws UnsupportedEncodingException {
        Map<String,Object> attrs = this.getRequest().getAttributes();
        String identifier = java.net.URLDecoder.
                decode((String) attrs.get("identifier"), "UTF-8");
        String format = (String) attrs.get("format");
        String region = (String) attrs.get("region");
        String size = (String) attrs.get("size");
        String rotation = (String) attrs.get("rotation");
        String quality = (String) attrs.get("quality");
        Parameters params = new Parameters(identifier, region, size, rotation,
                quality, format);

        MediaType mediaType = new MediaType(
                Format.valueOf(format.toUpperCase()).getMediaType());
        return new ImageRepresentation(mediaType, params);
    }

}
